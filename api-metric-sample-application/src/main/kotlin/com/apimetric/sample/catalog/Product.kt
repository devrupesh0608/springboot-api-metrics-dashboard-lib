package com.apimetric.sample.catalog

/** Product stored by the sample catalog. */
data class Product(
    val id: String,
    val name: String,
    val sku: String,
    val price: Int,
)
