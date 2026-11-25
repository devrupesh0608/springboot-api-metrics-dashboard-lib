package com.apimetric.autoconfigure

import com.apimetric.autoconfigure.capture.ApiMetricCaptureFilter
import com.apimetric.autoconfigure.capture.ApiMetricDiscovery
import com.apimetric.autoconfigure.capture.ApiMetricEventQueue
import com.apimetric.autoconfigure.capture.ApiMetricRouteInterceptor
import com.apimetric.autoconfigure.capture.PayloadSanitizer
import com.apimetric.autoconfigure.web.ApiMetricBasicAuthFilter
import com.apimetric.autoconfigure.web.ApiMetricDashboardController
import com.apimetric.autoconfigure.web.ApiMetricInternalController
import com.apimetric.core.service.ApiMetricService
import com.apimetric.core.service.DefaultApiMetricService
import com.apimetric.storage.mysql.MysqlApiMetricStorage
import com.apimetric.storage.mysql.MysqlConnectionSettings
import com.apimetric.storage.mysql.MysqlMonitoringDatabase
import jakarta.servlet.DispatcherType
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.core.Ordered
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(WebMvcConfigurer::class)
@ConditionalOnProperty(prefix = "api-metric", name = ["enabled"], havingValue = "true")
@EnableConfigurationProperties(ApiMetricProperties::class)
open class ApiMetricAutoConfiguration {
    @Bean(destroyMethod = "close")
    open fun apiMetricMonitoringDatabase(properties: ApiMetricProperties): MysqlMonitoringDatabase {
        properties.validate()
        val mysql = properties.storage.mysql
        return MysqlMonitoringDatabase(
            MysqlConnectionSettings(
                mysql.url!!,
                mysql.username!!,
                mysql.password!!,
                mysql.driverClassName,
                mysql.maximumPoolSize,
                mysql.minimumIdle,
                mysql.connectionTimeoutMs,
            ),
        )
    }

    @Bean
    open fun apiMetricStorage(database: MysqlMonitoringDatabase): MysqlApiMetricStorage = MysqlApiMetricStorage(database)

    @Bean
    open fun apiMetricService(storage: MysqlApiMetricStorage): ApiMetricService = DefaultApiMetricService(storage)

    @Bean(destroyMethod = "close")
    open fun apiMetricEventQueue(
        properties: ApiMetricProperties,
        service: ApiMetricService,
    ): ApiMetricEventQueue {
        val processing = properties.processing
        return ApiMetricEventQueue(
            service,
            processing.queueCapacity,
            processing.batchSize,
            processing.flushIntervalMs,
            properties.retention.requestLogsDays,
        )
    }

    @Bean
    open fun apiMetricPayloadSanitizer(properties: ApiMetricProperties): PayloadSanitizer {
        val capture = properties.capture
        return PayloadSanitizer(
            capture.maxPayloadSize,
            capture.maskFields,
            capture.excludedContentTypes,
        )
    }

    @Bean
    open fun apiMetricRouteInterceptor(properties: ApiMetricProperties): ApiMetricRouteInterceptor = ApiMetricRouteInterceptor(properties)

    @Bean
    open fun apiMetricWebMvcConfigurer(interceptor: ApiMetricRouteInterceptor): WebMvcConfigurer =
        object : WebMvcConfigurer {
            override fun addInterceptors(registry: InterceptorRegistry) {
                registry.addInterceptor(interceptor)
            }
        }

    @Bean
    open fun apiMetricCaptureFilter(
        properties: ApiMetricProperties,
        routes: ApiMetricRouteInterceptor,
        queue: ApiMetricEventQueue,
        sanitizer: PayloadSanitizer,
    ): FilterRegistrationBean<ApiMetricCaptureFilter> {
        val registration = FilterRegistrationBean<ApiMetricCaptureFilter>()
        registration.setFilter(ApiMetricCaptureFilter(properties, routes, queue, sanitizer))
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20)
        registration.addUrlPatterns("/*")
        registration.setDispatcherTypes(DispatcherType.REQUEST)
        return registration
    }

    @Bean
    open fun apiMetricBasicAuthFilter(properties: ApiMetricProperties): FilterRegistrationBean<ApiMetricBasicAuthFilter> {
        val registration = FilterRegistrationBean<ApiMetricBasicAuthFilter>()
        registration.setFilter(ApiMetricBasicAuthFilter(properties))
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10)
        val patterns = ApiMetricBasicAuthFilter.urlPatterns(properties.dashboard.path, properties.internalPath)
        if (patterns.isEmpty()) {
            registration.isEnabled = false
        } else {
            registration.addUrlPatterns(*patterns.toTypedArray())
        }
        registration.setDispatcherTypes(DispatcherType.REQUEST)
        return registration
    }

    @Bean
    open fun apiMetricDiscovery(
        mapping: RequestMappingHandlerMapping,
        service: ApiMetricService,
        routes: ApiMetricRouteInterceptor,
    ): ApiMetricDiscovery = ApiMetricDiscovery(mapping, service, routes)

    @Bean
    open fun apiMetricInternalController(
        properties: ApiMetricProperties,
        service: ApiMetricService,
        queue: ApiMetricEventQueue,
    ): ApiMetricInternalController = ApiMetricInternalController(properties, service, queue)

    @Bean
    @ConditionalOnProperty(
        prefix = "api-metric.dashboard",
        name = ["enabled"],
        havingValue = "true",
        matchIfMissing = true,
    )
    open fun apiMetricDashboardController(properties: ApiMetricProperties): ApiMetricDashboardController =
        ApiMetricDashboardController(properties)
}
