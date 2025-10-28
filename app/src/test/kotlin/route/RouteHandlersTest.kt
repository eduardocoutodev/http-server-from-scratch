package route

import ServerContext
import domain.HTTPRequest
import domain.HTTPStatus
import domain.HttpMethod
import domain.Route
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RouteHandlersTest {
    @TempDir
    lateinit var tempDir: Path

    private var originalFilesDirectory: Path? = null

    @BeforeEach
    fun setup() {
        originalFilesDirectory = ServerContext.filesDirectory
        ServerContext.filesDirectory = tempDir
    }

    @AfterEach
    fun teardown() {
        originalFilesDirectory?.let {
            ServerContext.filesDirectory = it
        }
    }

    @Test
    fun `rootRouteHandler should return 200 OK`() {
        val request = HTTPRequest()
        val response = rootRouteHandler(request)

        assertEquals(HTTPStatus.OK().code, response.status.code)
    }

    @Test
    fun `echoRouteHandler should echo back the string parameter`() {
        val request =
            HTTPRequest(
                routeArguments = mapOf("str" to "hello"),
            )

        val response = echoRouteHandler(request)

        assertEquals(HTTPStatus.OK().code, response.status.code)
        assertEquals("text/plain", response.contentType)
        assertEquals("hello", response.body)
    }

    @Test
    fun `echoRouteHandler should return 400 for null parameter`() {
        val request =
            HTTPRequest(
                routeArguments = mapOf(),
            )

        val response = echoRouteHandler(request)

        assertEquals(HTTPStatus.BADREQUEST().code, response.status.code)
    }

    @Test
    fun `echoRouteHandler should return 400 for blank parameter`() {
        val request =
            HTTPRequest(
                routeArguments = mapOf("str" to "   "),
            )

        val response = echoRouteHandler(request)

        assertEquals(HTTPStatus.BADREQUEST().code, response.status.code)
    }

    @Test
    fun `echoRouteHandler should handle special characters`() {
        val request =
            HTTPRequest(
                routeArguments = mapOf("str" to "hello-world_123"),
            )

        val response = echoRouteHandler(request)

        assertEquals(HTTPStatus.OK().code, response.status.code)
        assertEquals("hello-world_123", response.body)
    }

    @Test
    fun `echoUserAgent should return user agent header`() {
        val request =
            HTTPRequest(
                headers = mapOf("user-agent" to "Mozilla/5.0"),
            )

        val response = echoUserAgent(request)

        assertEquals(HTTPStatus.OK().code, response.status.code)
        assertEquals("text/plain", response.contentType)
        assertEquals("Mozilla/5.0", response.body)
    }

    @Test
    fun `echoUserAgent should return 400 when user-agent is missing`() {
        val request =
            HTTPRequest(
                headers = mapOf(),
            )

        val response = echoUserAgent(request)

        assertEquals(HTTPStatus.BADREQUEST().code, response.status.code)
    }

    @Test
    fun `echoUserAgent should return 400 when user-agent is blank`() {
        val request =
            HTTPRequest(
                headers = mapOf("user-agent" to "   "),
            )

        val response = echoUserAgent(request)

        assertEquals(HTTPStatus.BADREQUEST().code, response.status.code)
    }

    @OptIn(ExperimentalPathApi::class)
    @Test
    fun `retrieveFile should return file content when file exists`() {
        // Create a test file
        val testFile = tempDir.resolve("test.txt")
        testFile.writeText("Hello from file!")

        val request =
            HTTPRequest(
                routeArguments = mapOf("filename" to "test.txt"),
            )

        val response = retrieveFile(request)

        assertEquals(HTTPStatus.OK().code, response.status.code)
        assertEquals("application/octet-stream", response.contentType)
        assertEquals("Hello from file!", response.body)
    }

    @Test
    fun `retrieveFile should return 404 when file does not exist`() {
        val request =
            HTTPRequest(
                routeArguments = mapOf("filename" to "nonexistent.txt"),
            )

        val response = retrieveFile(request)

        assertEquals(HTTPStatus.NOTFOUND().code, response.status.code)
    }

    @Test
    fun `retrieveFile should return 400 when filename is null`() {
        val request =
            HTTPRequest(
                routeArguments = mapOf(),
            )

        val response = retrieveFile(request)

        assertEquals(HTTPStatus.BADREQUEST().code, response.status.code)
    }

    @Test
    fun `retrieveFile should return 400 when filename is blank`() {
        val request =
            HTTPRequest(
                routeArguments = mapOf("filename" to "   "),
            )

        val response = retrieveFile(request)

        assertEquals(HTTPStatus.BADREQUEST().code, response.status.code)
    }

    @OptIn(ExperimentalPathApi::class)
    @Test
    fun `retrieveFile should handle files with different extensions`() {
        val testFile = tempDir.resolve("data.json")
        testFile.writeText("""{"key":"value"}""")

        val request =
            HTTPRequest(
                routeArguments = mapOf("filename" to "data.json"),
            )

        val response = retrieveFile(request)

        assertEquals(HTTPStatus.OK().code, response.status.code)
        assertEquals("""{"key":"value"}""", response.body)
    }

    @OptIn(ExperimentalPathApi::class)
    @Test
    fun `publishFile should create file with content`() {
        val request =
            HTTPRequest(
                routeArguments = mapOf("filename" to "newfile.txt"),
                body = "File content here",
            )

        val response = publishFile(request)

        assertEquals(HTTPStatus.CREATED().code, response.status.code)

        // Verify file was created
        val createdFile = tempDir.resolve("newfile.txt")
        assertTrue(createdFile.exists())
        assertEquals("File content here", File(createdFile.toUri()).readText())
    }

    @Test
    fun `publishFile should return 400 when filename is null`() {
        val request =
            HTTPRequest(
                routeArguments = mapOf(),
                body = "content",
            )

        val response = publishFile(request)

        assertEquals(HTTPStatus.BADREQUEST().code, response.status.code)
    }

    @Test
    fun `publishFile should return 400 when filename is blank`() {
        val request =
            HTTPRequest(
                routeArguments = mapOf("filename" to "   "),
                body = "content",
            )

        val response = publishFile(request)

        assertEquals(HTTPStatus.BADREQUEST().code, response.status.code)
    }

    @Test
    fun `publishFile should return 400 when body is null`() {
        val request =
            HTTPRequest(
                routeArguments = mapOf("filename" to "test.txt"),
                body = null,
            )

        val response = publishFile(request)

        assertEquals(HTTPStatus.BADREQUEST().code, response.status.code)
    }

    @Test
    fun `publishFile should return 400 when body is blank`() {
        val request =
            HTTPRequest(
                routeArguments = mapOf("filename" to "test.txt"),
                body = "   ",
            )

        val response = publishFile(request)

        assertEquals(HTTPStatus.BADREQUEST().code, response.status.code)
    }

    @OptIn(ExperimentalPathApi::class)
    @Test
    fun `publishFile should return 400 when file already exists`() {
        // Create existing file
        val existingFile = tempDir.resolve("existing.txt")
        existingFile.createFile()

        val request =
            HTTPRequest(
                routeArguments = mapOf("filename" to "existing.txt"),
                body = "new content",
            )

        val response = publishFile(request)

        assertEquals(HTTPStatus.BADREQUEST().code, response.status.code)
    }

    @OptIn(ExperimentalPathApi::class)
    @Test
    fun `publishFile should handle multiline content`() {
        val content =
            """
            Line 1
            Line 2
            Line 3
            """.trimIndent()

        val request =
            HTTPRequest(
                routeArguments = mapOf("filename" to "multiline.txt"),
                body = content,
            )

        val response = publishFile(request)

        assertEquals(HTTPStatus.CREATED().code, response.status.code)

        val createdFile = tempDir.resolve("multiline.txt")
        assertEquals(content, File(createdFile.toUri()).readText())
    }

    @OptIn(ExperimentalPathApi::class)
    @Test
    fun `publishFile should handle JSON content`() {
        val jsonContent = """{"name":"John","age":30}"""

        val request =
            HTTPRequest(
                routeArguments = mapOf("filename" to "data.json"),
                body = jsonContent,
            )

        val response = publishFile(request)

        assertEquals(HTTPStatus.CREATED().code, response.status.code)

        val createdFile = tempDir.resolve("data.json")
        assertEquals(jsonContent, File(createdFile.toUri()).readText())
    }

    @Test
    fun `ROUTE_HANDLERS should contain all expected routes`() {
        val routes = ROUTE_HANDLERS.keys

        assertTrue(routes.contains(Route(HttpMethod.GET, "/")))
        assertTrue(routes.contains(Route(HttpMethod.GET, "/echo/{str}")))
        assertTrue(routes.contains(Route(HttpMethod.GET, "/user-agent")))
        assertTrue(routes.contains(Route(HttpMethod.GET, "/files/{filename}")))
        assertTrue(routes.contains(Route(HttpMethod.POST, "/files/{filename}")))
    }

    @Test
    fun `ROUTE_HANDLERS should have correct number of routes`() {
        assertEquals(5, ROUTE_HANDLERS.size)
    }
}
