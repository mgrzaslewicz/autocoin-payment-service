package automate.profit.autocoin

import automate.profit.autocoin.api.undertow.HttpHandlerWrapper
import io.undertow.server.HttpHandler

class NoopHttpHandlerWrapper : HttpHandlerWrapper {
    override fun wrap(next: HttpHandler) = next
}