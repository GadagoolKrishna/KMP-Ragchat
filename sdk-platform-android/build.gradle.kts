plugins {
    id("ragchat.android.library")
    id("ragchat.publishing")
}

dependencies {
    api(project(":sdk-api"))
    api(project(":sdk-storage"))
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.okio)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.sqldelight.android.driver)
    testImplementation(kotlin("test"))
}
