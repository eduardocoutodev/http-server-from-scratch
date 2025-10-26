package domain

data class Route(
    val method: HttpMethod,
    val path: String,
)
