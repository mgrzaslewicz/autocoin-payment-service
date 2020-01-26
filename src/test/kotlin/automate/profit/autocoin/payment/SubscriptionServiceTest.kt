package automate.profit.autocoin.payment

import automate.profit.autocoin.config.ObjectMapperProvider
import automate.profit.autocoin.payment.SubscriptionsFile.subscriptionCode
import automate.profit.autocoin.payment.SubscriptionsFile.subscriptionsFileContent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Duration
import java.time.temporal.ChronoUnit.DAYS
import java.util.*


class SubscriptionServiceTest {
    private val objectMapper = ObjectMapperProvider().createObjectMapper()
    private lateinit var tempFolder: TemporaryFolder
    private lateinit var subscriptionsFile: File
    private lateinit var subscriptionRepository: SubscriptionRepository
    private lateinit var subscriptionService: SubscriptionService
    private val fixedClockMillis = listOf(10L, 50L)

    @BeforeEach
    fun setup() {
        tempFolder = TemporaryFolder()
        tempFolder.create()
        subscriptionsFile = tempFolder.newFile()
        subscriptionsFile.writeText(subscriptionsFileContent())

        subscriptionRepository = FileSubscriptionRepository(
                subscriptionsFilePath = subscriptionsFile.absolutePath,
                objectMapper = objectMapper
        )
        val timeMillisQueue = ArrayDeque(fixedClockMillis)
        subscriptionService = SubscriptionService(subscriptionRepository = subscriptionRepository, currentTimeMillis = {
            timeMillisQueue.pop()
        })
    }

    @AfterEach
    fun cleanup() {
        tempFolder.delete()
    }

    @Test
    fun shouldGetNoActiveSubscription() {
        assertThat(subscriptionService.getUserSubscriptionByCode(subscriptionCode(), UserAccountId("non-existing-user-account-id"))).isNull()
    }

    @Test
    fun shouldGetActiveUserSubscription() {
        // given
        val userAccountId = UserAccountId("78ad8ba4-d7ef-44e7-8404-35f63558f749")
        val subscriptionCode = subscriptionCode()
        subscriptionService.setUserSubscriptionActive(subscriptionCode, userAccountId)
        // when
        val activeUserSubscription = subscriptionService.getUserSubscriptionByCode(subscriptionCode(), userAccountId)
        // then
        assertThat(activeUserSubscription).isNotNull
        assertThat(activeUserSubscription!!.userAccountId()).isEqualTo(userAccountId)
        assertThat(activeUserSubscription.subscriptionCode).isEqualTo(subscriptionCode)
        assertThat(activeUserSubscription.validFrom).isEqualTo(fixedClockMillis[0])
        assertThat(activeUserSubscription.validTo).isEqualTo(fixedClockMillis[0] + Duration.of(30, DAYS).toMillis())
    }

    @Test
    fun shouldUpdateActiveUserSubscription() {
        // given
        val userAccountId = UserAccountId("88ad8ba4-d7ef-44e7-8404-35f63558f749")
        val subscriptionCode = subscriptionCode()
        subscriptionService.setUserSubscriptionActive(subscriptionCode, userAccountId)
        subscriptionService.setUserSubscriptionActive(subscriptionCode, userAccountId)
        // when
        val activeUserSubscription = subscriptionService.getUserSubscriptionByCode(subscriptionCode(), userAccountId)
        // then
        assertThat(activeUserSubscription).isNotNull
        assertThat(activeUserSubscription!!.userAccountId()).isEqualTo(userAccountId)
        assertThat(activeUserSubscription.subscriptionCode).isEqualTo(subscriptionCode)
        assertThat(activeUserSubscription.validFrom).isEqualTo(fixedClockMillis[1])
        assertThat(activeUserSubscription.validTo).isEqualTo(fixedClockMillis[1] + Duration.of(30, DAYS).toMillis())
    }

}
