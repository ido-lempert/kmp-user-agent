// swift-tools-version:5.5
import PackageDescription

// SPM distribution manifest for the Kotlin/Native XCFramework built from
// `library/build.gradle.kts`'s iOS targets (`iosArm64`, `iosSimulatorArm64`).
// Kotlin 2.4.10 (this repo's version) is below the 2.4.20-Beta2 threshold
// where this file is auto-generated, so it is hand-written -- following
// Kotlin's own documented pattern:
// https://kotlinlang.org/docs/multiplatform/multiplatform-spm-export.html
//
// swift-tools-version is 5.5, not the 5.3 minimum that remote binaryTarget
// itself requires -- `.iOS(.v15)` below needs PackageDescription 5.5
// (confirmed by a real compile error: "'v15' is unavailable ... introduced
// in PackageDescription 5.5" when this was still pinned at 5.3).
//
// The binaryTarget points at a GitHub Release asset for tag v0.4.0 (v-prefixed
// going forward, unlike the historical bare "0.2.0" tag -- see
// docs-site/guide/ios.md; SPM resolves both forms identically). The URL
// is deterministic from the tag name and does not require the release to
// exist at the moment this file is committed -- but it does require the
// release (with this exact asset) to exist by the time anyone resolves this
// package at this commit/tag.
let package = Package(
    name: "Library",
    // .v15, not a lower value -- the compiled XCFramework's actual
    // MinimumOSVersion is 15.0 (confirmed by inspecting the built
    // Library.xcframework's Info.plist directly). A lower declared platform
    // here would pass SPM's own check but still fail at link time.
    platforms: [
        .iOS(.v15),
    ],
    products: [
        .library(name: "Library", targets: ["Library"])
    ],
    targets: [
        .binaryTarget(
            name: "Library",
            url: "https://github.com/ido-lempert/kmp-user-agent/releases/download/v0.4.0/Library.xcframework.zip",
            checksum: "a71d391badd5f59c41cd021dedbf4a92130b1cf0d59dac9a7049e9e4fb5cc799"
        )
    ]
)
