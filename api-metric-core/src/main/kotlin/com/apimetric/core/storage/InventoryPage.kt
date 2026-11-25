package com.apimetric.core.storage

import com.apimetric.core.internal.Checks.immutableList
import com.apimetric.core.internal.Checks.nonNegative
import com.apimetric.core.internal.Checks.required
import com.apimetric.core.internal.recordEquals
import com.apimetric.core.internal.recordHash
import com.apimetric.core.internal.recordString
import com.apimetric.core.model.ApiInventoryEntry

/** One page of inventory rows plus the total number of rows that match the query. */
class InventoryPage(
    items: List<ApiInventoryEntry>?,
    totalCount: Long,
    page: PageRequest?,
) {
    @get:JvmName("items")
    val items: List<ApiInventoryEntry>

    @get:JvmName("totalCount")
    val totalCount: Long

    @get:JvmName("page")
    val page: PageRequest

    init {
        val copied = immutableList(items, "items")
        val normalizedTotal = nonNegative(totalCount, "totalCount")
        val checkedPage = required(page, "page")
        if (copied.size > checkedPage.pageSize) {
            throw IllegalArgumentException("items cannot exceed pageSize")
        }
        this.items = copied
        this.totalCount = normalizedTotal
        this.page = checkedPage
    }

    fun getItems(): List<ApiInventoryEntry> = items

    fun getTotalCount(): Long = totalCount

    fun getPage(): PageRequest = page

    override fun equals(other: Any?): Boolean =
        recordEquals(other) {
            items == it.items && totalCount == it.totalCount && page == it.page
        }

    override fun hashCode(): Int = recordHash(items, totalCount, page)

    override fun toString(): String =
        recordString(
            "InventoryPage",
            "items" to items,
            "totalCount" to totalCount,
            "page" to page,
        )
}
