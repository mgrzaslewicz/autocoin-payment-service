package automate.profit.autocoin.payment.listener

import automate.profit.autocoin.payment.UserPayment

interface PaymentListener {
    fun onPaymentCreated(userPayment: UserPayment)
}

