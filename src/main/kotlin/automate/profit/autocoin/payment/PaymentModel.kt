package automate.profit.autocoin.payment

import automate.profit.autocoin.payment.PaymentStatus.APPROVED_MANUALLY
import automate.profit.autocoin.payment.PaymentStatus.PAID
import java.math.BigDecimal
import java.time.Instant
import java.util.*

data class UserPayment(
        val userPaymentId: String = UUID.randomUUID().toString(),
        val userAccountId: String,
        val subscriptionCode: String,
        val btcSenderAddress: String?,
        val ethSenderAddress: String?,
        val btcReceiverAddress: String?,
        val ethReceiverAddress: String?,
        val btcAmountRequired: BigDecimal?,
        val ethAmountRequired: BigDecimal?,
        val amountPaid: BigDecimal?,
        val paymentStatus: PaymentStatus,
        val insertTime: Instant,
        val updateTime: Instant? = null
) {
    fun isPaidOrApproved(): Boolean {
        return paymentStatus == PAID || paymentStatus == APPROVED_MANUALLY
    }

    fun validate() {
        check((btcSenderAddress != null) xor (ethSenderAddress != null)) { "One of btc/eth sender address must be set" }
        if (btcSenderAddress != null) {
            check(btcAmountRequired != null) { "btcAmountRequired must be set" }
            check(btcReceiverAddress != null) { "btcReceiverAddress must be set" }
        }
        if (ethSenderAddress != null) {
            check(ethAmountRequired != null) { "ethAmountRequired must be set" }
            check(ethReceiverAddress != null) { "ethReceiverAddress must be set" }
        }
        if (paymentStatus == PAID) {
            check(amountPaid != null) { "amountPaid must be set" }
            check(amountPaid.toDouble() > 0) { "amountPaid must be > 0" }
        }
    }
}


data class UserSubscription(
        val lastUserPaymentId: String,
        val userAccountId: String,
        val subscriptionCode: String,
        val validFrom: Instant,
        val validTo: Instant,
        val insertTime: Instant = Instant.now(),
        val updateTime: Instant? = null
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserSubscription

        if (userAccountId != other.userAccountId) return false
        if (subscriptionCode != other.subscriptionCode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userAccountId.hashCode()
        result = 31 * result + subscriptionCode.hashCode()
        return result
    }
}

enum class PaymentStatus {
    NEW,
    PAID,
    APPROVED_MANUALLY
}

/**
 * Represents access to product that user pays for
 */
data class Subscription(
        val subscriptionCode: String,
        val btcReceiverAddress: String?,
        val ethReceiverAddress: String?,
        val usdAmount: BigDecimal,
        val description: String,
        val periodDays: Long
) {
    fun validate() {
        check(btcReceiverAddress != null || ethReceiverAddress != null) { "At least one currency address must be set" }
        check(usdAmount > BigDecimal.ZERO) { "usdAmount must be > 0" }
        check(!description.isNullOrBlank()) { "description must be set" }
        check(periodDays > 0) { "periodDays must be > 0" }
    }
}
