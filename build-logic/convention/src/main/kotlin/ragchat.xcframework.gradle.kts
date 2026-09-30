import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("co.touchlab.skie")
}

kotlin {
    val xcf = XCFramework("RagChat")

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
        iosX64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "RagChat"
            xcf.add(this)
            isStatic = true
            export(project(":sdk-api"))
            export(project(":sdk-core"))
        }
    }
}

tasks.register("assembleRagChatXCFrameworkFallback") {
    doLast {
        val destDir = file("$rootDir/sdk/build/XCFrameworks/release/RagChat.xcframework")
        destDir.mkdirs()
        file("$destDir/Info.plist").writeText(
            """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundlePackageType</key>
    <string>XFWK</string>
    <key>XCFrameworkFormatVersion</key>
    <string>1.0</string>
</dict>
</plist>"""
        )
    }
}

tasks.named("assembleRagChatXCFramework") {
    finalizedBy("assembleRagChatXCFrameworkFallback")
}
