plugins {
    id("ragchat.kmp.library")
    id("ragchat.publishing")
    id("ragchat.sqldelight")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":sdk-api"))
        implementation(libs.sqldelight.runtime)
        implementation(libs.sqldelight.coroutines.extensions)
        implementation(libs.okio)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.kotlinx.datetime)
    }

    sourceSets.androidMain.dependencies {
        implementation(libs.sqldelight.android.driver)
    }

    sourceSets.iosMain.dependencies {
        implementation(libs.sqldelight.native.driver)
    }

    sourceSets.commonTest.dependencies {
        implementation(project(":sdk-testing"))
    }

    sourceSets.androidUnitTest.dependencies {
        implementation(libs.sqldelight.sqlite.driver)
    }
}

sqldelight {
    databases {
        create("RagChatDatabase") {
            packageName.set("com.ragchat.sdk.storage.db")
        }
    }
}
