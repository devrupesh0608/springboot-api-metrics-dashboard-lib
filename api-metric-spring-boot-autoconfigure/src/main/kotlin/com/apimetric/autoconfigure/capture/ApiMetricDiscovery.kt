package com.apimetric.autoconfigure.capture

import com.apimetric.core.model.ApiIdentity
import com.apimetric.core.model.ApiMetadata
import com.apimetric.core.service.ApiMetricService
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.time.Instant

/** Records Spring MVC routes after the application context has started. */
class ApiMetricDiscovery(
    private val handlerMapping: RequestMappingHandlerMapping,
    private val service: ApiMetricService,
    private val routes: ApiMetricRouteInterceptor,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        val discovered = ArrayList<ApiMetadata>()
        for ((info, handler) in handlerMapping.handlerMethods) {
            collect(info, handler, discovered)
        }
        service.registerDiscoveredApis(discovered, Instant.now())
        log.info("API Metric discovered {} routes", discovered.size)
    }

    private fun collect(
        info: RequestMappingInfo,
        handler: HandlerMethod,
        discovered: MutableList<ApiMetadata>,
    ) {
        if (handler.beanType.name.startsWith("org.springframework.boot")) {
            return
        }
        val paths = info.pathPatternsCondition?.patternValues ?: emptySet()
        val declaredMethods = info.methodsCondition.methods
        val methods: Set<RequestMethod> =
            if (declaredMethods.isEmpty()) {
                setOf(
                    RequestMethod.GET,
                    RequestMethod.POST,
                    RequestMethod.PUT,
                    RequestMethod.DELETE,
                    RequestMethod.PATCH,
                )
            } else {
                declaredMethods
            }
        for (path in paths) {
            if (routes.isExcluded(path)) {
                continue
            }
            for (method in methods) {
                try {
                    discovered.add(
                        ApiMetadata(
                            ApiIdentity.of(method.name, path),
                            handler.beanType.name,
                            handler.method.name,
                            null,
                        ),
                    )
                } catch (ex: RuntimeException) {
                    log.debug("Skipping route {} {}: {}", method, path, ex.javaClass.simpleName)
                }
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ApiMetricDiscovery::class.java)
    }
}
