package automate.profit.autocoin.api.payment

import automate.profit.autocoin.api.undertow.ApiHandler
import automate.profit.autocoin.api.undertow.HttpHandlerWrapper
import automate.profit.autocoin.oauth.server.authorizeWithOauth2
import automate.profit.autocoin.payment.Subscription
import automate.profit.autocoin.payment.SubscriptionService
import automate.profit.autocoin.price.PriceService
import com.fasterxml.jackson.databind.ObjectMapper
import io.undertow.server.HttpHandler
import io.undertow.util.Methods
import io.undertow.util.StatusCodes

data class SubscriptionDto(
        val btcReceiverAddress: String?,
        val ethReceiverAddress: String?,
        val btcAmount: String?,
        val ethAmount: String?,
        val usdAmount: String,
        val description: String
)

fun Subscription.toDto(btcAmount: String?, ethAmount: String?) = SubscriptionDto(
        btcReceiverAddress = btcReceiverAddress,
        ethReceiverAddress = ethReceiverAddress,
        usdAmount = usdAmount.toString(),
        btcAmount = btcAmount,
        ethAmount = ethAmount,
        description = description
)

class GetSubscriptionHandler(
        private val subscriptionService: SubscriptionService,
        private val priceService: PriceService,
        private val objectMapper: ObjectMapper,
        private val oauth2BearerTokenAuthHandlerWrapper: HttpHandlerWrapper
) : ApiHandler {

    override val method = Methods.GET!!
    override val urlTemplate = "/subscription"
    override val httpHandler = HttpHandler {
        val subscriptionCode = it.queryParameters["code"]?.first
        if (subscriptionCode.isNullOrBlank()) {
            it.statusCode = StatusCodes.BAD_REQUEST
            it.responseSender.send("code query parameter not set")
        } else {
            val subscription = subscriptionService.getSubscriptionByCode(subscriptionCode)
            if (subscription != null) {
                val btcAmount = if (subscription.btcReceiverAddress != null) priceService.getCurrencyAmountGivenUsdAmount("BTC", subscription.usdAmount).asScaledString() else null
                val ethAmount = if (subscription.ethReceiverAddress != null) priceService.getCurrencyAmountGivenUsdAmount("ETH", subscription.usdAmount).asScaledString() else null
                it.responseSender.send(objectMapper.writeValueAsString(subscription.toDto(
                        btcAmount = btcAmount,
                        ethAmount = ethAmount
                )))
            } else {
                it.statusCode = StatusCodes.NOT_FOUND
            }
        }
    }.authorizeWithOauth2(oauth2BearerTokenAuthHandlerWrapper)
}
