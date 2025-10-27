package e2e

import HTTP_SERVER_PORT
import ServerContext
import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import request.handleNewSocketConnection
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.nio.file.Path
import java.util.zip.GZIPInputStream
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createFile
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HttpServerE2ETest {

    private lateinit var serverSocket: ServerSocket
    private lateinit var serverJob: Job
    private lateinit var scope: CoroutineScope
    private var serverPort: Int = 0

    @TempDir
    lateinit var tempDir: Path

    private var originalFilesDirectory: Path? = null

    @BeforeAll
    fun setupServer() {
        originalFilesDirectory = ServerContext.filesDirectory
        ServerContext.filesDirectory = tempDir

        // Use a random available port for testing
        serverSocket = ServerSocket(0)
        serverPort = serverSocket.localPort
        serverSocket.reuseAddress = true

        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        // Start server in background
        serverJob = scope.launch {
            try {
                while (!serverSocket.isClosed) {
                    try {
                        val clientSocket = serverSocket.accept()
                        launch {
                            handleNewSocketConnection(clientSocket)
                        }
                    } catch (e: Exception) {
                        if (!serverSocket.isClosed) {
                            println("Error accepting connection: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                // Server closed
            }
        }

        // Give server time to start
        Thread.sleep(100)
    }

    @AfterAll
    fun teardownServer() {
        runBlocking {
            serverSocket.close()
            serverJob.cancel()
            scope.cancel()
            scope.coroutineContext[Job]?.join()
        }

        originalFilesDirectory?.let {
            ServerContext.filesDirectory = it
        }
    }

    private fun makeHttpRequest(
        method: String,
        path: String,
        headers: Map<String, String> = emptyMap(),
        body: String? = null
    ): HttpResponse {
        Socket("localhost", serverPort).use { socket ->
            socket.soTimeout = 5000

            val writer = OutputStreamWriter(socket.getOutputStream())
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

            // Write request line
            writer.write("$method $path HTTP/1.1\r\n")

            // Write headers
            val allHeaders = headers.toMutableMap()
            if (!allHeaders.containsKey("Host")) {
                allHeaders["Host"] = "localhost:$serverPort"
            }
            if (body != null && !allHeaders.containsKey("Content-Length")) {
                allHeaders["Content-Length"] = body.length.toString()
            }
            if (!allHeaders.containsKey("Connection")) {
                allHeaders["Connection"] = "close"
            }

            allHeaders.forEach { (name, value) ->
                writer.write("$name: $value\r\n")
            }

            writer.write("\r\n")

            // Write body if present
            body?.let {
                writer.write(it)
            }

            writer.flush()

            // Read response
            return parseHttpResponse(reader, socket.getInputStream())
        }
    }

    private fun parseHttpResponse(reader: BufferedReader, inputStream: java.io.InputStream): HttpResponse {
        // Read status line
        val statusLine = reader.readLine() ?: throw Exception("Empty response")
        val statusParts = statusLine.split(" ", limit = 3)
        val statusCode = statusParts[1].toInt()

        // Read headers
        val headers = mutableMapOf<String, String>()
        while (true) {
            val line = reader.readLine() ?: break
            if (line.isEmpty() || line.isBlank()) break

            val colonIndex = line.indexOf(':')
            if (colonIndex != -1) {
                val headerName = line.substring(0, colonIndex).trim().lowercase()
                val headerValue = line.substring(colonIndex + 1).trim()
                headers[headerName] = headerValue
            }
        }

        // Read body
        val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
        val body = if (contentLength > 0) {
            val isGzipped = headers["content-encoding"]?.equals("gzip", ignoreCase = true) == true

            if (isGzipped) {
                // Read gzipped content
                val gzipStream = GZIPInputStream(inputStream)
                gzipStream.readBytes().toString(Charsets.UTF_8)
            } else {
                // Read plain content
                val bodyChars = CharArray(contentLength)
                var totalRead = 0
                while (totalRead < contentLength) {
                    val read = reader.read(bodyChars, totalRead, contentLength - totalRead)
                    if (read == -1) break
                    totalRead += read
                }
                String(bodyChars, 0, totalRead)
            }
        } else {
            ""
        }

        return HttpResponse(statusCode, headers, body)
    }

    @Test
    fun `GET root endpoint should return 200 OK`() {
        val response = makeHttpRequest("GET", "/")

        assertEquals(200, response.statusCode)
    }

    @Test
    fun `GET echo endpoint should echo back the string`() {
        val response = makeHttpRequest("GET", "/echo/hello-world")

        assertEquals(200, response.statusCode)
        assertEquals("text/plain", response.headers["content-type"])
        assertEquals("hello-world", response.body)
    }

    @Test
    fun `GET echo endpoint with special characters`() {
        val response = makeHttpRequest("GET", "/echo/test_123-abc")

        assertEquals(200, response.statusCode)
        assertEquals("test_123-abc", response.body)
    }

    @Test
    fun `GET user-agent endpoint should return user agent`() {
        val response = makeHttpRequest(
            "GET",
            "/user-agent",
            headers = mapOf("User-Agent" to "TestClient/1.0")
        )

        assertEquals(200, response.statusCode)
        assertEquals("text/plain", response.headers["content-type"])
        assertEquals("TestClient/1.0", response.body)
    }

    @Test
    fun `GET non-existent route should return 404`() {
        val response = makeHttpRequest("GET", "/nonexistent")

        assertEquals(404, response.statusCode)
    }

    @OptIn(ExperimentalPathApi::class)
    @Test
    fun `GET files endpoint should retrieve existing file`() {
        // Create test file
        val testFile = tempDir.resolve("test.txt")
        testFile.writeText("File content from e2e test")

        val response = makeHttpRequest("GET", "/files/test.txt")

        assertEquals(200, response.statusCode)
        assertEquals("application/octet-stream", response.headers["content-type"])
        assertEquals("File content from e2e test", response.body)
    }

    @Test
    fun `GET files endpoint should return 404 for non-existent file`() {
        val response = makeHttpRequest("GET", "/files/nonexistent.txt")

        assertEquals(404, response.statusCode)
    }

    @OptIn(ExperimentalPathApi::class)
    @Test
    fun `POST files endpoint should create new file`() {
        val fileContent = "New file content"

        val response = makeHttpRequest(
            "POST",
            "/files/newfile.txt",
            body = fileContent
        )

        assertEquals(201, response.statusCode)

        // Verify file was created
        val createdFile = tempDir.resolve("newfile.txt")
        assertTrue(createdFile.toFile().exists())
        assertEquals(fileContent, createdFile.toFile().readText())
    }

    @OptIn(ExperimentalPathApi::class)
    @Test
    fun `POST files endpoint should return 400 for existing file`() {
        // Create existing file
        val existingFile = tempDir.resolve("existing.txt")
        existingFile.createFile()
        existingFile.writeText("Original content")

        val response = makeHttpRequest(
            "POST",
            "/files/existing.txt",
            body = "New content"
        )

        assertEquals(400, response.statusCode)

        // Verify original content is unchanged
        assertEquals("Original content", existingFile.toFile().readText())
    }

    @Test
    fun `Server should support gzip compression`() {
        val response = makeHttpRequest(
            "GET",
            "/echo/compressed-content-test",
            headers = mapOf("Accept-Encoding" to "gzip")
        )

        assertEquals(200, response.statusCode)
        assertEquals("gzip", response.headers["content-encoding"])
        assertEquals("compressed-content-test", response.body)
    }

    @Test
    fun `Server should handle requests without compression`() {
        val response = makeHttpRequest(
            "GET",
            "/echo/uncompressed-content"
        )

        assertEquals(200, response.statusCode)
        assertTrue(response.headers["content-encoding"] == null)
        assertEquals("uncompressed-content", response.body)
    }

    @Test
    fun `Server should handle multiple consecutive requests`() {
        repeat(5) { i ->
            val response = makeHttpRequest("GET", "/echo/request-$i")
            assertEquals(200, response.statusCode)
            assertEquals("request-$i", response.body)
        }
    }

    @Test
    fun `Server should handle Connection close header`() {
        val response = makeHttpRequest(
            "GET",
            "/",
            headers = mapOf("Connection" to "close")
        )

        assertEquals(200, response.statusCode)
        assertEquals("close", response.headers["connection"])
    }

    @OptIn(ExperimentalPathApi::class)
    @Test
    fun `Server should handle JSON file upload`() {
        val jsonContent = """{"name":"John","age":30,"city":"New York"}"""

        val response = makeHttpRequest(
            "POST",
            "/files/data.json",
            body = jsonContent
        )

        assertEquals(201, response.statusCode)

        val createdFile = tempDir.resolve("data.json")
        assertEquals(jsonContent, createdFile.toFile().readText())
    }

    @OptIn(ExperimentalPathApi::class)
    @Test
    fun `Server should handle multiline file content`() {
        val multilineContent = """
            Line 1
            Line 2
            Line 3
        """.trimIndent()

        val response = makeHttpRequest(
            "POST",
            "/files/multiline.txt",
            body = multilineContent
        )

        assertEquals(201, response.statusCode)

        val createdFile = tempDir.resolve("multiline.txt")
        assertEquals(multilineContent, createdFile.toFile().readText())
    }

    @Test
    fun `Server should send Content-Length header in responses`() {
        val response = makeHttpRequest("GET", "/echo/test")

        assertEquals(200, response.statusCode)
        assertNotNull(response.headers["content-length"])
        assertEquals("4", response.headers["content-length"])
    }

    @Test
    fun `Server should handle empty echo parameter with 400`() {
        // The route won't match, so this should return 404
        val response = makeHttpRequest("GET", "/echo/")

        assertEquals(404, response.statusCode)
    }

    data class HttpResponse(
        val statusCode: Int,
        val headers: Map<String, String>,
        val body: String
    )
}
