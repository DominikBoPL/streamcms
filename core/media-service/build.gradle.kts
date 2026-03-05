// Here we APPLY the plugins that were declared (but not applied)
// in the root build.gradle.kts
plugins {
    // alias() references plugins from libs.versions.toml
    alias(libs.plugins.kotlin.jvm)

    // kotlin-spring adds "open" modifier to Spring-annotated classes
    // In Kotlin, classes are "final" by default (can't be subclassed)
    // Spring needs to subclass your beans for proxying (AOP, transactions)
    // This plugin handles that automatically
    alias(libs.plugins.kotlin.spring)

    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    // libs.* references entries from gradle/libs.versions.toml
    // No version numbers here - they're managed centrally!

    // Web layer - REST endpoints
    implementation(libs.spring.boot.starter.web)

    // Database - JPA + Hibernate
    implementation(libs.spring.boot.starter.data.jpa)

    // Security - we'll add JWT validation later
    implementation(libs.spring.boot.starter.security)

    // Messaging - RabbitMQ integration
    implementation(libs.spring.boot.starter.amqp)

    // PostgreSQL driver
    runtimeOnly(libs.postgresql)
    // "runtimeOnly" means: needed to run but not to compile
    // The driver is loaded by Spring at startup, not referenced in code directly

    // Testing
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.mockk)
}
