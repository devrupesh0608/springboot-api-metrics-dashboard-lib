package com.apimetric.core.storage

import com.apimetric.core.model.ApiInventoryEntry
import com.apimetric.core.model.ApiLatencyStatistics
import com.apimetric.core.model.ApiMetadata
import com.apimetric.core.model.ApiRequestEvent
import com.apimetric.core.model.ApiRequestLog
import com.apimetric.core.model.ApiUsageSnapshot
import com.apimetric.core.model.TrafficSeries
import com.apimetric.core.model.UnusedApiCandidate
import java.time.Instant
import java.util.Optional

/**
 * Persistence port for monitoring data.
 *
 * Implementations own the monitoring schema only. A status from 200 inclusive to 400 exclusive is
 * a successful hit; every other retained status is a failed hit. Null arguments are rejected.
 * Persistence failures propagate to the caller so an asynchronous worker can record them.
 */
interface ApiMetricStorage {
    /**
     * Inserts newly discovered routes and refreshes `lastDiscoveredAt` for routes that already
     * exist. Does not change `firstDiscoveredAt` or `lastAccessedAt`.
     */
    fun upsertDiscoveredApis(
        discovered: List<ApiMetadata>?,
        discoveredAt: Instant?,
    )

    /**
     * Marks stored APIs whose keys are absent from `currentApiKeys` as no longer discovered. An
     * empty set marks every stored API. Callers pass the complete scan result.
     */
    fun markApisNotRediscovered(
        currentApiKeys: Set<String>?,
        scanAt: Instant?,
    )

    /** Persists a batch of completed invocations. An empty batch is a no-op. */
    fun saveRequestEvents(events: List<ApiRequestEvent>?)

    fun queryInventory(query: InventoryQuery?): InventoryPage

    fun findApiById(id: Long): Optional<ApiInventoryEntry>

    fun searchRequestLogs(query: RequestLogQuery?): RequestLogPage

    fun findRequestLogById(id: Long): Optional<ApiRequestLog>

    fun queryTraffic(query: TrafficQuery?): TrafficSeries

    fun queryLatency(query: LatencyQuery?): ApiLatencyStatistics

    fun queryUsageSnapshot(query: UsageSnapshotQuery?): ApiUsageSnapshot

    fun findUnusedCandidates(query: UnusedApiQuery?): List<UnusedApiCandidate>

    /**
     * Deletes at most `batchSize` request logs older than `cutoff`. Returns the number of rows
     * deleted. `batchSize` must be positive.
     */
    fun deleteRequestLogsOlderThan(
        cutoff: Instant?,
        batchSize: Int,
    ): Int

    /** Returns false when the monitoring database cannot be reached. */
    fun checkConnectivity(): Boolean
}
