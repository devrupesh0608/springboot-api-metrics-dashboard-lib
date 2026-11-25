package com.apimetric.core.storage

import com.apimetric.core.enums.ApiUsageStatus
import com.apimetric.core.enums.DiscoveryStatus
import com.apimetric.core.enums.InventorySort
import com.apimetric.core.enums.SortDirection
import com.apimetric.core.internal.Checks.blankToNull
import com.apimetric.core.internal.Checks.positiveDuration
import com.apimetric.core.internal.Checks.required
import com.apimetric.core.internal.recordEquals
import com.apimetric.core.internal.recordHash
import com.apimetric.core.internal.recordString
import java.time.Duration
import java.time.Instant
import java.util.Locale

/** Filter, sort, and page for the API inventory. */
class InventoryQuery(
    searchText: String?,
    httpMethod: String?,
    discoveryStatus: DiscoveryStatus?,
    usageStatus: ApiUsageStatus?,
    inactivityThreshold: Duration?,
    evaluatedAt: Instant?,
    sort: InventorySort?,
    direction: SortDirection?,
    page: PageRequest?,
) {
    @get:JvmName("searchText")
    val searchText: String?

    @get:JvmName("httpMethod")
    val httpMethod: String?

    @get:JvmName("discoveryStatus")
    val discoveryStatus: DiscoveryStatus?

    @get:JvmName("usageStatus")
    val usageStatus: ApiUsageStatus?

    @get:JvmName("inactivityThreshold")
    val inactivityThreshold: Duration

    @get:JvmName("evaluatedAt")
    val evaluatedAt: Instant

    @get:JvmName("sort")
    val sort: InventorySort

    @get:JvmName("direction")
    val direction: SortDirection

    @get:JvmName("page")
    val page: PageRequest

    init {
        val normalizedSearch = blankToNull(searchText)
        val normalizedMethod =
            if (httpMethod == null) {
                null
            } else {
                blankToNull(httpMethod.uppercase(Locale.ROOT))
            }
        val normalizedThreshold = positiveDuration(inactivityThreshold, "inactivityThreshold")
        val checkedEvaluatedAt = required(evaluatedAt, "evaluatedAt")
        val checkedSort = required(sort, "sort")
        val checkedDirection = required(direction, "direction")
        val checkedPage = required(page, "page")
        this.searchText = normalizedSearch
        this.httpMethod = normalizedMethod
        this.discoveryStatus = discoveryStatus
        this.usageStatus = usageStatus
        this.inactivityThreshold = normalizedThreshold
        this.evaluatedAt = checkedEvaluatedAt
        this.sort = checkedSort
        this.direction = checkedDirection
        this.page = checkedPage
    }

    fun getSearchText(): String? = searchText

    fun getHttpMethod(): String? = httpMethod

    fun getDiscoveryStatus(): DiscoveryStatus? = discoveryStatus

    fun getUsageStatus(): ApiUsageStatus? = usageStatus

    fun getInactivityThreshold(): Duration = inactivityThreshold

    fun getEvaluatedAt(): Instant = evaluatedAt

    fun getSort(): InventorySort = sort

    fun getDirection(): SortDirection = direction

    fun getPage(): PageRequest = page

    override fun equals(other: Any?): Boolean =
        recordEquals(other) {
            searchText == it.searchText &&
                httpMethod == it.httpMethod &&
                discoveryStatus == it.discoveryStatus &&
                usageStatus == it.usageStatus &&
                inactivityThreshold == it.inactivityThreshold &&
                evaluatedAt == it.evaluatedAt &&
                sort == it.sort &&
                direction == it.direction &&
                page == it.page
        }

    override fun hashCode(): Int =
        recordHash(
            searchText,
            httpMethod,
            discoveryStatus,
            usageStatus,
            inactivityThreshold,
            evaluatedAt,
            sort,
            direction,
            page,
        )

    override fun toString(): String =
        recordString(
            "InventoryQuery",
            "searchText" to searchText,
            "httpMethod" to httpMethod,
            "discoveryStatus" to discoveryStatus,
            "usageStatus" to usageStatus,
            "inactivityThreshold" to inactivityThreshold,
            "evaluatedAt" to evaluatedAt,
            "sort" to sort,
            "direction" to direction,
            "page" to page,
        )
}
