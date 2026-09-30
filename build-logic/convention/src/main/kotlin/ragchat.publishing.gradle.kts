plugins {
    `maven-publish`
    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

publishing {
    repositories {
        maven {
            name = "LocalBuild"
            url = uri(layout.buildDirectory.dir("repo"))
        }
    }
}
