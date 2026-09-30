plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.gradleplugin.android)
    implementation(libs.gradleplugin.kotlin)
    implementation("org.jetbrains.kotlin:kotlin-serialization:2.0.21")
    implementation(libs.gradleplugin.compose.compiler)
    implementation(libs.gradleplugin.sqldelight)
    implementation(libs.gradleplugin.skie)
    implementation(libs.gradleplugin.detekt)
    implementation(libs.gradleplugin.dokka)
    implementation(libs.gradleplugin.kover)
    implementation(libs.gradleplugin.bcv)
    implementation(libs.gradleplugin.spotless)
}
