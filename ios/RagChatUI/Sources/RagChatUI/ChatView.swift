import SwiftUI

public struct ChatView: View {
    public let version: String

    public init(version: String = "0.1.0-alpha01") {
        self.version = version
    }

    public var body: some View {
        VStack {
            Text("RagChat SDK SwiftUI Companion")
                .font(.headline)
            Text("Version: \(version)")
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .padding()
    }
}
