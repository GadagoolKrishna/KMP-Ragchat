// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "SampleApp",
    platforms: [
        .iOS(.v16),
        .macOS(.v13)
    ],
    products: [
        .executable(
            name: "SampleApp",
            targets: ["SampleApp"]
        )
    ],
    targets: [
        .binaryTarget(
            name: "RagChat",
            path: "../../sdk/build/XCFrameworks/release/RagChat.xcframework"
        ),
        .executableTarget(
            name: "SampleApp",
            dependencies: [
                "RagChat"
            ]
        )
    ]
)
