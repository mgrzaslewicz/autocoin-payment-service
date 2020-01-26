package automate.profit.autocoin.payment

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogging
import java.io.File
import java.nio.file.Path


data class UserFileJsonV1(
        val userSubscriptions: Set<UserSubscription>,
        val userPayments: List<UserPayment>
)

class FileSubscriptionRepository(
        private val subscriptionsFilePath: String,
        private val objectMapper: ObjectMapper
) : SubscriptionRepository {
    companion object : KLogging()

    private val subscriptionsFile = File(subscriptionsFilePath)
    private val subscriptionDirectory: Path

    init {
        check(subscriptionsFile.exists()) { "Subscriptions file $subscriptionsFilePath does not exist" }
        subscriptionDirectory = subscriptionsFile.toPath().parent
    }

    private fun UserAccountId.userFileName() = "user-$value.json"
    private fun UserAccountId.userFile() = subscriptionDirectory.resolve(userFileName()).toFile()
    private fun UserAccountId.userFileJson(): UserFileJsonV1? {
        return try {
            val userFile = userFile()
            return if (userFile.exists()) {
                val userFileJson = objectMapper.readValue(userFile.readText(), UserFileJsonV1::class.java)
                userFileJson
            } else null
        } catch (e: Exception) {
            logger.error(e) { "Could not read user file ${this.userFileName()}" }
            null
        }
    }


    override fun getSubscriptionByCode(subscriptionCode: String): Subscription? {
        return getAllSubscriptions().firstOrNull { it.subscriptionCode == subscriptionCode }
    }

    override fun getAllSubscriptions(): List<Subscription> {
        return objectMapper.readValue(subscriptionsFile.readText(), Array<Subscription>::class.java)
                .toList()
                .onEach { it.validate() }
                .also { check(it.isNotEmpty()) { "No subscriptions configured" } }
    }

    override fun getUserSubscriptionByCode(subscriptionCode: String, userAccountId: UserAccountId): UserSubscription? {
        val userFileJson = userAccountId.userFileJson()
        return userFileJson?.userSubscriptions?.firstOrNull { it.subscriptionCode == subscriptionCode }
    }

    override fun save(userSubscription: UserSubscription) {
        val userAccountId = userSubscription.userAccountId()
        val userFileJson = userAccountId.userFileJson()
        if (userFileJson == null) {
            userAccountId.userFile().writeText(objectMapper.writeValueAsString(UserFileJsonV1(
                    userSubscriptions = setOf(userSubscription),
                    userPayments = listOf()
            )))
        } else {
            val newSubscriptions = userFileJson.userSubscriptions.toMutableSet()
            newSubscriptions.remove(userSubscription)
            newSubscriptions.add(userSubscription)
            userAccountId.userFile().writeText(objectMapper.writeValueAsString(UserFileJsonV1(
                    userSubscriptions = newSubscriptions,
                    userPayments = userFileJson.userPayments
            )))
        }
    }

}