package route

import ServerContext
import domain.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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
    fun tearDown() {
        originalFilesDirectory?.let {
            ServerContext.filesDirectory = it
        }
    }

    @Test
    fun `rootRouteHandler should return 200 OK`() {
        val request = HTTPRequest(route = Route(HttpMethod.GET, "/"))

        val response = rootRouteHandler(request)

        assertEquals("200 OK", response.status.code)
    }

    @Test
    fun `rootRouteHandler should return response with no body`() {
        val request = HTTPRequest(route = Route(HttpMethod.GET, "/"))

        val response = rootRouteHandler(request)

        assertEquals(null, response.body)
    }

    @Test
    fun `rootRouteHandler should return response with no content type`() {
        val request = HTTPRequest(route = Route(HttpMethod.GET, "/"))

        val response = rootRouteHandler(request)

        assertEquals(null, response.contentType)
    }

    @Test
    fun `echoRouteHandler should echo string argument`() {
        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/echo/{str}"),
            routeArguments = mapOf("str" to "hello")
        )

        val response = echoRouteHandler(request)

        assertEquals("200 OK", response.status.code)
        assertEquals("text/plain", response.contentType)
        assertEquals("hello", response.body)
    }

    @Test
    fun `echoRouteHandler should echo different strings`() {
        val testStrings = listOf("world", "test123", "kotlin-server", "hello-world")

        testStrings.forEach { str ->
            val request = HTTPRequest(
                route = Route(HttpMethod.GET, "/echo/{str}"),
                routeArguments = mapOf("str" to str)
            )

            val response = echoRouteHandler(request)

            assertEquals("200 OK", response.status.code)
            assertEquals("text/plain", response.contentType)
            assertEquals(str, response.body)
        }
    }

    @Test
    fun `echoRouteHandler should return 400 when str argument is null`() {
        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/echo/{str}"),
            routeArguments = mapOf()
        )

        val response = echoRouteHandler(request)

        assertEquals("400 Bad Request", response.status.code)
    }

    @Test
    fun `echoRouteHandler should return 400 when str argument is blank`() {
        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/echo/{str}"),
            routeArguments = mapOf("str" to "")
        )

        val response = echoRouteHandler(request)

        assertEquals("400 Bad Request", response.status.code)
    }

    @Test
    fun `echoRouteHandler should return 400 when str argument is whitespace`() {
        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/echo/{str}"),
            routeArguments = mapOf("str" to "   ")
        )

        val response = echoRouteHandler(request)

        assertEquals("400 Bad Request", response.status.code)
    }

    @Test
    fun `echoUserAgent should echo user-agent header`() {
        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/user-agent"),
            headers = mapOf("user-agent" to "TestClient/1.0")
        )

        val response = echoUserAgent(request)

        assertEquals("200 OK", response.status.code)
        assertEquals("text/plain", response.contentType)
        assertEquals("TestClient/1.0", response.body)
    }

    @Test
    fun `echoUserAgent should echo different user agents`() {
        val testUserAgents = listOf(
            "Mozilla/5.0",
            "curl/7.68.0",
            "PostmanRuntime/7.26.8",
            "MyCustomAgent/1.0"
        )

        testUserAgents.forEach { userAgent ->
            val request = HTTPRequest(
                route = Route(HttpMethod.GET, "/user-agent"),
                headers = mapOf("user-agent" to userAgent)
            )

            val response = echoUserAgent(request)

            assertEquals("200 OK", response.status.code)
            assertEquals("text/plain", response.contentType)
            assertEquals(userAgent, response.body)
        }
    }

    @Test
    fun `echoUserAgent should return 400 when user-agent header is null`() {
        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/user-agent"),
            headers = mapOf()
        )

        val response = echoUserAgent(request)

        assertEquals("400 Bad Request", response.status.code)
    }

    @Test
    fun `echoUserAgent should return 400 when user-agent header is blank`() {
        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/user-agent"),
            headers = mapOf("user-agent" to "")
        )

        val response = echoUserAgent(request)

        assertEquals("400 Bad Request", response.status.code)
    }

    @Test
    fun `echoUserAgent should return 400 when user-agent header is whitespace`() {
        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/user-agent"),
            headers = mapOf("user-agent" to "   ")
        )

        val response = echoUserAgent(request)

        assertEquals("400 Bad Request", response.status.code)
    }

    @Test
    fun `retrieveFile should return file content when file exists`() {
        val fileName = "test.txt"
        val fileContent = "Hello, World!"
        val file = File(tempDir.toFile(), fileName)
        file.writeText(fileContent)

        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/files/{filename}"),
            routeArguments = mapOf("filename" to fileName)
        )

        val response = retrieveFile(request)

        assertEquals("200 OK", response.status.code)
        assertEquals("application/octet-stream", response.contentType)
        assertEquals(fileContent, response.body)
    }

    @Test
    fun `retrieveFile should return 404 when file does not exist`() {
        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/files/{filename}"),
            routeArguments = mapOf("filename" to "nonexistent.txt")
        )

        val response = retrieveFile(request)

        assertEquals("404 Not Found", response.status.code)
    }

    @Test
    fun `retrieveFile should return 400 when filename is null`() {
        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/files/{filename}"),
            routeArguments = mapOf()
        )

        val response = retrieveFile(request)

        assertEquals("400 Bad Request", response.status.code)
    }

    @Test
    fun `retrieveFile should return 400 when filename is blank`() {
        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/files/{filename}"),
            routeArguments = mapOf("filename" to "")
        )

        val response = retrieveFile(request)

        assertEquals("400 Bad Request", response.status.code)
    }

    @Test
    fun `retrieveFile should handle different file extensions`() {
        val testFiles = mapOf(
            "document.pdf" to "PDF content",
            "image.png" to "PNG content",
            "data.json" to """{"key":"value"}""",
            "script.js" to "console.log('hello');"
        )

        testFiles.forEach { (fileName, content) ->
            val file = File(tempDir.toFile(), fileName)
            file.writeText(content)

            val request = HTTPRequest(
                route = Route(HttpMethod.GET, "/files/{filename}"),
                routeArguments = mapOf("filename" to fileName)
            )

            val response = retrieveFile(request)

            assertEquals("200 OK", response.status.code)
            assertEquals("application/octet-stream", response.contentType)
            assertEquals(content, response.body)
        }
    }

    @Test
    fun `retrieveFile should handle empty file`() {
        val fileName = "empty.txt"
        val file = File(tempDir.toFile(), fileName)
        file.writeText("")

        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/files/{filename}"),
            routeArguments = mapOf("filename" to fileName)
        )

        val response = retrieveFile(request)

        assertEquals("200 OK", response.status.code)
        assertEquals("", response.body)
    }

    @Test
    fun `retrieveFile should handle large file content`() {
        val fileName = "large.txt"
        val largeContent = "A".repeat(10000)
        val file = File(tempDir.toFile(), fileName)
        file.writeText(largeContent)

        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/files/{filename}"),
            routeArguments = mapOf("filename" to fileName)
        )

        val response = retrieveFile(request)

        assertEquals("200 OK", response.status.code)
        assertEquals(largeContent, response.body)
    }

    @Test
    fun `publishFile should create new file with content`() {
        val fileName = "newfile.txt"
        val fileContent = "New file content"
        val request = HTTPRequest(
            route = Route(HttpMethod.POST, "/files/{filename}"),
            routeArguments = mapOf("filename" to fileName),
            body = fileContent
        )

        val response = publishFile(request)

        assertEquals("201 Created", response.status.code)

        val filePath = Path(tempDir.toString(), fileName)
        assertTrue(filePath.exists())
        assertEquals(fileContent, File(filePath.toUri()).readText())
    }

    @Test
    fun `publishFile should return 400 when filename is null`() {
        val request = HTTPRequest(
            route = Route(HttpMethod.POST, "/files/{filename}"),
            routeArguments = mapOf(),
            body = "content"
        )

        val response = publishFile(request)

        assertEquals("400 Bad Request", response.status.code)
    }

    @Test
    fun `publishFile should return 400 when filename is blank`() {
        val request = HTTPRequest(
            route = Route(HttpMethod.POST, "/files/{filename}"),
            routeArguments = mapOf("filename" to ""),
            body = "content"
        )

        val response = publishFile(request)

        assertEquals("400 Bad Request", response.status.code)
    }

    @Test
    fun `publishFile should return 400 when body is null`() {
        val request = HTTPRequest(
            route = Route(HttpMethod.POST, "/files/{filename}"),
            routeArguments = mapOf("filename" to "test.txt"),
            body = null
        )

        val response = publishFile(request)

        assertEquals("400 Bad Request", response.status.code)
    }

    @Test
    fun `publishFile should return 400 when body is blank`() {
        val request = HTTPRequest(
            route = Route(HttpMethod.POST, "/files/{filename}"),
            routeArguments = mapOf("filename" to "test.txt"),
            body = ""
        )

        val response = publishFile(request)

        assertEquals("400 Bad Request", response.status.code)
    }

    @Test
    fun `publishFile should return 400 when file already exists`() {
        val fileName = "existing.txt"
        val file = File(tempDir.toFile(), fileName)
        file.writeText("existing content")

        val request = HTTPRequest(
            route = Route(HttpMethod.POST, "/files/{filename}"),
            routeArguments = mapOf("filename" to fileName),
            body = "new content"
        )

        val response = publishFile(request)

        assertEquals("400 Bad Request", response.status.code)
        assertEquals("existing content", file.readText()) // Content unchanged
    }

    @Test
    fun `publishFile should handle different file types`() {
        val testFiles = mapOf(
            "document.txt" to "Text content",
            "data.json" to """{"key":"value"}""",
            "script.js" to "console.log('test');",
            "style.css" to "body { margin: 0; }"
        )

        testFiles.forEach { (fileName, content) ->
            val request = HTTPRequest(
                route = Route(HttpMethod.POST, "/files/{filename}"),
                routeArguments = mapOf("filename" to fileName),
                body = content
            )

            val response = publishFile(request)

            assertEquals("201 Created", response.status.code)

            val filePath = Path(tempDir.toString(), fileName)
            assertTrue(filePath.exists())
            assertEquals(content, File(filePath.toUri()).readText())
        }
    }

    @Test
    fun `publishFile should handle special characters in content`() {
        val fileName = "special.txt"
        val content = "Special chars: \r\n\t<>&\"'"
        val request = HTTPRequest(
            route = Route(HttpMethod.POST, "/files/{filename}"),
            routeArguments = mapOf("filename" to fileName),
            body = content
        )

        val response = publishFile(request)

        assertEquals("201 Created", response.status.code)

        val filePath = Path(tempDir.toString(), fileName)
        assertTrue(filePath.exists())
        assertEquals(content, File(filePath.toUri()).readText())
    }

    @Test
    fun `ROUTE_HANDLERS should contain all route handler mappings`() {
        assertEquals(5, ROUTE_HANDLERS.size)

        assertNotNull(ROUTE_HANDLERS[Route(HttpMethod.GET, "/")])
        assertNotNull(ROUTE_HANDLERS[Route(HttpMethod.GET, "/echo/{str}")])
        assertNotNull(ROUTE_HANDLERS[Route(HttpMethod.GET, "/user-agent")])
        assertNotNull(ROUTE_HANDLERS[Route(HttpMethod.GET, "/files/{filename}")])
        assertNotNull(ROUTE_HANDLERS[Route(HttpMethod.POST, "/files/{filename}")])
    }

    @Test
    fun `ROUTE_HANDLERS should map to correct handler functions`() {
        val rootRequest = HTTPRequest(route = Route(HttpMethod.GET, "/"))
        val rootHandler = ROUTE_HANDLERS[Route(HttpMethod.GET, "/")]
        assertNotNull(rootHandler)
        val rootResponse = rootHandler.invoke(rootRequest)
        assertEquals("200 OK", rootResponse.status.code)

        val echoRequest = HTTPRequest(
            route = Route(HttpMethod.GET, "/echo/{str}"),
            routeArguments = mapOf("str" to "test")
        )
        val echoHandler = ROUTE_HANDLERS[Route(HttpMethod.GET, "/echo/{str}")]
        assertNotNull(echoHandler)
        val echoResponse = echoHandler.invoke(echoRequest)
        assertEquals("test", echoResponse.body)

        val userAgentRequest = HTTPRequest(
            route = Route(HttpMethod.GET, "/user-agent"),
            headers = mapOf("user-agent" to "TestAgent")
        )
        val userAgentHandler = ROUTE_HANDLERS[Route(HttpMethod.GET, "/user-agent")]
        assertNotNull(userAgentHandler)
        val userAgentResponse = userAgentHandler.invoke(userAgentRequest)
        assertEquals("TestAgent", userAgentResponse.body)
    }

    @Test
    fun `HTTPResponseCallback typealias should work correctly`() {
        val customHandler: HTTPResponseCallback = { request ->
            HTTPResponse(
                status = HTTPStatus.OK(),
                body = "Custom response"
            )
        }

        val request = HTTPRequest(route = Route(HttpMethod.GET, "/custom"))
        val response = customHandler.invoke(request)

        assertEquals("200 OK", response.status.code)
        assertEquals("Custom response", response.body)
    }
}
