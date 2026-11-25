package com.apimetric.core.model

import com.apimetric.core.enums.DiscoveryStatus
import com.apimetric.core.internal.Checks.blankToNull
import com.apimetric.core.internal.Checks.nonNegative
import com.apimetric.core.internal.Checks.positive
import com.apimetric.core.internal.Checks.required
import com.apimetric.core.internal.Checks.requiredText
import com.apimetric.core.internal.recordEquals
import com.apimetric.core.internal.recordHash
import com.apimetric.core.internal.recordString
import java.time.Instant

/**
 * One inventory row. Usage classification is derived from these facts by `ApiUsageClassifier`; it
 * is not stored on the row.
 */
class ApiInventoryEntry(
    id: Long,
    identity: ApiIdentity?,
    controllerName: String?,
    handlerMethod: String?,
    displayName: String?,
    firstDiscoveredAt: Instant?,
    lastDiscoveredAt: Instant?,
    lastAccessedAt: Instant?,
    totalHits: Long,
    successfulHits: Long,
    failedHits: Long,
    discoveryStatus: DiscoveryStatus?,
) {
    @get:JvmName("id")
    val id: Long

    @get:JvmName("identity")
    val identity: ApiIdentity

    @get:JvmName("controllerName")
    val controllerName: String

    @get:JvmName("handlerMethod")
    val handlerMethod: String

    @get:JvmName("displayName")
    val displayName: String?

    @get:JvmName("firstDiscoveredAt")
    val firstDiscoveredAt: Instant

    @get:JvmName("lastDiscoveredAt")
    val lastDiscoveredAt: Instant

    @get:JvmName("lastAccessedAt")
    val lastAccessedAt: Instant?

    @get:JvmName("totalHits")
    val totalHits: Long

    @get:JvmName("successfulHits")
    val successfulHits: Long

    @get:JvmName("failedHits")
    val failedHits: Long

    @get:JvmName("discoveryStatus")
    val discoveryStatus: DiscoveryStatus

    init {
        val normalizedId = positive(id, "id")
        val checkedIdentity = required(identity, "identity")
        val normalizedController = requiredText(controllerName, "controllerName")
        val normalizedHandler = requiredText(handlerMethod, "handlerMethod")
        val normalizedDisplayName = blankToNull(displayName)
        val checkedFirst = required(firstDiscoveredAt, "firstDiscoveredAt")
        val checkedLast = required(lastDiscoveredAt, "lastDiscoveredAt")
        if (checkedLast.isBefore(checkedFirst)) {
            throw IllegalArgumentException("lastDiscoveredAt must not be before firstDiscoveredAt")
        }
        val normalizedTotal = nonNegative(totalHits, "totalHits")
        val normalizedSuccessful = nonNegative(successfulHits, "successfulHits")
        val normalizedFailed = nonNegative(failedHits, "failedHits")
        if (normalizedSuccessful + normalizedFailed != normalizedTotal) {
            throw IllegalArgumentException("successfulHits and failedHits must add up to totalHits")
        }
        val checkedStatus = required(discoveryStatus, "discoveryStatus")
        this.id = normalizedId
        this.identity = checkedIdentity
        this.controllerName = normalizedController
        this.handlerMethod = normalizedHandler
        this.displayName = normalizedDisplayName
        this.firstDiscoveredAt = checkedFirst
        this.lastDiscoveredAt = checkedLast
        this.lastAccessedAt = lastAccessedAt
        this.totalHits = normalizedTotal
        this.successfulHits = normalizedSuccessful
        this.failedHits = normalizedFailed
        this.discoveryStatus = checkedStatus
    }

    fun getId(): Long = id

    fun getIdentity(): ApiIdentity = identity

    fun getControllerName(): String = controllerName

    fun getHandlerMethod(): String = handlerMethod

    fun getDisplayName(): String? = displayName

    fun getFirstDiscoveredAt(): Instant = firstDiscoveredAt

    fun getLastDiscoveredAt(): Instant = lastDiscoveredAt

    fun getLastAccessedAt(): Instant? = lastAccessedAt

    fun getTotalHits(): Long = totalHits

    fun getSuccessfulHits(): Long = successfulHits

    fun getFailedHits(): Long = failedHits

    fun getDiscoveryStatus(): DiscoveryStatus = discoveryStatus

    override fun equals(other: Any?): Boolean =
        recordEquals(other) {
            id == it.id &&
                identity == it.identity &&
                controllerName == it.controllerName &&
                handlerMethod == it.handlerMethod &&
                displayName == it.displayName &&
                firstDiscoveredAt == it.firstDiscoveredAt &&
                lastDiscoveredAt == it.lastDiscoveredAt &&
                lastAccessedAt == it.lastAccessedAt &&
                totalHits == it.totalHits &&
                successfulHits == it.successfulHits &&
                failedHits == it.failedHits &&
                discoveryStatus == it.discoveryStatus
        }

    override fun hashCode(): Int =
        recordHash(
            id,
            identity,
            controllerName,
            handlerMethod,
            displayName,
            firstDiscoveredAt,
            lastDiscoveredAt,
            lastAccessedAt,
            totalHits,
            successfulHits,
            failedHits,
            discoveryStatus,
        )

    override fun toString(): String =
        recordString(
            "ApiInventoryEntry",
            "id" to id,
            "identity" to identity,
            "controllerName" to controllerName,
            "handlerMethod" to handlerMethod,
            "displayName" to displayName,
            "firstDiscoveredAt" to firstDiscoveredAt,
            "lastDiscoveredAt" to lastDiscoveredAt,
            "lastAccessedAt" to lastAccessedAt,
            "totalHits" to totalHits,
            "successfulHits" to successfulHits,
            "failedHits" to failedHits,
            "discoveryStatus" to discoveryStatus,
        )
}
