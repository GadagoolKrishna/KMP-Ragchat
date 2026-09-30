plugins {
    id("ragchat.kmp.library")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":sdk-retrieval"))
        implementation(project(":sdk-ingestion"))
    }
}
