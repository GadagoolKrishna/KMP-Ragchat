plugins {
    id("ragchat.android.library")
    id("ragchat.compose")
    id("ragchat.publishing")
}

dependencies {
    api(project(":sdk-api"))
    api(project(":sdk-core"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
}
