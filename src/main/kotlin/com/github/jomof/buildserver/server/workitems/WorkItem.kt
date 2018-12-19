package com.github.jomof.buildserver.server.workitems

import org.picocontainer.PicoContainer
import java.net.Socket

open class WorkItem(val socket : Socket, pico : PicoContainer)