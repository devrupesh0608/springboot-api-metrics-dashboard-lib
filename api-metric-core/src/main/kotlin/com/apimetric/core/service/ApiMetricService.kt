package com.apimetric.core.service

import com.apimetric.core.model.ApiInventoryEntry
import com.apimetric.core.model.ApiLatencyStatistics
import com.apimetric.core.model.ApiMetadata
import com.apimetric.core.model.ApiRequestEvent
import com.apimetric.core.model.ApiRequestLog
import com.apimetric.core.model.ApiUsageSnapshot
import com.apimetric.core.model.TrafficSeries
import com.apimetric.core.model.UnusedApiCandidate
import com.apimetric.core.storage.InventoryPage
import com.apimetric.core.storage.InventoryQuery
import com.apimetric.core.storage.LatencyQuery
import com.apimetric.core.storage.RequestLogPage
import com.apimetric.core.storage.RequestLogQuery
import com.apimetric.core.storage.TrafficQuery
import com.apimetric.core.storage.UnusedApiQuery
import com.apimetric.core.storage.UsageSnapshotQuery
import java.time.Instant
import java.util.Optional

/**
 * Application-facing monitoring operations.
 *
 * [registerDiscoveredApis] upserts the scanned routes and marks every stored API that was absent
 * from that scan as no longer discovered. It does not reset first-discovered or last-accessed
 * timestamps for routes that are still present.
 */
interface ApiMetricService {
    fun registerDiscoveredApis(
        discovered: List<ApiMetadata>?,
        discoveredAt: Instant?,
    )

    fun recordInvocations(events: List<ApiRequestEvent>?)

    fun inventory(query: InventoryQuery?): InventoryPage

    fun findApi(id: Long): Optional<ApiInventoryEntry>

    fun searchRequestLogs(query: RequestLogQuery?): RequestLogPage

    fun findRequestLog(id: Long): Optional<ApiRequestLog>

    fun traffic(query: TrafficQuery?): TrafficSeries

    fun latency(query: LatencyQuery?): ApiLatencyStatistics

    fun usageSnapshot(query: UsageSnapshotQuery?): ApiUsageSnapshot

    fun findUnusedCandidates(query: UnusedApiQuery?): List<UnusedApiCandidate>

    /**
     * Deletes at most `batchSize` request logs created before `cutoff`. Returns the number of rows
     * deleted.
     */
    fun deleteExpiredRequestLogs(
        cutoff: Instant?,
        batchSize: Int,
    ): Int

    /** Returns false when monitoring storage cannot be reached. */
    fun storageConnected(): Boolean
}
