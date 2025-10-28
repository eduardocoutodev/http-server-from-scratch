package response

import CRLF
import HTTP_VERSION
import domain.HTTPResponse
import domain.HTTPRequest
import request.shouldCloseSocketConnection
import java.io.IOException
import java.net.SocketException

import java.net.Socket

import java.nio.charset.StandardCharsets
import kotlin.collections.forEach
import kotlin.let
import kotlin.text.toByteArray

fun respondHttpRequest(
    clientSocket: Socket,
    request: HTTPRequest,
    response: HTTPResponse,
) {
    if (clientSocket.isClosed) {
        println("Not responding to closed socket connection")
        return
    }

    try {
        val outputStream = clientSocket.outputStream
        val (bodyBytes, headers) = prepareResponse(request, response)

        val statusLine = "$HTTP_VERSION ${response.status.code}$CRLF"
        outputStream.write(statusLine.toByteArray(StandardCharsets.UTF_8))

        headers.forEach { (name, value) ->
            val headerLine = "$name: $value$CRLF"
            outputStream.write(headerLine.toByteArray(StandardCharsets.UTF_8))
        }

        outputStream.write(CRLF.toByteArray(StandardCharsets.UTF_8))

        bodyBytes?.let {
            outputStream.write(it)
        }

        outputStream.flush()

        if (shouldCloseSocketConnection(request = request)) {
            clientSocket.close()
        }
    } catch (e: SocketException) {
        println("Client disconnected: ${e.message}")
    } catch (e: IOException) {
        println("IO error writing response: ${e.message}")
    }
}

private fun prepareResponse(
    request: HTTPRequest,
    response: HTTPResponse
): Pair<ByteArray?, Map<String, String>> {
    val originalBody = response.body?.toByteArray(StandardCharsets.UTF_8)

    val compressionResult = compressBodyIfNeeded(
        request = request,
        body = originalBody
    )

    val headers = buildResponseHeaders(
        request = request,
        response = response,
        bodyLength = compressionResult?.body?.size ?: 0,
        additionalHeaders = compressionResult?.headers
    )

    return compressionResult?.body to headers
}

@OptIn(ExperimentalStdlibApi::class)
fun buildResponseHeaders(
    request: HTTPRequest,
    response: HTTPResponse,
    bodyLength: Int,
    additionalHeaders: Map<String, String>?
): Map<String, String> {
    return buildMap {
        response.contentType?.let {
            put("Content-Type", it)
        }

        put("Content-Length", bodyLength.toString())

        response.headers.let {
            putAll(it)
        }

        if (shouldCloseSocketConnection(request)) {
            put("Connection", "close")
        }

        additionalHeaders?.let {
            putAll(it)
        }
    }
}
