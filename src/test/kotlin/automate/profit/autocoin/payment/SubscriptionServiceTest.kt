package automate.profit.autocoin.payment

import automate.profit.autocoin.LoggableArrayDeque
import automate.profit.autocoin.TestDb
import automate.profit.autocoin.price.PriceService
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS


class SubscriptionServiceTest {
    private lateinit var subscriptionService: SubscriptionService
    private val fixedClockMillis = listOf(10L, 50L, 60L)
    private lateinit var database: PostgreSQLContainer<*>
    private val sampleSubscription = TestData.subscription()
    private val userAccountId = "78ad8ba4-d7ef-44e7-8404-35f63558f749"
    private val sampleUserPayment = UserPayment(
            userAccountId = userAccountId,
            userPaymentId = "11d4955d-6b0d-4522-8ec4-749f86f4f60b",
            subscriptionCode = sampleSubscription.subscriptionCode,
            btcSenderAddress = "btc-address",
            ethSenderAddress = null,
            btcReceiverAddress = sampleSubscription.btcReceiverAddress,
            ethReceiverAddress = sampleSubscription.ethReceiverAddress,
            btcAmountRequired = 5.toBigDecimal(),
            ethAmountRequired = null,
            amountPaid = 5.toBigDecimal(),
            paymentStatus = PaymentStatus.PAID,
            insertTime = Instant.now()
    )

    private val priceService = mock<PriceService>().apply {
        whenever(this.getCurrencyAmountGivenUsdAmount(eq("BTC"), any())).thenReturn(0.45.toBigDecimal())
        whenever(this.getCurrencyAmountGivenUsdAmount(eq("ETH"), any())).thenReturn(0.15.toBigDecimal())
    }

    @BeforeEach
    fun setup() {
        val (jdbi, postgre) = TestDb.startDb()
        database = postgre
        val timeMillisQueue = LoggableArrayDeque(fixedClockMillis)

        subscriptionService = SubscriptionService(priceService, jdbi, currentTimeMillis = {
            timeMillisQueue.pop()
        })
        subscriptionService.addSbubscriptions(TestData.subscriptions())
    }

    @AfterEach
    fun cleanup() {
        database.close()
    }

    @Test
    fun shouldGetNoActiveSubscription() {
        assertThat(subscriptionService.getUserSubscriptionByCode(sampleSubscription.subscriptionCode, "non-existing-user-account-id")).isNull()
    }

    @Test
    fun shouldCreateUserPayment() {
        // when
        subscriptionService.createOrUpdateUnpaidUserPayment(
                userAccountId = userAccountId,
                subscriptionCode = sampleSubscription.subscriptionCode,
                btcSenderAddress = "btc-sender-address",
                ethSenderAddress = null
        )
        // then
        val foundPayment = subscriptionService.getLatestUnpaidUserPayment(userAccountId, sampleSubscription.subscriptionCode)
        assertThat(foundPayment).isNotNull
        assertThat(foundPayment!!.userAccountId).isEqualTo(userAccountId)
        assertThat(foundPayment.paymentStatus).isEqualTo(PaymentStatus.NEW)
        assertThat(foundPayment.subscriptionCode).isEqualTo(sampleSubscription.subscriptionCode)
        assertThat(foundPayment.btcSenderAddress).isEqualTo("btc-sender-address")
        assertThat(foundPayment.ethSenderAddress).isNull()
        assertThat(foundPayment.btcReceiverAddress).isEqualTo(sampleSubscription.btcReceiverAddress)
        assertThat(foundPayment.ethReceiverAddress).isEqualTo(sampleSubscription.ethReceiverAddress)
        assertThat(foundPayment.amountPaid).isNull()
        assertThat(foundPayment.btcAmountRequired).isEqualTo(0.45.toBigDecimal())
        assertThat(foundPayment.ethAmountRequired).isNull()
    }

    @Test
    fun shouldGetUserSubscription() {
        // given
        val createdUserPayment = subscriptionService.createOrUpdateUnpaidUserPayment(
                userAccountId = userAccountId,
                subscriptionCode = sampleSubscription.subscriptionCode,
                btcSenderAddress = "btc-sender-address",
                ethSenderAddress = null
        )
        subscriptionService.activateUserSubscription(sampleUserPayment.copy(userPaymentId = createdUserPayment!!.userPaymentId))
        // when
        val userSubscription = subscriptionService.getUserSubscriptionByCode(sampleSubscription.subscriptionCode, userAccountId)
        // then
        assertThat(userSubscription).isNotNull
        assertThat(userSubscription!!.userAccountId).isEqualTo(userAccountId)
        assertThat(userSubscription.subscriptionCode).isEqualTo(sampleSubscription.subscriptionCode)
        assertThat(userSubscription.validFrom.toEpochMilli()).isEqualTo(fixedClockMillis[1])
        assertThat(userSubscription.validTo.toEpochMilli()).isEqualTo(fixedClockMillis[1] + Duration.of(30, DAYS).toMillis())
    }

    @Test
    fun shouldSetUserSubscriptionValidDateAndFrom() {
        // given
        val createdUserPayment = subscriptionService.createOrUpdateUnpaidUserPayment(
                userAccountId = userAccountId,
                subscriptionCode = sampleSubscription.subscriptionCode,
                btcSenderAddress = "btc-sender-address",
                ethSenderAddress = null
        )
        subscriptionService.activateUserSubscription(sampleUserPayment.copy(userPaymentId = createdUserPayment!!.userPaymentId))
        subscriptionService.activateUserSubscription(sampleUserPayment.copy(userPaymentId = createdUserPayment.userPaymentId))
        // when
        val activeUserSubscription = subscriptionService.getUserSubscriptionByCode(sampleSubscription.subscriptionCode, userAccountId)
        // then
        assertThat(activeUserSubscription).isNotNull
        assertThat(activeUserSubscription!!.userAccountId).isEqualTo(userAccountId)
        assertThat(activeUserSubscription.subscriptionCode).isEqualTo(sampleSubscription.subscriptionCode)
        assertThat(activeUserSubscription.validFrom.toEpochMilli()).isEqualTo(fixedClockMillis[2])
        assertThat(activeUserSubscription.validTo.toEpochMilli()).isEqualTo(fixedClockMillis[2] + Duration.of(30, DAYS).toMillis())
    }


    @Test
    fun shouldActiveUserSubscriptionWhenUserPaymentPaid() {
        // given
        val createdUserPayment = subscriptionService.createOrUpdateUnpaidUserPayment(
                userAccountId = userAccountId,
                subscriptionCode = sampleSubscription.subscriptionCode,
                btcSenderAddress = "btc-sender-address",
                ethSenderAddress = null
        )
        subscriptionService.setPaymentAsPaidOrApproved(createdUserPayment!!.userPaymentId, PaymentStatus.PAID)
        // when
        val activeUserSubscription = subscriptionService.getUserSubscriptionByCode(subscriptionCode = sampleSubscription.subscriptionCode, userAccountId = userAccountId)
        // then
        assertThat(activeUserSubscription).isNotNull
        assertThat(activeUserSubscription!!.userAccountId).isEqualTo(userAccountId)
        assertThat(activeUserSubscription.subscriptionCode).isEqualTo(sampleSubscription.subscriptionCode)
        assertThat(activeUserSubscription.validFrom.toEpochMilli()).isEqualTo(fixedClockMillis[2])
        assertThat(activeUserSubscription.validTo.toEpochMilli()).isEqualTo(fixedClockMillis[2] + Duration.of(30, DAYS).toMillis())
    }

    @Test
    fun shouldFindNoPaymentToPayWhenUserPaymentPaid() {
        // given
        val createdUserPayment = subscriptionService.createOrUpdateUnpaidUserPayment(
                userAccountId = userAccountId,
                subscriptionCode = sampleSubscription.subscriptionCode,
                btcSenderAddress = "btc-sender-address",
                ethSenderAddress = null
        )
        subscriptionService.setPaymentAsPaidOrApproved(createdUserPayment!!.userPaymentId, PaymentStatus.PAID)
        // when
        val userPayment = subscriptionService.getLatestUnpaidUserPayment(userAccountId, sampleSubscription.subscriptionCode)
        // then
        assertThat(userPayment).isNull()
    }

}
