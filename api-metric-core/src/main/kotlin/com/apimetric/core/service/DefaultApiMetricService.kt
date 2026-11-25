package com.apimetric.core.service

import com.apimetric.core.internal.Checks.required
import com.apimetric.core.model.ApiInventoryEntry
import com.apimetric.core.model.ApiLatencyStatistics
import com.apimetric.core.model.ApiMetadata
import com.apimetric.core.model.ApiRequestEvent
import com.apimetric.core.model.ApiRequestLog
import com.apimetric.core.model.ApiUsageSnapshot
import com.apimetric.core.model.TrafficSeries
import com.apimetric.core.model.UnusedApiCandidate
import com.apimetric.core.storage.ApiMetricStorage
import com.apimetric.core.storage.InventoryPage
import com.apimetric.core.storage.InventoryQuery
import com.apimetric.core.storage.LatencyQuery
import com.apimetric.core.storage.RequestLogPage
import com.apimetric.core.storage.RequestLogQuery
import com.apimetric.core.storage.TrafficQuery
import com.apimetric.core.storage.UnusedApiQuery
import com.apimetric.core.storage.UsageSnapshotQuery
import java.time.Instant
import java.util.LinkedHashSet
import java.util.Optional

/** Delegates application operations to the storage port. */
class DefaultApiMetricService(
    storage: ApiMetricStorage?,
) : ApiMetricService {
    private val storage: ApiMetricStorage = required(storage, "storage")

    override fun registerDiscoveredApis(
        discovered: List<ApiMetadata>?,
        discoveredAt: Instant?,
    ) {
        storage.upsertDiscoveredApis(discovered, discoveredAt)
        val keys = LinkedHashSet<String>()
        for (metadata in discovered ?: throw NullPointerException()) {
            keys.add(metadata.identity.apiKey())
        }
        storage.markApisNotRediscovered(keys, discoveredAt)
    }

    override fun recordInvocations(events: List<ApiRequestEvent>?) {
        storage.saveRequestEvents(events)
    }

    override fun inventory(query: InventoryQuery?): InventoryPage = storage.queryInventory(query)

    override fun findApi(id: Long): Optional<ApiInventoryEntry> = storage.findApiById(id)

    override fun searchRequestLogs(query: RequestLogQuery?): RequestLogPage = storage.searchRequestLogs(query)

    override fun findRequestLog(id: Long): Optional<ApiRequestLog> = storage.findRequestLogById(id)

    override fun traffic(query: TrafficQuery?): TrafficSeries = storage.queryTraffic(query)

    override fun latency(query: LatencyQuery?): ApiLatencyStatistics = storage.queryLatency(query)

    override fun usageSnapshot(query: UsageSnapshotQuery?): ApiUsageSnapshot = storage.queryUsageSnapshot(query)

    override fun findUnusedCandidates(query: UnusedApiQuery?): List<UnusedApiCandidate> = storage.findUnusedCandidates(query)

    override fun deleteExpiredRequestLogs(
        cutoff: Instant?,
        batchSize: Int,
    ): Int = storage.deleteRequestLogsOlderThan(cutoff, batchSize)

    override fun storageConnected(): Boolean = storage.checkConnectivity()
}
