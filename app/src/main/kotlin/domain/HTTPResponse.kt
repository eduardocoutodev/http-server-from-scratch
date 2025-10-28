package domain

data class HTTPResponse(
    val status: HTTPStatus,
    val contentType: String? = null,
    val headers: MutableMap<String, String> = mutableMapOf<String, String>(),
    val body: String? = null,
)
