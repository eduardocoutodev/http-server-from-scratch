package domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class HTTPStatusTest {

    @Test
    fun `OK status should have correct code`() {
        val status = HTTPStatus.OK()
        assertEquals("200 OK", status.code)
    }

    @Test
    fun `CREATED status should have correct code`() {
        val status = HTTPStatus.CREATED()
        assertEquals("201 Created", status.code)
    }

    @Test
    fun `BAD_REQUEST status should have correct code`() {
        val status = HTTPStatus.BAD_REQUEST()
        assertEquals("400 Bad Request", status.code)
    }

    @Test
    fun `NOT_FOUND status should have correct code`() {
        val status = HTTPStatus.NOT_FOUND()
        assertEquals("404 Not Found", status.code)
    }

    @Test
    fun `different OK instances should have the same code`() {
        val status1 = HTTPStatus.OK()
        val status2 = HTTPStatus.OK()
        assertEquals(status1.code, status2.code)
    }

    @Test
    fun `different CREATED instances should have the same code`() {
        val status1 = HTTPStatus.CREATED()
        val status2 = HTTPStatus.CREATED()
        assertEquals(status1.code, status2.code)
    }

    @Test
    fun `different status codes should not be equal`() {
        val ok = HTTPStatus.OK()
        val created = HTTPStatus.CREATED()
        val badRequest = HTTPStatus.BAD_REQUEST()
        val notFound = HTTPStatus.NOT_FOUND()

        assertNotEquals(ok.code, created.code)
        assertNotEquals(ok.code, badRequest.code)
        assertNotEquals(ok.code, notFound.code)
        assertNotEquals(created.code, badRequest.code)
        assertNotEquals(created.code, notFound.code)
        assertNotEquals(badRequest.code, notFound.code)
    }

    @Test
    fun `HTTPStatus should be a sealed class`() {
        val statuses = listOf(
            HTTPStatus.OK(),
            HTTPStatus.CREATED(),
            HTTPStatus.BAD_REQUEST(),
            HTTPStatus.NOT_FOUND()
        )

        statuses.forEach { status ->
            assert(status is HTTPStatus)
        }
    }

    @Test
    fun `status codes should start with correct HTTP status number`() {
        assert(HTTPStatus.OK().code.startsWith("200"))
        assert(HTTPStatus.CREATED().code.startsWith("201"))
        assert(HTTPStatus.BAD_REQUEST().code.startsWith("400"))
        assert(HTTPStatus.NOT_FOUND().code.startsWith("404"))
    }
}
