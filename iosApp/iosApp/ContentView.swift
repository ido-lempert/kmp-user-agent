import SwiftUI
import Library

private let sampleUserAgent =
    "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 " +
    "(KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1"

/// Thin harness proving `:library` works as a consumed dependency on iOS --
/// not a real app experience. Parses a representative UA string and
/// generates a UA string from structured data, displaying both as text.
struct ContentView: View {
    @State private var showContent = false

    private let parsed = UserAgentParser.shared.parse(userAgent: sampleUserAgent)
    private let generated = UserAgentGenerator.shared.generate(
        info: UserAgentInfo(
            browser: Component(name: "Safari", version: "17.5"),
            engine: Component(name: "WebKit", version: "605.1.15"),
            os: Component(name: "iOS", version: "17.5"),
            device: Device(brand: "Apple", model: "iPhone", name: "iPhone")
        )
    )

    var body: some View {
        VStack {
            Button("Click me!") {
                withAnimation {
                    showContent = !showContent
                }
            }

            if showContent {
                VStack(alignment: .leading, spacing: 8) {
                    Text("UserAgentParser.parse():").bold()
                    Text("UA: \(sampleUserAgent)")
                    Text("browser: \(String(describing: parsed.browser))")
                    Text("engine: \(String(describing: parsed.engine))")
                    Text("os: \(String(describing: parsed.os))")
                    Text("device: \(String(describing: parsed.device))")
                    Text("UserAgentGenerator.generate():").bold()
                    Text(generated)
                }
                .transition(.move(edge: .top).combined(with: .opacity))
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .padding()
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
