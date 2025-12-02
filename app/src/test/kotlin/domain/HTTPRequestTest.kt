package domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HTTPRequestTest {

    @Test
    fun `should create HTTPRequest with all default values`() {
        val request = HTTPRequest()

        assertNull(request.route)
        assertTrue(request.routeArguments.isEmpty())
        assertTrue(request.headers.isEmpty())
        assertNull(request.body)
    }

    @Test
    fun `should create HTTPRequest with route only`() {
        val route = Route(HttpMethod.GET, "/test")
        val request = HTTPRequest(route = route)

        assertEquals(route, request.route)
        assertTrue(request.routeArguments.isEmpty())
        assertTrue(request.headers.isEmpty())
        assertNull(request.body)
    }

    @Test
    fun `should create HTTPRequest with route and headers`() {
        val route = Route(HttpMethod.POST, "/api/users")
        val headers = mapOf("Content-Type" to "application/json", "Authorization" to "Bearer token")
        val request = HTTPRequest(route = route, headers = headers)

        assertEquals(route, request.route)
        assertEquals(headers, request.headers)
        assertTrue(request.routeArguments.isEmpty())
        assertNull(request.body)
    }

    @Test
    fun `should create HTTPRequest with route, headers, and body`() {
        val route = Route(HttpMethod.POST, "/api/users")
        val headers = mapOf("Content-Type" to "application/json")
        val body = """{"name": "John", "age": 30}"""
        val request = HTTPRequest(route = route, headers = headers, body = body)

        assertEquals(route, request.route)
        assertEquals(headers, request.headers)
        assertEquals(body, request.body)
        assertTrue(request.routeArguments.isEmpty())
    }

    @Test
    fun `should create HTTPRequest with route arguments`() {
        val route = Route(HttpMethod.GET, "/files/{filename}")
        val routeArgs = mapOf("filename" to "test.txt")
        val request = HTTPRequest(route = route, routeArguments = routeArgs)

        assertEquals(route, request.route)
        assertEquals(routeArgs, request.routeArguments)
        assertEquals("test.txt", request.routeArguments["filename"])
        assertTrue(request.headers.isEmpty())
        assertNull(request.body)
    }

    @Test
    fun `should create HTTPRequest with all fields populated`() {
        val route = Route(HttpMethod.PUT, "/api/users/{id}")
        val routeArgs = mapOf("id" to "123")
        val headers = mapOf("Content-Type" to "application/json", "User-Agent" to "TestClient")
        val body = """{"name": "Jane", "email": "jane@example.com"}"""

        val request = HTTPRequest(
            route = route,
            routeArguments = routeArgs,
            headers = headers,
            body = body
        )

        assertEquals(route, request.route)
        assertEquals(routeArgs, request.routeArguments)
        assertEquals(headers, request.headers)
        assertEquals(body, request.body)
        assertEquals("123", request.routeArguments["id"])
        assertEquals("application/json", request.headers["Content-Type"])
    }

    @Test
    fun `should support multiple route arguments`() {
        val route = Route(HttpMethod.GET, "/api/{resource}/{id}")
        val routeArgs = mapOf("resource" to "users", "id" to "456")
        val request = HTTPRequest(route = route, routeArguments = routeArgs)

        assertEquals(2, request.routeArguments.size)
        assertEquals("users", request.routeArguments["resource"])
        assertEquals("456", request.routeArguments["id"])
    }

    @Test
    fun `should support multiple headers`() {
        val route = Route(HttpMethod.GET, "/test")
        val headers = mapOf(
            "Host" to "localhost:444",
            "User-Agent" to "TestClient/1.0",
            "Accept" to "application/json",
            "Authorization" to "Bearer xyz123"
        )
        val request = HTTPRequest(route = route, headers = headers)

        assertEquals(4, request.headers.size)
        assertEquals("localhost:444", request.headers["Host"])
        assertEquals("TestClient/1.0", request.headers["User-Agent"])
        assertEquals("application/json", request.headers["Accept"])
        assertEquals("Bearer xyz123", request.headers["Authorization"])
    }

    @Test
    fun `data class should support copy with modifications`() {
        val original = HTTPRequest(
            route = Route(HttpMethod.GET, "/test"),
            headers = mapOf("Host" to "localhost")
        )

        val modified = original.copy(body = "new body")

        assertEquals(original.route, modified.route)
        assertEquals(original.headers, modified.headers)
        assertEquals("new body", modified.body)
        assertNull(original.body)
    }

    @Test
    fun `data class should support equality comparison`() {
        val request1 = HTTPRequest(
            route = Route(HttpMethod.GET, "/test"),
            headers = mapOf("Host" to "localhost")
        )

        val request2 = HTTPRequest(
            route = Route(HttpMethod.GET, "/test"),
            headers = mapOf("Host" to "localhost")
        )

        assertEquals(request1, request2)
    }

    @Test
    fun `should handle empty body string`() {
        val request = HTTPRequest(
            route = Route(HttpMethod.POST, "/test"),
            body = ""
        )

        assertEquals("", request.body)
    }
}
