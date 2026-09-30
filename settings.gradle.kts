pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ragchat-sdk"

include(":sdk-api")
include(":sdk-core")
include(":sdk-ingestion")
include(":sdk-retrieval")
include(":sdk-governance")
include(":sdk-storage")
include(":sdk-platform-android")
include(":sdk-platform-ios")
include(":sdk-cloud")
include(":sdk-ui-compose")
include(":sdk")
include(":sdk-testing")
include(":sample-android")
include(":benchmarks")
