package automate.profit.autocoin.config

import java.io.File
import java.lang.System.getProperty
import java.lang.System.getenv

data class AppConfig(
        val appServerPort: Int = getPropertyThenEnv("APP_SERVER_PORT", "10023").toInt(),
        val dataFolderPath: String = getPropertyThenEnv("APP_DATA_PATH", "data"),
        val subscriptionsFilePath: String = getPropertyThenEnv("APP_SUBSCRIPTION_FILE_PATH", dataFolderPath + File.separator + "subscriptions.json"),
        val oauth2ServerUrl: String = getPropertyThenEnv("OAUTH2_SERVER_URL", "https://users-apiv2.autocoin-trader.com"),
        val oauth2ClientId: String = getPropertyThenEnv("APP_OAUTH_CLIENT_ID", "autocoin-payment-service"),
        val oauth2ClientSecret: String = getPropertyThenEnv("APP_OAUTH_CLIENT_SECRET"),
        val exchangeMediatorApiUrl: String = getPropertyThenEnv("EXCHANGE_MEDIATOR_API_URL", "https://orders-api.autocoin-trader.com")
)

fun loadConfig(): AppConfig {
    return AppConfig()
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