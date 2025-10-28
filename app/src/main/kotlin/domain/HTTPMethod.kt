package domain

enum class HttpMethod {
    GET,
    POST,
    PUT,
    DELETE,
    PATCH,
}

fun String.toHttpMethod(): HttpMethod {
    return HttpMethod.valueOf(this.uppercase())
}
