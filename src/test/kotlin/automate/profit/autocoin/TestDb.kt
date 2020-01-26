package automate.profit.autocoin

import automate.profit.autocoin.config.JdbiProvider
import org.jdbi.v3.core.Jdbi
import org.testcontainers.containers.PostgreSQLContainer

data class Db(
        val jdbi: Jdbi,
        val postgre: PostgreSQLContainer<*>
)

object TestDb {

    fun startDb(): Db {
        val dbPassword = "dbPpassword123"
        val dbUser = "dbUser123"
        val jdbiProvider = JdbiProvider()
        val database = PostgreSQLContainer<Nothing>("postgres:11.0")
                .apply {
                    withUsername(dbUser)
                    withPassword(dbPassword)
                    withInitScript("schema.sql")
                    // use in memory storage for faster execuction
                    withTmpFs(mapOf("/var/lib/postgresql/data" to "rw"))
                }
        database.start()
        val jdbi = jdbiProvider.createJdbi(database.jdbcUrl, dbUser, dbPassword)
        return Db(jdbi, database)
    }


}