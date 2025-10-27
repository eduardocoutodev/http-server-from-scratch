package route

import domain.HttpMethod
import domain.Route
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RouteParserTest {

    @Test
    fun `matchRoute should match exact paths`() {
        val route = Route(HttpMethod.GET, "/users")
        val incomingRoute = Route(HttpMethod.GET, "/users")

        assertTrue(matchRoute(incomingRoute, route))
    }

    @Test
    fun `matchRoute should not match different methods`() {
        val route = Route(HttpMethod.GET, "/users")
        val incomingRoute = Route(HttpMethod.POST, "/users")

        assertFalse(matchRoute(incomingRoute, route))
    }

    @Test
    fun `matchRoute should not match different paths`() {
        val route = Route(HttpMethod.GET, "/users")
        val incomingRoute = Route(HttpMethod.GET, "/posts")

        assertFalse(matchRoute(incomingRoute, route))
    }

    @Test
    fun `matchRoute should match dynamic routes`() {
        val route = Route(HttpMethod.GET, "/users/{id}")
        val incomingRoute = Route(HttpMethod.GET, "/users/123")

        assertTrue(matchRoute(incomingRoute, route))
    }

    @Test
    fun `matchRoute should match multiple dynamic parameters`() {
        val route = Route(HttpMethod.GET, "/users/{id}/posts/{postId}")
        val incomingRoute = Route(HttpMethod.GET, "/users/123/posts/456")

        assertTrue(matchRoute(incomingRoute, route))
    }

    @Test
    fun `matchRoute should not match different lengths`() {
        val route = Route(HttpMethod.GET, "/users/{id}")
        val incomingRoute = Route(HttpMethod.GET, "/users/123/posts")

        assertFalse(matchRoute(incomingRoute, route))
    }

    @Test
    fun `matchRoute should ignore trailing slashes`() {
        val route = Route(HttpMethod.GET, "/users")
        val incomingRoute = Route(HttpMethod.GET, "/users/")

        assertTrue(matchRoute(incomingRoute, route))
    }

    @Test
    fun `matchRoute should handle root path`() {
        val route = Route(HttpMethod.GET, "/")
        val incomingRoute = Route(HttpMethod.GET, "/")

        assertTrue(matchRoute(incomingRoute, route))
    }

    @Test
    fun `matchRoute should remove query parameters before matching`() {
        val route = Route(HttpMethod.GET, "/search")
        val incomingRoute = Route(HttpMethod.GET, "/search?query=test&page=1")

        assertTrue(matchRoute(incomingRoute, route))
    }

    @Test
    fun `matchRoute should handle query params with dynamic routes`() {
        val route = Route(HttpMethod.GET, "/users/{id}")
        val incomingRoute = Route(HttpMethod.GET, "/users/123?include=posts")

        assertTrue(matchRoute(incomingRoute, route))
    }

    @Test
    fun `extractRouteArgs should extract single parameter`() {
        val route = Route(HttpMethod.GET, "/users/{id}")
        val incomingRoute = Route(HttpMethod.GET, "/users/123")

        val args = extractRouteArgs(incomingRoute, route)

        assertEquals(1, args.size)
        assertEquals("123", args["id"])
    }

    @Test
    fun `extractRouteArgs should extract multiple parameters`() {
        val route = Route(HttpMethod.GET, "/users/{userId}/posts/{postId}")
        val incomingRoute = Route(HttpMethod.GET, "/users/123/posts/456")

        val args = extractRouteArgs(incomingRoute, route)

        assertEquals(2, args.size)
        assertEquals("123", args["userId"])
        assertEquals("456", args["postId"])
    }

    @Test
    fun `extractRouteArgs should return empty map for static routes`() {
        val route = Route(HttpMethod.GET, "/users")
        val incomingRoute = Route(HttpMethod.GET, "/users")

        val args = extractRouteArgs(incomingRoute, route)

        assertEquals(0, args.size)
    }

    @Test
    fun `extractRouteArgs should return empty map when route is null`() {
        val incomingRoute = Route(HttpMethod.GET, "/users/123")

        val args = extractRouteArgs(incomingRoute, null)

        assertEquals(0, args.size)
    }

    @Test
    fun `extractRouteArgs should return empty map when paths don't match`() {
        val route = Route(HttpMethod.GET, "/users/{id}")
        val incomingRoute = Route(HttpMethod.GET, "/posts/123")

        val args = extractRouteArgs(incomingRoute, route)

        assertEquals(0, args.size)
    }

    @Test
    fun `extractRouteArgs should handle mixed static and dynamic segments`() {
        val route = Route(HttpMethod.GET, "/api/v1/users/{id}/profile")
        val incomingRoute = Route(HttpMethod.GET, "/api/v1/users/123/profile")

        val args = extractRouteArgs(incomingRoute, route)

        assertEquals(1, args.size)
        assertEquals("123", args["id"])
    }

    @Test
    fun `extractRouteArgs should handle special characters in parameter values`() {
        val route = Route(HttpMethod.GET, "/files/{filename}")
        val incomingRoute = Route(HttpMethod.GET, "/files/my-file_name.txt")

        val args = extractRouteArgs(incomingRoute, route)

        assertEquals(1, args.size)
        assertEquals("my-file_name.txt", args["filename"])
    }

    @Test
    fun `isDynamicRoute should return true for dynamic route`() {
        assertTrue(isDynamicRoute("{id}"))
        assertTrue(isDynamicRoute("{userId}"))
        assertTrue(isDynamicRoute("{filename}"))
    }

    @Test
    fun `isDynamicRoute should return false for static route`() {
        assertFalse(isDynamicRoute("users"))
        assertFalse(isDynamicRoute("api"))
        assertFalse(isDynamicRoute("123"))
    }

    @Test
    fun `isDynamicRoute should return false for malformed dynamic route`() {
        assertFalse(isDynamicRoute("{"))
        assertFalse(isDynamicRoute("}"))
        assertFalse(isDynamicRoute("{}"))
        assertFalse(isDynamicRoute("{ }"))
    }

    @Test
    fun `findIncomingRoute should find matching route from ROUTES`() {
        val incomingRoute = Route(HttpMethod.GET, "/")
        val foundRoute = findIncomingRoute(incomingRoute)

        assertNotNull(foundRoute)
        assertEquals("/", foundRoute.path)
        assertEquals(HttpMethod.GET, foundRoute.method)
    }

    @Test
    fun `findIncomingRoute should find dynamic route`() {
        val incomingRoute = Route(HttpMethod.GET, "/echo/test")
        val foundRoute = findIncomingRoute(incomingRoute)

        assertNotNull(foundRoute)
        assertEquals("/echo/{str}", foundRoute.path)
        assertEquals(HttpMethod.GET, foundRoute.method)
    }

    @Test
    fun `findIncomingRoute should return null for non-existent route`() {
        val incomingRoute = Route(HttpMethod.GET, "/nonexistent")
        val foundRoute = findIncomingRoute(incomingRoute)

        assertNull(foundRoute)
    }

    @Test
    fun `findIncomingRoute should find file routes`() {
        val incomingRoute = Route(HttpMethod.GET, "/files/test.txt")
        val foundRoute = findIncomingRoute(incomingRoute)

        assertNotNull(foundRoute)
        assertEquals("/files/{filename}", foundRoute.path)
        assertEquals(HttpMethod.GET, foundRoute.method)
    }
}
