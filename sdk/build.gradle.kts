plugins {
    id("ragchat.kmp.library")
    id("ragchat.xcframework")
    id("ragchat.publishing")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":sdk-api"))
        api(project(":sdk-core"))
        api(project(":sdk-ingestion"))
        api(project(":sdk-retrieval"))
        api(project(":sdk-governance"))
        api(project(":sdk-storage"))
        api(project(":sdk-cloud"))
    }
}
