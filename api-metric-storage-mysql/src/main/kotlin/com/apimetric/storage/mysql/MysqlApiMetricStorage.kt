package com.apimetric.storage.mysql

import com.apimetric.core.enums.ApiUsageStatus
import com.apimetric.core.enums.CaptureStatus
import com.apimetric.core.enums.DiscoveryStatus
import com.apimetric.core.enums.InventorySort
import com.apimetric.core.enums.SortDirection
import com.apimetric.core.enums.TrafficBucketSize
import com.apimetric.core.model.ApiIdentity
import com.apimetric.core.model.ApiInventoryEntry
import com.apimetric.core.model.ApiLatencyStatistics
import com.apimetric.core.model.ApiMetadata
import com.apimetric.core.model.ApiRequestEvent
import com.apimetric.core.model.ApiRequestLog
import com.apimetric.core.model.ApiUsageSnapshot
import com.apimetric.core.model.CapturedPayload
import com.apimetric.core.model.LatencyBucket
import com.apimetric.core.model.TrafficPoint
import com.apimetric.core.model.TrafficSeries
import com.apimetric.core.model.UnusedApiCandidate
import com.apimetric.core.service.ApiUsageClassifier
import com.apimetric.core.storage.ApiMetricStorage
import com.apimetric.core.storage.InventoryPage
import com.apimetric.core.storage.InventoryQuery
import com.apimetric.core.storage.LatencyQuery
import com.apimetric.core.storage.RequestLogPage
import com.apimetric.core.storage.RequestLogQuery
import com.apimetric.core.storage.TrafficQuery
import com.apimetric.core.storage.UnusedApiQuery
import com.apimetric.core.storage.UsageSnapshotQuery
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.ArrayList
import java.util.Objects
import java.util.Optional

/** JDBC implementation of the monitoring storage port. Uses the library-owned pool only. */
class MysqlApiMetricStorage(
    database: MysqlMonitoringDatabase?,
) : ApiMetricStorage {
    private val jdbc: JdbcTemplate
    private val transactions: TransactionTemplate

    init {
        val checked = Objects.requireNonNull(database, "database")!!
        jdbc = checked.jdbcTemplate()
        transactions = TransactionTemplate(DataSourceTransactionManager(checked.dataSource()))
    }

    override fun upsertDiscoveredApis(
        discovered: List<ApiMetadata>?,
        discoveredAt: Instant?,
    ) {
        val apis = Objects.requireNonNull(discovered, "discovered")!!
        val discoveredTimestamp = timestamp(Objects.requireNonNull(discoveredAt, "discoveredAt")!!)
        if (apis.isEmpty()) {
            return
        }
        transactions.executeWithoutResult {
            for (metadata in apis) {
                jdbc.update(
                    javaText(
                        """
                        INSERT INTO api_metric_api_inventory (
                            api_key, http_method, api_path, controller_name, handler_method, display_name,
                            first_discovered_at, last_discovered_at, total_hits, successful_hits, failed_hits,
                            is_currently_discovered)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, 0, 0, 1)
                        ON DUPLICATE KEY UPDATE
                            http_method = VALUES(http_method),
                            api_path = VALUES(api_path),
                            controller_name = VALUES(controller_name),
                            handler_method = VALUES(handler_method),
                            display_name = VALUES(display_name),
                            last_discovered_at = VALUES(last_discovered_at),
                            is_currently_discovered = 1
                        """.trimIndent(),
                    ),
                    metadata.identity.apiKey(),
                    metadata.identity.httpMethod,
                    metadata.identity.pathPattern,
                    metadata.controllerName,
                    metadata.handlerMethod,
                    metadata.displayName,
                    discoveredTimestamp,
                    discoveredTimestamp,
                )
            }
        }
    }

    override fun markApisNotRediscovered(
        currentApiKeys: Set<String>?,
        scanAt: Instant?,
    ) {
        val keys = Objects.requireNonNull(currentApiKeys, "currentApiKeys")!!
        Objects.requireNonNull(scanAt, "scanAt")
        transactions.executeWithoutResult {
            jdbc.update("UPDATE api_metric_api_inventory SET is_currently_discovered = 0")
            if (keys.isEmpty()) {
                return@executeWithoutResult
            }
            val placeholders = keys.joinToString(", ") { "?" }
            jdbc.update(
                "UPDATE api_metric_api_inventory SET is_currently_discovered = 1 WHERE api_key IN (" +
                    placeholders +
                    ")",
                *keys.toTypedArray(),
            )
        }
    }

    override fun saveRequestEvents(events: List<ApiRequestEvent>?) {
        val batch = Objects.requireNonNull(events, "events")!!
        if (batch.isEmpty()) {
            return
        }
        transactions.executeWithoutResult {
            for (event in batch) {
                val apiId = ensureApi(event)
                val success = if (event.successful()) 1 else 0
                jdbc.update(
                    javaText(
                        """
                        UPDATE api_metric_api_inventory
                        SET last_accessed_at = ?,
                            total_hits = total_hits + 1,
                            successful_hits = successful_hits + ?,
                            failed_hits = failed_hits + ?
                        WHERE id = ?
                        """.trimIndent(),
                    ),
                    timestamp(event.occurredAt),
                    success,
                    1 - success,
                    apiId,
                )
                jdbc.update(
                    javaText(
                        """
                        INSERT INTO api_metric_request_log (
                            api_id, correlation_id, http_status, duration_ms, request_payload,
                            response_payload, request_content_type, response_content_type,
                            request_capture_status, response_capture_status, request_headers,
                            response_headers, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """.trimIndent(),
                    ),
                    apiId,
                    event.correlationId,
                    event.httpStatus,
                    event.durationMillis,
                    event.requestPayload.body,
                    event.responsePayload.body,
                    event.requestPayload.contentType,
                    event.responsePayload.contentType,
                    event.requestPayload.status.name,
                    event.responsePayload.status.name,
                    event.requestHeaders,
                    event.responseHeaders,
                    timestamp(event.occurredAt),
                )
                recordAggregate(apiId, event, TrafficBucketSize.MINUTE)
                recordAggregate(apiId, event, TrafficBucketSize.HOUR)
            }
        }
    }

    override fun queryInventory(query: InventoryQuery?): InventoryPage {
        val checked = Objects.requireNonNull(query, "query")!!
        val filter = inventoryFilter(checked)
        val total =
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM api_metric_api_inventory WHERE " + filter.sql,
                Long::class.javaObjectType,
                *filter.args.toTypedArray(),
            )
        val args = ArrayList(filter.args)
        args.add(checked.page.pageSize)
        args.add(checked.page.offset())
        val items =
            jdbc.query(
                "SELECT * FROM api_metric_api_inventory WHERE " +
                    filter.sql +
                    " ORDER BY " +
                    sortColumn(checked.sort) +
                    " " +
                    sortDirection(checked.direction) +
                    " LIMIT ? OFFSET ?",
                { rs, _ -> mapInventory(rs) },
                *args.toTypedArray(),
            )
        return InventoryPage(items, if (total == null) 0 else total, checked.page)
    }

    override fun findApiById(id: Long): Optional<ApiInventoryEntry> {
        val rows =
            jdbc.query(
                "SELECT * FROM api_metric_api_inventory WHERE id = ?",
                { rs, _ -> mapInventory(rs) },
                id,
            )
        return rows.stream().findFirst()
    }

    override fun searchRequestLogs(query: RequestLogQuery?): RequestLogPage {
        val checked = Objects.requireNonNull(query, "query")!!
        val filter = requestFilter(checked)
        val from =
            "\n" +
                """
                FROM api_metric_request_log log
                JOIN api_metric_api_inventory inventory ON inventory.id = log.api_id
                WHERE
                """.trimIndent() +
                " " +
                filter.sql
        val total =
            jdbc.queryForObject(
                "SELECT COUNT(*) " + from,
                Long::class.javaObjectType,
                *filter.args.toTypedArray(),
            )
        val args = ArrayList(filter.args)
        args.add(checked.page.pageSize)
        args.add(checked.page.offset())
        val items =
            jdbc.query(
                "SELECT log.*, inventory.api_key " +
                    from +
                    " ORDER BY log.created_at DESC, log.id DESC LIMIT ? OFFSET ?",
                { rs, _ -> mapRequest(rs) },
                *args.toTypedArray(),
            )
        return RequestLogPage(items, if (total == null) 0 else total, checked.page)
    }

    override fun findRequestLogById(id: Long): Optional<ApiRequestLog> {
        val rows =
            jdbc.query(
                javaText(
                    """
                    SELECT log.*, inventory.api_key
                    FROM api_metric_request_log log
                    JOIN api_metric_api_inventory inventory ON inventory.id = log.api_id
                    WHERE log.id = ?
                    """.trimIndent(),
                ),
                { rs, _ -> mapRequest(rs) },
                id,
            )
        return rows.stream().findFirst()
    }

    override fun queryTraffic(query: TrafficQuery?): TrafficSeries {
        val checked = Objects.requireNonNull(query, "query")!!
        val args = ArrayList<Any?>()
        val sql =
            StringBuilder(
                javaText(
                    """
                    SELECT bucket_start, SUM(total_hits) total_hits, SUM(successful_hits) successful_hits,
                           SUM(failed_hits) failed_hits, SUM(total_duration_ms) total_duration_ms
                    FROM api_metric_usage_aggregate
                    WHERE bucket_size = ? AND bucket_start >= ? AND bucket_start < ?
                    """.trimIndent(),
                ),
            )
        args.add(checked.bucketSize.name)
        args.add(timestamp(checked.range.startInclusive))
        args.add(timestamp(checked.range.endExclusive))
        if (checked.apiId != null) {
            sql.append(" AND api_id = ?")
            args.add(checked.apiId)
        }
        sql.append(" GROUP BY bucket_start ORDER BY bucket_start")
        val points =
            jdbc.query(
                sql.toString(),
                { rs, _ ->
                    TrafficPoint(
                        instant(rs.getTimestamp("bucket_start")),
                        rs.getLong("total_hits"),
                        rs.getLong("successful_hits"),
                        rs.getLong("failed_hits"),
                        rs.getLong("total_duration_ms"),
                    )
                },
                *args.toTypedArray(),
            )
        return TrafficSeries(checked.bucketSize, points)
    }

    override fun queryLatency(query: LatencyQuery?): ApiLatencyStatistics {
        val checked = Objects.requireNonNull(query, "query")!!
        val rows =
            jdbc.query(
                javaText(
                    """
                    SELECT COALESCE(SUM(total_hits), 0) samples,
                           MIN(min_duration_ms) min_duration,
                           MAX(max_duration_ms) max_duration,
                           COALESCE(SUM(total_duration_ms), 0) total_duration
                    FROM api_metric_usage_aggregate
                    WHERE api_id = ? AND bucket_size = 'MINUTE'
                      AND bucket_start >= ? AND bucket_start < ?
                    """.trimIndent(),
                ),
                { rs, _ ->
                    LatencyStatsRow(
                        rs.getLong("samples"),
                        if (rs.getObject("min_duration") == null) null else rs.getLong("min_duration"),
                        if (rs.getObject("max_duration") == null) null else rs.getLong("max_duration"),
                        rs.getLong("total_duration"),
                    )
                },
                checked.apiId,
                timestamp(checked.range.startInclusive),
                timestamp(checked.range.endExclusive),
            )
        val stats = if (rows.isEmpty()) LatencyStatsRow.empty() else rows[0]
        if (stats.samples == 0L) {
            return ApiLatencyStatistics(0, null, null, null, null, null, false, java.util.List.of())
        }
        val buckets =
            jdbc.query(
                javaText(
                    """
                    SELECT upper_bound_ms, SUM(hit_count) hit_count
                    FROM api_metric_latency_bucket
                    WHERE api_id = ? AND bucket_size = 'MINUTE'
                      AND bucket_start >= ? AND bucket_start < ?
                    GROUP BY upper_bound_ms
                    ORDER BY upper_bound_ms
                    """.trimIndent(),
                ),
                { rs, _ ->
                    LatencyBucket(rs.getLong("upper_bound_ms"), rs.getLong("hit_count"))
                },
                checked.apiId,
                timestamp(checked.range.startInclusive),
                timestamp(checked.range.endExclusive),
            )
        val p95 = if (buckets.isEmpty()) null else percentile(buckets, 0.95)
        val p99 = if (buckets.isEmpty()) null else percentile(buckets, 0.99)
        val average = stats.totalDuration.toDouble() / stats.samples.toDouble()
        return ApiLatencyStatistics(
            stats.samples,
            stats.minDuration,
            stats.maxDuration,
            average,
            p95,
            p99,
            p95 != null || p99 != null,
            buckets,
        )
    }

    override fun queryUsageSnapshot(query: UsageSnapshotQuery?): ApiUsageSnapshot {
        val checked = Objects.requireNonNull(query, "query")!!
        val activeSince = checked.evaluatedAt.minus(checked.inactivityThreshold)
        val counts =
            jdbc.query(
                javaText(
                    """
                    SELECT
                      COALESCE(SUM(CASE WHEN is_currently_discovered = 1 THEN 1 ELSE 0 END), 0) discovered,
                      COALESCE(SUM(CASE WHEN total_hits = 0 AND last_accessed_at IS NULL THEN 1 ELSE 0 END), 0) never_called,
                      COALESCE(SUM(CASE WHEN last_accessed_at IS NOT NULL AND last_accessed_at >= ? THEN 1 ELSE 0 END), 0) active,
                      COALESCE(SUM(CASE WHEN NOT (total_hits = 0 AND last_accessed_at IS NULL)
                        AND (last_accessed_at IS NULL OR last_accessed_at < ?) THEN 1 ELSE 0 END), 0) inactive,
                      COALESCE(SUM(total_hits), 0) total_hits
                    FROM api_metric_api_inventory
                    """.trimIndent(),
                ),
                { rs, _ ->
                    longArrayOf(
                        rs.getLong("discovered"),
                        rs.getLong("never_called"),
                        rs.getLong("active"),
                        rs.getLong("inactive"),
                        rs.getLong("total_hits"),
                    )
                },
                timestamp(activeSince),
                timestamp(activeSince),
            )
        val inventory = if (counts.isEmpty()) LongArray(5) else counts[0]
        val requests =
            jdbc.query(
                javaText(
                    """
                    SELECT COUNT(*) total_requests,
                           COALESCE(SUM(CASE WHEN http_status < 200 OR http_status >= 400 THEN 1 ELSE 0 END), 0) failed_requests
                    FROM api_metric_request_log
                    WHERE created_at >= ? AND created_at < ?
                    """.trimIndent(),
                ),
                { rs, _ ->
                    longArrayOf(rs.getLong("total_requests"), rs.getLong("failed_requests"))
                },
                timestamp(checked.requestRange.startInclusive),
                timestamp(checked.requestRange.endExclusive),
            )
        val traffic = if (requests.isEmpty()) LongArray(2) else requests[0]
        return ApiUsageSnapshot(
            traffic[0],
            traffic[1],
            inventory[0],
            inventory[2],
            inventory[1],
            inventory[1] + inventory[3],
            inventory[4],
            checked.requestRange,
        )
    }

    override fun findUnusedCandidates(query: UnusedApiQuery?): List<UnusedApiCandidate> {
        val checked = Objects.requireNonNull(query, "query")!!
        val rows = jdbc.query("SELECT * FROM api_metric_api_inventory", { rs, _ -> mapInventory(rs) })
        val candidates = ArrayList<UnusedApiCandidate>()
        for (row in rows) {
            val classification =
                ApiUsageClassifier.classify(
                    row.firstDiscoveredAt,
                    row.lastAccessedAt,
                    row.totalHits,
                    row.discoveryStatus == DiscoveryStatus.CURRENTLY_DISCOVERED,
                    checked.inactivityThreshold,
                    checked.evaluatedAt,
                )
            if (classification.usageStatus != ApiUsageStatus.ACTIVE) {
                candidates.add(UnusedApiCandidate(row, classification.usageStatus))
            }
        }
        return java.util.List.copyOf(candidates)
    }

    override fun deleteRequestLogsOlderThan(
        cutoff: Instant?,
        batchSize: Int,
    ): Int {
        val checked = Objects.requireNonNull(cutoff, "cutoff")!!
        if (batchSize < 1) {
            throw IllegalArgumentException("batchSize must be positive")
        }
        return jdbc.update(
            "DELETE FROM api_metric_request_log WHERE created_at < ? ORDER BY id LIMIT ?",
            timestamp(checked),
            batchSize,
        )
    }

    override fun checkConnectivity(): Boolean {
        try {
            val value = jdbc.queryForObject("SELECT 1", Int::class.javaObjectType)
            return value != null && value == 1
        } catch (ex: RuntimeException) {
            return false
        }
    }

    private fun ensureApi(event: ApiRequestEvent): Long {
        val occurred = timestamp(event.occurredAt)
        jdbc.update(
            javaText(
                """
                INSERT INTO api_metric_api_inventory (
                    api_key, http_method, api_path, controller_name, handler_method, display_name,
                    first_discovered_at, last_discovered_at, total_hits, successful_hits, failed_hits,
                    is_currently_discovered)
                VALUES (?, ?, ?, 'unknown', 'unknown', NULL, ?, ?, 0, 0, 0, 0)
                ON DUPLICATE KEY UPDATE api_key = VALUES(api_key)
                """.trimIndent(),
            ),
            event.identity.apiKey(),
            event.identity.httpMethod,
            event.identity.pathPattern,
            occurred,
            occurred,
        )
        val id =
            jdbc.queryForObject(
                "SELECT id FROM api_metric_api_inventory WHERE api_key = ?",
                Long::class.javaObjectType,
                event.identity.apiKey(),
            )
        if (id == null) {
            throw IllegalStateException("API Metric could not resolve an inventory row")
        }
        return id
    }

    private fun recordAggregate(
        apiId: Long,
        event: ApiRequestEvent,
        bucketSize: TrafficBucketSize,
    ) {
        val bucketStart = truncate(event.occurredAt, bucketSize)
        val success = if (event.successful()) 1 else 0
        jdbc.update(
            javaText(
                """
                INSERT INTO api_metric_usage_aggregate (
                    api_id, bucket_start, bucket_size, total_hits, successful_hits, failed_hits,
                    total_duration_ms, min_duration_ms, max_duration_ms)
                VALUES (?, ?, ?, 1, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    total_hits = total_hits + VALUES(total_hits),
                    successful_hits = successful_hits + VALUES(successful_hits),
                    failed_hits = failed_hits + VALUES(failed_hits),
                    total_duration_ms = total_duration_ms + VALUES(total_duration_ms),
                    min_duration_ms = LEAST(min_duration_ms, VALUES(min_duration_ms)),
                    max_duration_ms = GREATEST(max_duration_ms, VALUES(max_duration_ms))
                """.trimIndent(),
            ),
            apiId,
            timestamp(bucketStart),
            bucketSize.name,
            success,
            1 - success,
            event.durationMillis,
            event.durationMillis,
            event.durationMillis,
        )
        jdbc.update(
            javaText(
                """
                INSERT INTO api_metric_latency_bucket (
                    api_id, bucket_start, bucket_size, upper_bound_ms, hit_count)
                VALUES (?, ?, ?, ?, 1)
                ON DUPLICATE KEY UPDATE hit_count = hit_count + VALUES(hit_count)
                """.trimIndent(),
            ),
            apiId,
            timestamp(bucketStart),
            bucketSize.name,
            latencyBound(event.durationMillis),
        )
    }

    private fun inventoryFilter(query: InventoryQuery): Filter {
        val clauses = ArrayList<String>()
        val args = ArrayList<Any?>()
        clauses.add("1 = 1")
        val searchText = query.searchText
        if (searchText != null) {
            val like = "%" + escapeLike(searchText) + "%"
            clauses.add(
                "(api_path LIKE ? ESCAPE '\\\\' OR controller_name LIKE ? ESCAPE '\\\\' OR api_key LIKE ? ESCAPE '\\\\')",
            )
            args.add(like)
            args.add(like)
            args.add(like)
        }
        if (query.httpMethod != null) {
            clauses.add("http_method = ?")
            args.add(query.httpMethod)
        }
        if (query.discoveryStatus != null) {
            clauses.add("is_currently_discovered = ?")
            args.add(query.discoveryStatus == DiscoveryStatus.CURRENTLY_DISCOVERED)
        }
        val usageStatus = query.usageStatus
        if (usageStatus != null) {
            val activeSince = query.evaluatedAt.minus(query.inactivityThreshold)
            when (usageStatus) {
                ApiUsageStatus.NEVER_CALLED -> clauses.add("total_hits = 0 AND last_accessed_at IS NULL")
                ApiUsageStatus.ACTIVE -> {
                    clauses.add("last_accessed_at IS NOT NULL AND last_accessed_at >= ?")
                    args.add(timestamp(activeSince))
                }
                ApiUsageStatus.POTENTIALLY_UNUSED -> {
                    clauses.add(
                        "NOT (total_hits = 0 AND last_accessed_at IS NULL) AND (last_accessed_at IS NULL OR last_accessed_at < ?)",
                    )
                    args.add(timestamp(activeSince))
                }
            }
        }
        return Filter(clauses.joinToString(" AND "), args)
    }

    private fun escapeLike(value: String): String = value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    private fun requestFilter(query: RequestLogQuery): Filter {
        val clauses = ArrayList<String>()
        val args = ArrayList<Any?>()
        clauses.add("1 = 1")
        if (query.apiId != null) {
            clauses.add("log.api_id = ?")
            args.add(query.apiId)
        }
        if (query.httpMethod != null) {
            clauses.add("inventory.http_method = ?")
            args.add(query.httpMethod)
        }
        if (query.minimumStatus != null) {
            clauses.add("log.http_status >= ?")
            args.add(query.minimumStatus)
        }
        if (query.maximumStatus != null) {
            clauses.add("log.http_status <= ?")
            args.add(query.maximumStatus)
        }
        val createdAt = query.createdAt
        if (createdAt != null) {
            clauses.add("log.created_at >= ? AND log.created_at < ?")
            args.add(timestamp(createdAt.startInclusive))
            args.add(timestamp(createdAt.endExclusive))
        }
        val text = query.text
        if (text != null) {
            val pattern = "%" + escapeLike(text) + "%"
            clauses.add(
                "(inventory.api_path LIKE ? ESCAPE '\\\\' OR inventory.http_method LIKE ? ESCAPE '\\\\'" +
                    " OR CAST(log.http_status AS CHAR) LIKE ? ESCAPE '\\\\')",
            )
            args.add(pattern)
            args.add(pattern)
            args.add(pattern)
        }
        return Filter(clauses.joinToString(" AND "), args)
    }

    private fun mapInventory(rs: ResultSet): ApiInventoryEntry =
        ApiInventoryEntry(
            rs.getLong("id"),
            ApiIdentity.fromApiKey(rs.getString("api_key")),
            rs.getString("controller_name"),
            rs.getString("handler_method"),
            rs.getString("display_name"),
            instant(rs.getTimestamp("first_discovered_at")),
            instant(rs.getTimestamp("last_discovered_at")),
            instant(rs.getTimestamp("last_accessed_at")),
            rs.getLong("total_hits"),
            rs.getLong("successful_hits"),
            rs.getLong("failed_hits"),
            if (rs.getBoolean("is_currently_discovered")) {
                DiscoveryStatus.CURRENTLY_DISCOVERED
            } else {
                DiscoveryStatus.NO_LONGER_DISCOVERED
            },
        )

    private fun mapRequest(rs: ResultSet): ApiRequestLog =
        ApiRequestLog(
            rs.getLong("id"),
            rs.getLong("api_id"),
            ApiIdentity.fromApiKey(rs.getString("api_key")),
            rs.getInt("http_status"),
            rs.getLong("duration_ms"),
            rs.getString("correlation_id"),
            payload(
                rs.getString("request_capture_status"),
                rs.getString("request_content_type"),
                rs.getString("request_payload"),
            ),
            payload(
                rs.getString("response_capture_status"),
                rs.getString("response_content_type"),
                rs.getString("response_payload"),
            ),
            rs.getString("request_headers"),
            rs.getString("response_headers"),
            instant(rs.getTimestamp("created_at")),
        )

    private fun payload(
        statusName: String?,
        contentType: String?,
        body: String?,
    ): CapturedPayload {
        val status = CaptureStatus.valueOf(statusName!!)
        if (status == CaptureStatus.CAPTURED || status == CaptureStatus.TRUNCATED) {
            return CapturedPayload(status, contentType, body ?: "")
        }
        return CapturedPayload(status, contentType, null)
    }

    private fun sortColumn(sort: InventorySort): String =
        when (sort) {
            InventorySort.API_PATH -> "api_path"
            InventorySort.HTTP_METHOD -> "http_method"
            InventorySort.TOTAL_HITS -> "total_hits"
            InventorySort.LAST_ACCESSED -> "last_accessed_at"
            InventorySort.FIRST_DISCOVERED -> "first_discovered_at"
        }

    private fun sortDirection(direction: SortDirection): String = if (direction == SortDirection.DESC) "DESC" else "ASC"

    private fun truncate(
        instant: Instant,
        bucketSize: TrafficBucketSize,
    ): Instant {
        val unit = if (bucketSize == TrafficBucketSize.HOUR) ChronoUnit.HOURS else ChronoUnit.MINUTES
        return instant.atZone(ZoneOffset.UTC).truncatedTo(unit).toInstant()
    }

    private fun latencyBound(durationMillis: Long): Long {
        for (bound in LATENCY_BOUNDS_MS) {
            if (durationMillis <= bound) {
                return bound
            }
        }
        return LATENCY_BOUNDS_MS[LATENCY_BOUNDS_MS.size - 1]
    }

    private fun percentile(
        buckets: List<LatencyBucket>,
        ratio: Double,
    ): Long? {
        val total = buckets.sumOf { it.count }
        if (total == 0L) {
            return null
        }
        val target = Math.ceil(total.toDouble() * ratio).toLong()
        var cumulative = 0L
        for (bucket in buckets) {
            cumulative += bucket.count
            if (cumulative >= target) {
                return bucket.inclusiveUpperBoundMillis
            }
        }
        return buckets[buckets.size - 1].inclusiveUpperBoundMillis
    }

    private fun timestamp(instant: Instant?): Timestamp? = if (instant == null) null else Timestamp.from(instant)

    private fun instant(timestamp: Timestamp?): Instant? = if (timestamp == null) null else timestamp.toInstant()

    private fun javaText(sql: String): String = "\n" + sql + "\n"

    private data class Filter(
        val sql: String,
        val args: List<Any?>,
    )

    private data class LatencyStatsRow(
        val samples: Long,
        val minDuration: Long?,
        val maxDuration: Long?,
        val totalDuration: Long,
    ) {
        companion object {
            fun empty(): LatencyStatsRow = LatencyStatsRow(0, null, null, 0)
        }
    }

    companion object {
        private val LATENCY_BOUNDS_MS =
            longArrayOf(
                10,
                25,
                50,
                100,
                250,
                500,
                1000,
                2500,
                5000,
                10_000,
                60_000,
                Long.MAX_VALUE,
            )
    }
}
