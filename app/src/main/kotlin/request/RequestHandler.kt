package request

import domain.HTTPRequest
import domain.HTTPResponse
import domain.HTTPStatus
import response.respondHttpRequest
import route.ROUTE_HANDLERS
import java.io.IOException
import java.net.Socket
import java.net.SocketTimeoutException
import kotlin.io.bufferedReader

suspend fun handleNewSocketConnection(clientSocket: Socket) {
    clientSocket.use { socket ->
        socket.soTimeout = 5000
        try {
            println("Accepted new connection")

            var shouldKeepSocketAlive = true
            while (shouldKeepSocketAlive && socket.isConnected && !socket.isClosed) {
                try {
                    shouldKeepSocketAlive = handleIncomingRequestAndReturnIfSocketShouldBeAlive(clientSocket = socket)
                } catch (exception: SocketTimeoutException) {
                    println("Socket timeout, closing connection, exception : ${exception.message}")
                    break
                } catch (e: IOException) {
                    println("IO Error: ${e.message}")
                    break
                }
            }

            println("Closed connection")
        } catch (e: HTTPParseException) {
            println("Bad request: ${e.message}")
            try {
                respondHttpRequest(
                    clientSocket = socket,
                    request = HTTPRequest(),
                    response = HTTPResponse(
                        status = HTTPStatus.BAD_REQUEST(),
                    )
                )
            } catch (ignored: IOException) {
                // Client already disconnected
            }
        } catch (e: Exception) {
            println("Error: ${e.message}")
        }
    }
}

fun handleIncomingRequestAndReturnIfSocketShouldBeAlive(clientSocket: Socket): Boolean {
    val readerStream = clientSocket.inputStream

    val rawRequest = readerStream
        .bufferedReader()

    val requestParsed: HTTPRequest = parseHTTPRequestInvocation(rawRequest)
    val httpResponseCallBack = ROUTE_HANDLERS[requestParsed.route]

    val shouldCloseSocket = shouldCloseSocketConnection(request = requestParsed)

    if (httpResponseCallBack == null) {
        println("Didn't found route")

        respondHttpRequest(
            clientSocket = clientSocket,
            request = requestParsed,
            response = HTTPResponse(status = HTTPStatus.NOT_FOUND())
        )

        if(shouldCloseSocket){
            clientSocket.close()
        }

        return !shouldCloseSocket
    }

    val httpResponse = httpResponseCallBack.invoke(requestParsed)

    respondHttpRequest(
        clientSocket = clientSocket,
        request = requestParsed,
        response = httpResponse
    )

    println("Responded to client")
    return !shouldCloseSocket
}

fun shouldCloseSocketConnection(request: HTTPRequest): Boolean {
    return request.headers["connection"]?.equals("close", ignoreCase = true) == true
}