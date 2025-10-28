package domain

data class HTTPRequest(
    val route: Route? = null,
    val routeArguments: Map<String, String> = mapOf(),
    val headers: Map<String, String> = mapOf(),
    val body: String? = null,
)
