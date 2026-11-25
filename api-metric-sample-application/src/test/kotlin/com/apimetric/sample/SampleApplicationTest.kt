package com.apimetric.sample

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.util.TreeSet

@SpringBootTest(properties = ["api-metric.enabled=false"])
class SampleApplicationTest
    @Autowired
    constructor(
        private val handlerMapping: RequestMappingHandlerMapping,
    ) {
        @Test
        fun loadsSampleRoutes() {
            val routes = TreeSet<String>()
            for (info in handlerMapping.handlerMethods.keys) {
                val patterns = info.pathPatternsCondition ?: continue
                for (method in info.methodsCondition.methods) {
                    for (pattern in patterns.patternValues) {
                        routes.add(method.name + " " + pattern)
                    }
                }
            }

            assertThat(routes)
                .contains(
                    "GET /sample/items",
                    "GET /sample/items/{id}",
                    "POST /sample/items",
                    "PUT /sample/items/{id}",
                    "DELETE /sample/items/{id}",
                    "POST /sample/orders",
                    "GET /api/v1/products",
                    "GET /api/v1/products/{id}",
                    "POST /api/v1/products",
                    "PUT /api/v1/products/{id}",
                    "DELETE /api/v1/products/{id}",
                    "GET /api/v2/products",
                    "GET /api/v2/products/{id}",
                    "POST /api/v2/products",
                    "PUT /api/v2/products/{id}",
                    "DELETE /api/v2/products/{id}",
                )
        }
    }
