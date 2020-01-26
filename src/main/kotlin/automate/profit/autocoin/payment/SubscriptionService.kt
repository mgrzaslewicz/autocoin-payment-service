package automate.profit.autocoin.payment

import java.time.Duration
import java.time.temporal.ChronoUnit

class SubscriptionService(
        private val subscriptionRepository: SubscriptionRepository,
        private val currentTimeMillis: () -> Long = System::currentTimeMillis) {

    fun getSubscriptionByCode(subscriptionCode: String): Subscription? {
        return subscriptionRepository.getSubscriptionByCode(subscriptionCode)
    }

    fun getAllSubscriptions() = subscriptionRepository.getAllSubscriptions()

    fun getUserSubscriptionByCode(subscriptionCode: String, userAccountId: UserAccountId): UserSubscription? {
        return subscriptionRepository.getUserSubscriptionByCode(subscriptionCode, userAccountId)
    }

    fun setUserSubscriptionActive(subscriptionCode: String, userAccountId: UserAccountId): Boolean {
        val subscription = getSubscriptionByCode(subscriptionCode)
        return if (subscription == null) {
            false
        } else {
            val validFrom = currentTimeMillis()
            val validTo = validFrom + Duration.of(subscription.periodDays, ChronoUnit.DAYS).toMillis()
            val userSubscription = getUserSubscriptionByCode(subscriptionCode, userAccountId)?.copy(validFrom = validFrom, validTo = validTo)
                    ?: UserSubscription(
                            userAccountId = userAccountId.value,
                            subscriptionCode = subscriptionCode,
                            validFrom = validFrom,
                            validTo = validTo
                    )
            subscriptionRepository.save(userSubscription)
            true
        }
    }

}