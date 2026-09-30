plugins {
    id("ragchat.android.library")
    id("ragchat.publishing")
}

dependencies {
    api(project(":sdk-api"))
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.work.runtime.ktx)
}
