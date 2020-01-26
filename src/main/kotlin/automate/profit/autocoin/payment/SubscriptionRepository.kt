package automate.profit.autocoin.payment

interface SubscriptionRepository {
    fun getSubscriptionByCode(subscriptionCode: String): Subscription?
    fun getAllSubscriptions(): List<Subscription>
    fun getUserSubscriptionByCode(subscriptionCode: String, userAccountId: UserAccountId): UserSubscription?
    fun save(userSubscription: UserSubscription)
}