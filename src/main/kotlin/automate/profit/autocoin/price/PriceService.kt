package automate.profit.autocoin.price

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap


data class CurrencyPriceDto(
        val price: Double,
        val baseCurrency: String,
        val counterCurrency: String
)

class PriceService(private val exchangeMediatorApiUrl: String,
                   private val httpClient: OkHttpClient,
                   private val objectMapper: ObjectMapper,
                   private val maxPriceCacheAgeMs: Long = Duration.of(1, ChronoUnit.HOURS).toMillis(),
                   private val currentTimeMillis: () -> Long = System::currentTimeMillis) {

    private data class ValueWithTimestamp(
            val value: BigDecimal,
            val calculatedAtMillis: Long
    )

    private val usdPriceCache = ConcurrentHashMap<String, ValueWithTimestamp>()

    companion object : KLogging()

    fun getUsdPrice(currencyCode: String): BigDecimal {
        return if (currencyCode == "USD") {
            return BigDecimal.ONE
        } else {
            fetchPrice(currencyCode)
            usdPriceCache.getValue(currencyCode).value
        }
    }

    fun getCurrencyAmountGivenUsdAmount(currencyCode: String, usdAmount: BigDecimal): BigDecimal {
        return usdAmount.setScale(8).divide(getUsdPrice(currencyCode), RoundingMode.HALF_EVEN)
    }

    private fun fetchPrice(currencyCode: String) {
        synchronized(usdPriceCache) {
            if (usdPriceCache.containsKey(currencyCode)) {
                val valueWithTimestamp = usdPriceCache[currencyCode]!!
                if (isOlderThanMaxCacheAge(valueWithTimestamp.calculatedAtMillis)) {
                    usdPriceCache.remove(currencyCode)
                }
            }

            usdPriceCache.computeIfAbsent(currencyCode) {
                ValueWithTimestamp(
                        calculatedAtMillis = currentTimeMillis(),
                        value = fetchUsdPrice(currencyCode)
                )
            }
        }
    }

    fun getUsdAmountGivenCurrencyAmount(currencyCode: String, amount: BigDecimal): BigDecimal {
        if (currencyCode == "USD") {
            return amount
        }
        fetchPrice(currencyCode)
        val price = usdPriceCache.getValue(currencyCode).value
        return amount.multiply(price)
    }


    private fun isOlderThanMaxCacheAge(calculatedAtMillis: Long): Boolean {
        return currentTimeMillis() - calculatedAtMillis > maxPriceCacheAgeMs
    }

    private fun fetchUsdPrice(currencyCode: String): BigDecimal {
        logger.info { "Fetching price for $currencyCode" }
        val request = Request.Builder()
                .url("$exchangeMediatorApiUrl/prices/USD?currencyCodes=${currencyCode}")
                .get()
                .build()
        val priceResponse = httpClient.newCall(request).execute()
        priceResponse.use {
            check(priceResponse.code == 200) { "Could not get price for $currencyCode/USD, code=${priceResponse.code}" }
            val priceDto = objectMapper.readValue(priceResponse.body?.string(), Array<CurrencyPriceDto>::class.java)
            check(priceDto.size == 1) { "No required price in response for $currencyCode" }
            return priceDto.first().price.toBigDecimal()
        }
    }

}