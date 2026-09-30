plugins {
    id("ragchat.kmp.library")
    id("ragchat.publishing")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":sdk-api"))
        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.content.negotiation)
        implementation(libs.ktor.serialization.kotlinx.json)
    }
    sourceSets.androidMain.dependencies {
        implementation(libs.ktor.client.okhttp)
    }
    sourceSets.iosMain.dependencies {
        implementation(libs.ktor.client.darwin)
    }
}
