package e2e

import ServerContext
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.junit.jupiter.api.*
import request.handleNewSocketConnection
import java.net.ServerSocket
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createFile
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HttpServerE2ETest {

    private lateinit var serverSocket: ServerSocket
    private lateinit var serverJob: Job
    private lateinit var scope: CoroutineScope
    private var serverPort: Int = 0
    private lateinit var client: OkHttpClient
    private lateinit var baseUrl: String

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
        baseUrl = "http://localhost:$serverPort"

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

        // Initialize OkHttp client
        client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build()

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

        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }

    @Test
    fun `GET root endpoint should return 200 OK`() {
        val request = Request.Builder()
            .url("$baseUrl/")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
        }
    }

    @Test
    fun `GET echo endpoint should echo back the string`() {
        val request = Request.Builder()
            .url("$baseUrl/echo/hello-world")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
            assertEquals("text/plain", response.header("Content-Type"))
            assertEquals("hello-world", response.body?.string())
        }
    }

    @Test
    fun `GET echo endpoint with special characters`() {
        val request = Request.Builder()
            .url("$baseUrl/echo/test_123-abc")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
            assertEquals("test_123-abc", response.body?.string())
        }
    }

    @Test
    fun `GET user-agent endpoint should return user agent`() {
        val request = Request.Builder()
            .url("$baseUrl/user-agent")
            .header("User-Agent", "TestClient/1.0")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
            assertEquals("text/plain", response.header("Content-Type"))
            assertEquals("TestClient/1.0", response.body?.string())
        }
    }

    @Test
    fun `GET non-existent route should return 404`() {
        val request = Request.Builder()
            .url("$baseUrl/nonexistent")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(404, response.code)
        }
    }

    @OptIn(ExperimentalPathApi::class)
    @Test
    fun `GET files endpoint should retrieve existing file`() {
        // Create test file
        val testFile = tempDir.resolve("test.txt")
        testFile.writeText("File content from e2e test")

        val request = Request.Builder()
            .url("$baseUrl/files/test.txt")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
            assertEquals("application/octet-stream", response.header("Content-Type"))
            assertEquals("File content from e2e test", response.body?.string())
        }
    }

    @Test
    fun `GET files endpoint should return 404 for non-existent file`() {
        val request = Request.Builder()
            .url("$baseUrl/files/nonexistent.txt")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(404, response.code)
        }
    }

    @OptIn(ExperimentalPathApi::class)
    @Test
    fun `POST files endpoint should create new file`() {
        val fileContent = "New file content"
        val requestBody = fileContent.toRequestBody("text/plain".toMediaType())

        val request = Request.Builder()
            .url("$baseUrl/files/newfile.txt")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(201, response.code)
        }

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

        val requestBody = "New content".toRequestBody("text/plain".toMediaType())
        val request = Request.Builder()
            .url("$baseUrl/files/existing.txt")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(400, response.code)
        }

        // Verify original content is unchanged
        assertEquals("Original content", existingFile.toFile().readText())
    }

    @Test
    fun `Server should support gzip compression`() {
        val request = Request.Builder()
            .url("$baseUrl/echo/compressed-content-test")
            .header("Accept-Encoding", "gzip")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
            assertEquals("gzip", response.header("Content-Encoding"))
            // OkHttp automatically decompresses gzip responses
            assertEquals("compressed-content-test", response.body?.string())
        }
    }

    @Test
    fun `Server should handle requests without compression`() {
        // Create client without automatic gzip support
        val noGzipClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("$baseUrl/echo/uncompressed-content")
            .get()
            .build()

        noGzipClient.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
            assertNull(response.header("Content-Encoding"))
            assertEquals("uncompressed-content", response.body?.string())
        }

        noGzipClient.dispatcher.executorService.shutdown()
        noGzipClient.connectionPool.evictAll()
    }

    @Test
    fun `Server should handle multiple consecutive requests`() {
        repeat(5) { i ->
            val request = Request.Builder()
                .url("$baseUrl/echo/request-$i")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                assertEquals(200, response.code)
                assertEquals("request-$i", response.body?.string())
            }
        }
    }

    @Test
    fun `Server should handle Connection close header`() {
        val request = Request.Builder()
            .url("$baseUrl/")
            .header("Connection", "close")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
            assertEquals("close", response.header("Connection"))
        }
    }

    @OptIn(ExperimentalPathApi::class)
    @Test
    fun `Server should handle JSON file upload`() {
        val jsonContent = """{"name":"John","age":30,"city":"New York"}"""
        val requestBody = jsonContent.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$baseUrl/files/data.json")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(201, response.code)
        }

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
        val requestBody = multilineContent.toRequestBody("text/plain".toMediaType())

        val request = Request.Builder()
            .url("$baseUrl/files/multiline.txt")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(201, response.code)
        }

        val createdFile = tempDir.resolve("multiline.txt")
        assertEquals(multilineContent, createdFile.toFile().readText())
    }

    @Test
    fun `Server should send Content-Length header in responses`() {
        val request = Request.Builder()
            .url("$baseUrl/echo/test")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
            assertNotNull(response.header("Content-Length"))
            // OkHttp may decompress, so check the actual body
            val body = response.body?.string()
            assertEquals("test", body)
        }
    }

    @Test
    fun `Server should handle empty echo parameter with 404`() {
        val request = Request.Builder()
            .url("$baseUrl/echo/")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(404, response.code)
        }
    }

    @Test
    fun `Server should handle custom headers in requests`() {
        val request = Request.Builder()
            .url("$baseUrl/")
            .header("X-Custom-Header", "custom-value")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
        }
    }

    @Test
    fun `Server should handle POST with empty body as bad request`() {
        val requestBody = "".toRequestBody("text/plain".toMediaType())
        val request = Request.Builder()
            .url("$baseUrl/files/empty.txt")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(400, response.code)
        }
    }

    @Test
    fun `Server should handle concurrent requests`() = runBlocking {
        val jobs = List(10) { i ->
            async(Dispatchers.IO) {
                val request = Request.Builder()
                    .url("$baseUrl/echo/concurrent-$i")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    assertEquals(200, response.code)
                    assertEquals("concurrent-$i", response.body?.string())
                }
            }
        }

        jobs.awaitAll()
    }
}
