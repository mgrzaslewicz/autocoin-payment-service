package automate.profit.autocoin.api.payment

import automate.profit.autocoin.payment.PaymentStatus
import automate.profit.autocoin.payment.UserPayment
import automate.profit.autocoin.payment.UserSubscription
import java.math.BigDecimal
import java.math.RoundingMode

fun BigDecimal.asScaledString() = setScale(8, RoundingMode.HALF_EVEN).toPlainString()

fun UserSubscription.toDto(currentTimeMillis: Long) = UserSubscriptionDto(
        userAccountId = userAccountId,
        subscriptionCode = subscriptionCode,
        validFrom = validFrom.toEpochMilli(),
        validTo = validTo.toEpochMilli(),
        active = validTo.toEpochMilli() < currentTimeMillis
)

data class UserPaymentDto(
        val userAccountId: String,
        val subscriptionCode: String,
        val paymentStatus: PaymentStatus,
        val btcSenderAddress: String?,
        val ethSenderAddress: String?,
        val btcAmountRequired: String?,
        val ethAmountRequired: String?,
        val amountPaid: String?
)

fun UserPayment.toDto() = UserPaymentDto(
        userAccountId = userAccountId,
        subscriptionCode = subscriptionCode,
        paymentStatus = paymentStatus,
        btcSenderAddress = btcSenderAddress,
        ethSenderAddress = ethSenderAddress,
        btcAmountRequired = btcAmountRequired?.asScaledString(),
        ethAmountRequired = ethAmountRequired?.asScaledString(),
        amountPaid = amountPaid?.asScaledString()
)

data class UserPaymentDetailsDto(
        val subscription: SubscriptionDto,
        val userPayment: UserPaymentDto?,
        val userSubscription: UserSubscriptionDto?
)
