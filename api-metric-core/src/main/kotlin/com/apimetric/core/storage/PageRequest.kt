package com.apimetric.core.storage

import com.apimetric.core.internal.recordString
import kotlin.jvm.JvmRecord

/** Zero-based page window. `pageSize` is capped so a query cannot load an unbounded page. */
@JvmRecord
data class PageRequest(
    val page: Int,
    val pageSize: Int,
) {
    init {
        if (page < 0) {
            throw IllegalArgumentException("page must be >= 0")
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw IllegalArgumentException("pageSize must be between 1 and $MAX_PAGE_SIZE")
        }
    }

    fun offset(): Long = page.toLong() * pageSize

    override fun toString(): String = recordString("PageRequest", "page" to page, "pageSize" to pageSize)

    companion object {
        const val MAX_PAGE_SIZE: Int = 200

        @JvmStatic
        fun first(pageSize: Int): PageRequest = PageRequest(0, pageSize)
    }
}
