package automate.profit.autocoin.config

import automate.profit.autocoin.api.payment.*
import automate.profit.autocoin.api.undertow.ServerBuilder
import automate.profit.autocoin.oauth.client.AccessTokenAuthenticator
import automate.profit.autocoin.oauth.client.AccessTokenInterceptor
import automate.profit.autocoin.oauth.client.ClientCredentialsAccessTokenProvider
import automate.profit.autocoin.oauth.server.AccessTokenChecker
import automate.profit.autocoin.oauth.server.Oauth2AuthenticationMechanism
import automate.profit.autocoin.oauth.server.Oauth2BearerTokenAuthHandlerWrapper
import automate.profit.autocoin.payment.SubscriptionService
import automate.profit.autocoin.payment.listener.NoOpPaymentListener
import automate.profit.autocoin.payment.listener.PaymentListener
import automate.profit.autocoin.payment.listener.TelegramBot
import automate.profit.autocoin.payment.listener.TelegramPaymentListener
import automate.profit.autocoin.price.PriceService
import mu.KLogging
import okhttp3.OkHttpClient
import java.util.concurrent.Executors

class AppContext(private val appConfig: AppConfig) {
    companion object : KLogging()

    val objectMapper = ObjectMapperProvider().createObjectMapper()
    val httpClientWithoutAuthorization = OkHttpClient()

    val accessTokenChecker = AccessTokenChecker(
            httpClientWithoutAuthorization,
            objectMapper,
            oauthServerUrl = appConfig.oauth2ServerUrl,
            oauthClientId = appConfig.oauth2ClientId,
            oauthClientSecret = appConfig.oauth2ClientSecret
    )
    val accessTokenProvider = ClientCredentialsAccessTokenProvider(
            httpClient = httpClientWithoutAuthorization,
            objectMapper = objectMapper,
            oauthServerUrl = appConfig.oauth2ServerUrl,
            oauthClientId = appConfig.oauth2ClientId,
            oauthClientSecret = appConfig.oauth2ClientSecret
    )
    val oauth2AuthenticationMechanism = Oauth2AuthenticationMechanism(accessTokenChecker)
    val oauth2BearerTokenAuthHandlerWrapper = Oauth2BearerTokenAuthHandlerWrapper(oauth2AuthenticationMechanism)
    val accessTokenAuthenticator = AccessTokenAuthenticator(accessTokenProvider)
    val accessTokenInterceptor = AccessTokenInterceptor(accessTokenProvider)
    val oauth2HttpClient = OkHttpClient.Builder()
            .authenticator(accessTokenAuthenticator)
            .addInterceptor(accessTokenInterceptor)
            .build()

    val scheduledExecutorService = Executors.newScheduledThreadPool(3)

    val priceService = PriceService(
            exchangeMediatorApiUrl = appConfig.exchangeMediatorApiUrl,
            httpClient = oauth2HttpClient,
            objectMapper = objectMapper
    )

    val jdbi = JdbiProvider().createJdbi(
            jdbcUrl = appConfig.jdbcUrl,
            user = appConfig.dbUsername,
            password = appConfig.dbPassword
    )
    val subscriptionService = SubscriptionService(priceService = priceService, jdbi = jdbi)

    val getSubscriptionHandler = GetSubscriptionHandler(
            subscriptionService = subscriptionService,
            priceService = priceService,
            objectMapper = objectMapper,
            oauth2BearerTokenAuthHandlerWrapper = oauth2BearerTokenAuthHandlerWrapper
    )

    val getActiveUserSubscriptionHandler = GetActiveUserSubscriptionHandler(
            objectMapper = objectMapper,
            subscriptionService = subscriptionService,
            oauth2BearerTokenAuthHandlerWrapper = oauth2BearerTokenAuthHandlerWrapper
    )

    val createOrUpdateUserPaymentHandler = CreateOrUpdateUserPaymentHandler(
            subscriptionService = subscriptionService,
            paymentListener = getPaymentListener(),
            objectMapper = objectMapper,
            oauth2BearerTokenAuthHandlerWrapper = oauth2BearerTokenAuthHandlerWrapper
    )

    val setUserPaymentPaidHandler = SetUserPaymentPaidHandler(
            subscriptionService = subscriptionService,
            objectMapper = objectMapper,
            oauth2BearerTokenAuthHandlerWrapper = oauth2BearerTokenAuthHandlerWrapper
    )

    val getUserPaymentDetailsHandler = GetUserPaymentDetailsHandler(
            subscriptionService = subscriptionService,
            priceService = priceService,
            objectMapper = objectMapper,
            oauth2BearerTokenAuthHandlerWrapper = oauth2BearerTokenAuthHandlerWrapper
    )

    private fun getPaymentListener(): PaymentListener {
        return if ((appConfig.telegramBotToken?.isNotBlank() == true)
                && (appConfig.telegramBotUserName?.isNotBlank() == true)
                && appConfig.telegramPaymentNotificationChatId != null) {
            TelegramPaymentListener(
                    executorService = scheduledExecutorService,
                    telegramBot = TelegramBot(
                            botUserName = appConfig.telegramBotUserName,
                            botToken = appConfig.telegramBotToken,
                            chatId = appConfig.telegramPaymentNotificationChatId
                    )
            )
        } else {
            logger.warn { "Using NoOpPaymentListener" }
            NoOpPaymentListener()
        }
    }

    val server = ServerBuilder(
            appServerPort = appConfig.appServerPort,
            apiHandlers = listOf(
                    getSubscriptionHandler,
                    getActiveUserSubscriptionHandler,
                    createOrUpdateUserPaymentHandler,
                    setUserPaymentPaidHandler,
                    getUserPaymentDetailsHandler
            )
    ).build()

    fun start() {
        logger.info { "Configured subscriptions: ${subscriptionService.getAllSubscriptions()}" }
        logger.info { "Scheduling jobs" }

        logger.info { "Starting server" }
        server.start()
    }

}