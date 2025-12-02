package response

import domain.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.net.Socket
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResponseHandlerTest {

    @Test
    fun `buildResponseHeaders should include Content-Type when present`() {
        val request = HTTPRequest(route = Route(HttpMethod.GET, "/"))
        val response = HTTPResponse(
            status = HTTPStatus.OK(),
            contentType = "text/plain"
        )

        val headers = buildResponseHeaders(
            request = request,
            response = response,
            bodyLength = 0,
            additionalHeaders = null
        )

        assertEquals("text/plain", headers["Content-Type"])
    }

    @Test
    fun `buildResponseHeaders should not include Content-Type when null`() {
        val request = HTTPRequest(route = Route(HttpMethod.GET, "/"))
        val response = HTTPResponse(status = HTTPStatus.OK())

        val headers = buildResponseHeaders(
            request = request,
            response = response,
            bodyLength = 0,
            additionalHeaders = null
        )

        assertFalse(headers.containsKey("Content-Type"))
    }

    @Test
    fun `buildResponseHeaders should always include Content-Length`() {
        val request = HTTPRequest(route = Route(HttpMethod.GET, "/"))
        val response = HTTPResponse(status = HTTPStatus.OK())

        val headers = buildResponseHeaders(
            request = request,
            response = response,
            bodyLength = 42,
            additionalHeaders = null
        )

        assertEquals("42", headers["Content-Length"])
    }

    @Test
    fun `buildResponseHeaders should include Content-Length 0 for empty body`() {
        val request = HTTPRequest(route = Route(HttpMethod.GET, "/"))
        val response = HTTPResponse(status = HTTPStatus.OK())

        val headers = buildResponseHeaders(
            request = request,
            response = response,
            bodyLength = 0,
            additionalHeaders = null
        )

        assertEquals("0", headers["Content-Length"])
    }

    @Test
    fun `buildResponseHeaders should include response headers`() {
        val request = HTTPRequest(route = Route(HttpMethod.GET, "/"))
        val responseHeaders = mutableMapOf(
            "X-Custom-Header" to "custom-value",
            "Cache-Control" to "no-cache"
        )
        val response = HTTPResponse(
            status = HTTPStatus.OK(),
            headers = responseHeaders
        )

        val headers = buildResponseHeaders(
            request = request,
            response = response,
            bodyLength = 0,
            additionalHeaders = null
        )

        assertEquals("custom-value", headers["X-Custom-Header"])
        assertEquals("no-cache", headers["Cache-Control"])
    }

    @Test
    fun `buildResponseHeaders should include Connection close when request has connection close`() {
        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/"),
            headers = mapOf("connection" to "close")
        )
        val response = HTTPResponse(status = HTTPStatus.OK())

        val headers = buildResponseHeaders(
            request = request,
            response = response,
            bodyLength = 0,
            additionalHeaders = null
        )

        assertEquals("close", headers["Connection"])
    }

    @Test
    fun `buildResponseHeaders should not include Connection close when request keeps alive`() {
        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/"),
            headers = mapOf("connection" to "keep-alive")
        )
        val response = HTTPResponse(status = HTTPStatus.OK())

        val headers = buildResponseHeaders(
            request = request,
            response = response,
            bodyLength = 0,
            additionalHeaders = null
        )

        assertFalse(headers.containsKey("Connection"))
    }

    @Test
    fun `buildResponseHeaders should include additional headers`() {
        val request = HTTPRequest(route = Route(HttpMethod.GET, "/"))
        val response = HTTPResponse(status = HTTPStatus.OK())
        val additionalHeaders = mapOf("Content-Encoding" to "gzip")

        val headers = buildResponseHeaders(
            request = request,
            response = response,
            bodyLength = 100,
            additionalHeaders = additionalHeaders
        )

        assertEquals("gzip", headers["Content-Encoding"])
    }

    @Test
    fun `buildResponseHeaders should merge all header sources correctly`() {
        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/"),
            headers = mapOf("connection" to "close")
        )
        val responseHeaders = mutableMapOf("X-Response-Header" to "response-value")
        val response = HTTPResponse(
            status = HTTPStatus.OK(),
            contentType = "application/json",
            headers = responseHeaders
        )
        val additionalHeaders = mapOf("Content-Encoding" to "gzip")

        val headers = buildResponseHeaders(
            request = request,
            response = response,
            bodyLength = 123,
            additionalHeaders = additionalHeaders
        )

        assertEquals("application/json", headers["Content-Type"])
        assertEquals("123", headers["Content-Length"])
        assertEquals("response-value", headers["X-Response-Header"])
        assertEquals("close", headers["Connection"])
        assertEquals("gzip", headers["Content-Encoding"])
    }

    @Test
    fun `buildResponseHeaders should handle null additional headers`() {
        val request = HTTPRequest(route = Route(HttpMethod.GET, "/"))
        val response = HTTPResponse(status = HTTPStatus.OK())

        val headers = buildResponseHeaders(
            request = request,
            response = response,
            bodyLength = 0,
            additionalHeaders = null
        )

        assertEquals("0", headers["Content-Length"])
    }

    @Test
    fun `buildResponseHeaders should handle all common content types`() {
        val testCases = listOf(
            "text/plain",
            "text/html",
            "application/json",
            "application/xml",
            "application/octet-stream",
            "image/png",
            "image/jpeg"
        )

        testCases.forEach { contentType ->
            val request = HTTPRequest(route = Route(HttpMethod.GET, "/"))
            val response = HTTPResponse(
                status = HTTPStatus.OK(),
                contentType = contentType
            )

            val headers = buildResponseHeaders(
                request = request,
                response = response,
                bodyLength = 0,
                additionalHeaders = null
            )

            assertEquals(contentType, headers["Content-Type"])
        }
    }

    @Test
    fun `respondHttpRequest should not write to closed socket`() {
        val socket = mockk<Socket>()
        every { socket.isClosed } returns true

        val request = HTTPRequest(route = Route(HttpMethod.GET, "/"))
        val response = HTTPResponse(status = HTTPStatus.OK())

        respondHttpRequest(socket, request, response)

        verify(exactly = 0) { socket.outputStream }
    }

    @Test
    fun `respondHttpRequest should write status line correctly`() {
        val outputStream = ByteArrayOutputStream()
        val socket = mockk<Socket>()
        every { socket.isClosed } returns false
        every { socket.outputStream } returns outputStream
        every { socket.close() } returns Unit

        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/"),
            headers = mapOf("connection" to "close")
        )
        val response = HTTPResponse(status = HTTPStatus.OK())

        respondHttpRequest(socket, request, response)

        val output = outputStream.toString("UTF-8")
        assertTrue(output.startsWith("HTTP/1.1 200 OK\r\n"))
    }

    @Test
    fun `respondHttpRequest should write headers correctly`() {
        val outputStream = ByteArrayOutputStream()
        val socket = mockk<Socket>()
        every { socket.isClosed } returns false
        every { socket.outputStream } returns outputStream
        every { socket.close() } returns Unit

        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/"),
            headers = mapOf("connection" to "close")
        )
        val response = HTTPResponse(
            status = HTTPStatus.OK(),
            contentType = "text/plain"
        )

        respondHttpRequest(socket, request, response)

        val output = outputStream.toString("UTF-8")
        assertTrue(output.contains("Content-Type: text/plain\r\n"))
        assertTrue(output.contains("Content-Length: 0\r\n"))
    }

    @Test
    fun `respondHttpRequest should write body when present`() {
        val outputStream = ByteArrayOutputStream()
        val socket = mockk<Socket>()
        every { socket.isClosed } returns false
        every { socket.outputStream } returns outputStream
        every { socket.close() } returns Unit

        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/"),
            headers = mapOf("connection" to "close")
        )
        val body = "Hello, World!"
        val response = HTTPResponse(
            status = HTTPStatus.OK(),
            contentType = "text/plain",
            body = body
        )

        respondHttpRequest(socket, request, response)

        val output = outputStream.toString("UTF-8")
        assertTrue(output.endsWith("\r\n\r\n$body"))
    }

    @Test
    fun `respondHttpRequest should close socket when connection close header present`() {
        val outputStream = ByteArrayOutputStream()
        val socket = mockk<Socket>()
        every { socket.isClosed } returns false
        every { socket.outputStream } returns outputStream
        every { socket.close() } returns Unit

        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/"),
            headers = mapOf("connection" to "close")
        )
        val response = HTTPResponse(status = HTTPStatus.OK())

        respondHttpRequest(socket, request, response)

        verify(exactly = 1) { socket.close() }
    }

    @Test
    fun `respondHttpRequest should not close socket when connection keep-alive`() {
        val outputStream = ByteArrayOutputStream()
        val socket = mockk<Socket>()
        every { socket.isClosed } returns false
        every { socket.outputStream } returns outputStream

        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/"),
            headers = mapOf("connection" to "keep-alive")
        )
        val response = HTTPResponse(status = HTTPStatus.OK())

        respondHttpRequest(socket, request, response)

        verify(exactly = 0) { socket.close() }
    }

    @Test
    fun `respondHttpRequest should handle different status codes`() {
        val testCases = listOf(
            HTTPStatus.OK() to "200 OK",
            HTTPStatus.CREATED() to "201 Created",
            HTTPStatus.BAD_REQUEST() to "400 Bad Request",
            HTTPStatus.NOT_FOUND() to "404 Not Found"
        )

        testCases.forEach { (status, expectedCode) ->
            val outputStream = ByteArrayOutputStream()
            val socket = mockk<Socket>()
            every { socket.isClosed } returns false
            every { socket.outputStream } returns outputStream
            every { socket.close() } returns Unit

            val request = HTTPRequest(
                route = Route(HttpMethod.GET, "/"),
                headers = mapOf("connection" to "close")
            )
            val response = HTTPResponse(status = status)

            respondHttpRequest(socket, request, response)

            val output = outputStream.toString("UTF-8")
            assertTrue(output.startsWith("HTTP/1.1 $expectedCode\r\n"))
        }
    }

    @Test
    fun `respondHttpRequest should include custom headers from response`() {
        val outputStream = ByteArrayOutputStream()
        val socket = mockk<Socket>()
        every { socket.isClosed } returns false
        every { socket.outputStream } returns outputStream
        every { socket.close() } returns Unit

        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/"),
            headers = mapOf("connection" to "close")
        )
        val responseHeaders = mutableMapOf("X-Custom" to "value")
        val response = HTTPResponse(
            status = HTTPStatus.OK(),
            headers = responseHeaders
        )

        respondHttpRequest(socket, request, response)

        val output = outputStream.toString("UTF-8")
        assertTrue(output.contains("X-Custom: value\r\n"))
    }

    @Test
    fun `respondHttpRequest should handle JSON response`() {
        val outputStream = ByteArrayOutputStream()
        val socket = mockk<Socket>()
        every { socket.isClosed } returns false
        every { socket.outputStream } returns outputStream
        every { socket.close() } returns Unit

        val request = HTTPRequest(
            route = Route(HttpMethod.POST, "/api/users"),
            headers = mapOf("connection" to "close")
        )
        val jsonBody = """{"id":1,"name":"John"}"""
        val response = HTTPResponse(
            status = HTTPStatus.CREATED(),
            contentType = "application/json",
            body = jsonBody
        )

        respondHttpRequest(socket, request, response)

        val output = outputStream.toString("UTF-8")
        assertTrue(output.contains("HTTP/1.1 201 Created\r\n"))
        assertTrue(output.contains("Content-Type: application/json\r\n"))
        assertTrue(output.endsWith("\r\n\r\n$jsonBody"))
    }

    @Test
    fun `respondHttpRequest should compress response when gzip accepted`() {
        val outputStream = ByteArrayOutputStream()
        val socket = mockk<Socket>()
        every { socket.isClosed } returns false
        every { socket.outputStream } returns outputStream
        every { socket.close() } returns Unit

        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/"),
            headers = mapOf("accept-encoding" to "gzip", "connection" to "close")
        )
        val body = "Hello, World!"
        val response = HTTPResponse(
            status = HTTPStatus.OK(),
            contentType = "text/plain",
            body = body
        )

        respondHttpRequest(socket, request, response)

        val output = outputStream.toString("UTF-8")
        assertTrue(output.contains("Content-Encoding: gzip\r\n"))
    }
}
