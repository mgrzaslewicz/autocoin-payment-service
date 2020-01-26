package automate.profit.autocoin.api.payment

import automate.profit.autocoin.FreePortFinder.getFreePort
import automate.profit.autocoin.MockSecurityContextHandlerWrapper
import automate.profit.autocoin.api.undertow.ServerBuilder
import automate.profit.autocoin.config.ObjectMapperProvider
import automate.profit.autocoin.oauth.server.UserAccount
import automate.profit.autocoin.payment.*
import automate.profit.autocoin.price.PriceService
import com.nhaarman.mockitokotlin2.whenever
import io.undertow.Undertow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Instant
import java.time.temporal.ChronoUnit

@ExtendWith(MockitoExtension::class)
class GetUserPaymentDetailsHandlerTest {

    private val httpClientWithoutAuthorization = OkHttpClient()
    private val objectMapper = ObjectMapperProvider().createObjectMapper()
    private val sampleSubscription = TestData.subscription()

    private var port: Int = 0
    private lateinit var server: Undertow

    @Mock
    private lateinit var subscriptionService: SubscriptionService

    @Mock
    private lateinit var priceService: PriceService

    @BeforeEach
    fun setup() {
        port = getFreePort()
        whenever(priceService.getCurrencyAmountGivenUsdAmount("BTC", sampleSubscription.usdAmount)).thenReturn(6.toBigDecimal())
        whenever(priceService.getCurrencyAmountGivenUsdAmount("ETH", sampleSubscription.usdAmount)).thenReturn(2.3.toBigDecimal())
        whenever(subscriptionService.getSubscriptionByCode(sampleSubscription.subscriptionCode)).thenReturn(sampleSubscription)
        val testedHandler = GetUserPaymentDetailsHandler(
                subscriptionService = subscriptionService,
                priceService = priceService,
                objectMapper = objectMapper,
                oauth2BearerTokenAuthHandlerWrapper = MockSecurityContextHandlerWrapper(
                        UserAccount(
                                userName = "does not matter",
                                userAccountId = "user-account-id-1",
                                authorities = emptySet()
                        ))
        )
        val serverBuilder = ServerBuilder(
                appServerPort = port,
                apiHandlers = listOf(testedHandler)
        )
        server = serverBuilder.build()
        server.start()
    }

    @AfterEach
    fun cleanup() {
        server.stop()
    }

    @Test
    fun shouldGetUserPaymentDetails() {
        // given
        val userPayment = UserPayment(
                userAccountId = "user-account-id-1",
                subscriptionCode = sampleSubscription.subscriptionCode,
                btcSenderAddress = "btc-sender-address",
                btcReceiverAddress = sampleSubscription.btcReceiverAddress,
                btcAmountRequired = 0.234.toBigDecimal(),
                ethReceiverAddress = null,
                ethSenderAddress = null,
                ethAmountRequired = 0.0543.toBigDecimal(),
                amountPaid = null,
                paymentStatus = PaymentStatus.NEW,
                insertTime = Instant.now()
        )
        val userSubscription = UserSubscription(
                lastUserPaymentId = userPayment.userPaymentId,
                userAccountId = userPayment.userAccountId,
                subscriptionCode = sampleSubscription.subscriptionCode,
                validFrom = Instant.now(),
                validTo = Instant.now().plus(30, ChronoUnit.DAYS)
        )
        whenever(subscriptionService.getLatestUnpaidUserPayment("user-account-id-1", sampleSubscription.subscriptionCode)).thenReturn(userPayment)
        whenever(subscriptionService.getUserSubscriptionByCode(sampleSubscription.subscriptionCode, "user-account-id-1")).thenReturn(userSubscription)
        // when
        val request = Request.Builder()
                .url("http://localhost:$port/user-payment-details?code=${sampleSubscription.subscriptionCode}")
                .build()
        val response = httpClientWithoutAuthorization.newCall(request).execute()
        // then
        assertThat(response.code).isEqualTo(200)
        response.use {
            val bodyString = it.body?.string()
            val userPaymentDetailsDto = objectMapper.readValue(bodyString, UserPaymentDetailsDto::class.java)
            assertThat(userPaymentDetailsDto).isEqualTo(UserPaymentDetailsDto(
                    subscription = sampleSubscription.toDto(btcAmount = "6.00000000", ethAmount = "2.30000000"),
                    userPayment = userPayment.toDto(),
                    userSubscription = userSubscription.toDto(Instant.now().toEpochMilli())
            ))
        }
    }

}
