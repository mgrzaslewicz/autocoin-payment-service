package automate.profit.autocoin.config

import java.lang.System.getProperty
import java.lang.System.getenv

data class AppConfig(
        val appServerPort: Int = getPropertyThenEnv("APP_SERVER_PORT", "10023").toInt(),
        val oauth2ServerUrl: String = getPropertyThenEnv("OAUTH2_SERVER_URL", "https://users-apiv2.autocoin-trader.com"),
        val oauth2ClientId: String = getPropertyThenEnv("APP_OAUTH_CLIENT_ID", "autocoin-payment-service"),
        val oauth2ClientSecret: String = getPropertyThenEnv("APP_OAUTH_CLIENT_SECRET"),
        val exchangeMediatorApiUrl: String = getPropertyThenEnv("EXCHANGE_MEDIATOR_API_URL", "https://orders-api.autocoin-trader.com"),
        val jdbcUrl: String = getPropertyThenEnv("JDBC_URL"),
        val dbUsername: String = getPropertyThenEnv("DB_USERNAME"),
        val dbPassword: String = getPropertyThenEnv("DB_PASSWORD"),
        val telegramBotUserName: String? = getOptionalPropertyThenEnv("TELEGRAM_BOT_USERNAME"),
        val telegramBotToken: String? = getOptionalPropertyThenEnv("TELEGRAM_BOT_TOKEN"),
        val telegramPaymentNotificationChatId: Long? = getOptionalPropertyThenEnv("TELEGRAM_PAYMENT_NOTIFICATION_CHAT_ID")?.toLong()
) {
    override fun toString(): String {
        return "AppConfig(" +
                "appServerPort=$appServerPort, " +
                "oauth2ServerUrl='$oauth2ServerUrl', " +
                "oauth2ClientId='$oauth2ClientId', " +
                "oauth2ClientSecret provided='${!oauth2ClientSecret.isNullOrBlank()}', " +
                "exchangeMediatorApiUrl='$exchangeMediatorApiUrl', " +
                "jdbcUrl='$jdbcUrl', " +
                "dbUsername provided=${dbUsername.isNotBlank()}, " +
                "dbPassword provided='${dbPassword.isNotBlank()}', " +
                "telegramBotUserName provided=${!telegramBotUserName.isNullOrBlank()}, " +
                "telegramBotToken provided=${!telegramBotToken.isNullOrBlank()}, " +
                "telegramPaymentNotificationChatId provided=${telegramPaymentNotificationChatId != null}" +
                ")"
    }
}

fun loadConfig(): AppConfig {
    return AppConfig()
}

private fun getOptionalPropertyThenEnv(propertyName: String): String? {
    return getProperty(propertyName, getenv(propertyName))
}

private fun getPropertyThenEnv(propertyName: String): String {
    return getProperty(propertyName, getenv(propertyName))
}

private fun <T> getPropertyThenEnv(propertyName: String, existingPropertyParser: (String) -> T, defaultValue: T): T {
    val propertyValue = getProperty(propertyName, getenv(propertyName))
    return if (propertyValue != null) {
        existingPropertyParser(propertyValue)
    } else {
        defaultValue
    }
}

private fun getPropertyThenEnv(propertyName: String, defaultValue: String): String {
    return getProperty(propertyName, getenv(propertyName).orElse(defaultValue))
}

private fun String?.orElse(value: String) = this ?: value