# iOS Usage

`kmp-user-agent`'s iOS build is distributed as a Swift Package Manager binary
package: an XCFramework assembled from the library's Kotlin/Native iOS
targets (`ios-arm64` device and `ios-arm64-simulator` slices), attached to
each GitHub Release and resolved via a `Package.swift` manifest at the repo
root -- the same detection/generation logic used by every other platform.

## Install

### Xcode

**File &rarr; Add Package Dependencies...**, enter the repo URL:

```
https://github.com/ido-lempert/kmp-user-agent.git
```

pick version `0.2.0` (or "Up to Next Major Version"), and add the `Library`
product to your app target.

### Package.swift (for a Swift package)

```swift
dependencies: [
    .package(url: "https://github.com/ido-lempert/kmp-user-agent.git", from: "0.2.0")
],
targets: [
    .target(
        name: "YourTarget",
        dependencies: [.product(name: "Library", package: "kmp-user-agent")]
    )
]
```

> `0.2.0` above is a hand-pinned version, not something SPM resolves for you
> -- bump it whenever you upgrade to a newer release, checking the
> [Releases page](https://github.com/ido-lempert/kmp-user-agent/releases)
> for the latest published version.

SPM downloads and links the prebuilt XCFramework directly -- no CocoaPods
step, no Gradle or JDK install, nothing to build from Kotlin source locally.

## Parse

A real UA string to parse is available from `WKWebView` or
`URLSession`'s default headers in a real app; the example below uses a
representative literal instead so it runs standalone.

```swift
import Library

let sampleUserAgent =
    "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 " +
    "(KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1"

// `UserAgentParser`/`UserAgentGenerator` are top-level Kotlin functions
// taking a `vararg`, which Kotlin/Native's Objective-C/Swift export surfaces
// as a static `Kt`-suffixed-class method taking a `KotlinArray` (not a
// native Swift Array) -- confirmed against the actual generated
// Library.framework/Headers/Library.h rather than guessed.
let packs = KotlinArray<UserAgentTypePack>(size: 1) { _ in UserAgentAllTypesPackKt.UserAgentAllTypes }

let parsed = UserAgentParserKt.UserAgentParser(packs: packs)(sampleUserAgent)

print(parsed.browser)  // e.g. Component(name: Safari, version: 17.5)
print(parsed.engine)
print(parsed.os)
print(parsed.device)
print(parsed.bot)
print(parsed.aiAgent)
```

## Generate

```swift
let generated = UserAgentGeneratorKt.UserAgentGenerator(packs: packs)(
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

print(generated)
```

`packs` above is built once and reused across both calls, the same way the
repo's own `iosApp` sample builds it as a `static let` -- cheap either way,
but worth doing once rather than rebuilding the `KotlinArray` wrapper on
every call. See Core Concepts'
["Cost of calling the factories"](./core-concepts#cost-of-calling-the-factories)
for why the underlying detection setup cost doesn't change either way.

## Next steps

For the platform-agnostic `UserAgentInfo` model and the built-in type packs
behind `UserAgentAllTypes` used above, see [Core Concepts](./core-concepts).
