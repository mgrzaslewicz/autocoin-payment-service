package automate.profit.autocoin.api.payment

import automate.profit.autocoin.payment.PaymentStatus

data class PaymentStatusChangeDto(
        val paymentStatus: PaymentStatus,
        val changedTime: Long
)