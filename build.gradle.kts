// "plugins" block declares build tools we want to use
// The backtick syntax is used for plugin IDs that contain dots
import org.gradle.api.plugins.JavaPluginExtension

plugins {
    // Kotlin JVM plugin - we apply it here but it will be
    // activated per-subproject (hence "apply false")
    kotlin("jvm") version "1.9.22" apply false

    // Spring Boot plugin - same pattern, declared here,
    // applied only in subprojects that need it
    id("org.springframework.boot") version "3.2.3" apply false

    // Spring dependency management - handles version conflicts
    // between Spring libraries automatically
    id("io.spring.dependency-management") version "1.1.4" apply false
}

// "allprojects" block runs for root AND every subproject
allprojects {
    // The group is like a Maven groupId - your "company" identifier
    // Convention: reverse domain name
    group = "com.streamcms"

    // Project version - we'll update this as we build
    version = "0.1.0-SNAPSHOT"

    repositories {
        // Look for dependencies in Maven Central first
        mavenCentral()
    }
}

// "subprojects" block runs only for subprojects, not root
subprojects {
    // Apply Java plugin to all subprojects
    plugins.apply("java")

    // Configure Java toolchain version
    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    // Common test configuration for all subprojects
    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

