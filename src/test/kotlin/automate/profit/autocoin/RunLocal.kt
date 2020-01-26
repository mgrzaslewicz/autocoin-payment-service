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
    setProperty("APP_DATA_PATH", "c:\\data\\tmp\\autocoin-data\\autocoin-payment-service")
//    setProperty("APP_OAUTH_CLIENT_ID", "changeme")
//    setProperty("APP_OAUTH_CLIENT_SECRET", "changeme")
    main(emptyArray())
}
