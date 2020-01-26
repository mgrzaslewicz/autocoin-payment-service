package automate.profit.autocoin.api.undertow

interface ApiController {
    fun apiHandlers(): List<ApiHandler>
}