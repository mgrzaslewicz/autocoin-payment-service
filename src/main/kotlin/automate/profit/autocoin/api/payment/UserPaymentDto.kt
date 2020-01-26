package automate.profit.autocoin.api.payment

data class UserPaymentDto(
        val userAccountId: String,
        val paymentDescription: String,
        val paymentStatusChanges: List<PaymentStatusChangeDto>
)