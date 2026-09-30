// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "RagChatUI",
    platforms: [
        .iOS(.v16),
        .macOS(.v13)
    ],
    products: [
        .library(
            name: "RagChatUI",
            targets: ["RagChatUI"]
        )
    ],
    dependencies: [
        // Dependent on binaryTarget XCFramework
    ],
    targets: [
        .target(
            name: "RagChatUI",
            dependencies: []
        )
    ]
)
