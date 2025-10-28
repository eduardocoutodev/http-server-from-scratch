package request

import domain.HttpMethod
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.BufferedReader
import java.io.StringReader
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RequestParserTest {
    @Test
    fun `parseRequestLine should parse valid GET request`() {
        val requestLine = "GET /index.html HTTP/1.1"
        val result = parseRequestLine(requestLine)

        assertEquals(HttpMethod.GET, result.method)
        assertEquals("/index.html", result.target)
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

    @Test
    fun `parseRequestLine should throw exception for invalid format`() {
        val requestLine = "GET /index.html"

        assertThrows<HTTPParseException> {
            parseRequestLine(requestLine)
        }
    }

    @Test
    fun `parseRequestLine should throw exception for invalid HTTP version`() {
        val requestLine = "GET /index.html HTTPS/1.1"

        assertThrows<HTTPParseException> {
            parseRequestLine(requestLine)
        }
    }

    @Test
    fun `parseRequestLine should handle extra whitespace`() {
        val requestLine = "  GET   /index.html   HTTP/1.1  "
        val result = parseRequestLine(requestLine)

        assertEquals(HttpMethod.GET, result.method)
        assertEquals("/index.html", result.target)
        assertEquals("HTTP/1.1", result.httpVersion)
    }

    @Test
    fun `parseHeaders should parse single header`() {
        val headerText = "Host: example.com\r\n\r\n"
        val reader = BufferedReader(StringReader(headerText))
        val headers = parseHeaders(reader)

        assertEquals(1, headers.size)
        assertEquals("example.com", headers["host"])
    }

    @Test
    fun `parseHeaders should parse multiple headers`() {
        val headerText =
            """
            Host: example.com
            User-Agent: Mozilla/5.0
            Accept: text/html
            Content-Length: 100

            """.trimIndent()

        val reader = BufferedReader(StringReader(headerText))
        val headers = parseHeaders(reader)

        assertEquals(4, headers.size)
        assertEquals("example.com", headers["host"])
        assertEquals("Mozilla/5.0", headers["user-agent"])
        assertEquals("text/html", headers["accept"])
        assertEquals("100", headers["content-length"])
    }

    @Test
    fun `parseHeaders should normalize header names to lowercase`() {
        val headerText = "Content-Type: application/json\r\n\r\n"
        val reader = BufferedReader(StringReader(headerText))
        val headers = parseHeaders(reader)

        assertTrue(headers.containsKey("content-type"))
        assertEquals("application/json", headers["content-type"])
    }

    @Test
    fun `parseHeaders should handle headers with spaces around colon`() {
        val headerText = "Content-Type : application/json\r\n\r\n"
        val reader = BufferedReader(StringReader(headerText))
        val headers = parseHeaders(reader)

        assertEquals("application/json", headers["content-type"])
    }

    @Test
    fun `parseHeaders should throw exception for header without colon`() {
        val headerText = "InvalidHeader\r\n\r\n"
        val reader = BufferedReader(StringReader(headerText))

        assertThrows<HTTPParseException> {
            parseHeaders(reader)
        }
    }

    @Test
    fun `parseHeaders should throw exception for empty header name`() {
        val headerText = ": value\r\n\r\n"
        val reader = BufferedReader(StringReader(headerText))

        assertThrows<HTTPParseException> {
            parseHeaders(reader)
        }
    }

    @Test
    fun `parseHeaders should handle empty headers section`() {
        val headerText = "\r\n"
        val reader = BufferedReader(StringReader(headerText))
        val headers = parseHeaders(reader)

        assertEquals(0, headers.size)
    }

    @Test
    fun `parseRequestBody should read exact number of characters`() {
        val bodyText = "Hello, World!"
        val reader = BufferedReader(StringReader(bodyText))
        val body = parseRequestBody(reader, 13)

        assertEquals("Hello, World!", body)
    }

    @Test
    fun `parseRequestBody should read partial content when length is less than available`() {
        val bodyText = "Hello, World!"
        val reader = BufferedReader(StringReader(bodyText))
        val body = parseRequestBody(reader, 5)

        assertEquals("Hello", body)
    }

    @Test
    fun `parseRequestBody should handle empty body`() {
        val bodyText = ""
        val reader = BufferedReader(StringReader(bodyText))
        val body = parseRequestBody(reader, 0)

        assertEquals("", body)
    }

    @Test
    fun `parseRequestBody should handle JSON body`() {
        val bodyText = """{"name":"John","age":30}"""
        val reader = BufferedReader(StringReader(bodyText))
        val body = parseRequestBody(reader, bodyText.length)

        assertEquals("""{"name":"John","age":30}""", body)
    }

    @Test
    fun `parseRequestBody should handle multiline body`() {
        val bodyText = "Line 1\nLine 2\nLine 3"
        val reader = BufferedReader(StringReader(bodyText))
        val body = parseRequestBody(reader, bodyText.length)

        assertEquals("Line 1\nLine 2\nLine 3", body)
    }
}
