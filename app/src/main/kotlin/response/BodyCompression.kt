package response

import domain.HTTPRequest
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import java.nio.charset.StandardCharsets.UTF_8

val supportedCompressions = listOf("gzip")

sealed interface CompressionStrategy {
    val encoding: String
    fun compressBody(originalBody: ByteArray): ByteArray
}

object GzipCompression : CompressionStrategy {
    override val encoding = "gzip"
    override fun compressBody(originalBody: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).bufferedWriter(UTF_8).use { it.write(String(originalBody)) }

        return bos.toByteArray()
    }
}

data class CompressionResult(
    val body: ByteArray,
    val headers: Map<String, String>
)

fun compressBodyIfNeeded(
    request: HTTPRequest,
    body: ByteArray?
): CompressionResult? {
    if (body == null || body.isEmpty()) {
        return null
    }

    val acceptEncoding = request.headers["accept-encoding"] ?: return CompressionResult(
        body = body,
        headers = emptyMap()
    )

    val strategy = selectCompression(acceptEncoding)

    return if (strategy != null) {
        CompressionResult(
            body = strategy.compressBody(body),
            headers = mapOf("Content-Encoding" to strategy.encoding)
        )
    } else {
        CompressionResult(
            body = body,
            headers = emptyMap()
        )
    }
}

@OptIn(ExperimentalStdlibApi::class)
private fun selectCompression(acceptEncoding: String): CompressionStrategy? {
    val encodings = acceptEncoding.split(",").map { it.trim().lowercase() }
    val encondingToMatch = encodings.find { it in supportedCompressions }

    return when {
        encondingToMatch?.equals("gzip", ignoreCase = true) == true -> GzipCompression
        else -> null
    }
}
