package com.github.jomof

import org.junit.Test
import org.picocontainer.DefaultPicoContainer
import org.picocontainer.PicoContainer
import org.picocontainer.behaviors.Caching

class PicoContainTest {

    interface Fish
    class WatchDirectory(val directory : String, pico : PicoContainer)
    class Cod(w : WatchDirectory) : Fish {
        init {
            println("In Cod")
        }
    }
    class Shark(w : WatchDirectory) : Fish {
        init {
            println("In Shark")
        }
    }
    data class Bowl(val fishes: Array<Fish>, val abc : String)

    @Test
    fun fishBowl() {
        val server = DefaultPicoContainer(Caching())
        val dir = server.makeChildContainer()
        dir.addComponent(dir)
        dir.addComponent(Shark::class.java)
        dir.addComponent(Cod::class.java)
        dir.addComponent(Bowl::class.java)
        dir.addComponent(WatchDirectory("xyz", dir))
        dir.addComponent("abc", "xyz")
        val bowl = dir.getComponent(Bowl::class.java)

        println(bowl)
        println(dir.getComponent("abc"))


    }
}