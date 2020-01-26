package automate.profit.autocoin.api.undertow

import io.undertow.server.HttpHandler

interface HttpHandlerWrapper {
    fun wrap(next: HttpHandler): HttpHandler
}