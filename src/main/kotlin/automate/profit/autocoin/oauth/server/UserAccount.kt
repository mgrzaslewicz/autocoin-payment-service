package automate.profit.autocoin.oauth.server

import io.undertow.security.idm.Account
import java.security.Principal

fun CheckTokenDto.toAccount(): Account {
    return if (isClientOnly) {
        ClientAccount(clientName = clientTokenDto!!.clientId, scope = clientTokenDto.scope)
    } else {
        UserAccount(userTokenDto!!.userName, userTokenDto.userAccount.userAccountId, userTokenDto.authorities)

    }
}

interface OauthAccount: Account {
    val isClientOnly: Boolean
}

class UserAccount(
        private val userName: String,
        private val userAccountId: String,
        private val authorities: Set<String>
) : OauthAccount {
    override fun getRoles() = authorities
    override fun getPrincipal() = Principal { userAccountId }
    override val isClientOnly = false
}

class ClientAccount(
        private val clientName: String,
        private val scope: List<String>
) : OauthAccount {
    override fun getRoles() = scope.toSet()
    override fun getPrincipal() = Principal { clientName }
    override val isClientOnly = true
}