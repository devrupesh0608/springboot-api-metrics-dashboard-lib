package com.apimetric.sample.catalog

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import java.util.Comparator
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicLong

/** In-memory product store shared by the versioned sample APIs. */
@Component
class ProductCatalog {
    private val ids = AtomicLong(2)
    private val products: ConcurrentMap<String, Product> = ConcurrentHashMap()

    init {
        products["1"] = Product("1", "Notebook", "NB-1", 120)
    }

    fun list(): List<Product> =
        products.values
            .stream()
            .sorted(Comparator.comparingLong { product: Product -> java.lang.Long.parseLong(product.id) })
            .toList()

    fun get(id: String): Product {
        val product = products[id]
        if (product == null) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "product was not found")
        }
        return product
    }

    fun create(
        name: String?,
        sku: String?,
        price: Int,
    ): Product {
        requireName(name)
        requirePrice(price)
        val id = ids.getAndIncrement().toString()
        val product = Product(id, name!!.trim(), if (sku == null) "" else sku.trim(), price)
        products[id] = product
        return product
    }

    fun update(
        id: String,
        name: String?,
        sku: String?,
        price: Int,
    ): Product {
        requireName(name)
        requirePrice(price)
        val existing = get(id)
        val updated = Product(existing.id, name!!.trim(), if (sku == null) "" else sku.trim(), price)
        products[id] = updated
        return updated
    }

    fun delete(id: String) {
        if (products.remove(id) == null) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "product was not found")
        }
    }

    companion object {
        private fun requireName(name: String?) {
            if (name == null || name.isBlank()) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required")
            }
        }

        private fun requirePrice(price: Int) {
            if (price < 0) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "price must be >= 0")
            }
        }
    }
}
