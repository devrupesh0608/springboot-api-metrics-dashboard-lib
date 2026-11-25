package com.apimetric.storage.mysql

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.springframework.jdbc.core.JdbcTemplate
import java.io.IOException
import java.net.URI
import java.nio.charset.StandardCharsets
import java.sql.SQLException
import java.util.Objects
import java.util.logging.Logger
import javax.sql.DataSource

/** Library-owned MySQL pool. This object is not registered as a Spring [DataSource] bean. */
class MysqlMonitoringDatabase(
    settings: MysqlConnectionSettings?,
) : AutoCloseable {
    private val pool: HikariDataSource
    private val jdbc: JdbcTemplate

    init {
        val checked = Objects.requireNonNull(settings, "settings")!!
        val config = HikariConfig()
        config.setPoolName("api-metric")
        config.setJdbcUrl(checked.jdbcUrl)
        config.setUsername(checked.username)
        config.setPassword(checked.password)
        config.setDriverClassName(checked.driverClassName)
        config.setMaximumPoolSize(checked.maximumPoolSize)
        config.setMinimumIdle(checked.minimumIdle)
        config.setConnectionTimeout(checked.connectionTimeoutMs)
        config.setValidationTimeout(Math.min(checked.connectionTimeoutMs, 2000L))
        config.setConnectionInitSql("SET time_zone = '+00:00'")
        config.setInitializationFailTimeout(checked.connectionTimeoutMs)
        val opened = HikariDataSource(config)
        try {
            migrate(opened)
        } catch (ex: RuntimeException) {
            opened.close()
            throw IllegalStateException(initializationMessage(checked.jdbcUrl), ex)
        }
        pool = opened
        val template = JdbcTemplate(opened)
        template.setQueryTimeout(5)
        jdbc = template
    }

    fun jdbcTemplate(): JdbcTemplate = jdbc

    fun dataSource(): DataSource = pool

    override fun close() {
        pool.close()
    }

    companion object {
        private val log = Logger.getLogger(MysqlMonitoringDatabase::class.java.name)

        private val SCHEMA_SCRIPTS =
            arrayOf(
                "/db/api-metric/migration/V1__create_monitoring_schema.sql",
                "/db/api-metric/migration/V2__add_request_headers.sql",
            )

        private fun initializationMessage(jdbcUrl: String?): String =
            "API Metric could not initialize its MySQL schema at " +
                safeTarget(jdbcUrl) +
                ". Check that the monitoring database is reachable. The password is omitted from this" +
                " message."

        @JvmStatic
        fun safeTarget(jdbcUrl: String?): String {
            if (jdbcUrl == null || jdbcUrl.isBlank()) {
                return "an unspecified JDBC URL"
            }
            var withoutPrefix =
                if (jdbcUrl.startsWith("jdbc:")) jdbcUrl.substring("jdbc:".length) else jdbcUrl
            val query = withoutPrefix.indexOf('?')
            if (query >= 0) {
                withoutPrefix = withoutPrefix.substring(0, query)
            }
            return try {
                val uri = URI.create(withoutPrefix)
                val host = uri.host ?: "unknown-host"
                val port = uri.port
                val path = uri.path ?: ""
                if (port < 0) host + path else host + ":" + port + path
            } catch (ex: IllegalArgumentException) {
                "the configured monitoring database"
            }
        }

        private fun migrate(pool: HikariDataSource) {
            try {
                Flyway
                    .configure()
                    .dataSource(pool)
                    .locations("classpath:db/api-metric/migration")
                    .table("api_metric_flyway_schema_history")
                    .baselineOnMigrate(false)
                    .cleanDisabled(true)
                    .load()
                    .migrate()
            } catch (ex: RuntimeException) {
                if (!brokenProcedureCatalog(ex)) {
                    throw ex
                }
                log.warning(
                    "API Metric skipped Flyway because this database cannot query mysql.proc. " +
                        "The monitoring tables were created directly. Run mysql_upgrade on the server " +
                        "so later startups can use Flyway.",
                )
                applySchemaDirectly(pool)
            }
        }

        private fun brokenProcedureCatalog(failure: Throwable): Boolean {
            var current: Throwable? = failure
            while (current != null) {
                val throwable = current
                if (throwable is SQLException && throwable.errorCode == 1558) {
                    return true
                }
                val message = throwable.message
                if (message != null && message.contains("mysql.proc")) {
                    return true
                }
                current = throwable.cause
            }
            return false
        }

        private fun applySchemaDirectly(pool: HikariDataSource) {
            try {
                pool.connection.use { connection ->
                    connection.createStatement().use { statement ->
                        for (location in SCHEMA_SCRIPTS) {
                            for (raw in readSchemaScript(location).split(Regex(";"))) {
                                val sql = raw.trim { it <= ' ' }
                                if (sql.isEmpty()) {
                                    continue
                                }
                                try {
                                    statement.execute(sql)
                                } catch (ex: SQLException) {
                                    if (ex.errorCode != 1060) {
                                        throw ex
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (ex: SQLException) {
                throw IllegalStateException("API Metric could not create its monitoring tables", ex)
            }
        }

        private fun readSchemaScript(location: String): String {
            try {
                val input =
                    MysqlMonitoringDatabase::class.java.getResourceAsStream(location)
                        ?: throw IllegalStateException("API Metric schema script is missing")
                return input.use { stream ->
                    String(stream.readAllBytes(), StandardCharsets.UTF_8)
                }
            } catch (ex: IOException) {
                throw IllegalStateException("API Metric could not read its schema script", ex)
            }
        }
    }
}
