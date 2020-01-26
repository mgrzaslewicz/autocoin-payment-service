package automate.profit.autocoin.api.payment

import automate.profit.autocoin.api.undertow.ApiHandler
import automate.profit.autocoin.api.undertow.HttpHandlerWrapper
import automate.profit.autocoin.oauth.server.authorizeWithOauth2
import automate.profit.autocoin.oauth.server.oauthAccount
import automate.profit.autocoin.payment.PaymentStatus
import automate.profit.autocoin.payment.PaymentStatus.APPROVED_MANUALLY
import automate.profit.autocoin.payment.PaymentStatus.PAID
import automate.profit.autocoin.payment.SubscriptionService
import com.fasterxml.jackson.databind.ObjectMapper
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.util.Methods.POST
import io.undertow.util.StatusCodes
import mu.KLogging


class SetUserPaymentPaidHandler(
        private val subscriptionService: SubscriptionService,
        private val objectMapper: ObjectMapper,
        private val oauth2BearerTokenAuthHandlerWrapper: HttpHandlerWrapper
) : ApiHandler {
    companion object : KLogging()

    private val allowedNewPaymentStatuses = listOf(PAID.name, APPROVED_MANUALLY.name)
    override val method = POST!!
    override val urlTemplate = "/set-user-payment-paid"
    override val httpHandler = HttpHandler {
        val userPaymentId = getUserPaymentIdOrSendBadRequest(it)
        if (userPaymentId != null) {
            logger.info { "Setting userPayment as paid: $userPaymentId" }
            val newPaymentStatusString = getQueryParameterOrSendBadRequest(it, "paymentStatus", allowedNewPaymentStatuses)
            if (newPaymentStatusString != null) {
                val newPaymentStatus = PaymentStatus.valueOf(newPaymentStatusString)
                val userPayment = subscriptionService.setPaymentAsPaidOrApproved(userPaymentId, newPaymentStatus)
                it.responseSender.send(objectMapper.writeValueAsString(userPayment.toDto()))
            }
        }
    }.authorizeWithOauth2(oauth2BearerTokenAuthHandlerWrapper)

    private fun getQueryParameterOrSendBadRequest(serverExchange: HttpServerExchange, parameterName: String, allowedValues: List<String>): String? {
        val parameterValue = serverExchange.queryParameters[parameterName]?.first
        return if (parameterValue == null) {
            logger.warn { "Missing required parameter $parameterName" }
            serverExchange.statusCode = StatusCodes.BAD_REQUEST
            serverExchange.responseSender.send("No $parameterName provided")
            null
        } else {
            parameterValue
        }
    }

    private fun getUserPaymentIdOrSendBadRequest(serverExchange: HttpServerExchange): String? {
        val userPaymentId = serverExchange.queryParameters["userPaymentId"]?.first
        val account = serverExchange.oauthAccount()

        return if (userPaymentId != null) {
            if (account.isClientOnly) {
                logger.info { "Service ${account.principal.name} is updating payment $userPaymentId" }
                userPaymentId
            } else {
                if (account.isAdmin()) {
                    logger.info { "Admin user ${account.principal.name} is updating userPayment $userPaymentId" }
                    userPaymentId
                } else {
                    val userPaymentBelongsToUser = subscriptionService.userPaymentBelongsToUser(userPaymentId, account.principal.name)
                    if (userPaymentBelongsToUser) {
                        userPaymentId
                    } else {
                        logger.warn { "Tried to update userPayment $userPaymentId from account ${account.principal.name}. Only admin can update others account" }
                        null
                    }
                }
            }
        } else {
            serverExchange.statusCode = StatusCodes.BAD_REQUEST
            serverExchange.responseSender.send("No userPaymentId provided")
            null
        }
    }

}
