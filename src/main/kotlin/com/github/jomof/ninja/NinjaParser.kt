package com.github.jomof.ninja

import com.github.jomof.ninja.FileState.*
import com.github.jomof.ninja.State.*
import java.io.File
import java.io.FileReader
import java.io.Reader

private enum class State {
    START,
    EXPECT_EOL,
    START_ASSIGNMENT_STATE,
    AFTER_EQUALS_ASSIGNMENT_STATE,
    START_INCLUDE_STATE,
    START_SUBNINJA_STATE,
    BUILD_EXPECT_OUTPUT_OR_COLON,
    BUILD_EXPECT_RULE,
    BUILD_AFTER_RULE,
    BUILD_EXPECT_PROPERTIES,
    BUILD_EXPECT_PROPERTY_IDENTIFIER,
    BUILD_EXPECT_PROPERTY_EQUALS,
    BUILD_EXPECT_PROPERTY_VALUE,
    BUILD_EXPECT_PROPERTIES_EOL,
    DEFAULT_EXPECT_BUILD_REF,
    RULE_EXPECT_RULE_NAME,
    RULE_EXPECT_EOL_AFTER_RULE_NAME,
    RULE_EXPECT_PROPERTIES,
    RULE_EXPECT_PROPERTY_IDENTIFIER,
    RULE_EXPECT_PROPERTY_EQUALS,
    RULE_EXPECT_PROPERTY_VALUE,
    RULE_EXPECT_PROPERTIES_EOL,
    POOL_EXPECT_POOL_NAME,
    POOL_EXPECT_EOL_AFTER_POOL_NAME,
    POOL_EXPECT_PROPERTIES,
    POOL_EXPECT_PROPERTY_IDENTIFIER,
    POOL_EXPECT_PROPERTY_EQUALS,
    POOL_EXPECT_PROPERTY_VALUE,
    POOL_EXPECT_PROPERTIES_EOL
}

enum class FileState {
    EXPLICIT,
    IMPLICIT,
    ORDER_ONLY
}


typealias BuildRefMap = MutableMap<FileState, MutableList<BuildRef>>

fun parseNinja(file : File) : NinjaFileDef {
    return parseNinja(file.parentFile.absolutePath, FileReader(file))
}

@Suppress("UNCHECKED_CAST")
fun parseNinja(folder : String, reader : Reader) : NinjaFileDef {
    val tops = mutableListOf<Node>()

    var state = START
    var fileState = EXPLICIT
    val stack : MutableList<Any> = mutableListOf()
    fun error(token : String) =
            RuntimeException("$state $token $stack")
    fun push(node : Any) {
        stack.add(0, node)
    }
    fun pop() : Any {
        val first = stack[0]
        stack.removeAt(0)
        return first
    }
    fun fileMapOfBuildRef() : BuildRefMap {
        val result = mutableMapOf<FileState, MutableList<BuildRef>>()
        result[EXPLICIT] = mutableListOf()
        result[IMPLICIT] = mutableListOf()
        result[ORDER_ONLY] = mutableListOf()
        return result
    }
    reader.forEachNinjaToken { token ->
        var done: Boolean
        do {
            done = true
            when (token) {
                "|" -> fileState = IMPLICIT
                "||" -> fileState = ORDER_ONLY
                else -> when (state) {
                    START -> {
                        stack.clear()
                        fileState = EXPLICIT
                        when (token) {
                            "include" -> {
                                state = START_INCLUDE_STATE
                            }
                            "subninja" -> {
                                state = START_SUBNINJA_STATE
                            }
                            "build" -> {
                                push(fileMapOfBuildRef())
                                state = BUILD_EXPECT_OUTPUT_OR_COLON
                            }
                            "rule" -> {
                                state = RULE_EXPECT_RULE_NAME
                            }
                            "pool" -> {
                                state = POOL_EXPECT_POOL_NAME
                            }
                            "default" -> {
                                push(mutableListOf<BuildRef>())
                                state = DEFAULT_EXPECT_BUILD_REF
                            }
                            END_OF_FILE_TOKEN, END_OF_LINE_TOKEN -> {
                            }
                            else -> {
                                push(IdentifierRef(token))
                                state = START_ASSIGNMENT_STATE
                            }
                        }
                    }
                    START_ASSIGNMENT_STATE -> when (token) {
                        "=" -> state = AFTER_EQUALS_ASSIGNMENT_STATE
                        else ->
                            throw error(token)
                    }
                    AFTER_EQUALS_ASSIGNMENT_STATE -> {
                        val identifier = pop() as IdentifierRef
                        tops += Assignment(identifier, UninstantiatedLiteral(token))
                        state = EXPECT_EOL
                    }
                    START_INCLUDE_STATE -> {
                        tops += Include(NinjaFileRef(token))
                        state = EXPECT_EOL
                    }
                    START_SUBNINJA_STATE -> {
                        val ref = NinjaFileRef(token)
                        tops += SubNinja(ref, ref)
                        state = EXPECT_EOL
                    }
                    EXPECT_EOL -> {
                        if (token != END_OF_LINE_TOKEN) {
                            error(token)
                        }
                        state = START
                    }
                    BUILD_EXPECT_OUTPUT_OR_COLON -> {
                        when (token) {
                            ":" -> {
                                state = BUILD_EXPECT_RULE
                                fileState = EXPLICIT
                            }
                            else -> {
                                val outputs = pop() as BuildRefMap
                                outputs[fileState]!! += BuildRef(token)
                                push(outputs)
                                state = BUILD_EXPECT_OUTPUT_OR_COLON
                            }
                        }
                    }
                    BUILD_EXPECT_RULE -> {
                        push(RuleRef(token))
                        push(fileMapOfBuildRef())
                        state = BUILD_AFTER_RULE
                    }
                    BUILD_AFTER_RULE -> {
                        state = when (token) {
                            END_OF_LINE_TOKEN -> {
                                push(mutableListOf<Assignment>())
                                BUILD_EXPECT_PROPERTIES
                            }
                            else -> {
                                // An input
                                val outputs = pop() as BuildRefMap
                                outputs[fileState]!!.add(BuildRef(token))
                                push(outputs)
                                BUILD_AFTER_RULE
                            }
                        }
                    }
                    BUILD_EXPECT_PROPERTIES -> when (token) {
                        INDENT_TOKEN -> {
                            state = BUILD_EXPECT_PROPERTY_IDENTIFIER
                        }
                        else -> {
                            val properties = pop() as List<Assignment>
                            val inputs = pop() as BuildRefMap
                            val rule = pop() as RuleRef
                            val outputs = pop() as BuildRefMap
                            tops += BuildDef(
                                    explicitOutputs = outputs[EXPLICIT]!!,
                                    implicitOutputs = outputs[IMPLICIT]!!,
                                    rule = rule,
                                    explicitInputs = inputs[EXPLICIT]!!,
                                    implicitInputs = inputs[IMPLICIT]!!,
                                    orderOnlyInputs = inputs[ORDER_ONLY]!!,
                                    properties = properties)
                            state = START
                            done = false
                        }
                    }
                    RULE_EXPECT_PROPERTIES -> when (token) {
                        INDENT_TOKEN -> {
                            state = RULE_EXPECT_PROPERTY_IDENTIFIER
                        }
                        else -> {
                            val properties = pop() as List<Assignment>
                            val rule = pop() as RuleRef
                            tops += RuleDef(rule, properties)
                            state = START
                            done = false
                        }
                    }
                    POOL_EXPECT_PROPERTIES -> when (token) {
                        INDENT_TOKEN -> {
                            state = POOL_EXPECT_PROPERTY_IDENTIFIER
                        }
                        else -> {
                            val properties = pop() as List<Assignment>
                            val pool = pop() as PoolRef
                            tops += PoolDef(pool, properties)
                            state = START
                            done = false
                        }
                    }
                    BUILD_EXPECT_PROPERTY_IDENTIFIER -> {
                        push(IdentifierRef(token))
                        state = BUILD_EXPECT_PROPERTY_EQUALS
                    }
                    RULE_EXPECT_PROPERTY_IDENTIFIER -> {
                        push(IdentifierRef(token))
                        state = RULE_EXPECT_PROPERTY_EQUALS
                    }
                    POOL_EXPECT_PROPERTY_IDENTIFIER -> {
                        push(IdentifierRef(token))
                        state = POOL_EXPECT_PROPERTY_EQUALS
                    }
                    BUILD_EXPECT_PROPERTY_EQUALS -> {
                        if (token != "=") throw error(token)
                        state = BUILD_EXPECT_PROPERTY_VALUE
                    }
                    RULE_EXPECT_PROPERTY_EQUALS -> {
                        if (token != "=") throw error(token)
                        state = RULE_EXPECT_PROPERTY_VALUE
                    }
                    POOL_EXPECT_PROPERTY_EQUALS -> {
                        if (token != "=") throw error(token)
                        state = POOL_EXPECT_PROPERTY_VALUE
                    }
                    BUILD_EXPECT_PROPERTY_VALUE -> {
                        val identifier = pop() as IdentifierRef
                        val properties = pop() as MutableList<Assignment>
                        properties += Assignment(identifier, UninstantiatedLiteral(token))
                        push(properties)
                        state = BUILD_EXPECT_PROPERTIES_EOL
                    }
                    RULE_EXPECT_PROPERTY_VALUE -> {
                        val identifier = pop() as IdentifierRef
                        val properties = pop() as MutableList<Assignment>
                        properties += Assignment(identifier, UninstantiatedLiteral(token))
                        push(properties)
                        state = RULE_EXPECT_PROPERTIES_EOL
                    }
                    POOL_EXPECT_PROPERTY_VALUE -> {
                        val identifier = pop() as IdentifierRef
                        val properties = pop() as MutableList<Assignment>
                        properties += Assignment(identifier, UninstantiatedLiteral(token))
                        push(properties)
                        state = POOL_EXPECT_PROPERTIES_EOL
                    }
                    BUILD_EXPECT_PROPERTIES_EOL -> {
                        when (token) {
                            END_OF_LINE_TOKEN, END_OF_FILE_TOKEN -> {
                                state = BUILD_EXPECT_PROPERTIES
                            }
                            else -> throw error(token)
                        }
                    }
                    RULE_EXPECT_PROPERTIES_EOL -> {
                        when (token) {
                            END_OF_LINE_TOKEN, END_OF_FILE_TOKEN -> {
                                state = RULE_EXPECT_PROPERTIES
                            }
                            else -> throw error(token)
                        }
                    }
                    POOL_EXPECT_PROPERTIES_EOL -> {
                        when (token) {
                            END_OF_LINE_TOKEN, END_OF_FILE_TOKEN -> {
                                state = POOL_EXPECT_PROPERTIES
                            }
                            else -> throw error(token)
                        }
                    }
                    DEFAULT_EXPECT_BUILD_REF -> {
                        when (token) {
                            END_OF_LINE_TOKEN, END_OF_FILE_TOKEN -> {
                                val targets = pop() as MutableList<BuildRef>
                                tops += Default(targets)
                                state = START
                            }
                            else -> {
                                val targets = pop() as MutableList<BuildRef>
                                targets.add(BuildRef(token))
                                push(targets)
                            }
                        }
                    }
                    RULE_EXPECT_RULE_NAME -> {
                        push(RuleRef(token))
                        state = RULE_EXPECT_EOL_AFTER_RULE_NAME
                    }
                    POOL_EXPECT_POOL_NAME -> {
                        push(PoolRef(token))
                        state = POOL_EXPECT_EOL_AFTER_POOL_NAME
                    }
                    RULE_EXPECT_EOL_AFTER_RULE_NAME -> {
                        push(mutableListOf<Assignment>())
                        state = RULE_EXPECT_PROPERTIES
                    }
                    POOL_EXPECT_EOL_AFTER_POOL_NAME -> {
                        push(mutableListOf<Assignment>())
                        state = POOL_EXPECT_PROPERTIES
                    }
                }
            }
        } while(!done)
    }
    if (!stack.isEmpty()) {
        throw RuntimeException(stack.toString())
    }
    return NinjaFileDef(folder, tops)
}