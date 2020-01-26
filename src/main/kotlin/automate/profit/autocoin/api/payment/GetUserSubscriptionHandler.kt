package automate.profit.autocoin.api.payment

import automate.profit.autocoin.api.undertow.ApiHandler
import automate.profit.autocoin.api.undertow.HttpHandlerWrapper
import automate.profit.autocoin.oauth.server.authorizeWithOauth2
import automate.profit.autocoin.oauth.server.oauthAccount
import automate.profit.autocoin.payment.SubscriptionService
import com.fasterxml.jackson.databind.ObjectMapper
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.util.Methods.GET
import io.undertow.util.StatusCodes

data class UserSubscriptionDto(
        val userAccountId: String,
        val subscriptionCode: String,
        val active: Boolean,
        val validFrom: Long,
        val validTo: Long
)

/**
 * Use to get true/false response if user has active subscription.
 */
class GetActiveUserSubscriptionHandler(
        private val subscriptionService: SubscriptionService,
        private val objectMapper: ObjectMapper,
        private val oauth2BearerTokenAuthHandlerWrapper: HttpHandlerWrapper,
        private val currentTimeMillis: () -> Long = System::currentTimeMillis
) : ApiHandler {

    override val method = GET!!
    override val urlTemplate = "/user-subscription"
    override val httpHandler = HttpHandler {
        val subscriptionCode = it.queryParameters["code"]?.first
        if (subscriptionCode == null) {
            it.statusCode = StatusCodes.BAD_REQUEST
        } else {
            val userAccountId = getUserAccountIdOrSendError(it)
            if (userAccountId != null) {
                val userSubscription = subscriptionService.getUserSubscriptionByCode(subscriptionCode, userAccountId)
                if (userSubscription != null) {
                    it.responseSender.send(objectMapper.writeValueAsString(userSubscription.toDto(currentTimeMillis())))
                } else {
                    it.statusCode = StatusCodes.NOT_FOUND
                }
            }
        }
    }.authorizeWithOauth2(oauth2BearerTokenAuthHandlerWrapper)

    private fun getUserAccountIdOrSendError(serverExchange: HttpServerExchange): String? {
        val account = serverExchange.oauthAccount()
        return if (account.isClientOnly) {
            val userAccountId = serverExchange.queryParameters["userAccountId"]?.first
            return if (userAccountId == null) {
                serverExchange.statusCode = StatusCodes.BAD_REQUEST
                serverExchange.responseSender.send("No userAccountId provided")
                null
            } else {
                userAccountId
            }
        } else {
            account.principal.name
        }
    }
}
