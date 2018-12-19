package com.github.jomof.buildserver.server.workitems

import org.picocontainer.PicoContainer
import java.net.Socket

class NewRequestWorkItem(
        socket : Socket,
        pico : PicoContainer) : WorkItem(socket, pico)