import com.diffplug.gradle.spotless.SpotlessExtension
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import groovy.util.Node
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management") apply false
    id("com.diffplug.spotless") apply false
    id("com.vanniktech.maven.publish") apply false
    id("org.jetbrains.dokka") apply false
    kotlin("jvm") apply false
    kotlin("plugin.spring") apply false
}

if (JavaVersion.current() < JavaVersion.VERSION_17) {
    throw GradleException("Java 17 or later is required to build. Current version is ${JavaVersion.current()}.")
}

val githubRepository = "https://github.com/devrupesh0608/springboot-api-metrics-dashboard-lib"

subprojects {
    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(17))
            }
        }

        tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
            options.compilerArgs.add("-parameters")
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }

        dependencies {
            add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
        }
    }

    plugins.withId("io.spring.dependency-management") {
        extensions.configure<DependencyManagementExtension> {
            imports {
                mavenBom("org.springframework.boot:spring-boot-dependencies:4.2.0")
            }
            // Write resolved versions into the published POM. A dependencyManagement
            // import is not applied to Maven consumers, and Central expects a version
            // on each dependency.
            generatedPomCustomization {
                enabled(false)
            }
        }
    }

    plugins.withId("maven-publish") {
        extensions.configure<PublishingExtension>("publishing") {
            publications.withType(MavenPublication::class.java).configureEach {
                versionMapping {
                    usage("java-api") {
                        fromResolutionOf("runtimeClasspath")
                    }
                    usage("java-runtime") {
                        fromResolutionResult()
                    }
                }
            }
        }
    }

    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
            jvmToolchain(17)
        }
        tasks.withType<KotlinCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
                javaParameters.set(true)
            }
        }
    }

    plugins.withId("com.diffplug.spotless") {
        extensions.configure<SpotlessExtension> {
            java {
                target("src/**/*.java")
                googleJavaFormat("1.28.0")
                trimTrailingWhitespace()
                endWithNewline()
            }
            kotlin {
                target("src/**/*.kt")
                ktlint("1.5.0")
                trimTrailingWhitespace()
                endWithNewline()
            }
        }
        plugins.withId("java") {
            tasks.named("check").configure {
                dependsOn(tasks.named("spotlessCheck"))
            }
        }
    }

    plugins.withId("com.vanniktech.maven.publish") {
        extensions.configure<MavenPublishBaseExtension>("mavenPublishing") {
            publishToMavenCentral(automaticRelease = false)
            signAllPublications()
            pom {
                name.set(project.name)
                description.set(project.provider { project.description ?: project.name })
                inceptionYear.set("2026")
                url.set(githubRepository)
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("devrupesh0608")
                        name.set("devrupesh0608")
                        url.set("https://github.com/devrupesh0608")
                    }
                }
                scm {
                    url.set(githubRepository)
                    connection.set("scm:git:git://github.com/devrupesh0608/springboot-api-metrics-dashboard-lib.git")
                    developerConnection.set("scm:git:ssh://git@github.com/devrupesh0608/springboot-api-metrics-dashboard-lib.git")
                }
                withXml {
                    val dependencies = (asNode() as Node).children().filterIsInstance<Node>()
                        .firstOrNull { it.name().toString().substringAfterLast('}') == "dependencies" }
                        ?: return@withXml
                    dependencies.children().filterIsInstance<Node>().forEach { dependency ->
                        val artifactId = dependency.children().filterIsInstance<Node>()
                            .firstOrNull { it.name().toString().substringAfterLast('}') == "artifactId" }
                            ?.text()
                        val alreadyOptional = dependency.children().filterIsInstance<Node>()
                            .any { it.name().toString().substringAfterLast('}') == "optional" }
                        if (artifactId == "spring-boot-autoconfigure" && !alreadyOptional) {
                            dependency.appendNode("optional", "true")
                        }
                    }
                }
            }
        }
    }
}
