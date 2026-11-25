plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("com.diffplug.spotless")
}

description = "Sample Servlet MVC application that uses the API Metric starter."

dependencies {
    implementation(project(":api-metric-spring-boot-starter"))
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

springBoot {
    mainClass.set("com.apimetric.sample.SampleApplication")
}
