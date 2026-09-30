plugins {
    id("ragchat.kmp.library")
    id("ragchat.publishing")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":sdk-api"))
        api(project(":sdk-storage"))
        implementation(libs.okio)
        implementation(libs.kotlinx.coroutines.core)
    }

    sourceSets.iosMain.dependencies {
        implementation(libs.sqldelight.native.driver)
    }
}
