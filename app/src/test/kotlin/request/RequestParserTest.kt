package request

import domain.HttpMethod
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.BufferedReader
import java.io.StringReader
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RequestParserTest {

    @Test
    fun `parseRequestLine should parse valid GET request`() {
        val requestLine = "GET / HTTP/1.1"
        val result = parseRequestLine(requestLine)

        assertEquals(HttpMethod.GET, result.method)
        assertEquals("/", result.target)
        assertEquals("HTTP/1.1", result.httpVersion)
    }

    @Test
    fun `parseRequestLine should parse valid POST request`() {
        val requestLine = "POST /api/users HTTP/1.1"
        val result = parseRequestLine(requestLine)

        assertEquals(HttpMethod.POST, result.method)
        assertEquals("/api/users", result.target)
        assertEquals("HTTP/1.1", result.httpVersion)
    }

    @ParameterizedTest
    @ValueSource(strings = ["GET", "POST", "PUT", "DELETE", "PATCH"])
    fun `parseRequestLine should parse all HTTP methods`(method: String) {
        val requestLine = "$method /test HTTP/1.1"
        val result = parseRequestLine(requestLine)

        assertEquals(HttpMethod.valueOf(method), result.method)
        assertEquals("/test", result.target)
        assertEquals("HTTP/1.1", result.httpVersion)
    }

    @Test
    fun `parseRequestLine should parse request with path parameters`() {
        val requestLine = "GET /files/test.txt HTTP/1.1"
        val result = parseRequestLine(requestLine)

        assertEquals(HttpMethod.GET, result.method)
        assertEquals("/files/test.txt", result.target)
        assertEquals("HTTP/1.1", result.httpVersion)
    }

    @Test
    fun `parseRequestLine should parse request with query parameters`() {
        val requestLine = "GET /search?q=kotlin&lang=en HTTP/1.1"
        val result = parseRequestLine(requestLine)

        assertEquals(HttpMethod.GET, result.method)
        assertEquals("/search?q=kotlin&lang=en", result.target)
        assertEquals("HTTP/1.1", result.httpVersion)
    }

    @Test
    fun `parseRequestLine should handle HTTP 1_0 version`() {
        val requestLine = "GET / HTTP/1.0"
        val result = parseRequestLine(requestLine)

        assertEquals(HttpMethod.GET, result.method)
        assertEquals("/", result.target)
        assertEquals("HTTP/1.0", result.httpVersion)
    }

    @Test
    fun `parseRequestLine should handle HTTP 2_0 version`() {
        val requestLine = "GET / HTTP/2.0"
        val result = parseRequestLine(requestLine)

        assertEquals(HttpMethod.GET, result.method)
        assertEquals("/", result.target)
        assertEquals("HTTP/2.0", result.httpVersion)
    }

    @Test
    fun `parseRequestLine should trim whitespace`() {
        val requestLine = "  GET   /test   HTTP/1.1  "
        val result = parseRequestLine(requestLine)

        assertEquals(HttpMethod.GET, result.method)
        assertEquals("/test", result.target)
        assertEquals("HTTP/1.1", result.httpVersion)
    }

    @Test
    fun `parseRequestLine should throw exception for invalid format with too few parts`() {
        val requestLine = "GET /"
        val exception = assertThrows<HTTPParseException> {
            parseRequestLine(requestLine)
        }
        assertTrue(exception.message!!.contains("Invalid request line"))
    }

    @Test
    fun `parseRequestLine should throw exception for invalid format with too many parts`() {
        val requestLine = "GET / HTTP/1.1 extra"
        val exception = assertThrows<HTTPParseException> {
            parseRequestLine(requestLine)
        }
        assertTrue(exception.message!!.contains("Invalid request line"))
    }

    @Test
    fun `parseRequestLine should throw exception for invalid HTTP version`() {
        val requestLine = "GET / HTTPS/1.1"
        val exception = assertThrows<HTTPParseException> {
            parseRequestLine(requestLine)
        }
        assertTrue(exception.message!!.contains("Invalid HTTP version"))
    }

    @Test
    fun `parseRequestLine should throw exception for invalid method`() {
        val requestLine = "INVALID / HTTP/1.1"
        assertThrows<IllegalArgumentException> {
            parseRequestLine(requestLine)
        }
    }

    @Test
    fun `parseRequestLine should throw exception for empty request line`() {
        val requestLine = ""
        assertThrows<HTTPParseException> {
            parseRequestLine(requestLine)
        }
    }

    @Test
    fun `parseHeaders should parse single header`() {
        val input = "Host: localhost:444\r\n\r\n"
        val reader = BufferedReader(StringReader(input))

        val headers = parseHeaders(reader)

        assertEquals(1, headers.size)
        assertEquals("localhost:444", headers["host"])
    }

    @Test
    fun `parseHeaders should parse multiple headers`() {
        val input = """
            Host: localhost:444
            User-Agent: TestClient/1.0
            Accept: application/json

        """.trimIndent()
        val reader = BufferedReader(StringReader(input))

        val headers = parseHeaders(reader)

        assertEquals(3, headers.size)
        assertEquals("localhost:444", headers["host"])
        assertEquals("TestClient/1.0", headers["user-agent"])
        assertEquals("application/json", headers["accept"])
    }

    @Test
    fun `parseHeaders should make header names lowercase`() {
        val input = """
            HOST: localhost
            User-Agent: test
            CONTENT-TYPE: application/json

        """.trimIndent()
        val reader = BufferedReader(StringReader(input))

        val headers = parseHeaders(reader)

        assertTrue(headers.containsKey("host"))
        assertTrue(headers.containsKey("user-agent"))
        assertTrue(headers.containsKey("content-type"))
        assertEquals("localhost", headers["host"])
    }

    @Test
    fun `parseHeaders should trim header values`() {
        val input = "Content-Type:   application/json   \n\n"
        val reader = BufferedReader(StringReader(input))

        val headers = parseHeaders(reader)

        assertEquals("application/json", headers["content-type"])
    }

    @Test
    fun `parseHeaders should handle headers with colons in value`() {
        val input = "Custom-Header: value:with:colons\n\n"
        val reader = BufferedReader(StringReader(input))

        val headers = parseHeaders(reader)

        assertEquals("value:with:colons", headers["custom-header"])
    }

    @Test
    fun `parseHeaders should handle empty header value`() {
        val input = "Empty-Header:\n\n"
        val reader = BufferedReader(StringReader(input))

        val headers = parseHeaders(reader)

        assertEquals("", headers["empty-header"])
    }

    @Test
    fun `parseHeaders should stop at blank line`() {
        val input = """
            Header1: value1
            Header2: value2

            This should not be parsed
        """.trimIndent()
        val reader = BufferedReader(StringReader(input))

        val headers = parseHeaders(reader)

        assertEquals(2, headers.size)
        assertEquals("value1", headers["header1"])
        assertEquals("value2", headers["header2"])
    }

    @Test
    fun `parseHeaders should return empty map when no headers`() {
        val input = "\n"
        val reader = BufferedReader(StringReader(input))

        val headers = parseHeaders(reader)

        assertTrue(headers.isEmpty())
    }

    @Test
    fun `parseHeaders should throw exception for header without colon`() {
        val input = "Invalid Header Line\n\n"
        val reader = BufferedReader(StringReader(input))

        val exception = assertThrows<HTTPParseException> {
            parseHeaders(reader)
        }
        assertTrue(exception.message!!.contains("Invalid header format"))
    }

    @Test
    fun `parseHeaders should throw exception for empty header name`() {
        val input = ": value\n\n"
        val reader = BufferedReader(StringReader(input))

        val exception = assertThrows<HTTPParseException> {
            parseHeaders(reader)
        }
        assertTrue(exception.message!!.contains("Empty header name"))
    }

    @Test
    fun `parseRequestBody should parse body with correct length`() {
        val bodyContent = "Hello, World!"
        val reader = BufferedReader(StringReader(bodyContent))

        val result = parseRequestBody(reader, bodyContent.length)

        assertEquals(bodyContent, result)
    }

    @Test
    fun `parseRequestBody should parse JSON body`() {
        val jsonBody = """{"name":"John","age":30}"""
        val reader = BufferedReader(StringReader(jsonBody))

        val result = parseRequestBody(reader, jsonBody.length)

        assertEquals(jsonBody, result)
    }

    @Test
    fun `parseRequestBody should parse body with special characters`() {
        val body = "Test\r\nWith\nNewlines"
        val reader = BufferedReader(StringReader(body))

        val result = parseRequestBody(reader, body.length)

        assertEquals(body, result)
    }

    @Test
    fun `parseRequestBody should handle empty body`() {
        val reader = BufferedReader(StringReader(""))

        val result = parseRequestBody(reader, 0)

        assertEquals("", result)
    }

    @Test
    fun `parseRequestBody should parse exact body length`() {
        val fullContent = "This is the body content and extra content"
        val bodyLength = 24 // "This is the body content"
        val reader = BufferedReader(StringReader(fullContent))

        val result = parseRequestBody(reader, bodyLength)

        assertEquals("This is the body content", result)
    }

    @Test
    fun `parseRequestBody should handle unicode characters`() {
        val body = "Hello 世界 🌍"
        val reader = BufferedReader(StringReader(body))

        val result = parseRequestBody(reader, body.length)

        assertEquals(body, result)
    }

    @Test
    fun `HTTPParseException should have message`() {
        val exception = HTTPParseException("Test error message")

        assertEquals("Test error message", exception.message)
        assertNull(exception.cause)
    }

    @Test
    fun `HTTPParseException should support cause`() {
        val cause = RuntimeException("Root cause")
        val exception = HTTPParseException("Test error", cause)

        assertEquals("Test error", exception.message)
        assertNotNull(exception.cause)
        assertEquals(cause, exception.cause)
    }

    @Test
    fun `parseHTTPRequestInvocation should parse complete GET request`() {
        val request = """
            GET / HTTP/1.1
            Host: localhost:444
            User-Agent: TestClient

        """.trimIndent()
        val reader = BufferedReader(StringReader(request))

        val result = parseHTTPRequestInvocation(reader)

        assertNotNull(result.route)
        assertEquals(HttpMethod.GET, result.route?.method)
        assertEquals("/", result.route?.path)
        assertEquals(2, result.headers.size)
        assertEquals("localhost:444", result.headers["host"])
        assertEquals("TestClient", result.headers["user-agent"])
        assertNull(result.body)
    }

    @Test
    fun `parseHTTPRequestInvocation should parse POST request with body`() {
        val bodyContent = """{"name":"test"}"""
        val request = """
            POST /api/users HTTP/1.1
            Host: localhost:444
            Content-Type: application/json
            Content-Length: ${bodyContent.length}

            $bodyContent
        """.trimIndent()
        val reader = BufferedReader(StringReader(request))

        val result = parseHTTPRequestInvocation(reader)

        assertNotNull(result.route)
        assertEquals(HttpMethod.POST, result.route?.method)
        assertEquals("/api/users", result.route?.path)
        assertEquals(3, result.headers.size)
        assertEquals(bodyContent, result.body)
    }

    @Test
    fun `parseHTTPRequestInvocation should throw exception for empty request`() {
        val reader = mockk<BufferedReader>()
        every { reader.readLine() } returns null

        val exception = assertThrows<HTTPParseException> {
            parseHTTPRequestInvocation(reader)
        }

        assertTrue(exception.message!!.contains("Empty request"))
    }

    @Test
    fun `parseHTTPRequestInvocation should handle request without body`() {
        val request = """
            GET /test HTTP/1.1
            Host: localhost

        """.trimIndent()
        val reader = BufferedReader(StringReader(request))

        val result = parseHTTPRequestInvocation(reader)

        assertNotNull(result.route)
        assertNull(result.body)
    }

    @Test
    fun `RequestLine data class should have correct properties`() {
        val requestLine = RequestLine(
            method = HttpMethod.GET,
            target = "/test",
            httpVersion = "HTTP/1.1"
        )

        assertEquals(HttpMethod.GET, requestLine.method)
        assertEquals("/test", requestLine.target)
        assertEquals("HTTP/1.1", requestLine.httpVersion)
    }

    @Test
    fun `RequestLine should support equality`() {
        val line1 = RequestLine(HttpMethod.GET, "/test", "HTTP/1.1")
        val line2 = RequestLine(HttpMethod.GET, "/test", "HTTP/1.1")

        assertEquals(line1, line2)
    }

    @Test
    fun `RequestLine should support copy`() {
        val original = RequestLine(HttpMethod.GET, "/test", "HTTP/1.1")
        val modified = original.copy(method = HttpMethod.POST)

        assertEquals(HttpMethod.POST, modified.method)
        assertEquals("/test", modified.target)
        assertEquals("HTTP/1.1", modified.httpVersion)
    }
}
