import ServerContext.registerServerContext
import request.handleNewSocketConnection
import java.net.ServerSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

suspend fun main(args: Array<String>) {
    println("Starting HTTP server")

    registerServerContext(args)

    val serverSocket = ServerSocket(HTTP_SERVER_PORT)
    serverSocket.reuseAddress = true

    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    Runtime.getRuntime().addShutdownHook(Thread {
        println("Shutting down server")
        serverSocket.close()
        runBlocking {
            scope.cancel()
            scope.coroutineContext[Job]?.join()
        }
    })

    try {
        while (!serverSocket.isClosed) {
            try {
                val clientSocket = serverSocket.accept()
                println("Accepted connection from ${clientSocket.inetAddress.hostAddress}")

                scope.launch {
                    handleNewSocketConnection(clientSocket)
                }
            } catch (e: Exception) {
                if (!serverSocket.isClosed) {
                    println("Error accepting connection: ${e.message}")
                }
            }
        }
    } finally {
        serverSocket.close()
        scope.cancel()
    }
}

