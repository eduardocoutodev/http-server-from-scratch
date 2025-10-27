package response

import domain.HTTPRequest
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BodyCompressionTest {

    @Test
    fun `compressBodyIfNeeded should return null for null body`() {
        val request = HTTPRequest(
            headers = mapOf("accept-encoding" to "gzip")
        )

        val result = compressBodyIfNeeded(request, null)

        assertNull(result)
    }

    @Test
    fun `compressBodyIfNeeded should return null for empty body`() {
        val request = HTTPRequest(
            headers = mapOf("accept-encoding" to "gzip")
        )

        val result = compressBodyIfNeeded(request, ByteArray(0))

        assertNull(result)
    }

    @Test
    fun `compressBodyIfNeeded should return uncompressed body when no accept-encoding header`() {
        val request = HTTPRequest()
        val body = "Hello, World!".toByteArray()

        val result = compressBodyIfNeeded(request, body)

        assertNotNull(result)
        assertContentEquals(body, result.body)
        assertEquals(0, result.headers.size)
    }

    @Test
    fun `compressBodyIfNeeded should compress body when gzip is accepted`() {
        val request = HTTPRequest(
            headers = mapOf("accept-encoding" to "gzip")
        )
        val originalBody = "Hello, World! This is a test body for compression."
        val body = originalBody.toByteArray()

        val result = compressBodyIfNeeded(request, body)

        assertNotNull(result)
        assertTrue(result.body.isNotEmpty())
        assertEquals("gzip", result.headers["Content-Encoding"])

        // Verify the body is actually compressed (gzip compressed should be different)
        val decompressed = GZIPInputStream(ByteArrayInputStream(result.body)).readBytes()
        assertEquals(originalBody, String(decompressed))
    }

    @Test
    fun `compressBodyIfNeeded should handle multiple encodings and pick gzip`() {
        val request = HTTPRequest(
            headers = mapOf("accept-encoding" to "deflate, gzip, br")
        )
        val originalBody = "Test body"
        val body = originalBody.toByteArray()

        val result = compressBodyIfNeeded(request, body)

        assertNotNull(result)
        assertEquals("gzip", result.headers["Content-Encoding"])

        val decompressed = GZIPInputStream(ByteArrayInputStream(result.body)).readBytes()
        assertEquals(originalBody, String(decompressed))
    }

    @Test
    fun `compressBodyIfNeeded should handle accept-encoding with spaces`() {
        val request = HTTPRequest(
            headers = mapOf("accept-encoding" to "gzip, deflate, br")
        )
        val originalBody = "Test body with spaces"
        val body = originalBody.toByteArray()

        val result = compressBodyIfNeeded(request, body)

        assertNotNull(result)
        assertEquals("gzip", result.headers["Content-Encoding"])
    }

    @Test
    fun `compressBodyIfNeeded should return uncompressed when unsupported encoding`() {
        val request = HTTPRequest(
            headers = mapOf("accept-encoding" to "deflate, br")
        )
        val body = "Test body".toByteArray()

        val result = compressBodyIfNeeded(request, body)

        assertNotNull(result)
        assertContentEquals(body, result.body)
        assertEquals(0, result.headers.size)
    }

    @Test
    fun `compressBodyIfNeeded should handle case-insensitive gzip`() {
        val request = HTTPRequest(
            headers = mapOf("accept-encoding" to "GZIP")
        )
        val originalBody = "Test body"
        val body = originalBody.toByteArray()

        val result = compressBodyIfNeeded(request, body)

        assertNotNull(result)
        assertEquals("gzip", result.headers["Content-Encoding"])
    }

    @Test
    fun `GzipCompression should compress and decompress correctly`() {
        val originalText = "This is a test string that should be compressed with GZIP!"
        val originalBytes = originalText.toByteArray()

        val compressed = GzipCompression.compressBody(originalBytes)

        assertTrue(compressed.isNotEmpty())

        // Decompress to verify
        val decompressed = GZIPInputStream(ByteArrayInputStream(compressed)).readBytes()
        assertEquals(originalText, String(decompressed))
    }

    @Test
    fun `GzipCompression should have correct encoding property`() {
        assertEquals("gzip", GzipCompression.encoding)
    }

    @Test
    fun `GzipCompression should handle empty body`() {
        val originalBytes = ByteArray(0)

        val compressed = GzipCompression.compressBody(originalBytes)

        assertTrue(compressed.isNotEmpty()) // GZIP header still exists
    }

    @Test
    fun `GzipCompression should handle large body`() {
        val originalText = "x".repeat(10000)
        val originalBytes = originalText.toByteArray()

        val compressed = GzipCompression.compressBody(originalBytes)

        // Compressed should be significantly smaller
        assertTrue(compressed.size < originalBytes.size)

        // Verify correctness
        val decompressed = GZIPInputStream(ByteArrayInputStream(compressed)).readBytes()
        assertEquals(originalText, String(decompressed))
    }

    @Test
    fun `GzipCompression should handle special characters`() {
        val originalText = "Special chars: 你好世界 🌍 \n\r\t"
        val originalBytes = originalText.toByteArray()

        val compressed = GzipCompression.compressBody(originalBytes)

        val decompressed = GZIPInputStream(ByteArrayInputStream(compressed)).readBytes()
        assertEquals(originalText, String(decompressed))
    }

    @Test
    fun `supportedCompressions should contain gzip`() {
        assertTrue(supportedCompressions.contains("gzip"))
    }
}
