---
title: 'Validate Parsing Across All Four Targets'
type: 'feature'
created: '2026-09-02'
status: 'done'
review_loop_iteration: 0
baseline_commit: 'f73d8492d5c560091b7a2b8a2fae443a1277fefb'
context:
  - '{project-root}/_bmad-output/specs/spec-user-agent/SPEC.md'
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-kmp-user-agent-2026-09-01/ARCHITECTURE-SPINE.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** There's no cross-target CI gate, no shared corpus of independently-sourced test cases (only ad-hoc UAs invented per story), and no runnable per-target sample apps proving the library actually works as a consumed dependency. Also blocks Story 2.3, whose ACs explicitly build on this story's corpus and sample apps.

**Approach:** By explicit user decision, this single story covers three deliverables together: (1) a `commonTest` corpus of 55 real uap-core test fixtures (verified against the current parser during planning), (2) a GitHub Actions CI workflow, (3) four thin per-target sample apps (Android/iOS/JVM/JS) depending on `:library`, repurposing the existing `androidApp`/`iosApp`/`webApp` template apps and retiring the now-orphaned `sharedLogic`/`sharedUI` modules.

## Boundaries & Constraints

**Always:**
- The corpus is hand-written Kotlin data (not another vendored-YAML+codegen pipeline) — the 55 fixtures below, sourced from uap-core's `tests/test_ua.yaml`/`test_os.yaml`/`test_device.yaml` at the same pinned commit as `regexes.yaml` (`73e7340c3ed8055051607b296bf46ead7aa5f19e`), each already verified against the current `UserAgentParser`. Attribute this data source in `library/NOTICE` alongside the existing `regexes.yaml` attribution.
- Each fixture asserts only the field(s) its source file actually covers (browser fixtures assert `.browser` only, OS fixtures `.os` only, device fixtures `.device` only) — do not assert `engine` (not part of uap-core's own fixtures) or fabricate expectations for untested fields.
- CI (new `.github/workflows/ci.yml`): triggers on push and pull_request, runs on `macos-latest` (needed for the iOS simulator target), single job running `./gradlew :library:build` (already exercises jvmTest/testAndroidHostTest/jsTest/iosSimulatorArm64Test via `allTests`/`check`).
- Sample apps are repurposed in place (`androidApp`, `iosApp`, `webApp` keep their paths), each now depending on `:library` instead of `sharedLogic`/`sharedUI`, each calling `UserAgentParser.parse()` (and, since `UserAgentGenerator` already exists, also `UserAgentGenerator.generate()`) on a representative UA/`UserAgentInfo` and displaying/logging the result — thin harnesses, not a real app experience.
- Add a new `jvmApp` module (plain Kotlin/JVM `application` — no existing precedent for this plugin combination in the repo, add `org.jetbrains.kotlin.jvm` + `application` plugin aliases) with a `main()` doing the same parse/generate-and-display.
- Retire `sharedLogic`/`sharedUI`: remove their `include(...)` lines from `settings.gradle.kts`, delete both module directories, update root `package.json`'s `workspaces` entry and `build:shared`/`build`/`start` scripts to reference `:library`'s JS output (`library/build/dist/js/developmentLibrary`) instead of `sharedLogic`'s.
- Sample-depends-on-library direction only, never reversed (AD-4) — unchanged from existing architecture.

**Ask First:**
- Any sample-app UI/display approach beyond "shows the parsed/generated result as text" (e.g. adding new UI chrome, icons, or interactions) — keep these strictly thin harnesses.

**Never:**
- Change `UserAgentParser`/`UserAgentGenerator`/`UserAgentInfo`/`Component`/`Device` behavior — this story only adds tests, CI, and sample apps around the existing, unchanged public API.
- Introduce a second vendored-YAML codegen pipeline for the test corpus.

</frozen-after-approval>

## Code Map

- `library/src/commonTest/kotlin/site/lempert/useragent/UapCoreFixtureCorpusTest.kt` (new) -- three `@Test` functions (`browserFixturesMatchUapCoreFixtures`, `osFixturesMatchUapCoreFixtures`, `deviceFixturesMatchUapCoreFixtures`), each iterating a `private val` list of `(userAgent: String, expected: Component?/Device?)` pairs and asserting via `UserAgentParser.parse`. Exact verified fixture data (55 entries total) is provided below in Design Notes -- copy verbatim, do not re-derive.
- `library/NOTICE` -- add a short paragraph noting the additional `tests/test_ua.yaml`/`test_os.yaml`/`test_device.yaml` source files at the same pinned commit, Apache-2.0.
- `.github/workflows/ci.yml` (new) -- see Boundaries for the exact trigger/runner/command.
- `settings.gradle.kts:30-32` -- remove `include(":sharedLogic")`/`include(":sharedUI")`, add `include(":jvmApp")`.
- `androidApp/build.gradle.kts`, `androidApp/src/main/kotlin/site/lempert/kmp_user_agent/MainActivity.kt` -- swap the `:sharedUI` dependency for `:library`; replace whatever `App()` currently renders (defined in `sharedUI`) with a minimal Composable calling `UserAgentParser.parse()`/`UserAgentGenerator.generate()` and showing the result as text.
- `iosApp/iosApp/ContentView.swift` -- replace the `import SharedLogic` / `Greeting().greet()` demo with a call into the library's iOS framework (exported the same way `SharedLogic` was, per Story 1.1's `iosArm64`/`iosSimulatorArm64` framework setup in `library/build.gradle.kts`) and display parse/generate output as text.
- `webApp/package.json`, `webApp/src/index.tsx` (or `webApp/src/components/...`) -- swap the `sharedLogic` npm dependency for `library`, replace the Greeting/JSLogo demo content with parse/generate output.
- `package.json` (root) -- update `workspaces`/`build:shared`/`build`/`start` per Boundaries.
- New `jvmApp/build.gradle.kts`, `jvmApp/src/main/kotlin/.../Main.kt` -- plain Kotlin/JVM console app, `dependencies { implementation(project(":library")) }`, `main()` prints parse/generate output.
- `sharedLogic/`, `sharedUI/` -- delete entirely (retired).

## Tasks & Acceptance

**Execution:**
- [ ] `library/src/commonTest/kotlin/site/lempert/useragent/UapCoreFixtureCorpusTest.kt` -- add the 55-fixture corpus (verbatim from Design Notes) -- fulfills AC-1
- [ ] `library/NOTICE` -- attribute the test-fixture source files
- [ ] `.github/workflows/ci.yml` -- add the CI gate -- fulfills AC-2
- [ ] `settings.gradle.kts` -- retire `sharedLogic`/`sharedUI`, add `jvmApp`
- [ ] `jvmApp/` -- new module, console harness calling parse/generate
- [ ] `androidApp/` -- repurpose to depend on `:library`, thin parse/generate harness
- [ ] `iosApp/iosApp/ContentView.swift` -- repurpose to depend on `:library`, thin parse/generate harness
- [ ] `webApp/` -- repurpose to depend on `:library`, thin parse/generate harness
- [ ] `package.json` (root) -- update workspace/script references from `sharedLogic` to `library`
- [ ] Delete `sharedLogic/`, `sharedUI/` directories

**Acceptance Criteria:**
- Given the 55-fixture `commonTest` corpus, when the test suite runs via `kotlin.test`, then it executes identically on all four MVP targets and passes for every fixture case.
- Given the shared test corpus, when a change is pushed to the repository, then `.github/workflows/ci.yml` runs `:library:build` as a gate on all four targets.
- Given the four thin per-target sample apps, when each calls `UserAgentParser.parse()`/`UserAgentGenerator.generate()` with representative data, then it displays/logs the result correctly, and each sample app depends on `:library` (never the reverse).

## Spec Change Log

## Design Notes

**Verified fixture data** (already confirmed against the current parser during planning; copy verbatim into the three lists):

Browser fixtures (assert `.browser`):
- `"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.78 Safari/537.36"` → `Component("Chrome", "60.0")`
- `"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112 Safari/537.36"` → `Component("Chrome", "60.0")`
- `"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko; Google Web Preview) Chrome/27.0 .1453 Safari/537.36."` → `Component("Chrome", "27.0")`
- `"Mozilla/5.0 (X11; U; SunOS i86pc; en-US; rv:1.8.0.5) Gecko/20060728 Firefox/1.5.0.5"` → `Component("Firefox", "1.5")`
- `"Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.2.12) Gecko/20101027 Ubuntu/10.04 (lucid) Firefox/3.6.12"` → `Component("Firefox", "3.6")`
- `"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/12.1.2 Safari/605.1.15"` → `Component("Safari", "12.1")`
- `"Mozilla/5.0 (Macintosh; U; PPC Mac OS X; en-us) AppleWebKit/418.8 (KHTML, like Gecko) Safari/419.3"` → `Component("Safari", null)`
- `"Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_5; en-us) AppleWebKit/533.18.1 (KHTML, like Gecko) Version/5.0.2 Safari/533.18.5"` → `Component("Safari", "5.0")`
- `"Safari/6533.18.5 CFNetwork/454.9.8 Darwin/10.4.0 (i386) (MacBookPro7,1)"` → `Component("Safari", "6533.18")`
- `"Safari/7536.30.1 CFNetwork/520.5.1 Darwin/11.4.2 (i386) (MacBook3,1)"` → `Component("Safari", "7536.30")`
- `"Safari/9537.71 CFNetwork/673.0.2 Darwin/13.0.1 (x86_64) (MacBookPro11,1)"` → `Component("Safari", "9537.71")`
- `"Mozilla/5.0 (Web0S; Linux/SmartTV) AppleWebKit/537.41 (KHTML, like Gecko) Large Screen WebAppManager Safari/537.41"` → `Component("Safari", null)`
- `"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.135 Safari/537.36 Edge/12.9600"` → `Component("Edge", "12.9600")`
- `"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.135 Safari/537.36 Edge/12"` → `Component("Edge", "12")`
- `"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3763.0 Safari/537.36 Edg/75.0.131.0"` → `Component("Edge", "75.0")`

OS fixtures (assert `.os`; note `Component.version` here is `major.minor.patch`, three groups, per `detectOs`):
- `"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) BoxNotes/1.3.0 Chrome/56.0.2924.87 Electron/1.6.8 Safari/537.36"` → `Component("Windows", "10")`
- `"Box/1.2.93;Windows/10;Intel64 Family 6 Model 158 Stepping 9, GenuineIntel/64bit"` → `Component("Windows", "10")`
- `"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.106 Safari/537.36 CitrixChromeApp"` → `Component("Windows", "10")`
- `"Mozilla/5.0 (Windows NT 6.3) AppleWebKit/537.36 (KHTML, like Gecko) BoxNotes/1.3.0 Chrome/56.0.2924.87 Electron/1.6.8 Safari/537.36"` → `Component("Windows", "8.1")`
- `"iTunes/12.7.1 (Windows; Microsoft Windows 7 Ultimate Edition Service Pack 1 (Build 7601)) AppleWebKit/7604.3005.2001.1"` → `Component("Windows", "7")`
- `"Mozilla/5.0+(Macintosh;+Intel+Mac+OS+X+10_11_6)+AppleWebKit/537.36+(KHTML,+like+Gecko)+Chrome/52.0.2743.116+Safari/537.36"` → `Component("Mac OS X", "10.11.6")`
- `"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_0) AppleWebKit/537.36 (KHTML, like Gecko) BoxNotes/1.3.0 Chrome/56.0.2924.87 Electron/1.6.8 Safari/537.36"` → `Component("Mac OS X", "10.13.0")`
- `"MacOutlook/16.12.0.180401 (Intelx64 Mac OS X Version 10.12.6 (build 16G29))"` → `Component("Mac OS X", "10.12.6")`
- `"iTunes/12.0.1 (Macintosh; OS X 10.9.2) AppleWebKit/537.74.9"` → `Component("Mac OS X", "10.9.2")`
- `"Box Sync/4.0.7848;Darwin/10.13;i386/64bit"` → `Component("Mac OS X", "10.13")`
- `"Mozilla/5.0 (iPad; CPU iPad OS 14_4_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148"` → `Component("iOS", "14.4.1")`
- `"Mozilla/5.0 (iPhone; CPU iPhone OS 15_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 Phantom/ios/22.06.08.44"` → `Component("iOS", "15.5")`
- `"Mozilla/5.0 (iPhone; CPU iPhone OS 14_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.3 Mobile/15E148 DuckDuckGo/7 Safari/605.1.15"` → `Component("iOS", "14.3")`
- `"Mozilla/5.0 (iPhone; CPU iPhone OS 12_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 Pandora/1904.1.1"` → `Component("iOS", "12.2")`
- `"App/0 CFNetwork/1442 Darwin/24.1.0"` → `Component("iOS", "18.1")`
- `"Mozilla/5.0 (Linux; Android 10; SM-G970F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3396.81 Mobile Safari/537.36"` → `Component("Android", "10")`
- `"Mozilla/5.0 (Linux; Android 11) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/87.0.4280.141 Mobile DuckDuckGo/5 Safari/537.36"` → `Component("Android", "11")`
- `"Mozilla/5.0 (Linux; Android 9; Pixel 2 XL Build/PPP5.180610.010; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/68.0.3440.85 Mobile Safari/537.36"` → `Component("Android", "9")`
- `"Mozilla/5.0 (Linux; Android 9; motorola one power) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.96 Mobile Safari/537.36"` → `Component("Android", "9")`
- `"Mozilla/5.0 (Linux; Android 7.0; SM-G930F Build/NRD90M; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/64.0.3282.137 Mobile Safari/537.36 Onefootball/Android/9.10.6"` → `Component("Android", "7.0")`
- `"Mozilla/5.0 (X11; Datanyze; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Safari/537.36"` → `Component("Linux", null)`
- `"Mozilla/5.0 (X11; Linux i686 (x86_64); rv:2.0b4) Gecko/20100818 Firefox/4.0b4"` → `Component("Linux", null)`
- `"python-requests/1.2.3 CPython/2.7.3 Linux/3.5.0-23-generic"` → `Component("Linux", "3.5.0")`
- `"ibm-cos-sdk-java/2.3.0 Linux/4.9.0-8-amd64 Java_HotSpot(TM)_64-Bit_Server_VM/9.0.4+11/9.0.4"` → `Component("Linux", "4.9.0")`
- `"ELinks (0.10.6; Linux 2.6.16-hardened-r10 i686; 80x25)"` → `Component("Linux", "2.6.16")`

Device fixtures (assert `.device`):
- `"Mozilla/5.0 (iPhone; CPU iPhone OS 15_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 Phantom/ios/22.06.08.44"` → `Device("Apple", "iPhone", "iPhone")`
- `"Mozilla/5.0 (iPhone; CPU iPhone OS 14_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.3 Mobile/15E148 DuckDuckGo/7 Safari/605.1.15"` → `Device("Apple", "iPhone", "iPhone")`
- `"Mozilla/5.0 (iPhone; CPU iPhone OS 12_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 Pandora/1904.1.1"` → `Device("Apple", "iPhone", "iPhone")`
- `"Mozilla/5.0 (iPhone; CPU iPhone OS 11_2_5 like Mac OS X) AppleWebKit/604.5.6 (KHTML, like Gecko) Mobile/15D60 Instagram 33.0.0.11.96 (iPhone9,3; iOS 11_2_5; en_AU; en-AU; scale=2.00; gamut=wide; 750x1334)"` → `Device("Apple", "iPhone9,3", "iPhone")`
- `"Mozilla/5.0 (iPhone5,2; iOS 7.0.3) FreeWheelAdManager/5.8.3-r10206-201309100316;com.vevo.iphone VEVO/5987"` → `Device("Apple", "iPhone5,2", "iPhone")`
- `"Mozilla/5.0 (iPad; CPU OS 12_1_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/16D57 Pandora/1902.1"` → `Device("Apple", "iPad", "iPad")`
- `"Mozilla/5.0 (iPad; CPU OS 10_0_2 like Mac OS X) AppleWebKit/602.1.50 (KHTML, like Gecko) Mobile/14A456 [FBAN/FBIOS;FBAV/68.0.0.49.70;FBBV/41924288;FBRV/0;FBDV/iPad4,1;FBMD/iPad;FBSN/iOS;FBSV/10.0.2;FBSS/2;FBCR/;FBID/tablet;FBLC/en_US;FBOP/5]"` → `Device("Apple", "iPad4,1", "iPad")`
- `"Mozilla/5.0 (iPad2,1; iPad; U; CPU OS 6_1_3 like Mac OS X; de_DE) com.google.GooglePlus/23341 (KHTML, like Gecko) Mobile/K93AP (gzip)"` → `Device("Apple", "iPad2,1", "iPad")`
- `"Mozilla/5.0 (iPad3,1; iPad; U; CPU OS 7_0_4 like Mac OS X; de_DE) com.google.GooglePlus/29676 (KHTML, like Gecko) Mobile/J1AP (gzip)"` → `Device("Apple", "iPad3,1", "iPad")`
- `"Mozilla/5.0 (iPad; CPU OS 7_0 like Mac OS X) AppleWebKit/537.51.1 (KHTML, like Gecko) Mobile/11A465 [FBAN/FBIOS;FBAV/8.0.0.28.18;FBBV/1665515;FBDV/iPad2,3;FBMD/iPad;FBSN/iPhone OS;FBSV/7.0;FBSS/1; FBCR/Verizon;FBID/tablet;FBLC/de_DE;FBOP/1]"` → `Device("Apple", "iPad2,3", "iPad")`
- `"Mozilla/5.0 (Linux; Android 9; Pixel 2 XL Build/PPP5.180610.010; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/68.0.3440.85 Mobile Safari/537.36"` → `Device("Google", "Pixel 2 XL", "Pixel 2 XL")`
- `"Mozilla/5.0 (Linux; Android 7.0; SM-G930F Build/NRD90M; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/64.0.3282.137 Mobile Safari/537.36 Onefootball/Android/9.10.6"` → `Device("Samsung", "SM-G930F", "Samsung SM-G930F")`
- `"ABC/4.3.392 version 6.2.15, build 392 (Pixel 6 Pro; google Pixel 6 Pro; Android 12) AppleWebKit"` → `Device("Google", "Pixel 6 Pro", "Pixel 6 Pro")`
- `"Mozilla/5.0 (Linux; U; en-us; KFAPWA Build/JDQ39) AppleWebKit/535.19 (KHTML, like Gecko) Silk/3.6 Safari/535.19 Silk-Accelerated=true"` → `Device("Amazon", "Kindle Fire HDX 8.9\" 4G", "Kindle Fire HDX 8.9\" 4G")`
- `"Mozilla/5.0 (PlayStation 4 1.75) AppleWebKit/536.26 (KHTML, like Gecko)"` → `Device("Sony", "PlayStation 4", "PlayStation 4")`

**Execution order suggestion** (not mandated): tackle the corpus + NOTICE first (self-contained, fastest to verify), then CI, then the module/settings restructuring (retiring sharedLogic/sharedUI, adding jvmApp), then the three repurposed apps last, since they depend on the module restructuring being in place.

## Verification

**Commands:**
- `./gradlew :library:build` -- expected: all targets compile; new corpus test passes
- `./gradlew :library:allTests` -- expected: all 55 fixture assertions plus existing tests pass on every configured target
- `./gradlew build` (root) -- expected: `androidApp`, `jvmApp` build; `webApp`'s build script runs against `:library`'s JS output
- Manual: run each sample app (Android emulator/device, iOS simulator, `./gradlew :jvmApp:run`, `npm start` for `webApp`) and confirm parse/generate output displays correctly

**Manual checks (if no CLI):**
- Open the iOS app in Xcode/simulator and visually confirm the parse/generate output renders (no automated CLI check available for the SwiftUI view beyond compiling).

## Suggested Review Order

**Deviation from the frozen scope: `@JsExport` on the public API**

- Necessary, transparently-flagged deviation: the JS sample app couldn't call the library at all without this (Kotlin/JS hides un-exported declarations from JS entirely). Changes no parsing/generation behavior -- purely a JS-target visibility annotation, a no-op on every other target.
  [`UserAgentInfo.kt:18`](../../library/src/commonMain/kotlin/site/lempert/useragent/UserAgentInfo.kt#L18), [`UserAgentParser.kt:17`](../../library/src/commonMain/kotlin/site/lempert/useragent/UserAgentParser.kt#L17), [`UserAgentGenerator.kt:24`](../../library/src/commonMain/kotlin/site/lempert/useragent/UserAgentGenerator.kt#L24)

**CI gate (hardened during review)**

- Base workflow from the implementer, hardened during review: added Gradle caching, least-privilege `permissions`, a `concurrency` group to cancel stale runs, and broadened the build command from `:library:build` to root `build` so the sample apps (`jvmApp`, `androidApp`) are also verified to compile in CI, not just the library.
  [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml)

**Shared test corpus**

- Three data-driven tests over the 55 pre-verified uap-core fixtures.
  [`UapCoreFixtureCorpusTest.kt:149`](../../library/src/commonTest/kotlin/site/lempert/useragent/UapCoreFixtureCorpusTest.kt#L149)

**Peripherals**

- New JVM console sample.
  [`jvmApp/src/main/kotlin/site/lempert/kmp_user_agent/Main.kt`](../../jvmApp/src/main/kotlin/site/lempert/kmp_user_agent/Main.kt)

- Repurposed Android sample (representative of the same pattern applied to `iosApp`/`webApp`, each independently verified via a real platform build during implementation: `xcodebuild`, `npm run build`).
  [`androidApp/src/main/kotlin/site/lempert/kmp_user_agent/MainActivity.kt`](../../androidApp/src/main/kotlin/site/lempert/kmp_user_agent/MainActivity.kt)
