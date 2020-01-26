package automate.profit.autocoin

import java.lang.System.setProperty

/**
 * Copy this file to src/main and provide settings to run
 * Add limits when running process
-Xmx150M
-XX:+ExitOnOutOfMemoryError
 */
fun main() {
    setProperty("logging.level", "DEBUG")
    setProperty("OAUTH2_SERVER_URL", "http://localhost:9002")
    setProperty("EXCHANGE_MEDIATOR_API_URL", "http://localhost:9001")
    setProperty("APP_OAUTH_CLIENT_ID", "changeme")
    setProperty("APP_OAUTH_CLIENT_SECRET", "changeme")

    setProperty("JDBC_URL", "jdbc:postgresql://ip:port/postgres?ApplicationName=localdev-autocoin-payment-service")
    setProperty("DB_PASSWORD", "changeme")
    setProperty("DB_USERNAME", "changeme")

    // optional telegram settings to notify about user payments
//    setProperty("TELEGRAM_BOT_USERNAME", "changeme")
//    setProperty("TELEGRAM_BOT_TOKEN", "changeme")
//    setProperty("TELEGRAM_PAYMENT_NOTIFICATION_CHAT_ID", "changeme")
    main(emptyArray())
}
