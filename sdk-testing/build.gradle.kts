plugins {
    id("ragchat.kmp.library")
    id("ragchat.publishing")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":sdk-api"))
            api(project(":sdk-storage"))
            api("org.jetbrains.kotlin:kotlin-test:2.0.21")
            api(libs.kotlinx.coroutines.test)
        }
    }
}
