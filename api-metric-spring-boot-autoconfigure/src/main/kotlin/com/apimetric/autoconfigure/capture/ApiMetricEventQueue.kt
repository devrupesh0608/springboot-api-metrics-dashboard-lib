package com.apimetric.autoconfigure.capture

import com.apimetric.core.model.ApiRequestEvent
import com.apimetric.core.service.ApiMetricService
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

/**
 * Bounded queue of monitoring events. A full queue drops the event instead of blocking the business
 * request. Events still in the queue are lost if the process is killed.
 */
class ApiMetricEventQueue(
    private val service: ApiMetricService,
    queueCapacity: Int,
    private val batchSize: Int,
    private val flushIntervalMs: Long,
    private val retentionDays: Int,
) : AutoCloseable {
    private val queue: BlockingQueue<ApiRequestEvent> = ArrayBlockingQueue(queueCapacity)
    private val dropped = AtomicLong()
    private val failures = AtomicLong()

    @Volatile
    private var lastSuccessfulFlush: Instant? = null

    @Volatile
    private var lastFailureSummary: String? = null

    @Volatile
    private var running = true

    private val worker = Thread(Runnable { loop() }, "api-metric-writer")

    init {
        worker.isDaemon = true
        worker.start()
    }

    fun publish(event: ApiRequestEvent): Boolean {
        if (!queue.offer(event)) {
            dropped.incrementAndGet()
            return false
        }
        return true
    }

    fun queueDepth(): Int = queue.size

    fun droppedCount(): Long = dropped.get()

    fun failureCount(): Long = failures.get()

    fun lastSuccessfulFlush(): Instant? = lastSuccessfulFlush

    fun lastFailureSummary(): String? = lastFailureSummary

    override fun close() {
        running = false
        worker.interrupt()
        try {
            worker.join(Duration.ofSeconds(5).toMillis())
        } catch (ex: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private fun loop() {
        deleteExpired()
        var lastRetention = System.nanoTime()
        while (running || queue.isNotEmpty()) {
            val batch = ArrayList<ApiRequestEvent>(batchSize)
            try {
                val first = queue.poll(flushIntervalMs, TimeUnit.MILLISECONDS)
                if (first != null) {
                    batch.add(first)
                    queue.drainTo(batch, batchSize - 1)
                }
            } catch (ex: InterruptedException) {
                if (!running) {
                    queue.drainTo(batch, batchSize)
                } else {
                    Thread.currentThread().interrupt()
                    continue
                }
            }
            if (batch.isNotEmpty()) {
                flush(batch)
            }
            if (System.nanoTime() - lastRetention > Duration.ofHours(1).toNanos()) {
                deleteExpired()
                lastRetention = System.nanoTime()
            }
        }
    }

    private fun flush(batch: List<ApiRequestEvent>) {
        var backoffMs = 50L
        for (attempt in 1..MAX_ATTEMPTS) {
            try {
                service.recordInvocations(batch)
                lastSuccessfulFlush = Instant.now()
                lastFailureSummary = null
                return
            } catch (ex: RuntimeException) {
                failures.incrementAndGet()
                lastFailureSummary = ex.javaClass.simpleName
                log.warn(
                    "API Metric failed to persist a batch of {} events ({}/{}): {}",
                    batch.size,
                    attempt,
                    MAX_ATTEMPTS,
                    ex.javaClass.simpleName,
                )
                if (attempt == MAX_ATTEMPTS || !running) {
                    dropped.addAndGet(batch.size.toLong())
                    return
                }
                try {
                    Thread.sleep(backoffMs)
                } catch (interrupted: InterruptedException) {
                    Thread.currentThread().interrupt()
                    dropped.addAndGet(batch.size.toLong())
                    return
                }
                backoffMs = min(backoffMs * 2, 1000L)
            }
        }
    }

    private fun deleteExpired() {
        try {
            service.deleteExpiredRequestLogs(Instant.now().minus(Duration.ofDays(retentionDays.toLong())), 1000)
        } catch (ex: RuntimeException) {
            log.warn("API Metric retention cleanup failed: {}", ex.javaClass.simpleName)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ApiMetricEventQueue::class.java)
        private const val MAX_ATTEMPTS = 3
    }
}
