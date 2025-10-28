package domain

sealed class HTTPStatus(val code: String) {
    class OK : HTTPStatus(
        code = "200 OK",
    )

    class CREATED : HTTPStatus(
        code = "201 Created",
    )

    class BADREQUEST : HTTPStatus(
        code = "400 Bad Request",
    )

    class NOTFOUND : HTTPStatus(
        code = "404 Not Found",
    )
}
