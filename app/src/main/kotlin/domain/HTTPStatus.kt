package domain

sealed class HTTPStatus(val code: String) {
    class OK : HTTPStatus(
        code = "200 OK"
    )
    class CREATED : HTTPStatus(
        code = "201 Created"
    )
    class BAD_REQUEST : HTTPStatus(
        code = "400 Bad Request"
    )
    class NOT_FOUND : HTTPStatus(
        code = "404 Not Found"
    )
}
