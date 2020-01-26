package automate.profit.autocoin.api.payment

import automate.profit.autocoin.api.undertow.ApiHandler
import automate.profit.autocoin.api.undertow.HttpHandlerWrapper
import automate.profit.autocoin.oauth.server.authorizeWithOauth2
import automate.profit.autocoin.oauth.server.oauthAccount
import automate.profit.autocoin.payment.SubscriptionService
import automate.profit.autocoin.price.PriceService
import com.fasterxml.jackson.databind.ObjectMapper
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.util.Methods.GET
import io.undertow.util.StatusCodes


/**
 * Use to get detailed information about user subscription and payment
 * to display payment form
 */
class GetUserPaymentDetailsHandler(
        private val subscriptionService: SubscriptionService,
        private val priceService: PriceService,
        private val objectMapper: ObjectMapper,
        private val oauth2BearerTokenAuthHandlerWrapper: HttpHandlerWrapper,
        private val currentTimeMillis: () -> Long = System::currentTimeMillis
) : ApiHandler {

    override val method = GET!!
    override val urlTemplate = "/user-payment-details"
    override val httpHandler = HttpHandler {
        val subscriptionCode = it.queryParameters["code"]?.first
        if (subscriptionCode == null) {
            it.statusCode = StatusCodes.BAD_REQUEST
        } else {
            val userAccountId = getUserAccountIdOrSendError(it)
            if (userAccountId != null) {
                val subscription = subscriptionService.getSubscriptionByCode(subscriptionCode)
                if (subscription != null) {
                    val userPayment = subscriptionService.getLatestUnpaidUserPayment(userAccountId = userAccountId, subscriptionCode = subscriptionCode)
                    if (userPayment != null) {
                        val btcAmount = priceService.getCurrencyAmountGivenUsdAmount("BTC", subscription.usdAmount)
                        val ethAmount = priceService.getCurrencyAmountGivenUsdAmount("ETH", subscription.usdAmount)
                        val userSubscription = subscriptionService.getUserSubscriptionByCode(subscriptionCode = subscriptionCode, userAccountId = userAccountId)
                        val userPaymentDetails = UserPaymentDetailsDto(
                                subscription = subscription.toDto(
                                        btcAmount = btcAmount.asScaledString(),
                                        ethAmount = ethAmount.asScaledString()
                                ),
                                userPayment = userPayment.toDto(),
                                userSubscription = userSubscription?.toDto(currentTimeMillis())

                        )
                        it.responseSender.send(objectMapper.writeValueAsString(userPaymentDetails))
                    } else {
                        it.sendNotFound("No userPayment found")
                    }
                } else {
                    it.sendNotFound("No subscription with code $subscriptionCode found")
                }
            }
        }
    }.authorizeWithOauth2(oauth2BearerTokenAuthHandlerWrapper)


    private fun HttpServerExchange.sendNotFound(message: String) {
        statusCode = StatusCodes.NOT_FOUND
        responseSender.send(message)
    }

    private fun HttpServerExchange.sendBadRequest(message: String) {
        statusCode = StatusCodes.BAD_REQUEST
        responseSender.send(message)
    }

    private fun getUserAccountIdOrSendError(serverExchange: HttpServerExchange): String? {
        val account = serverExchange.oauthAccount()
        return if (account.isClientOnly) {
            val userAccountId = serverExchange.queryParameters["userAccountId"]?.first
            return if (userAccountId == null) {
                serverExchange.sendBadRequest("No userAccountId provided")
                null
            } else {
                userAccountId
            }
        } else {
            account.principal.name
        }
    }

}
