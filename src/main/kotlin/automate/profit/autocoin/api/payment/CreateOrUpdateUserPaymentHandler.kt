package automate.profit.autocoin.api.payment

import automate.profit.autocoin.api.undertow.ApiHandler
import automate.profit.autocoin.api.undertow.HttpHandlerWrapper
import automate.profit.autocoin.oauth.server.authorizeWithOauth2
import automate.profit.autocoin.oauth.server.oauthAccount
import automate.profit.autocoin.payment.SubscriptionService
import automate.profit.autocoin.payment.listener.PaymentListener
import com.fasterxml.jackson.databind.ObjectMapper
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.util.Methods.POST
import io.undertow.util.StatusCodes
import mu.KLogging

data class UserPaymentRequestDto(
        val userAccountId: String?,
        val subscriptionCode: String,
        val btcSenderAddress: String?,
        val ethSenderAddress: String?
) {
    fun isValid(): Boolean {
        return (btcSenderAddress != null || ethSenderAddress != null)
    }
}

class CreateOrUpdateUserPaymentHandler(
        private val subscriptionService: SubscriptionService,
        private val paymentListener: PaymentListener,
        private val objectMapper: ObjectMapper,
        private val oauth2BearerTokenAuthHandlerWrapper: HttpHandlerWrapper
) : ApiHandler {
    companion object : KLogging()

    override val method = POST!!
    override val urlTemplate = "/user-payment"
    override val httpHandler = HttpHandler {
        it.requestReceiver.receiveFullString { _, message: String? ->
            val userPaymentRequest = objectMapper.readValue(message, UserPaymentRequestDto::class.java)
            logger.info { "Creating or updating user payment: $userPaymentRequest" }
            val userAccountId = getUserAccountIdOrSendError(it, userPaymentRequest?.userAccountId)
            if (userAccountId != null) {
                val userPayment = subscriptionService.createOrUpdateUnpaidUserPayment(
                        userAccountId = userAccountId,
                        subscriptionCode = userPaymentRequest.subscriptionCode,
                        btcSenderAddress = userPaymentRequest.btcSenderAddress,
                        ethSenderAddress = userPaymentRequest.ethSenderAddress
                )
                if (userPayment != null) {
                    paymentListener.onPaymentCreated(userPayment)
                    it.responseSender.send(objectMapper.writeValueAsString(userPayment.toDto()))
                } else {
                    it.statusCode = StatusCodes.BAD_REQUEST
                    it.responseSender.send("Cannot create userPayment")
                }
            }
        }
    }.authorizeWithOauth2(oauth2BearerTokenAuthHandlerWrapper)

    private fun getUserAccountIdOrSendError(serverExchange: HttpServerExchange, userAccountId: String?): String? {
        val account = serverExchange.oauthAccount()
        return if (account.isClientOnly) {
            return if (userAccountId == null) {
                serverExchange.statusCode = StatusCodes.BAD_REQUEST
                serverExchange.responseSender.send("No userAccountId provided for service2service communication")
                null
            } else {
                userAccountId
            }
        } else {
            return if (userAccountId != null) {
                when {
                    account.isAdmin() -> {
                        logger.info { "Admin user ${account.principal.name} is updating $userAccountId payment" }
                        userAccountId
                    }
                    account.principal.name != userAccountId -> {
                        logger.warn { "Tried to update account $userAccountId from account ${account.principal.name}. Only admin can update others account" }
                        serverExchange.statusCode = StatusCodes.BAD_REQUEST
                        serverExchange.responseSender.send("userAccountId cannot be provided for frontend communication")
                        null
                    }
                    else -> {
                        userAccountId
                    }
                }
            } else {
                account.principal.name
            }
        }
    }

}
