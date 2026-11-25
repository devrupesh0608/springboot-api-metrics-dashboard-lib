package com.apimetric.storage.mysql

/** Connection inputs for the library-owned monitoring pool. The password is never logged. */
@JvmRecord
data class MysqlConnectionSettings(
    val jdbcUrl: String,
    val username: String,
    val password: String,
    val driverClassName: String,
    val maximumPoolSize: Int,
    val minimumIdle: Int,
    val connectionTimeoutMs: Long,
)
