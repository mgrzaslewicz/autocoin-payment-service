package automate.profit.autocoin.api.payment

import automate.profit.autocoin.FreePortFinder.getFreePort
import automate.profit.autocoin.NoopHttpHandlerWrapper
import automate.profit.autocoin.api.undertow.ServerBuilder
import automate.profit.autocoin.config.ObjectMapperProvider
import automate.profit.autocoin.payment.SubscriptionService
import automate.profit.autocoin.payment.TestData
import automate.profit.autocoin.price.PriceService
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import okhttp3.OkHttpClient
import okhttp3.Request
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal

@ExtendWith(MockitoExtension::class)
class GetSubscriptionHandlerTest {

    private val httpClientWithoutAuthorization = OkHttpClient()
    private val objectMapper = ObjectMapperProvider().createObjectMapper()
    private val sampleSubscription = TestData.subscription()

    @Mock
    private lateinit var priceService: PriceService

    @BeforeEach
    fun setup() {
        whenever(priceService.getCurrencyAmountGivenUsdAmount("BTC", BigDecimal("30.5"))).thenReturn(BigDecimal("0.0005"))
        whenever(priceService.getCurrencyAmountGivenUsdAmount("ETH", BigDecimal("30.5"))).thenReturn(BigDecimal("0.0002"))
    }


    @Test
    fun shouldGetSubscriptionByCode() {
        // given
        val subscriptionService = mock<SubscriptionService>().apply { whenever(this.getSubscriptionByCode(sampleSubscription.subscriptionCode)).thenReturn(sampleSubscription) }
        val subscriptionHandler = GetSubscriptionHandler(
                subscriptionService = subscriptionService,
                priceService = priceService,
                objectMapper = objectMapper,
                oauth2BearerTokenAuthHandlerWrapper = NoopHttpHandlerWrapper()
        )
        val port = getFreePort()
        val serverBuilder = ServerBuilder(
                appServerPort = port,
                apiHandlers = listOf(subscriptionHandler)
        )
        val server = serverBuilder.build()
        server.start()
        // when
        val request = Request.Builder()
                .url("http://localhost:$port/subscription?code=${sampleSubscription.subscriptionCode}")
                .get()
                .build()
        val response = httpClientWithoutAuthorization.newCall(request).execute()
        // then
        assertThat(response.code).isEqualTo(200)
        assertThat(response.header("Content-Type")).isEqualTo("application/json")
        response.use {
            val subscriptionDto = objectMapper.readValue(it.body?.string(), SubscriptionDto::class.java)
            assertThat(subscriptionDto).isEqualTo(SubscriptionDto(
                    btcReceiverAddress = "0000000000000000000f557ed547d8c8102951fdeb93d2677b20d608be0085a2",
                    ethReceiverAddress = "0x32Be343B94f860124dD4fEe278FDCBD38C102D89",
                    btcAmount = "0.00050000",
                    ethAmount = "0.00020000",
                    usdAmount = "30.5",
                    description = "30 day access to some service"
            ))
        }
        server.stop()
    }
}