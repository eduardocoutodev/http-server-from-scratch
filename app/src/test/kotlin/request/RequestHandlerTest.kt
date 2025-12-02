package request

import domain.HTTPRequest
import domain.HttpMethod
import domain.Route
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RequestHandlerTest {

    @Test
    fun `shouldCloseSocketConnection should return true when connection header is close`() {
        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/"),
            headers = mapOf("connection" to "close")
        )

        val result = shouldCloseSocketConnection(request)

        assertTrue(result)
    }

    @Test
    fun `shouldCloseSocketConnection should return true when connection header is CLOSE in uppercase`() {
        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/"),
            headers = mapOf("connection" to "CLOSE")
        )

        val result = shouldCloseSocketConnection(request)

        assertTrue(result)
    }

    @Test
    fun `shouldCloseSocketConnection should return true when connection header is Close in mixed case`() {
        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/"),
            headers = mapOf("connection" to "Close")
        )

        val result = shouldCloseSocketConnection(request)

        assertTrue(result)
    }

    @Test
    fun `shouldCloseSocketConnection should return false when connection header is keep-alive`() {
        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/"),
            headers = mapOf("connection" to "keep-alive")
        )

        val result = shouldCloseSocketConnection(request)

        assertFalse(result)
    }

    @Test
    fun `shouldCloseSocketConnection should return false when connection header is absent`() {
        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/"),
            headers = mapOf()
        )

        val result = shouldCloseSocketConnection(request)

        assertFalse(result)
    }

    @Test
    fun `shouldCloseSocketConnection should return false when connection header is empty`() {
        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/"),
            headers = mapOf("connection" to "")
        )

        val result = shouldCloseSocketConnection(request)

        assertFalse(result)
    }

    @Test
    fun `shouldCloseSocketConnection should return false when connection header is upgrade`() {
        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/"),
            headers = mapOf("connection" to "upgrade")
        )

        val result = shouldCloseSocketConnection(request)

        assertFalse(result)
    }

    @ParameterizedTest
    @ValueSource(strings = ["close", "CLOSE", "Close", "cLoSe"])
    fun `shouldCloseSocketConnection should be case-insensitive for close value`(connectionValue: String) {
        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/"),
            headers = mapOf("connection" to connectionValue)
        )

        val result = shouldCloseSocketConnection(request)

        assertTrue(result)
    }

    @ParameterizedTest
    @ValueSource(strings = ["keep-alive", "Keep-Alive", "KEEP-ALIVE", "upgrade", "Upgrade"])
    fun `shouldCloseSocketConnection should return false for non-close values`(connectionValue: String) {
        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/"),
            headers = mapOf("connection" to connectionValue)
        )

        val result = shouldCloseSocketConnection(request)

        assertFalse(result)
    }

    @Test
    fun `shouldCloseSocketConnection should work with different routes`() {
        val routes = listOf(
            Route(HttpMethod.GET, "/"),
            Route(HttpMethod.POST, "/api/users"),
            Route(HttpMethod.PUT, "/api/users/123"),
            Route(HttpMethod.DELETE, "/api/users/123"),
            Route(HttpMethod.PATCH, "/api/users/123")
        )

        routes.forEach { route ->
            val requestWithClose = HTTPRequest(
                route = route,
                headers = mapOf("connection" to "close")
            )
            assertTrue(shouldCloseSocketConnection(requestWithClose))

            val requestWithKeepAlive = HTTPRequest(
                route = route,
                headers = mapOf("connection" to "keep-alive")
            )
            assertFalse(shouldCloseSocketConnection(requestWithKeepAlive))
        }
    }

    @Test
    fun `shouldCloseSocketConnection should only check connection header`() {
        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/"),
            headers = mapOf(
                "host" to "localhost",
                "user-agent" to "TestClient",
                "accept" to "application/json",
                "connection" to "close"
            )
        )

        val result = shouldCloseSocketConnection(request)

        assertTrue(result)
    }

    @Test
    fun `shouldCloseSocketConnection should handle request with only connection header`() {
        val requestClose = HTTPRequest(
            route = Route(HttpMethod.GET, "/"),
            headers = mapOf("connection" to "close")
        )

        assertTrue(shouldCloseSocketConnection(requestClose))

        val requestKeepAlive = HTTPRequest(
            route = Route(HttpMethod.GET, "/"),
            headers = mapOf("connection" to "keep-alive")
        )

        assertFalse(shouldCloseSocketConnection(requestKeepAlive))
    }

    @Test
    fun `shouldCloseSocketConnection should work with GET requests`() {
        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/"),
            headers = mapOf("connection" to "close")
        )

        assertTrue(shouldCloseSocketConnection(request))
    }

    @Test
    fun `shouldCloseSocketConnection should work with POST requests`() {
        val request = HTTPRequest(
            route = Route(HttpMethod.POST, "/api/users"),
            headers = mapOf("connection" to "close"),
            body = """{"name":"John"}"""
        )

        assertTrue(shouldCloseSocketConnection(request))
    }

    @Test
    fun `shouldCloseSocketConnection should work with PUT requests`() {
        val request = HTTPRequest(
            route = Route(HttpMethod.PUT, "/api/users/123"),
            headers = mapOf("connection" to "close"),
            body = """{"name":"Jane"}"""
        )

        assertTrue(shouldCloseSocketConnection(request))
    }

    @Test
    fun `shouldCloseSocketConnection should work with DELETE requests`() {
        val request = HTTPRequest(
            route = Route(HttpMethod.DELETE, "/api/users/123"),
            headers = mapOf("connection" to "close")
        )

        assertTrue(shouldCloseSocketConnection(request))
    }

    @Test
    fun `shouldCloseSocketConnection should work with PATCH requests`() {
        val request = HTTPRequest(
            route = Route(HttpMethod.PATCH, "/api/users/123"),
            headers = mapOf("connection" to "close"),
            body = """{"email":"new@example.com"}"""
        )

        assertTrue(shouldCloseSocketConnection(request))
    }

    @Test
    fun `shouldCloseSocketConnection should handle request with null route`() {
        val request = HTTPRequest(
            route = null,
            headers = mapOf("connection" to "close")
        )

        assertTrue(shouldCloseSocketConnection(request))
    }

    @Test
    fun `shouldCloseSocketConnection should handle request with route arguments`() {
        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/files/{filename}"),
            routeArguments = mapOf("filename" to "test.txt"),
            headers = mapOf("connection" to "close")
        )

        assertTrue(shouldCloseSocketConnection(request))
    }

    @Test
    fun `shouldCloseSocketConnection should handle request with body`() {
        val request = HTTPRequest(
            route = Route(HttpMethod.POST, "/files/{filename}"),
            routeArguments = mapOf("filename" to "test.txt"),
            headers = mapOf("connection" to "close"),
            body = "file content"
        )

        assertTrue(shouldCloseSocketConnection(request))
    }

    @Test
    fun `shouldCloseSocketConnection should return false for whitespace connection value`() {
        val request = HTTPRequest(
            route = Route(HttpMethod.GET, "/"),
            headers = mapOf("connection" to "   ")
        )

        val result = shouldCloseSocketConnection(request)

        assertFalse(result)
    }

    @Test
    fun `shouldCloseSocketConnection should handle connection with extra spaces`() {
        val requestWithSpaces = HTTPRequest(
            route = Route(HttpMethod.GET, "/"),
            headers = mapOf("connection" to "  close  ")
        )

        // The implementation doesn't trim, so this should return false
        val result = shouldCloseSocketConnection(requestWithSpaces)

        assertFalse(result)
    }

    @Test
    fun `shouldCloseSocketConnection should be strict about close value`() {
        val testCases = listOf(
            "close" to true,
            "CLOSE" to true,
            "Close" to true,
            "closed" to false,
            "close-connection" to false,
            "connection-close" to false,
            "close " to false,  // trailing space
            " close" to false   // leading space
        )

        testCases.forEach { (value, expected) ->
            val request = HTTPRequest(
                route = Route(HttpMethod.GET, "/"),
                headers = mapOf("connection" to value)
            )

            val result = shouldCloseSocketConnection(request)

            if (expected) {
                assertTrue(result, "Expected true for connection value: '$value'")
            } else {
                assertFalse(result, "Expected false for connection value: '$value'")
            }
        }
    }
}
