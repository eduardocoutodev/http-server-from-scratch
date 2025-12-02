package response

import domain.HTTPRequest
import domain.HttpMethod
import domain.Route
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BodyCompressionTest {

    @Test
    fun `GzipCompression should have correct encoding name`() {
        assertEquals("gzip", GzipCompression.encoding)
    }

    @Test
    fun `GzipCompression should compress body correctly`() {
        val originalText = "Hello, World!"
        val originalBytes = originalText.toByteArray(StandardCharsets.UTF_8)

        val compressed = GzipCompression.compressBody(originalBytes)

        assertTrue(compressed.isNotEmpty())
        assertTrue(compressed.size > 0)
    }

    @Test
    fun `GzipCompression should produce decompressible data`() {
        val originalText = "Test data for compression"
        val originalBytes = originalText.toByteArray(StandardCharsets.UTF_8)

        val compressed = GzipCompression.compressBody(originalBytes)

        val decompressed = GZIPInputStream(ByteArrayInputStream(compressed))
            .bufferedReader(StandardCharsets.UTF_8)
            .use { it.readText() }

        assertEquals(originalText, decompressed)
    }

    @Test
    fun `GzipCompression should handle empty string`() {
        val originalBytes = "".toByteArray(StandardCharsets.UTF_8)

        val compressed = GzipCompression.compressBody(originalBytes)

        assertTrue(compressed.isNotEmpty()) // GZIP header is still present

        val decompressed = GZIPInputStream(ByteArrayInputStream(compressed))
            .bufferedReader(StandardCharsets.UTF_8)
            .use { it.readText() }

        assertEquals("", decompressed)
    }

    @Test
    fun `GzipCompression should handle large text`() {
        val originalText = "A".repeat(10000)
        val originalBytes = originalText.toByteArray(StandardCharsets.UTF_8)

        val compressed = GzipCompression.compressBody(originalBytes)

        assertTrue(compressed.size < originalBytes.size) // Should be compressed

        val decompressed = GZIPInputStream(ByteArrayInputStream(compressed))
            .bufferedReader(StandardCharsets.UTF_8)
            .use { it.readText() }

        assertEquals(originalText, decompressed)
    }

    @Test
    fun `GzipCompression should handle special characters`() {
        val originalText = "Special chars: \r\n\t<>&\"'"
        val originalBytes = originalText.toByteArray(StandardCharsets.UTF_8)

        val compressed = GzipCompression.compressBody(originalBytes)

        val decompressed = GZIPInputStream(ByteArrayInputStream(compressed))
            .bufferedReader(StandardCharsets.UTF_8)
            .use { it.readText() }

        assertEquals(originalText, decompressed)
    }

    @Test
    fun `compressBodyIfNeeded should return null for null body`() {
        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/test"),
            headers = mapOf("accept-encoding" to "gzip")
        )

        val result = compressBodyIfNeeded(request, null)

        assertNull(result)
    }

    @Test
    fun `compressBodyIfNeeded should return null for empty body`() {
        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/test"),
            headers = mapOf("accept-encoding" to "gzip")
        )

        val result = compressBodyIfNeeded(request, ByteArray(0))

        assertNull(result)
    }

    @Test
    fun `compressBodyIfNeeded should return uncompressed body when no accept-encoding header`() {
        val body = "Test body".toByteArray(StandardCharsets.UTF_8)
        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/test"),
            headers = mapOf()
        )

        val result = compressBodyIfNeeded(request, body)

        assertNotNull(result)
        assertContentEquals(body, result.body)
        assertTrue(result.headers.isEmpty())
    }

    @Test
    fun `compressBodyIfNeeded should compress with gzip when requested`() {
        val body = "Test body for compression".toByteArray(StandardCharsets.UTF_8)
        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/test"),
            headers = mapOf("accept-encoding" to "gzip")
        )

        val result = compressBodyIfNeeded(request, body)

        assertNotNull(result)
        assertTrue(result.body.isNotEmpty())
        assertEquals("gzip", result.headers["Content-Encoding"])
    }

    @Test
    fun `compressBodyIfNeeded should handle accept-encoding with multiple values`() {
        val body = "Test body".toByteArray(StandardCharsets.UTF_8)
        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/test"),
            headers = mapOf("accept-encoding" to "deflate, gzip, br")
        )

        val result = compressBodyIfNeeded(request, body)

        assertNotNull(result)
        assertEquals("gzip", result.headers["Content-Encoding"])
    }

    @Test
    fun `compressBodyIfNeeded should handle accept-encoding with spaces`() {
        val body = "Test body".toByteArray(StandardCharsets.UTF_8)
        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/test"),
            headers = mapOf("accept-encoding" to "  gzip  ,  deflate  ")
        )

        val result = compressBodyIfNeeded(request, body)

        assertNotNull(result)
        assertEquals("gzip", result.headers["Content-Encoding"])
    }

    @Test
    fun `compressBodyIfNeeded should handle accept-encoding with uppercase`() {
        val body = "Test body".toByteArray(StandardCharsets.UTF_8)
        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/test"),
            headers = mapOf("accept-encoding" to "GZIP")
        )

        val result = compressBodyIfNeeded(request, body)

        assertNotNull(result)
        assertEquals("gzip", result.headers["Content-Encoding"])
    }

    @Test
    fun `compressBodyIfNeeded should handle accept-encoding with mixed case`() {
        val body = "Test body".toByteArray(StandardCharsets.UTF_8)
        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/test"),
            headers = mapOf("accept-encoding" to "GZip")
        )

        val result = compressBodyIfNeeded(request, body)

        assertNotNull(result)
        assertEquals("gzip", result.headers["Content-Encoding"])
    }

    @Test
    fun `compressBodyIfNeeded should return uncompressed when unsupported encoding requested`() {
        val body = "Test body".toByteArray(StandardCharsets.UTF_8)
        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/test"),
            headers = mapOf("accept-encoding" to "deflate, br")
        )

        val result = compressBodyIfNeeded(request, body)

        assertNotNull(result)
        assertContentEquals(body, result.body)
        assertTrue(result.headers.isEmpty())
    }

    @Test
    fun `compressBodyIfNeeded should return uncompressed for invalid encoding`() {
        val body = "Test body".toByteArray(StandardCharsets.UTF_8)
        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/test"),
            headers = mapOf("accept-encoding" to "invalid")
        )

        val result = compressBodyIfNeeded(request, body)

        assertNotNull(result)
        assertContentEquals(body, result.body)
        assertTrue(result.headers.isEmpty())
    }

    @Test
    fun `compressBodyIfNeeded should compress JSON body`() {
        val jsonBody = """{"name":"John","age":30,"city":"New York"}"""
        val body = jsonBody.toByteArray(StandardCharsets.UTF_8)
        val request = HTTPRequest(
            route = Route(HttpMethod.POST, "/api/users"),
            headers = mapOf("accept-encoding" to "gzip")
        )

        val result = compressBodyIfNeeded(request, body)

        assertNotNull(result)
        assertTrue(result.body.isNotEmpty())
        assertEquals("gzip", result.headers["Content-Encoding"])

        val decompressed = GZIPInputStream(ByteArrayInputStream(result.body))
            .bufferedReader(StandardCharsets.UTF_8)
            .use { it.readText() }

        assertEquals(jsonBody, decompressed)
    }

    @Test
    fun `compressBodyIfNeeded should compress HTML body`() {
        val htmlBody = "<html><body><h1>Hello World</h1></body></html>"
        val body = htmlBody.toByteArray(StandardCharsets.UTF_8)
        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/test"),
            headers = mapOf("accept-encoding" to "gzip")
        )

        val result = compressBodyIfNeeded(request, body)

        assertNotNull(result)
        assertEquals("gzip", result.headers["Content-Encoding"])

        val decompressed = GZIPInputStream(ByteArrayInputStream(result.body))
            .bufferedReader(StandardCharsets.UTF_8)
            .use { it.readText() }

        assertEquals(htmlBody, decompressed)
    }

    @Test
    fun `supportedCompressions should contain gzip`() {
        assertTrue(supportedCompressions.contains("gzip"))
    }

    @Test
    fun `CompressionResult should hold body and headers`() {
        val body = "test".toByteArray()
        val headers = mapOf("Content-Encoding" to "gzip")

        val result = CompressionResult(body = body, headers = headers)

        assertContentEquals(body, result.body)
        assertEquals(headers, result.headers)
    }

    @Test
    fun `CompressionStrategy should be a sealed interface`() {
        val strategy: CompressionStrategy = GzipCompression

        assertEquals("gzip", strategy.encoding)
        assertNotNull(strategy.compressBody("test".toByteArray()))
    }
}
