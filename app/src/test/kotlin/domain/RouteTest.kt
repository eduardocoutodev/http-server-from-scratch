package domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class RouteTest {

    @Test
    fun `should create Route with GET method and path`() {
        val route = Route(method = HttpMethod.GET, path = "/")

        assertEquals(HttpMethod.GET, route.method)
        assertEquals("/", route.path)
    }

    @Test
    fun `should create Route with POST method and path`() {
        val route = Route(method = HttpMethod.POST, path = "/api/users")

        assertEquals(HttpMethod.POST, route.method)
        assertEquals("/api/users", route.path)
    }

    @Test
    fun `should create Route with PUT method and path`() {
        val route = Route(method = HttpMethod.PUT, path = "/api/users/123")

        assertEquals(HttpMethod.PUT, route.method)
        assertEquals("/api/users/123", route.path)
    }

    @Test
    fun `should create Route with DELETE method and path`() {
        val route = Route(method = HttpMethod.DELETE, path = "/api/users/123")

        assertEquals(HttpMethod.DELETE, route.method)
        assertEquals("/api/users/123", route.path)
    }

    @Test
    fun `should create Route with PATCH method and path`() {
        val route = Route(method = HttpMethod.PATCH, path = "/api/users/123")

        assertEquals(HttpMethod.PATCH, route.method)
        assertEquals("/api/users/123", route.path)
    }

    @Test
    fun `should create Route with dynamic path parameter`() {
        val route = Route(method = HttpMethod.GET, path = "/files/{filename}")

        assertEquals(HttpMethod.GET, route.method)
        assertEquals("/files/{filename}", route.path)
    }

    @Test
    fun `should create Route with multiple dynamic path parameters`() {
        val route = Route(method = HttpMethod.GET, path = "/api/{resource}/{id}")

        assertEquals(HttpMethod.GET, route.method)
        assertEquals("/api/{resource}/{id}", route.path)
    }

    @Test
    fun `should create Route with complex path`() {
        val route = Route(method = HttpMethod.GET, path = "/api/v1/users/{userId}/posts/{postId}/comments")

        assertEquals(HttpMethod.GET, route.method)
        assertEquals("/api/v1/users/{userId}/posts/{postId}/comments", route.path)
    }

    @Test
    fun `should create Route with root path`() {
        val route = Route(method = HttpMethod.GET, path = "/")

        assertEquals(HttpMethod.GET, route.method)
        assertEquals("/", route.path)
    }

    @Test
    fun `should create Route with echo path`() {
        val route = Route(method = HttpMethod.GET, path = "/echo/{str}")

        assertEquals(HttpMethod.GET, route.method)
        assertEquals("/echo/{str}", route.path)
    }

    @Test
    fun `should create Route with user-agent path`() {
        val route = Route(method = HttpMethod.GET, path = "/user-agent")

        assertEquals(HttpMethod.GET, route.method)
        assertEquals("/user-agent", route.path)
    }

    @Test
    fun `data class should support equality comparison`() {
        val route1 = Route(method = HttpMethod.GET, path = "/test")
        val route2 = Route(method = HttpMethod.GET, path = "/test")

        assertEquals(route1, route2)
    }

    @Test
    fun `data class should differentiate routes with different methods`() {
        val getRoute = Route(method = HttpMethod.GET, path = "/test")
        val postRoute = Route(method = HttpMethod.POST, path = "/test")

        assertNotEquals(getRoute, postRoute)
    }

    @Test
    fun `data class should differentiate routes with different paths`() {
        val route1 = Route(method = HttpMethod.GET, path = "/test1")
        val route2 = Route(method = HttpMethod.GET, path = "/test2")

        assertNotEquals(route1, route2)
    }

    @Test
    fun `data class should support copy with method modification`() {
        val original = Route(method = HttpMethod.GET, path = "/test")
        val modified = original.copy(method = HttpMethod.POST)

        assertEquals(HttpMethod.POST, modified.method)
        assertEquals("/test", modified.path)
        assertEquals(HttpMethod.GET, original.method)
    }

    @Test
    fun `data class should support copy with path modification`() {
        val original = Route(method = HttpMethod.GET, path = "/test")
        val modified = original.copy(path = "/new-path")

        assertEquals(HttpMethod.GET, modified.method)
        assertEquals("/new-path", modified.path)
        assertEquals("/test", original.path)
    }

    @Test
    fun `should handle paths with query parameters in path string`() {
        val route = Route(method = HttpMethod.GET, path = "/search?query=test")

        assertEquals(HttpMethod.GET, route.method)
        assertEquals("/search?query=test", route.path)
    }

    @Test
    fun `should handle paths with special characters`() {
        val route = Route(method = HttpMethod.GET, path = "/files/my-file_name.txt")

        assertEquals(HttpMethod.GET, route.method)
        assertEquals("/files/my-file_name.txt", route.path)
    }

    @Test
    fun `should handle deeply nested paths`() {
        val route = Route(method = HttpMethod.GET, path = "/a/b/c/d/e/f/g")

        assertEquals(HttpMethod.GET, route.method)
        assertEquals("/a/b/c/d/e/f/g", route.path)
    }

    @Test
    fun `hashCode should be consistent for equal routes`() {
        val route1 = Route(method = HttpMethod.GET, path = "/test")
        val route2 = Route(method = HttpMethod.GET, path = "/test")

        assertEquals(route1.hashCode(), route2.hashCode())
    }

    @Test
    fun `toString should provide useful representation`() {
        val route = Route(method = HttpMethod.GET, path = "/test")
        val toString = route.toString()

        assert(toString.contains("GET"))
        assert(toString.contains("/test"))
    }
}
