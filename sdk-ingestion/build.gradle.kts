plugins {
    id("ragchat.kmp.library")
    id("ragchat.publishing")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":sdk-api"))
    }
}
