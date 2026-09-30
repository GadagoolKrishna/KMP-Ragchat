plugins {
    id("ragchat.android.application")
    id("ragchat.compose")
}

dependencies {
    implementation(project(":sdk"))
    implementation(project(":sdk-ui-compose"))
    implementation(project(":sdk-testing"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
}
