plugins {
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.spotless) apply false
}

allprojects {
    group = "com.ragchat.sdk"
    version = "0.1.0-SNAPSHOT"
}
