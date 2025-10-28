package response

import domain.HTTPRequest
import domain.HTTPResponse
import domain.HTTPStatus
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResponseHandlerTest {
    @Test
    fun `buildResponseHeaders should include Content-Type when present`() {
        val request = HTTPRequest()
        val response =
            HTTPResponse(
                status = HTTPStatus.OK(),
                contentType = "application/json",
            )

        val headers =
            buildResponseHeaders(
                request = request,
                response = response,
                bodyLength = 0,
                additionalHeaders = null,
            )

        assertEquals("application/json", headers["Content-Type"])
    }

    @Test
    fun `buildResponseHeaders should not include Content-Type when null`() {
        val request = HTTPRequest()
        val response =
            HTTPResponse(
                status = HTTPStatus.OK(),
                contentType = null,
            )

        val headers =
            buildResponseHeaders(
                request = request,
                response = response,
                bodyLength = 0,
                additionalHeaders = null,
            )

        assertTrue(!headers.containsKey("Content-Type"))
    }

    @Test
    fun `buildResponseHeaders should always include Content-Length`() {
        val request = HTTPRequest()
        val response = HTTPResponse(status = HTTPStatus.OK())

        val headers =
            buildResponseHeaders(
                request = request,
                response = response,
                bodyLength = 123,
                additionalHeaders = null,
            )

        assertEquals("123", headers["Content-Length"])
    }

    @Test
    fun `buildResponseHeaders should include Content-Length zero for empty body`() {
        val request = HTTPRequest()
        val response = HTTPResponse(status = HTTPStatus.OK())

        val headers =
            buildResponseHeaders(
                request = request,
                response = response,
                bodyLength = 0,
                additionalHeaders = null,
            )

        assertEquals("0", headers["Content-Length"])
    }

    @Test
    fun `buildResponseHeaders should add Connection close when requested`() {
        val request =
            HTTPRequest(
                headers = mapOf("connection" to "close"),
            )
        val response = HTTPResponse(status = HTTPStatus.OK())

        val headers =
            buildResponseHeaders(
                request = request,
                response = response,
                bodyLength = 0,
                additionalHeaders = null,
            )

        assertEquals("close", headers["Connection"])
    }

    @Test
    fun `buildResponseHeaders should not add Connection header for keep-alive`() {
        val request =
            HTTPRequest(
                headers = mapOf("connection" to "keep-alive"),
            )
        val response = HTTPResponse(status = HTTPStatus.OK())

        val headers =
            buildResponseHeaders(
                request = request,
                response = response,
                bodyLength = 0,
                additionalHeaders = null,
            )

        assertTrue(!headers.containsKey("Connection"))
    }

    @Test
    fun `buildResponseHeaders should include response headers`() {
        val request = HTTPRequest()
        val response =
            HTTPResponse(
                status = HTTPStatus.OK(),
                headers =
                    mutableMapOf(
                        "X-Custom-Header" to "custom-value",
                        "Cache-Control" to "no-cache",
                    ),
            )

        val headers =
            buildResponseHeaders(
                request = request,
                response = response,
                bodyLength = 0,
                additionalHeaders = null,
            )

        assertEquals("custom-value", headers["X-Custom-Header"])
        assertEquals("no-cache", headers["Cache-Control"])
    }

    @Test
    fun `buildResponseHeaders should include additional headers`() {
        val request = HTTPRequest()
        val response = HTTPResponse(status = HTTPStatus.OK())

        val headers =
            buildResponseHeaders(
                request = request,
                response = response,
                bodyLength = 0,
                additionalHeaders = mapOf("Content-Encoding" to "gzip"),
            )

        assertEquals("gzip", headers["Content-Encoding"])
    }

    @Test
    fun `buildResponseHeaders should merge all header sources`() {
        val request = HTTPRequest()
        val response =
            HTTPResponse(
                status = HTTPStatus.OK(),
                contentType = "text/html",
                headers = mutableMapOf("X-Custom" to "value"),
            )

        val headers =
            buildResponseHeaders(
                request = request,
                response = response,
                bodyLength = 100,
                additionalHeaders = mapOf("Content-Encoding" to "gzip"),
            )

        assertEquals("text/html", headers["Content-Type"])
        assertEquals("100", headers["Content-Length"])
        assertEquals("value", headers["X-Custom"])
        assertEquals("gzip", headers["Content-Encoding"])
    }

    @Test
    fun `buildResponseHeaders should handle case-insensitive connection header`() {
        val request =
            HTTPRequest(
                headers = mapOf("connection" to "Close"),
            )
        val response = HTTPResponse(status = HTTPStatus.OK())

        val headers =
            buildResponseHeaders(
                request = request,
                response = response,
                bodyLength = 0,
                additionalHeaders = null,
            )

        assertEquals("close", headers["Connection"])
    }

    @Test
    fun `buildResponseHeaders should handle empty response headers map`() {
        val request = HTTPRequest()
        val response =
            HTTPResponse(
                status = HTTPStatus.OK(),
                headers = mutableMapOf(),
            )

        val headers =
            buildResponseHeaders(
                request = request,
                response = response,
                bodyLength = 0,
                additionalHeaders = null,
            )

        assertEquals("0", headers["Content-Length"])
    }

    @Test
    fun `buildResponseHeaders should handle all parameters together`() {
        val request =
            HTTPRequest(
                headers = mapOf("connection" to "close"),
            )
        val response =
            HTTPResponse(
                status = HTTPStatus.OK(),
                contentType = "application/json",
                headers =
                    mutableMapOf(
                        "X-Request-ID" to "12345",
                        "Cache-Control" to "max-age=3600",
                    ),
            )

        val headers =
            buildResponseHeaders(
                request = request,
                response = response,
                bodyLength = 256,
                additionalHeaders = mapOf("Content-Encoding" to "gzip"),
            )

        assertEquals("application/json", headers["Content-Type"])
        assertEquals("256", headers["Content-Length"])
        assertEquals("12345", headers["X-Request-ID"])
        assertEquals("max-age=3600", headers["Cache-Control"])
        assertEquals("gzip", headers["Content-Encoding"])
        assertEquals("close", headers["Connection"])
    }
}
