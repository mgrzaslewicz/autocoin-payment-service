package automate.profit.autocoin.config

import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.sqlobject.kotlin.KotlinSqlObjectPlugin


class JdbiProvider {
    fun createJdbi(jdbcUrl: String, user: String, password: String): Jdbi {
        return Jdbi
                .create(jdbcUrl, user, password)
                .installPlugin(KotlinPlugin())
                .installPlugin(KotlinSqlObjectPlugin())
    }
}