package com.github.jomof.ninja

import java.lang.RuntimeException

fun writeNinjaToString(ninja : Node) : String {
    val sb = StringBuilder()
    var indent = ""
    fun escape(value : String) : String {
        return value
                .replace("$", "$$")
                .replace(":", "$:")
                .replace(" ", "$ ")
    }
    fun write(node : Node) {
        when(node) {
            is NinjaFileDef -> with(node) {
                indent = ""
                tops.onEach { write(it) }
            }
            is Assignment -> with(node) {
                sb.append(indent)
                write(name)
                sb.append(" = ")
                write(value)
                sb.append("\n")
            }
            is IdentifierRef -> with(node) {
                sb.append(value)
            }
            is UninstantiatedLiteral -> with(node) {
                sb.append(value)
            }
            is RuleDef -> with(node) {
                sb.append("rule ")
                write(name)
                sb.append("\n")
                indent = "  "
                properties.onEach { write(it) }
                indent = ""
            }
            is PoolDef -> with(node) {
                sb.append("pool ")
                write(name)
                sb.append("\n")
                indent = "  "
                properties.onEach { write(it) }
                indent = ""
            }
            is RuleRef -> with(node) {
                sb.append(value)
            }
            is PoolRef -> with(node) {
                sb.append(value)
            }
            is BuildDef -> with(node) {
                sb.append("build ")
                explicitOutputs.onEach {
                    write(it)
                    sb.append(" ")
                }
                if (!implicitOutputs.isEmpty()) {
                    sb.append("| ")
                    implicitOutputs.onEach {
                        write(it)
                        sb.append(" ")
                    }
                }
                sb.append(": ")
                write(rule)
                sb.append(" ")
                explicitInputs.onEach {
                    write(it)
                    sb.append(" ")
                }
                if (!implicitInputs.isEmpty()) {
                    sb.append("| ")
                    implicitInputs.onEach {
                        write(it)
                        sb.append(" ")
                    }
                }
                if (!orderOnlyInputs.isEmpty()) {
                    sb.append("|| ")
                    orderOnlyInputs.onEach {
                        write(it)
                        sb.append(" ")
                    }
                }
                sb.append("\n")
                indent = "  "
                properties.onEach { write(it) }
                indent = ""
            }
            is BuildRef -> with(node) {
                sb.append(escape(value))
            }
            is SubNinja -> with(node) {
                sb.append("subninja ")
                write(node.original)
                sb.append("\n")
            }
            is Include -> with(node) {
                sb.append("include ")
                write(file)
                sb.append("\n")
            }
            is NinjaFileRef -> with(node) {
                sb.append(value)
            }
            is Default -> with(node) {
                sb.append("default ")
                file.onEach {
                    write(it)
                    sb.append(" ")
                }
                sb.append("\n")
            }
            is NinjaFileNotFound -> with(node) {
                sb.append(value)
            }
            else -> throw RuntimeException(node.toString())
        }

    }
    write(ninja)
    val result : String = sb.toString().replace(" \n", "\n")
    return result.trim('\n')
}

