package automate.profit.autocoin.payment

import automate.profit.autocoin.payment.PaymentStatus.*
import automate.profit.autocoin.price.PriceService
import mu.KLogging
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.useExtensionUnchecked
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class SubscriptionService(
        private val priceService: PriceService,
        private val jdbi: Jdbi,
        private val currentTimeMillis: () -> Long = System::currentTimeMillis) {
    companion object : KLogging()

    fun getSubscriptionByCode(subscriptionCode: String): Subscription? {
        var result: Subscription? = null
        jdbi.useExtensionUnchecked(SubscriptionRepository::class) { repository ->
            result = repository.getSubscriptionByCode(subscriptionCode)
        }
        return result
    }

    fun getAllSubscriptions(): List<Subscription> {
        var result: List<Subscription> = emptyList()
        jdbi.useExtensionUnchecked(SubscriptionRepository::class) { repository ->
            result = repository.getAllSubscriptions()
        }
        return result
    }

    fun getUserSubscriptionByCode(subscriptionCode: String, userAccountId: String): UserSubscription? {
        var result: UserSubscription? = null
        jdbi.useExtensionUnchecked(SubscriptionRepository::class) { repository ->
            result = repository.getUserSubscriptionByCode(subscriptionCode, userAccountId)
        }
        return result
    }

    /**
     * Looks for last unpaid user payment or creates new one.
     * BTC and ETH amount is calculated and updated  based on current USD price
     */
    fun createOrUpdateUnpaidUserPayment(userAccountId: String, subscriptionCode: String, btcSenderAddress: String?, ethSenderAddress: String?): UserPayment? {
        logger.info { "Creating or updating unpaid user payment userAccountId=$userAccountId, subscriptionCode=$subscriptionCode, btcSenderAddress=$btcSenderAddress, ethSenderAddress=$ethSenderAddress" }
        if (btcSenderAddress == null && ethSenderAddress == null) {
            logger.error { "One of sender address needs to be set for user $userAccountId and subscription $subscriptionCode" }
            return null
        }
        val subscription = getSubscriptionByCode(subscriptionCode)
        if (subscription == null) {
            logger.error { "Subscription $subscriptionCode does not exist" }
            return null
        }
        var btcAmountRequired: BigDecimal? = null
        var ethAmountRequired: BigDecimal? = null
        if (btcSenderAddress != null) {
            btcAmountRequired = priceService.getCurrencyAmountGivenUsdAmount("BTC", subscription.usdAmount)
        } else {
            ethAmountRequired = priceService.getCurrencyAmountGivenUsdAmount("ETH", subscription.usdAmount)
        }
        val existingUserPayment = getLatestUnpaidUserPayment(userAccountId, subscriptionCode)
        return if (existingUserPayment != null) {
            val updatedPayment = existingUserPayment.copy(
                    btcSenderAddress = btcSenderAddress,
                    ethSenderAddress = ethSenderAddress,
                    btcAmountRequired = btcAmountRequired,
                    ethAmountRequired = ethAmountRequired,
                    btcReceiverAddress = subscription.btcReceiverAddress,
                    ethReceiverAddress = subscription.ethReceiverAddress,
                    updateTime = Instant.ofEpochMilli(currentTimeMillis())
            )
            logger.info { "Updating $existingUserPayment -> $updatedPayment" }
            jdbi.useExtensionUnchecked(SubscriptionRepository::class) { repository ->
                repository.update(existingUserPayment, subscription)
            }
            updatedPayment
        } else {
            val newUserPayment = UserPayment(
                    userAccountId = userAccountId,
                    subscriptionCode = subscriptionCode,
                    btcSenderAddress = btcSenderAddress,
                    ethSenderAddress = ethSenderAddress,
                    btcAmountRequired = btcAmountRequired,
                    ethAmountRequired = ethAmountRequired,
                    btcReceiverAddress = subscription.btcReceiverAddress,
                    ethReceiverAddress = subscription.ethReceiverAddress,
                    amountPaid = null,
                    paymentStatus = NEW,
                    insertTime = Instant.ofEpochMilli(currentTimeMillis())
            )
            logger.info { "Creating new user payment $newUserPayment" }
            jdbi.useExtensionUnchecked(SubscriptionRepository::class) { repository ->
                repository.create(newUserPayment)
            }
            newUserPayment
        }
    }

    fun userPaymentBelongsToUser(userPaymentId: String, userAccountId: String): Boolean {
        var result = false
        jdbi.useExtensionUnchecked(SubscriptionRepository::class) { repository ->
            val userPayment = repository.getUserPaymentByUserPaymentId(userPaymentId)
            result = userPayment?.userAccountId == userAccountId
        }
        return result
    }

    fun getLatestUnpaidUserPayment(userAccountId: String, subscriptionCode: String): UserPayment? {
        var result: UserPayment? = null
        jdbi.useExtensionUnchecked(SubscriptionRepository::class) { repository ->
            result = repository.getLatestUnpaidUserPayment(subscriptionCode, userAccountId)
        }
        return result
    }

    fun activateUserSubscription(userPayment: UserPayment): Boolean {
        logger.info { "Setting user ${userPayment.userAccountId} subscription ${userPayment.subscriptionCode} active" }
        if (!userPayment.isPaidOrApproved()) {
            logger.warn { "Cannot create or update user subscription for userPayment with wrong status $userPayment" }
            return false
        }
        val subscription = getSubscriptionByCode(userPayment.subscriptionCode)
        if (subscription == null) {
            logger.error { "Subscription with code ${userPayment.subscriptionCode} does not exist" }
            return false
        }

        val currentTime = Instant.ofEpochMilli(currentTimeMillis())
        val validTo = Instant.ofEpochMilli(currentTime.toEpochMilli() + Duration.of(subscription.periodDays, ChronoUnit.DAYS).toMillis())

        val existingUserSubscription = getUserSubscriptionByCode(subscription.subscriptionCode, userPayment.userAccountId)
        if (existingUserSubscription != null) {
            val updatedSubscription = existingUserSubscription.copy(
                    validFrom = currentTime,
                    validTo = validTo,
                    updateTime = currentTime
            )
            logger.info { "Updating existing userSubscription $existingUserSubscription ->  $updatedSubscription" }
            jdbi.useExtensionUnchecked(SubscriptionRepository::class) { repository ->
                repository.update(updatedSubscription)
            }
        } else {
            val newUserSubscription = UserSubscription(
                    lastUserPaymentId = userPayment.userPaymentId,
                    userAccountId = userPayment.userAccountId,
                    subscriptionCode = userPayment.subscriptionCode,
                    validFrom = currentTime,
                    validTo = validTo,
                    insertTime = currentTime
            )
            logger.info { "Creating new userSubscription $newUserSubscription" }
            jdbi.useExtensionUnchecked(SubscriptionRepository::class) { repository ->
                repository.insert(newUserSubscription)
            }
        }
        return true
    }

    fun addSubscription(subscription: Subscription): Boolean {
        logger.info { "Adding subscription $subscription" }
        subscription.validate()
        val existingSubscription = getSubscriptionByCode(subscription.subscriptionCode)
        return if (existingSubscription != null) {
            logger.error { "Subscription with code ${subscription.subscriptionCode} already exists" }
            false
        } else {
            jdbi.useExtensionUnchecked(SubscriptionRepository::class) { repository ->
                repository.insert(subscription)
            }
            logger.info { "Subscription $subscription added" }
            true
        }
    }

    fun addSbubscriptions(subscriptions: List<Subscription>) {
        subscriptions.forEach { addSubscription(it) }
    }

    fun setPaymentAsPaidOrApproved(userPaymentId: String, newPaymentStatus: PaymentStatus): UserPayment {
        check(newPaymentStatus == APPROVED_MANUALLY || newPaymentStatus == PAID) { "Wrong paymentStatus given, APPROVED_MANUALLY or PAID is valid" }
        var updatedUserPayment: UserPayment? = null
        jdbi.useExtensionUnchecked(SubscriptionRepository::class) { repository ->
            val userPayment = repository.getUserPaymentByUserPaymentId(userPaymentId)!!
            updatedUserPayment = userPayment.copy(
                    paymentStatus = newPaymentStatus,
                    updateTime = Instant.ofEpochMilli(currentTimeMillis())
            )
        }
        jdbi.useExtensionUnchecked(SubscriptionRepository::class) { repository ->
            repository.update(updatedUserPayment!!)
        }
        activateUserSubscription(updatedUserPayment!!)
        return updatedUserPayment!!
    }
}