package automate.profit.autocoin

import java.net.ServerSocket

object FreePortFinder {
    fun getFreePort(): Int {
        val socket = ServerSocket(0)
        socket.reuseAddress = true
        val port = socket.localPort
        socket.close()
        return port
    }
}