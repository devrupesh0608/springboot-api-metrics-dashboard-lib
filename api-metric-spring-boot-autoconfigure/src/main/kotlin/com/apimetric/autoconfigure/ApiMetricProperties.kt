package com.apimetric.autoconfigure

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/** Binds `api-metric.*`. The MySQL password is never logged. */
@ConfigurationProperties(prefix = "api-metric")
class ApiMetricProperties {
    var enabled: Boolean = true
    var internalPath: String = "/api-metric/internal"
    var dashboard: Dashboard = Dashboard()
    var storage: Storage = Storage()
    var capture: Capture = Capture()
    var security: Security = Security()
    var analytics: Analytics = Analytics()
    var retention: Retention = Retention()
    var processing: Processing = Processing()

    fun validate() {
        if (!enabled) {
            return
        }
        val mysql = storage.mysql
        if (isBlank(mysql.url) || isBlank(mysql.username) || mysql.password == null) {
            throw IllegalStateException(
                "API Metric is enabled but api-metric.storage.mysql.url, username, and password are" +
                    " required. Set the password from an environment variable. In-memory storage is not" +
                    " supported.",
            )
        }
        if (mysql.maximumPoolSize < 1 || mysql.minimumIdle < 0) {
            throw IllegalStateException(
                "API Metric MySQL pool settings are invalid. maximum-pool-size must be >= 1 and" +
                    " minimum-idle must be >= 0.",
            )
        }
        if (mysql.minimumIdle > mysql.maximumPoolSize) {
            throw IllegalStateException(
                "API Metric minimum-idle cannot be greater than maximum-pool-size.",
            )
        }
    }

    private fun isBlank(value: String?): Boolean = value == null || value.isBlank()

    class Dashboard {
        var enabled: Boolean = true
        var path: String = "/api-metric-dashboard"
    }

    class Storage {
        var mysql: Mysql = Mysql()
    }

    class Mysql {
        var url: String? = null
        var username: String? = null
        var password: String? = null
        var driverClassName: String = "com.mysql.cj.jdbc.Driver"
        var maximumPoolSize: Int = 5
        var minimumIdle: Int = 1
        var connectionTimeoutMs: Long = 3000
    }

    class Capture {
        var requestPayload: Boolean = true
        var responsePayload: Boolean = true
        var captureHeaders: Boolean = false
        var maxPayloadSize: Int = 10240
        var excludedPaths: MutableList<String?> = mutableListOf("/actuator/**")
        var excludedContentTypes: MutableList<String?> =
            mutableListOf("multipart/form-data", "application/octet-stream")
        var maskFields: MutableList<String?> =
            mutableListOf(
                "password",
                "token",
                "accessToken",
                "refreshToken",
                "authorization",
                "secret",
            )
    }

    class Security {
        var enabled: Boolean = true
        var allowUnauthenticated: Boolean = false
    }

    class Analytics {
        var unusedApiThresholdDays: Int = 30

        fun unusedThreshold(): Duration = Duration.ofDays(unusedApiThresholdDays.toLong())
    }

    class Retention {
        var requestLogsDays: Int = 30
    }

    class Processing {
        var queueCapacity: Int = 10_000
        var batchSize: Int = 100
        var flushIntervalMs: Long = 1000
    }
}
