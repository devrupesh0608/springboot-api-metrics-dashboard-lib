pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        id("org.springframework.boot") version "4.2.0"
        id("io.spring.dependency-management") version "1.1.7"
        id("com.diffplug.spotless") version "8.12.0"
        id("org.jetbrains.kotlin.jvm") version "2.4.21"
        id("org.jetbrains.kotlin.plugin.spring") version "2.4.21"
        id("org.jetbrains.dokka") version "2.3.0"
        id("com.vanniktech.maven.publish") version "0.37.0"
        id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention")
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "api-metric-library"

include(
    "api-metric-core",
    "api-metric-storage-mysql",
    "api-metric-spring-boot-autoconfigure",
    "api-metric-spring-boot-starter",
    "api-metric-sample-application",
)
