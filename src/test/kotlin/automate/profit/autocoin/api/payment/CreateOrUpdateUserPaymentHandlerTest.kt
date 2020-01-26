package automate.profit.autocoin.api.payment

import automate.profit.autocoin.FreePortFinder.getFreePort
import automate.profit.autocoin.MockSecurityContextHandlerWrapper
import automate.profit.autocoin.api.undertow.ServerBuilder
import automate.profit.autocoin.config.ObjectMapperProvider
import automate.profit.autocoin.oauth.server.UserAccount
import automate.profit.autocoin.payment.PaymentStatus
import automate.profit.autocoin.payment.SubscriptionService
import automate.profit.autocoin.payment.TestData
import automate.profit.autocoin.payment.UserPayment
import automate.profit.autocoin.payment.listener.NoOpPaymentListener
import com.nhaarman.mockitokotlin2.whenever
import io.undertow.Undertow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class CreateOrUpdateUserPaymentHandlerTest {

    private val httpClientWithoutAuthorization = OkHttpClient()
    private val objectMapper = ObjectMapperProvider().createObjectMapper()
    private val sampleSubscription = TestData.subscription()

    private var port: Int = 0
    private lateinit var server: Undertow

    @Mock
    private lateinit var subscriptionService: SubscriptionService

    @BeforeEach
    fun setup() {
        port = getFreePort()
        val testedHandler = CreateOrUpdateUserPaymentHandler(
                subscriptionService = subscriptionService,
                paymentListener = NoOpPaymentListener(),
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
    fun shouldRejectAttemptToModifyOthersAccount() {
        // when
        val request = Request.Builder()
                .url("http://localhost:$port/user-payment")
                .post("""
{
  "userAccountId": "user-account-id-1",                    
  "subscriptionCode": "${sampleSubscription.subscriptionCode}",                    
  "btcSenderAddress": "btc-sender-address-1"
}
                """.trimIndent().toRequestBody())
                .build()
        val response = httpClientWithoutAuthorization.newCall(request).execute()
        // then
        assertThat(response.code).isEqualTo(400)
    }

    @Test
    fun shouldCreateUserPayment() {
        // given
        subscriptionService.apply {
            whenever(this.createOrUpdateUnpaidUserPayment(
                    userAccountId = "user-account-id-1",
                    subscriptionCode = sampleSubscription.subscriptionCode,
                    btcSenderAddress = "btc-sender-address-1",
                    ethSenderAddress = null
            )).thenReturn(UserPayment(
                    userAccountId = "user-account-id-1",
                    subscriptionCode = sampleSubscription.subscriptionCode,
                    btcSenderAddress = "btc-sender-address-1",
                    ethSenderAddress = null,
                    btcReceiverAddress = "btc-receiver-address-1",
                    ethReceiverAddress = null,
                    btcAmountRequired = 23.toBigDecimal(),
                    ethAmountRequired = null,
                    amountPaid = null,
                    paymentStatus = PaymentStatus.NEW,
                    insertTime = Instant.now()
            ))
        }
        // when
        val request = Request.Builder()
                .url("http://localhost:$port/user-payment")
                .post("""
{
  "userAccountId": "user-account-id-1",                    
  "subscriptionCode": "${sampleSubscription.subscriptionCode}",                    
  "btcSenderAddress": "btc-sender-address-1"
}
                """.trimIndent().toRequestBody())
                .build()
        val response = httpClientWithoutAuthorization.newCall(request).execute()
        // then
        assertThat(response.code).isEqualTo(200)
        response.use {
            val userPaymentDto = objectMapper.readValue(it.body?.string(), UserPaymentDto::class.java)
            assertThat(userPaymentDto).isEqualTo(UserPaymentDto(
                    userAccountId = "user-account-id-1",
                    subscriptionCode = sampleSubscription.subscriptionCode,
                    paymentStatus = PaymentStatus.NEW,
                    btcSenderAddress = "btc-sender-address-1",
                    ethSenderAddress = null,
                    btcAmountRequired = "23.00000000",
                    ethAmountRequired = null,
                    amountPaid = null
            ))
        }
    }
}