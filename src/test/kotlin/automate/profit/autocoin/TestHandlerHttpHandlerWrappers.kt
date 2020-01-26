package automate.profit.autocoin

import automate.profit.autocoin.api.undertow.HttpHandlerWrapper
import automate.profit.autocoin.oauth.server.OauthAccount
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.undertow.security.api.SecurityContext
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange

class NoopHttpHandlerWrapper : HttpHandlerWrapper {
    override fun wrap(next: HttpHandler) = next
}

class MockSecurityHttpHandler(
        private val oauthAccount: OauthAccount,
        private val httpHandler: HttpHandler
) : HttpHandler {
    override fun handleRequest(exchange: HttpServerExchange) {
        exchange.securityContext = mock<SecurityContext>().apply {
            whenever(this.authenticatedAccount).thenReturn(oauthAccount)
        }
        httpHandler.handleRequest(exchange)
    }
}

class MockSecurityContextHandlerWrapper(private val oauthAccount: OauthAccount) : HttpHandlerWrapper {
    override fun wrap(next: HttpHandler) = MockSecurityHttpHandler(oauthAccount, next)
}