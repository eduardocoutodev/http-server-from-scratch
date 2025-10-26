package request

import domain.HTTPRequest
import domain.HttpMethod
import domain.Route
import domain.toHttpMethod
import route.extractRouteArgs
import route.findIncomingRoute
import java.io.BufferedReader
import java.io.IOException
import kotlin.collections.set
import kotlin.text.indexOf
import kotlin.text.isBlank
import kotlin.text.isEmpty
import kotlin.text.lowercase
import kotlin.text.split
import kotlin.text.startsWith
import kotlin.text.substring
import kotlin.text.trim

fun parseHTTPRequestInvocation(requestBufferedReader: BufferedReader): HTTPRequest {
    try {
        val requestLine = requestBufferedReader.readLine()
            ?: throw HTTPParseException("Empty request")

        val (method, target) = parseRequestLine(requestLine)
        val incomingRoute = Route(
            method = method,
            path = target
        )
        val matchedRoute = findIncomingRoute(incomingRoute)

        val headers = parseHeaders(requestBufferedReader)
        val routeArgs = extractRouteArgs(
            incomingRoute = incomingRoute,
            route = matchedRoute
        )

        // Should parse body in case it exists
        val requestBody = headers["content-length"]?.let { bodyLength ->
            parseRequestBody(
                reader = requestBufferedReader,
                bodyLength = bodyLength.toInt()
            )
        }

        return HTTPRequest(
            route = matchedRoute,
            headers = headers,
            routeArguments = routeArgs,
            body = requestBody
        )
    } catch (e: IOException) {
        throw HTTPParseException("Failed to read request", e)
    }
}

data class RequestLine(
    val method: HttpMethod,
    val target: String,
    val httpVersion: String
)

fun parseRequestLine(requestLine: String): RequestLine {
    val parts = requestLine.trim().split(" ")

    if (parts.size != 3) {
        throw HTTPParseException("Invalid request line: $requestLine")
    }

    val (method, target, httpVersion) = parts

    if (!httpVersion.startsWith("HTTP/")) {
        throw HTTPParseException("Invalid HTTP version: $httpVersion")
    }

    return RequestLine(method.toHttpMethod(), target, httpVersion)
}

@OptIn(ExperimentalStdlibApi::class)
fun parseHeaders(reader: BufferedReader): Map<String, String> {
    val headers = mutableMapOf<String, String>()

    while (true) {
        val line = reader.readLine() ?: break

        if (line.isEmpty() || line.isBlank()) {
            break
        }

        val colonIndex = line.indexOf(':')
        if (colonIndex == -1) {
            throw HTTPParseException("Invalid header format: $line")
        }

        val headerName = line.substring(0, colonIndex).trim().lowercase()
        val headerValue = line.substring(colonIndex + 1).trim()

        if (headerName.isEmpty()) {
            throw HTTPParseException("Empty header name: $line")
        }


        headers[headerName] = headerValue
    }

    return headers
}

fun parseRequestBody(reader: BufferedReader, bodyLength: Int): String {
    val bodyBuilder = StringBuilder()
    for (i in 0 until bodyLength) {
        val char = reader.read().toChar()
        bodyBuilder.append(char)
    }
    return bodyBuilder.toString()
}

class HTTPParseException(message: String, cause: Throwable? = null) : Exception(message, cause)