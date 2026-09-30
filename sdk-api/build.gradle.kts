plugins {
    id("ragchat.kmp.library")
    id("ragchat.publishing")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(libs.kotlinx.coroutines.core)
        api(libs.kotlinx.serialization.json)
        api(libs.kotlinx.datetime)
        api(libs.okio)
    }
}
