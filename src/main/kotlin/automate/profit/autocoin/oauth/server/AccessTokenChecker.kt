package automate.profit.autocoin.oauth.server

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserAccountDto(
        val userAccountId: String
)

data class CheckTokenDto(
        val userTokenDto: UserTokenDto? = null,
        val clientTokenDto: ClientTokenDto? = null
) {
    val isClientOnly = userTokenDto == null
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserTokenDto(
        @JsonProperty("user_name")
        val userName: String,
        val authorities: Set<String>,
        val userAccount: UserAccountDto
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ClientTokenDto(
        val scope: List<String>,
        @JsonProperty("client_id")
        val clientId: String
)

class AccessTokenChecker(
        private val httpClient: OkHttpClient,
        private val objectMapper: ObjectMapper,
        private val oauthServerUrl: String,
        private val oauthClientId: String,
        private val oauthClientSecret: String
) {
    private val base64EncodedClientIdAndSecret = Base64.getEncoder().encodeToString("$oauthClientId:$oauthClientSecret".toByteArray())

    fun checkToken(bearerToken: String): CheckTokenDto? {
        val tokenCheckResponse = httpClient.newCall(Request.Builder()
                .post(FormBody.Builder().add("token", bearerToken).build())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Basic $base64EncodedClientIdAndSecret")
                .url("$oauthServerUrl/oauth/check_token")
                .build()
        ).execute()
        tokenCheckResponse.use {
            val responseBody = tokenCheckResponse.body?.string() // close response body
            return when (tokenCheckResponse.code) {
                401 -> null
                200 -> {
                    return if (responseBody?.contains("\"user_name\"") == false) {
                        val clientToken = objectMapper.readValue(responseBody, ClientTokenDto::class.java)
                        CheckTokenDto(clientTokenDto = clientToken)
                    } else {
                        val userToken = objectMapper.readValue(responseBody, UserTokenDto::class.java)
                        CheckTokenDto(userTokenDto = userToken)
                    }
                }
                else -> null
            }
        }
    }
}