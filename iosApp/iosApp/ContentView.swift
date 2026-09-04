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

    // `UserAgentParser`/`UserAgentGenerator` are top-level Kotlin functions
    // taking a `vararg`, which Kotlin/Native's Objective-C/Swift export
    // surfaces as a static `Kt`-suffixed-class method taking a `KotlinArray`
    // (not a native Swift Array) -- confirmed against the actual generated
    // Library.framework/Headers/Library.h rather than guessed. `static` so it
    // can be referenced from the other stored properties' initializers below
    // (a plain instance `let` can't reference a sibling instance property
    // before `self` exists).
    private static let allTypesPacks = KotlinArray<UserAgentTypePack>(size: 1) { _ in UserAgentAllTypesPackKt.UserAgentAllTypes }

    private let parsed = UserAgentParserKt.UserAgentParser(packs: allTypesPacks)(sampleUserAgent)
    private let generated = UserAgentGeneratorKt.UserAgentGenerator(packs: allTypesPacks)(
        UserAgentInfo(
            browser: Component(name: "Safari", version: "17.5"),
            engine: Component(name: "WebKit", version: "605.1.15"),
            os: Component(name: "iOS", version: "17.5"),
            device: Device(brand: "Apple", model: "iPhone", name: "iPhone"),
            bot: nil,
            aiAgent: nil,
            custom: [:]
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
