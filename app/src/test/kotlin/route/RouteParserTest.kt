package route

import domain.HttpMethod
import domain.Route
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RouteParserTest {

    @Test
    fun `isDynamicRoute should return true for valid dynamic route`() {
        assertTrue(isDynamicRoute("{filename}"))
        assertTrue(isDynamicRoute("{id}"))
        assertTrue(isDynamicRoute("{str}"))
        assertTrue(isDynamicRoute("{userId}"))
    }

    @Test
    fun `isDynamicRoute should return false for static route`() {
        assertFalse(isDynamicRoute("files"))
        assertFalse(isDynamicRoute("echo"))
        assertFalse(isDynamicRoute("user-agent"))
        assertFalse(isDynamicRoute("test"))
    }

    @Test
    fun `isDynamicRoute should return false for empty string`() {
        assertFalse(isDynamicRoute(""))
    }

    @Test
    fun `isDynamicRoute should return false for incomplete curly braces`() {
        assertFalse(isDynamicRoute("{filename"))
        assertFalse(isDynamicRoute("filename}"))
        assertFalse(isDynamicRoute("{"))
        assertFalse(isDynamicRoute("}"))
    }

    @Test
    fun `isDynamicRoute should return false for empty braces`() {
        assertFalse(isDynamicRoute("{}"))
    }

    @Test
    fun `isDynamicRoute should return true for route with underscores`() {
        assertTrue(isDynamicRoute("{user_id}"))
        assertTrue(isDynamicRoute("{file_name}"))
    }

    @Test
    fun `isDynamicRoute should return true for route with numbers`() {
        assertTrue(isDynamicRoute("{param1}"))
        assertTrue(isDynamicRoute("{test123}"))
    }

    @Test
    fun `matchRoute should match exact static routes with same method`() {
        val route = Route(HttpMethod.GET, "/")
        val incomingRoute = Route(HttpMethod.GET, "/")

        assertTrue(matchRoute(incomingRoute, route))
    }

    @Test
    fun `matchRoute should not match routes with different methods`() {
        val route = Route(HttpMethod.GET, "/test")
        val incomingRoute = Route(HttpMethod.POST, "/test")

        assertFalse(matchRoute(incomingRoute, route))
    }

    @Test
    fun `matchRoute should not match routes with different paths`() {
        val route = Route(HttpMethod.GET, "/test")
        val incomingRoute = Route(HttpMethod.GET, "/other")

        assertFalse(matchRoute(incomingRoute, route))
    }

    @Test
    fun `matchRoute should match static routes with multiple segments`() {
        val route = Route(HttpMethod.GET, "/api/users")
        val incomingRoute = Route(HttpMethod.GET, "/api/users")

        assertTrue(matchRoute(incomingRoute, route))
    }

    @Test
    fun `matchRoute should match dynamic routes`() {
        val route = Route(HttpMethod.GET, "/files/{filename}")
        val incomingRoute = Route(HttpMethod.GET, "/files/test.txt")

        assertTrue(matchRoute(incomingRoute, route))
    }

    @Test
    fun `matchRoute should match dynamic routes with different values`() {
        val route = Route(HttpMethod.GET, "/echo/{str}")
        val incomingRoute1 = Route(HttpMethod.GET, "/echo/hello")
        val incomingRoute2 = Route(HttpMethod.GET, "/echo/world")

        assertTrue(matchRoute(incomingRoute1, route))
        assertTrue(matchRoute(incomingRoute2, route))
    }

    @Test
    fun `matchRoute should match routes with multiple dynamic segments`() {
        val route = Route(HttpMethod.GET, "/api/{resource}/{id}")
        val incomingRoute = Route(HttpMethod.GET, "/api/users/123")

        assertTrue(matchRoute(incomingRoute, route))
    }

    @Test
    fun `matchRoute should not match routes with different segment counts`() {
        val route = Route(HttpMethod.GET, "/files/{filename}")
        val incomingRoute = Route(HttpMethod.GET, "/files/sub/test.txt")

        assertFalse(matchRoute(incomingRoute, route))
    }

    @Test
    fun `matchRoute should strip query parameters from incoming route`() {
        val route = Route(HttpMethod.GET, "/search")
        val incomingRoute = Route(HttpMethod.GET, "/search?q=test&limit=10")

        assertTrue(matchRoute(incomingRoute, route))
    }

    @Test
    fun `matchRoute should strip query parameters for dynamic routes`() {
        val route = Route(HttpMethod.GET, "/files/{filename}")
        val incomingRoute = Route(HttpMethod.GET, "/files/test.txt?download=true")

        assertTrue(matchRoute(incomingRoute, route))
    }

    @Test
    fun `matchRoute should handle root path`() {
        val route = Route(HttpMethod.GET, "/")
        val incomingRoute = Route(HttpMethod.GET, "/")

        assertTrue(matchRoute(incomingRoute, route))
    }

    @Test
    fun `matchRoute should handle trailing slashes correctly`() {
        val route = Route(HttpMethod.GET, "/test")
        val incomingRoute = Route(HttpMethod.GET, "/test/")

        // Both should have same normalized parts after filtering blank
        assertTrue(matchRoute(incomingRoute, route))
    }

    @ParameterizedTest
    @CsvSource(
        "GET,/,GET,/,true",
        "GET,/test,POST,/test,false",
        "GET,/echo/{str},GET,/echo/hello,true",
        "GET,/files/{filename},GET,/files/test.txt,true",
        "POST,/files/{filename},POST,/files/new.txt,true",
        "GET,/api/users,GET,/api/posts,false",
        "GET,/test,GET,/test/extra,false"
    )
    fun `matchRoute should correctly match various route combinations`(
        routeMethod: String,
        routePath: String,
        incomingMethod: String,
        incomingPath: String,
        shouldMatch: Boolean
    ) {
        val route = Route(HttpMethod.valueOf(routeMethod), routePath)
        val incomingRoute = Route(HttpMethod.valueOf(incomingMethod), incomingPath)

        assertEquals(shouldMatch, matchRoute(incomingRoute, route))
    }

    @Test
    fun `extractRouteArgs should return empty map for null route`() {
        val incomingRoute = Route(HttpMethod.GET, "/test")
        val result = extractRouteArgs(incomingRoute, null)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `extractRouteArgs should return empty map for static route`() {
        val incomingRoute = Route(HttpMethod.GET, "/")
        val route = Route(HttpMethod.GET, "/")
        val result = extractRouteArgs(incomingRoute, route)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `extractRouteArgs should extract single argument from dynamic route`() {
        val incomingRoute = Route(HttpMethod.GET, "/echo/hello")
        val route = Route(HttpMethod.GET, "/echo/{str}")
        val result = extractRouteArgs(incomingRoute, route)

        assertEquals(1, result.size)
        assertEquals("hello", result["str"])
    }

    @Test
    fun `extractRouteArgs should extract filename argument`() {
        val incomingRoute = Route(HttpMethod.GET, "/files/test.txt")
        val route = Route(HttpMethod.GET, "/files/{filename}")
        val result = extractRouteArgs(incomingRoute, route)

        assertEquals(1, result.size)
        assertEquals("test.txt", result["filename"])
    }

    @Test
    fun `extractRouteArgs should extract multiple arguments`() {
        val incomingRoute = Route(HttpMethod.GET, "/api/users/123")
        val route = Route(HttpMethod.GET, "/api/{resource}/{id}")
        val result = extractRouteArgs(incomingRoute, route)

        assertEquals(2, result.size)
        assertEquals("users", result["resource"])
        assertEquals("123", result["id"])
    }

    @Test
    fun `extractRouteArgs should handle complex paths with multiple dynamic segments`() {
        val incomingRoute = Route(HttpMethod.GET, "/api/v1/users/456/posts/789")
        val route = Route(HttpMethod.GET, "/api/v1/users/{userId}/posts/{postId}")
        val result = extractRouteArgs(incomingRoute, route)

        assertEquals(2, result.size)
        assertEquals("456", result["userId"])
        assertEquals("789", result["postId"])
    }

    @Test
    fun `extractRouteArgs should return empty map when segment counts differ`() {
        val incomingRoute = Route(HttpMethod.GET, "/files/sub/test.txt")
        val route = Route(HttpMethod.GET, "/files/{filename}")
        val result = extractRouteArgs(incomingRoute, route)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `extractRouteArgs should return empty map when static segments don't match`() {
        val incomingRoute = Route(HttpMethod.GET, "/api/users/123")
        val route = Route(HttpMethod.GET, "/files/{filename}")
        val result = extractRouteArgs(incomingRoute, route)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `extractRouteArgs should handle routes with special characters in values`() {
        val incomingRoute = Route(HttpMethod.GET, "/files/my-file_name.txt")
        val route = Route(HttpMethod.GET, "/files/{filename}")
        val result = extractRouteArgs(incomingRoute, route)

        assertEquals(1, result.size)
        assertEquals("my-file_name.txt", result["filename"])
    }

    @Test
    fun `extractRouteArgs should handle routes with numbers in values`() {
        val incomingRoute = Route(HttpMethod.GET, "/users/12345")
        val route = Route(HttpMethod.GET, "/users/{id}")
        val result = extractRouteArgs(incomingRoute, route)

        assertEquals(1, result.size)
        assertEquals("12345", result["id"])
    }

    @Test
    fun `extractRouteArgs should trim whitespace from path segments`() {
        val incomingRoute = Route(HttpMethod.GET, "/echo/  hello  ")
        val route = Route(HttpMethod.GET, "/echo/{str}")
        val result = extractRouteArgs(incomingRoute, route)

        assertEquals(1, result.size)
        assertEquals("hello", result["str"])
    }

    @Test
    fun `findIncomingRoute should find root route`() {
        val incomingRoute = Route(HttpMethod.GET, "/")
        val result = findIncomingRoute(incomingRoute)

        assertNotNull(result)
        assertEquals(HttpMethod.GET, result.method)
        assertEquals("/", result.path)
    }

    @Test
    fun `findIncomingRoute should find echo route`() {
        val incomingRoute = Route(HttpMethod.GET, "/echo/test")
        val result = findIncomingRoute(incomingRoute)

        assertNotNull(result)
        assertEquals(HttpMethod.GET, result.method)
        assertEquals("/echo/{str}", result.path)
    }

    @Test
    fun `findIncomingRoute should find user-agent route`() {
        val incomingRoute = Route(HttpMethod.GET, "/user-agent")
        val result = findIncomingRoute(incomingRoute)

        assertNotNull(result)
        assertEquals(HttpMethod.GET, result.method)
        assertEquals("/user-agent", result.path)
    }

    @Test
    fun `findIncomingRoute should find GET files route`() {
        val incomingRoute = Route(HttpMethod.GET, "/files/test.txt")
        val result = findIncomingRoute(incomingRoute)

        assertNotNull(result)
        assertEquals(HttpMethod.GET, result.method)
        assertEquals("/files/{filename}", result.path)
    }

    @Test
    fun `findIncomingRoute should find POST files route`() {
        val incomingRoute = Route(HttpMethod.POST, "/files/newfile.txt")
        val result = findIncomingRoute(incomingRoute)

        assertNotNull(result)
        assertEquals(HttpMethod.POST, result.method)
        assertEquals("/files/{filename}", result.path)
    }

    @Test
    fun `findIncomingRoute should return null for unmatched route`() {
        val incomingRoute = Route(HttpMethod.GET, "/nonexistent")
        val result = findIncomingRoute(incomingRoute)

        assertNull(result)
    }

    @Test
    fun `findIncomingRoute should return null for wrong method`() {
        val incomingRoute = Route(HttpMethod.DELETE, "/")
        val result = findIncomingRoute(incomingRoute)

        assertNull(result)
    }

    @Test
    fun `findIncomingRoute should handle routes with query parameters`() {
        val incomingRoute = Route(HttpMethod.GET, "/echo/test?param=value")
        val result = findIncomingRoute(incomingRoute)

        assertNotNull(result)
        assertEquals(HttpMethod.GET, result.method)
        assertEquals("/echo/{str}", result.path)
    }

    @Test
    fun `ROUTES should contain all defined route handlers`() {
        assertEquals(5, ROUTES.size)

        val routePaths = ROUTES.map { it.path }.toSet()
        assertTrue(routePaths.contains("/"))
        assertTrue(routePaths.contains("/echo/{str}"))
        assertTrue(routePaths.contains("/user-agent"))
        assertTrue(routePaths.contains("/files/{filename}"))
    }

    @Test
    fun `ROUTES should contain both GET and POST for files`() {
        val filesRoutes = ROUTES.filter { it.path == "/files/{filename}" }
        assertEquals(2, filesRoutes.size)

        val methods = filesRoutes.map { it.method }.toSet()
        assertTrue(methods.contains(HttpMethod.GET))
        assertTrue(methods.contains(HttpMethod.POST))
    }
}
