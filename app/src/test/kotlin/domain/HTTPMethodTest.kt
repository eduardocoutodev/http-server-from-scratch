package domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals

class HTTPMethodTest {

    @Test
    fun `should have all HTTP methods defined`() {
        val methods = HttpMethod.entries.toTypedArray()
        assertEquals(5, methods.size)
        assertEquals(HttpMethod.GET, methods[0])
        assertEquals(HttpMethod.POST, methods[1])
        assertEquals(HttpMethod.PUT, methods[2])
        assertEquals(HttpMethod.DELETE, methods[3])
        assertEquals(HttpMethod.PATCH, methods[4])
    }

    @ParameterizedTest
    @ValueSource(strings = ["GET", "POST", "PUT", "DELETE", "PATCH"])
    fun `toHttpMethod should convert valid uppercase strings to HttpMethod`(methodString: String) {
        val method = methodString.toHttpMethod()
        assertEquals(HttpMethod.valueOf(methodString), method)
    }

    @ParameterizedTest
    @ValueSource(strings = ["get", "post", "put", "delete", "patch"])
    fun `toHttpMethod should convert valid lowercase strings to HttpMethod`(methodString: String) {
        val method = methodString.toHttpMethod()
        assertEquals(HttpMethod.valueOf(methodString.uppercase()), method)
    }

    @ParameterizedTest
    @ValueSource(strings = ["GeT", "PoSt", "pUt", "DeLeTe", "PaTcH"])
    fun `toHttpMethod should convert valid mixed-case strings to HttpMethod`(methodString: String) {
        val method = methodString.toHttpMethod()
        assertEquals(HttpMethod.valueOf(methodString.uppercase()), method)
    }

    @ParameterizedTest
    @ValueSource(strings = ["INVALID", "HEAD", "OPTIONS", "TRACE", "CONNECT", ""])
    fun `toHttpMethod should throw IllegalArgumentException for invalid method strings`(invalidMethod: String) {
        assertThrows<IllegalArgumentException> {
            invalidMethod.toHttpMethod()
        }
    }

    @Test
    fun `HttpMethod enum values should have correct string representation`() {
        assertEquals("GET", HttpMethod.GET.name)
        assertEquals("POST", HttpMethod.POST.name)
        assertEquals("PUT", HttpMethod.PUT.name)
        assertEquals("DELETE", HttpMethod.DELETE.name)
        assertEquals("PATCH", HttpMethod.PATCH.name)
    }
}
