package automate.profit.autocoin.payment

import java.math.BigDecimal

data class UserPayment(
        val userPaymentId: String,
        val userAccountId: String,
        val subscriptionCode: String,
        val btcSenderAddress: String?,
        val ethSenderAddress: String?,
        val btcAmountRequired: BigDecimal,
        val ethAmountRequired: BigDecimal,
        val amountPaid: BigDecimal?,
        val paymentStatus: PaymentStatus
)

inline class UserAccountId(val value: String)

data class UserSubscription(
        /**
         * objectMapper cannot deserialize inlined classes, use userAccountId() instead
         */
        val userAccountId: String,
        val subscriptionCode: String,
        val validFrom: Long,
        val validTo: Long
) {

    fun userAccountId() = UserAccountId(userAccountId)

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
        val usdAmount: Double,
        val description: String,
        val periodDays: Long
) {
    fun validate() {
        check(btcReceiverAddress != null || ethReceiverAddress != null) { "At least one currency address must be set" }
        check(usdAmount > 0) { "usdAmount must be > 0" }
        check(!description.isNullOrBlank()) { "description must be set" }
        check(periodDays > 0) { "periodDays must be > 0" }
    }
}
