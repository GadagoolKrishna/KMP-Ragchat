plugins {
    id("ragchat.kmp.library")
    id("ragchat.publishing")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":sdk-api"))
        api(project(":sdk-governance"))
        api(project(":sdk-ingestion"))
        api(project(":sdk-retrieval"))
    }
}
