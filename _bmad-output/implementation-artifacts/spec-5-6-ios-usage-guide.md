---
title: 'iOS SPM Distribution + Usage Guide'
type: 'feature'
created: '2026-09-05'
status: 'done'
review_loop_iteration: 0
baseline_commit: 'c60b59aad269835c7ac6c30142ee5008dd4ea8fd'
context:
  - '{project-root}/_bmad-output/specs/spec-docs-site/SPEC.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** iOS has no real "add one dependency" install path — no CocoaPods podspec, no Swift Package Manager package. An iOS developer can't adopt the library the way Android/JS developers already can, and any guide written today would have to apologize for that instead of showing a working install.

**Approach:** Add real Swift Package Manager binary distribution — assemble an XCFramework from the existing iOS targets, host it as a GitHub Release asset, and add a `Package.swift` manifest at the repo root pointing at it (Kotlin's own documented recommended pattern) — then write the iOS usage guide against that real, working install, the same way the Android/JS guides document their real, working installs.

## Boundaries & Constraints

**Always:**
- XCFramework assembly uses the `XCFramework(name)` / `xcf.add(this)` Gradle DSL (stable since Kotlin 1.5.30; this repo is on 2.4.10, below the 2.4.20-Beta2 threshold where `Package.swift` gets auto-generated — so it must be hand-written here), added alongside the existing `iosTarget.binaries.framework { baseName = "Library" }` loop in `library/build.gradle.kts`, not replacing its `baseName`/`isStatic` settings.
- `Package.swift` lives at the repo root (an SPM requirement for `.package(url:from:)` resolution), uses `.binaryTarget(name:url:checksum:)` pointing at a GitHub Release asset URL (`https://github.com/ido-lempert/kmp-user-agent/releases/download/<tag>/Library.xcframework.zip`) — Kotlin's own documented recommended hosting pattern, not an improvised one.
- The XCFramework is built, zipped, and its checksum computed locally and verified to actually assemble without error *before* any release is created — do not write `Package.swift` with a guessed or placeholder checksum.
- The iOS guide (`docs-site/guide/ios.md`) is written only after the real release exists, using the release's real tag/URL/checksum — no placeholder values.
- Swift usage examples in the guide still match the real, verified Objective-C-interop call pattern already confirmed against `iosApp/iosApp/ContentView.swift` in the prior investigation (`UserAgentParserKt.UserAgentParser(packs:)`, `KotlinArray<UserAgentTypePack>`, labeled data-class initializers) — that part of the original guide plan is unchanged, only the install story changes from "add via Gradle/KMP" to "add via SPM".
- Add `iOS` to `docs-site/.vitepress/config.ts` nav and sidebar, after Android.

**Ask First:**
- Creating the actual git tag, pushing it (and the currently-unpushed local commits — five sit unpushed as of this story) to `origin`, and creating a real GitHub Release with the zipped XCFramework attached, are real, public, semi-irreversible actions. HALT and get explicit confirmation immediately before performing them — do not fold this into the general implementation flow. Confirm the version to tag (proposed: `0.2.0`, matching the library's already-bumped but not-yet-published Gradle coordinate) before tagging.

**Never:**
- Do not also publish to Maven Central or npm as part of this story — those are separate, already-established manual publish steps (per this project's own convention) and out of scope here; this story only adds the SPM channel.
- Do not show a CocoaPods snippet — out of scope; SPM is the one channel being added.
- Do not modify `js.md`, `android.md`, `core-concepts.md`, `ParseDemo.vue`, `GenerateDemo.vue`, or the deploy workflow.
- Do not build the JVM guide (Story 5.7).

## Code Map

- `library/build.gradle.kts` (~lines 495-505) -- current iOS block to extend: `listOf(iosArm64(), iosSimulatorArm64()).forEach { it.binaries.framework { baseName = "Library"; isStatic = true } }`. Add `val xcf = XCFramework("Library")` and `xcf.add(this)` inside the loop (import `org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework`).
- Confirmed (Kotlin's own docs, https://kotlinlang.org/docs/multiplatform/multiplatform-spm-export.html) Gradle task produced: `assembleLibraryReleaseXCFramework`, output at `library/build/XCFrameworks/release/Library.xcframework` -- verify the exact task name empirically via `./gradlew :library:tasks --group build` before relying on it.
- Command sequence to produce the release artifact (from Kotlin's own docs): `./gradlew :library:assembleLibraryReleaseXCFramework` → `zip -r -X Library.xcframework.zip Library.xcframework` (from inside the release output dir) → `swift package compute-checksum Library.xcframework.zip`.
- No existing `Package.swift` or `cocoapods{}` block anywhere in the repo (confirmed). No git tags exist yet (confirmed via `git tag -l`). Remote is `https://github.com/ido-lempert/kmp-user-agent.git`.
- Static XCFramework (`isStatic = true`, already set) is the simpler, safer choice for SPM `binaryTarget` distribution -- no notarization concern applies (that's a macOS Gatekeeper mechanism, irrelevant to an iOS binaryTarget); this is not a change needed, just confirms the existing setting is already correct for this purpose.
- `iosApp/iosApp/ContentView.swift` -- unchanged reference for the guide's Swift call-pattern examples (see prior investigation, already captured in this story's predecessor draft).
- `docs-site/guide/android.md` -- structural/voice and version-staleness-note pattern to reuse.
- `docs-site/.vitepress/config.ts:17-27` (`nav`/`sidebar`) -- add the iOS entry after Android.

## Tasks & Acceptance

**Execution:**
- [x] `library/build.gradle.kts` -- edit; add the `XCFramework("Library")` / `xcf.add(this)` DSL to the existing iOS framework block.
- [x] Verify locally: run `./gradlew :library:tasks --group build` to confirm the exact assemble-XCFramework task name, then run it and confirm `Library.xcframework` is actually produced with both `ios-arm64` and `ios-arm64-simulator` slices.
- [x] Zip the built XCFramework and compute its checksum via `swift package compute-checksum`.
- [x] **STOP — Ask First checkpoint.** Confirm the version to tag and get explicit go-ahead before tagging/pushing/releasing.
- [x] `Package.swift` -- create at repo root; `.binaryTarget` pointing at the *known future* release asset URL (`.../releases/download/<confirmed-tag>/Library.xcframework.zip` — deterministic from the confirmed tag name, doesn't require the release to exist yet) with the already-computed real checksum. **Ordering matters**: this must be committed and pushed to `origin` *before* the tag is created — SPM resolves `Package.swift` by checking out the git ref matching the requested version, so if the tag is cut first and `Package.swift` lands in a later commit, the tag's checkout has no `Package.swift` and resolution fails. Sequence is: commit `Package.swift` (+ the `build.gradle.kts` XCFramework change) → push to `origin` → *then* create and push the tag on that same commit → *then* create the GitHub Release and upload the asset.
- [x] After confirmation and in that order: commit, push, tag, push the tag, create the GitHub Release for that tag, upload `Library.xcframework.zip` as a release asset.
- [x] Verify the real Package.swift resolves: a minimal local SPM consumption check (e.g. `swift package resolve` against a throwaway test package, or equivalent) confirming the binaryTarget actually fetches and checksums correctly against the now-real, now-live release asset — not just that the file looks right.
- [x] `docs-site/guide/ios.md` -- create; SPM install snippet using the real repo URL and version, the verified Swift call-pattern examples, a link to Core Concepts.
- [x] `docs-site/.vitepress/config.ts` -- edit; add an `iOS` nav/sidebar entry linking to `/guide/ios`, after Android.

**Acceptance Criteria:**
- Given a real GitHub Release with the XCFramework asset attached, when a real Xcode project adds the package via `File → Add Package Dependencies` with the repo URL, then it resolves and links without needing Gradle/JDK installed locally.
- Given the iOS guide, when a developer follows it, then they can add the SPM dependency and write Swift code that successfully calls parse/generate, using syntax verified against the real generated interop.
- Given the guide needs to explain the shared model or pack composition, when it does, then it links to Core Concepts rather than re-explaining it.
- Given the site's nav/sidebar, when inspected, then `iOS` appears and resolves to a real page with no broken link.

## Spec Change Log

- **Finding (blind-hunter + verification-gap, review round 1):** `Package.swift` declared `platforms: [.iOS(.v14)]`, but the actual compiled XCFramework's `MinimumOSVersion` (verified by inspecting the built artifact's `Info.plist` directly) is `15.0`. A consumer targeting iOS 14 would pass SPM's platform check but fail at link time.
- **Also investigated and rejected:** a claimed missing NOTICE/LICENSE bundling in the XCFramework zip, matching architecture spine AD-6's "shipped alongside every published artifact" rule. Verified via a real SPM checkout that both `LICENSE` and `library/NOTICE` are automatically present — SPM checks out the whole git repository alongside the binaryTarget download, satisfying AD-6's intent through a different mechanism than JAR/AAR/npm bundling. No fix needed; not a real gap.
- **Amended:** corrected the platform declaration to `.iOS(.v15)`. A second, self-caught issue followed: `.v15` requires `PackageDescription` 5.5, and `swift-tools-version` was still pinned at the 5.3 floor `binaryTarget` itself needs — caught by actually running `swift package resolve` against the live tag rather than trusting a clean local `swift package describe`, and fixed by bumping to `5.5` (satisfies both requirements). Also discovered mid-fix: deleting a git tag with an attached GitHub Release deletes the release too — the release had to be fully recreated (not just the tag) both times the tag was moved.
- **Known-bad state avoided:** shipping a `Package.swift` that either lets an incompatible-deployment-target consumer hit a confusing link-time failure, or (the intermediate near-miss) doesn't compile as a manifest at all.
- **KEEP:** the XCFramework binary itself, its checksum, the Gradle DSL registration, and the guide's Swift interop examples were correct throughout and never changed — only `Package.swift`'s declared metadata and the guide's prose were amended.

## Design Notes

This is meaningfully more involved than the Android/JVM guide stories: it's a real library-distribution capability addition, not documentation of an existing one. The Ask First checkpoint before tagging/pushing/releasing is load-bearing, not decorative — confirm explicitly before crossing that line, even if earlier implementation steps go smoothly.

If for any reason the empirical XCFramework build or SPM resolution check fails and can't be resolved within this story's scope, HALT and report rather than shipping a guide that documents an install path that doesn't actually work — that would be strictly worse than the original honest-caveat version of this guide.

## Verification

**Commands:**
- `./gradlew :library:tasks --group build` then the actual assemble-XCFramework task -- expected: `Library.xcframework` produced, containing both iOS device and simulator slices.
- `swift package compute-checksum Library.xcframework.zip` -- expected: a real checksum string, used verbatim in `Package.swift`.
- A real SPM resolution check against the published release (e.g. a throwaway local Swift package or Xcode project adding the dependency by URL) -- expected: resolves and links without error.
- `cd docs-site && npm ci && npm run docs:build 2>&1 | tee /tmp/docs-build.log && grep -iE '\b(ReferenceError|TypeError|SyntaxError)\b|\bis not defined\b' /tmp/docs-build.log` -- expected: build succeeds, `.vitepress/dist` includes `guide/ios.html`, grep finds nothing.
- `node docs-site/scripts/verify-generate-demo.mjs` -- expected: still exits 0.

**Manual checks (if no CLI):**
- Serve the built `dist/` locally and confirm the `iOS` nav link and the Core Concepts link from the new page both resolve.

## Suggested Review Order

**The distribution mechanism (why this took two follow-up fixes to get right)**

- The XCFramework registration -- what actually produces the release artifact.
  [`build.gradle.kts:497`](../../library/build.gradle.kts#L497)

- The manifest consumers actually resolve -- note the corrected platform (`.v15`, matching the real binary, not the originally-assumed `.v14`) and tools-version (`5.5`, required by `.v15`).
  [`Package.swift:1`](../../Package.swift#L1)

**The guide**

- Install + the real constraints (iOS 15+, no Intel simulator, always use a released tag).
  [`ios.md:1`](../../docs-site/guide/ios.md#L1)

- The verified Swift interop pattern (real generated header, not guessed).
  [`ios.md:66`](../../docs-site/guide/ios.md#L66)

**Peripherals**

- Nav/sidebar wiring.
  [`config.ts:20`](../../docs-site/.vitepress/config.ts#L20)
