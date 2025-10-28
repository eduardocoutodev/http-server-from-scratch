package route

import domain.Route
import kotlin.collections.all
import kotlin.collections.filter
import kotlin.collections.find
import kotlin.collections.indices
import kotlin.collections.map
import kotlin.collections.mapNotNull
import kotlin.collections.toMap
import kotlin.collections.zip
import kotlin.text.isNotBlank
import kotlin.text.matches
import kotlin.text.split
import kotlin.text.substring
import kotlin.text.substringBefore
import kotlin.text.trim
import kotlin.to

val ROUTES = ROUTE_HANDLERS.keys

fun findIncomingRoute(incomingRoute: Route): Route? {
    return ROUTES
        .find { route -> matchRoute(incomingRoute, route) }
}

fun matchRoute(incomingRoute: Route, route: Route): Boolean {
    val incomingTarget = incomingRoute.path
    if (incomingRoute.method != route.method) {
        return false
    }

    // Remove query params to not affect
    val cleanTarget = incomingTarget.substringBefore('?')

    val targetParts = cleanTarget.split('/')
        .filter { it.isNotBlank() }

    val routeParts = route.path.split('/')
        .filter { it.isNotBlank() }

    if (targetParts.size != routeParts.size) {
        return false
    }

    return routeParts.zip(targetParts).all { (routePart, targetPart) ->
        isDynamicRoute(routePart) || routePart == targetPart
    }
}

// route: /echo/{arg1}/{arg2}
// incomingTarget: /echo/abc/xyz -> extract arg1 = abc and arg2 = xyz and returns as args
fun extractRouteArgs(incomingRoute: Route, route: Route?): Map<String, String> {
    if (route == null) {
        return mapOf()
    }

    val incomingTargetParts = incomingRoute.path.split("/")
        .map { it.trim() }
        .filter { it.isNotBlank() }

    val routeParts = route.path.split("/")
        .map { it.trim() }
        .filter { it.isNotBlank() }

    if (incomingTargetParts.size != routeParts.size) {
        return mapOf()
    }

    return routeParts.indices.mapNotNull { i ->
        val routePart = routeParts[i]
        val targetPart = incomingTargetParts[i]

        when {
            isDynamicRoute(routePart) -> {
                val paramName = routePart.substring(1, routePart.length - 1)
                paramName to targetPart
            }

            routePart != targetPart -> return emptyMap()
            else -> null
        }
    }.toMap()
}

fun isDynamicRoute(route: String): Boolean {
    return route.matches(Regex("\\{.+}"))
}
