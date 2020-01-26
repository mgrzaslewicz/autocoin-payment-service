package automate.profit.autocoin.payment

object TestData {
    fun subscriptions() = listOf(subscription())

    fun subscription() = Subscription(
            subscriptionCode = "some-service",
            btcReceiverAddress = "0000000000000000000f557ed547d8c8102951fdeb93d2677b20d608be0085a2",
            ethReceiverAddress = "0x32Be343B94f860124dD4fEe278FDCBD38C102D89",
            usdAmount = 30.5.toBigDecimal(),
            description = "30 day access to some service",
            periodDays = 30
    )

}