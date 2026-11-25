package com.apimetric.autoconfigure.web

import com.apimetric.autoconfigure.ApiMetricProperties
import com.apimetric.autoconfigure.capture.ApiMetricEventQueue
import com.apimetric.core.enums.InventorySort
import com.apimetric.core.enums.SortDirection
import com.apimetric.core.enums.TrafficBucketSize
import com.apimetric.core.model.ApiRequestLog
import com.apimetric.core.service.ApiMetricService
import com.apimetric.core.storage.InventoryQuery
import com.apimetric.core.storage.LatencyQuery
import com.apimetric.core.storage.PageRequest
import com.apimetric.core.storage.RequestLogQuery
import com.apimetric.core.storage.TimeRange
import com.apimetric.core.storage.TrafficQuery
import com.apimetric.core.storage.UnusedApiQuery
import com.apimetric.core.storage.UsageSnapshotQuery
import com.apimetric.storage.mysql.MysqlMonitoringDatabase
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.Duration
import java.time.Instant
import java.util.LinkedHashMap
import kotlin.math.min

/** Read API for the embedded dashboard. Payloads returned here are already sanitized. */
@RestController
@RequestMapping("\${api-metric.internal-path:/api-metric/internal}")
class ApiMetricInternalController(
    private val properties: ApiMetricProperties,
    private val service: ApiMetricService,
    private val queue: ApiMetricEventQueue,
) {
    @GetMapping("/overview")
    fun overview(
        @RequestParam(required = false) from: Instant?,
        @RequestParam(required = false) to: Instant?,
    ): Any {
        requireAccess()
        val range = range(from, to)
        return service.usageSnapshot(
            UsageSnapshotQuery(range, properties.analytics.unusedThreshold(), Instant.now()),
        )
    }

    @GetMapping("/apis")
    fun apis(
        @RequestParam(required = false) q: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
    ): Any {
        requireAccess()
        return service.inventory(
            InventoryQuery(
                q,
                null,
                null,
                null,
                properties.analytics.unusedThreshold(),
                Instant.now(),
                InventorySort.API_PATH,
                SortDirection.ASC,
                PageRequest(page, min(size, PageRequest.MAX_PAGE_SIZE)),
            ),
        )
    }

    @GetMapping("/apis/{id}")
    fun api(
        @PathVariable id: Long,
    ): Any {
        requireAccess()
        val entry =
            service.findApi(id).orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND)
            }
        val now = Instant.now()
        val range = TimeRange(now.minus(Duration.ofHours(24)), now.plusMillis(1))
        val body = LinkedHashMap<String, Any?>()
        body["api"] = entry
        body["traffic"] = service.traffic(TrafficQuery(id, range, TrafficBucketSize.HOUR))
        body["latency"] = service.latency(LatencyQuery(id, range))
        return body
    }

    @GetMapping("/apis/{id}/requests")
    fun requests(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): Any {
        requireAccess()
        return service.searchRequestLogs(
            RequestLogQuery(
                id,
                null,
                null,
                null,
                null,
                null,
                PageRequest(page, min(size, 100)),
            ),
        )
    }

    @GetMapping("/requests")
    fun recentRequests(
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) method: String?,
        @RequestParam(required = false) minStatus: Int?,
        @RequestParam(required = false) maxStatus: Int?,
        @RequestParam(required = false) from: Instant?,
        @RequestParam(required = false) to: Instant?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
    ): Any {
        requireAccess()
        return service.searchRequestLogs(
            RequestLogQuery(
                null,
                method,
                minStatus,
                maxStatus,
                optionalRange(from, to),
                q,
                PageRequest(page, min(size, 100)),
            ),
        )
    }

    @GetMapping("/requests/{requestId}")
    fun request(
        @PathVariable requestId: Long,
    ): ApiRequestLog {
        requireAccess()
        return service.findRequestLog(requestId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND)
        }
    }

    @GetMapping("/analytics/traffic")
    fun traffic(
        @RequestParam(required = false) apiId: Long?,
        @RequestParam(required = false) from: Instant?,
        @RequestParam(required = false) to: Instant?,
        @RequestParam(required = false) bucket: TrafficBucketSize?,
    ): Any {
        requireAccess()
        val size = bucket ?: TrafficBucketSize.HOUR
        return service.traffic(TrafficQuery(apiId, range(from, to), size))
    }

    @GetMapping("/analytics/unused")
    fun unused(): Any {
        requireAccess()
        return service.findUnusedCandidates(
            UnusedApiQuery(properties.analytics.unusedThreshold(), Instant.now()),
        )
    }

    @GetMapping("/diagnostics")
    fun diagnostics(): Map<String, Any?> {
        requireAccess()
        val body = LinkedHashMap<String, Any?>()
        body["enabled"] = properties.enabled
        body["dashboardPath"] = properties.dashboard.path
        body["captureRequestPayload"] = properties.capture.requestPayload
        body["captureResponsePayload"] = properties.capture.responsePayload
        body["maxPayloadSize"] = properties.capture.maxPayloadSize
        body["unusedApiThresholdDays"] = properties.analytics.unusedApiThresholdDays
        body["requestLogRetentionDays"] = properties.retention.requestLogsDays
        body["queueCapacity"] = properties.processing.queueCapacity
        body["queueDepth"] = queue.queueDepth()
        body["droppedEvents"] = queue.droppedCount()
        body["persistenceFailures"] = queue.failureCount()
        body["lastSuccessfulFlush"] = queue.lastSuccessfulFlush()
        body["lastFailureSummary"] = queue.lastFailureSummary()
        body["storageConnected"] = service.storageConnected()
        body["databaseTarget"] = databaseTarget()
        return body
    }

    private fun databaseTarget(): String = MysqlMonitoringDatabase.safeTarget(properties.storage.mysql.url)

    private fun requireAccess() {
        if (properties.security.enabled && !properties.security.allowUnauthenticated) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }
    }

    private fun optionalRange(
        from: Instant?,
        to: Instant?,
    ): TimeRange? {
        if (from == null && to == null) {
            return null
        }
        val end = if (to == null) Instant.now().plusMillis(1) else to
        val start = if (from == null) Instant.EPOCH else from
        if (!start.isBefore(end)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "from must be before to")
        }
        return TimeRange(start, end)
    }

    private fun range(
        from: Instant?,
        to: Instant?,
    ): TimeRange {
        val end = if (to == null) Instant.now().plusMillis(1) else to
        val start = if (from == null) end.minus(Duration.ofHours(24)) else from
        if (!start.isBefore(end)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "from must be before to")
        }
        return TimeRange(start, end)
    }
}
