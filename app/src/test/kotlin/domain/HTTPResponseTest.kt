package domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HTTPResponseTest {

    @Test
    fun `should create HTTPResponse with status only`() {
        val response = HTTPResponse(status = HTTPStatus.OK())

        assertEquals("200 OK", response.status.code)
        assertNull(response.contentType)
        assertTrue(response.headers.isEmpty())
        assertNull(response.body)
    }

    @Test
    fun `should create HTTPResponse with status and body`() {
        val body = "Hello, World!"
        val response = HTTPResponse(
            status = HTTPStatus.OK(),
            body = body
        )

        assertEquals("200 OK", response.status.code)
        assertEquals(body, response.body)
        assertNull(response.contentType)
        assertTrue(response.headers.isEmpty())
    }

    @Test
    fun `should create HTTPResponse with status, contentType, and body`() {
        val body = """{"message": "Success"}"""
        val response = HTTPResponse(
            status = HTTPStatus.OK(),
            contentType = "application/json",
            body = body
        )

        assertEquals("200 OK", response.status.code)
        assertEquals("application/json", response.contentType)
        assertEquals(body, response.body)
        assertTrue(response.headers.isEmpty())
    }

    @Test
    fun `should create HTTPResponse with status and headers`() {
        val headers = mutableMapOf("Content-Length" to "100", "Connection" to "close")
        val response = HTTPResponse(
            status = HTTPStatus.OK(),
            headers = headers
        )

        assertEquals("200 OK", response.status.code)
        assertEquals(headers, response.headers)
        assertEquals("100", response.headers["Content-Length"])
        assertEquals("close", response.headers["Connection"])
        assertNull(response.contentType)
        assertNull(response.body)
    }

    @Test
    fun `should create HTTPResponse with all fields populated`() {
        val headers = mutableMapOf("Cache-Control" to "no-cache", "Server" to "TestServer")
        val body = "<html><body>Hello</body></html>"
        val response = HTTPResponse(
            status = HTTPStatus.OK(),
            contentType = "text/html",
            headers = headers,
            body = body
        )

        assertEquals("200 OK", response.status.code)
        assertEquals("text/html", response.contentType)
        assertEquals(headers, response.headers)
        assertEquals(body, response.body)
    }

    @Test
    fun `should support different status codes`() {
        val okResponse = HTTPResponse(status = HTTPStatus.OK())
        val createdResponse = HTTPResponse(status = HTTPStatus.CREATED())
        val badRequestResponse = HTTPResponse(status = HTTPStatus.BAD_REQUEST())
        val notFoundResponse = HTTPResponse(status = HTTPStatus.NOT_FOUND())

        assertEquals("200 OK", okResponse.status.code)
        assertEquals("201 Created", createdResponse.status.code)
        assertEquals("400 Bad Request", badRequestResponse.status.code)
        assertEquals("404 Not Found", notFoundResponse.status.code)
    }

    @Test
    fun `headers should be mutable and allow additions`() {
        val response = HTTPResponse(
            status = HTTPStatus.OK(),
            headers = mutableMapOf("Initial-Header" to "value")
        )

        assertEquals(1, response.headers.size)
        assertEquals("value", response.headers["Initial-Header"])

        response.headers["New-Header"] = "new value"

        assertEquals(2, response.headers.size)
        assertEquals("new value", response.headers["New-Header"])
    }

    @Test
    fun `headers should be mutable and allow modifications`() {
        val response = HTTPResponse(
            status = HTTPStatus.OK(),
            headers = mutableMapOf("Content-Type" to "text/plain")
        )

        assertEquals("text/plain", response.headers["Content-Type"])

        response.headers["Content-Type"] = "application/json"

        assertEquals("application/json", response.headers["Content-Type"])
    }

    @Test
    fun `headers should be mutable and allow removals`() {
        val response = HTTPResponse(
            status = HTTPStatus.OK(),
            headers = mutableMapOf("Remove-Me" to "value", "Keep-Me" to "value")
        )

        assertEquals(2, response.headers.size)

        response.headers.remove("Remove-Me")

        assertEquals(1, response.headers.size)
        assertNull(response.headers["Remove-Me"])
        assertEquals("value", response.headers["Keep-Me"])
    }

    @Test
    fun `should support text plain content type`() {
        val response = HTTPResponse(
            status = HTTPStatus.OK(),
            contentType = "text/plain",
            body = "Plain text response"
        )

        assertEquals("text/plain", response.contentType)
        assertEquals("Plain text response", response.body)
    }

    @Test
    fun `should support application octet-stream content type`() {
        val response = HTTPResponse(
            status = HTTPStatus.OK(),
            contentType = "application/octet-stream",
            body = "binary data"
        )

        assertEquals("application/octet-stream", response.contentType)
        assertEquals("binary data", response.body)
    }

    @Test
    fun `should handle empty body`() {
        val response = HTTPResponse(
            status = HTTPStatus.OK(),
            body = ""
        )

        assertEquals("", response.body)
    }

    @Test
    fun `should handle response without body for 404`() {
        val response = HTTPResponse(status = HTTPStatus.NOT_FOUND())

        assertEquals("404 Not Found", response.status.code)
        assertNull(response.body)
    }

    @Test
    fun `data class should support copy with modifications`() {
        val original = HTTPResponse(
            status = HTTPStatus.OK(),
            contentType = "text/plain"
        )

        val modified = original.copy(body = "Modified body")

        assertEquals(original.status.code, modified.status.code)
        assertEquals(original.contentType, modified.contentType)
        assertEquals("Modified body", modified.body)
        assertNull(original.body)
    }

    @Test
    fun `should support multiple custom headers`() {
        val headers = mutableMapOf(
            "X-Custom-Header-1" to "value1",
            "X-Custom-Header-2" to "value2",
            "X-Custom-Header-3" to "value3"
        )

        val response = HTTPResponse(
            status = HTTPStatus.OK(),
            headers = headers
        )

        assertEquals(3, response.headers.size)
        assertEquals("value1", response.headers["X-Custom-Header-1"])
        assertEquals("value2", response.headers["X-Custom-Header-2"])
        assertEquals("value3", response.headers["X-Custom-Header-3"])
    }
}
