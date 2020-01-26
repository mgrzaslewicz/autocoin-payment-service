package automate.profit.autocoin.api.payment

import automate.profit.autocoin.api.undertow.ApiHandler
import automate.profit.autocoin.api.undertow.HttpHandlerWrapper
import automate.profit.autocoin.oauth.server.OauthAccount
import automate.profit.autocoin.oauth.server.authorizeWithOauth2
import automate.profit.autocoin.payment.SubscriptionService
import automate.profit.autocoin.payment.UserAccountId
import automate.profit.autocoin.payment.UserSubscription
import com.fasterxml.jackson.databind.ObjectMapper
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.util.Methods.GET
import io.undertow.util.StatusCodes

data class UserSubscriptionDto(
        val userAccountId: String,
        val subscriptionCode: String,
        val isActive: Boolean,
        val validFrom: Long,
        val validTo: Long
)

class GetActiveUserSubscriptionHandler(
        private val subscriptionService: SubscriptionService,
        private val objectMapper: ObjectMapper,
        private val oauth2BearerTokenAuthHandlerWrapper: HttpHandlerWrapper,
        private val currentTimeMillis: () -> Long = System::currentTimeMillis
) : ApiHandler {
    private fun UserSubscription.toDto() = UserSubscriptionDto(
            userAccountId = userAccountId,
            subscriptionCode = subscriptionCode,
            validFrom = validFrom,
            validTo = validTo,
            isActive = validTo < currentTimeMillis()
    )

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
                    it.responseSender.send(objectMapper.writeValueAsString(userSubscription.toDto()))
                } else {
                    it.statusCode = StatusCodes.NOT_FOUND
                }
            }
        }
    }.authorizeWithOauth2(oauth2BearerTokenAuthHandlerWrapper)

    private fun getUserAccountIdOrSendError(serverExchange: HttpServerExchange): UserAccountId? {
        val account = serverExchange.securityContext.authenticatedAccount as OauthAccount
        return if (account.isClientOnly) {
            val userAccountId = serverExchange.queryParameters["userAccountId"]?.first
            return if (userAccountId == null) {
                serverExchange.statusCode = StatusCodes.BAD_REQUEST
                serverExchange.responseSender.send("No userAccountId provided")
                null
            } else {
                UserAccountId(userAccountId)
            }
        } else {
            UserAccountId(account.principal.name)
        }
    }
}
