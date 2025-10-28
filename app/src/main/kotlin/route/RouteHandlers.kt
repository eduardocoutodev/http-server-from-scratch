package route

import ServerContext
import domain.HTTPRequest
import domain.HTTPResponse
import domain.HTTPStatus
import domain.HttpMethod
import domain.Route
import java.io.File
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.to

typealias HTTPResponseCallback = (request: HTTPRequest) -> HTTPResponse

val ROUTE_HANDLERS: Map<Route, HTTPResponseCallback> =
    mapOf(
        Route(path = "/", method = HttpMethod.GET) to ::rootRouteHandler,
        Route(path = "/echo/{str}", method = HttpMethod.GET) to ::echoRouteHandler,
        Route(path = "/user-agent", method = HttpMethod.GET) to ::echoUserAgent,
        Route(path = "/files/{filename}", method = HttpMethod.GET) to ::retrieveFile,
        Route(path = "/files/{filename}", method = HttpMethod.POST) to ::publishFile,
    )

fun rootRouteHandler(req: HTTPRequest): HTTPResponse = HTTPResponse(status = HTTPStatus.OK())

fun echoRouteHandler(req: HTTPRequest): HTTPResponse {
    val strArg = req.routeArguments["str"]

    if (strArg.isNullOrBlank()) {
        return HTTPResponse(
            status = HTTPStatus.BADREQUEST(),
        )
    }

    return HTTPResponse(
        status = HTTPStatus.OK(),
        contentType = "text/plain",
        body = strArg,
    )
}

fun echoUserAgent(req: HTTPRequest): HTTPResponse {
    val userAgent = req.headers["user-agent"]

    if (userAgent.isNullOrBlank()) {
        return HTTPResponse(
            status = HTTPStatus.BADREQUEST(),
        )
    }

    return HTTPResponse(
        status = HTTPStatus.OK(),
        contentType = "text/plain",
        body = userAgent,
    )
}

@OptIn(ExperimentalPathApi::class)
fun retrieveFile(req: HTTPRequest): HTTPResponse {
    val fileName = req.routeArguments["filename"]

    if (fileName.isNullOrBlank()) {
        return HTTPResponse(
            status = HTTPStatus.BADREQUEST(),
        )
    }

    val directory = ServerContext.filesDirectory
    val filenamePath = Path(directory.toString(), fileName)
    if (!filenamePath.exists()) {
        return HTTPResponse(
            status = HTTPStatus.NOTFOUND(),
        )
    }

    val fileContent = File(filenamePath.toUri()).readText()

    return HTTPResponse(
        status = HTTPStatus.OK(),
        contentType = "application/octet-stream",
        body = fileContent,
    )
}

@OptIn(ExperimentalPathApi::class)
fun publishFile(req: HTTPRequest): HTTPResponse {
    val fileName = req.routeArguments["filename"]

    if (fileName.isNullOrBlank()) {
        println("Filename is null or blank, bad request !")
        return HTTPResponse(
            status = HTTPStatus.BADREQUEST(),
        )
    }

    if (req.body.isNullOrBlank()) {
        println("Body is null or blank, bad request !")
        return HTTPResponse(
            status = HTTPStatus.BADREQUEST(),
        )
    }

    val directory = ServerContext.filesDirectory
    val filenamePath = Path(directory.toString(), fileName)
    if (filenamePath.exists()) {
        println("File already exists ! Bad Request")
        return HTTPResponse(
            status = HTTPStatus.BADREQUEST(),
        )
    }

    filenamePath.createFile()
    File(filenamePath.toUri()).appendText(req.body)

    return HTTPResponse(
        status = HTTPStatus.CREATED(),
    )
}
