// This is the entry point for Gradle - it defines the project name
// and tells Gradle which subprojects (modules) exist
rootProject.name = "streamcms"

// Each include() call registers a subproject
// The path uses ":" as separator, not "/"
// Gradle will look for build.gradle.kts in each of these directories
include(
    // Core services - the business logic layer
    "core:media-service",
    "core:cms-service",
    "core:live-service",
    "core:billing-service",

    // API Gateway - the proxy layer
    "api-proxy:gateway"

    // Frontend is managed by npm, not Gradle
    // so we don't include it here
)
