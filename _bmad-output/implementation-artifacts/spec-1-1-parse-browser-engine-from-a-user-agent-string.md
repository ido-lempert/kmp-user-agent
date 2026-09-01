---
title: 'Parse Browser & Engine from a User-Agent String'
type: 'feature'
created: '2026-09-01'
status: 'done'
review_loop_iteration: 0
baseline_commit: 'ef0f7e31dbd2c9135e75a8103a1ae9079ac0ac3a'
context:
  - '{project-root}/_bmad-output/specs/spec-user-agent/SPEC.md'
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-kmp-user-agent-2026-09-01/ARCHITECTURE-SPINE.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The repo is currently an app-template scaffold with no `library` module and no way to parse a User-Agent string into structured data from common Kotlin. Nothing in this KMP library can exist until the module, its build-time detection-rule pipeline, and a first working parse capability (browser/engine) are stood up.

**Approach:** Add a new `library` KMP module (Android/iOS/JVM/JS, stdlib-only) with a Gradle codegen task that vendors uap-core's `user_agent_parsers` rules into generated `commonMain` Kotlin, a fixed `UserAgentInfo`/`Component`/`Device` data model, and a stateless `UserAgentParser.parse(String)` that populates `browser`/`engine` (leaving `os`/`device` null, added in Stories 1.2/1.3).

## Boundaries & Constraints

**Always:**
- `library`'s production source sets (`commonMain`, `androidMain`, `iosMain`, `jvmMain`, `jsMain`) depend only on the Kotlin stdlib; `commonTest` may additionally depend on `kotlin.test`.
- Root package `site.lempert.useragent`; public types exactly `UserAgentInfo`, `Component`, `Device`, `UserAgentParser`.
- Declare the full fixed model now — `UserAgentInfo(browser: Component?, engine: Component?, os: Component?, device: Device?)`, `Component(name: String?, version: String?)`, `Device(brand: String?, model: String?, name: String?)` — even though `os`/`device` stay `null` until Stories 1.2/1.3, so the public API shape never breaks later.
- Unmatched fields are `null`, never a sentinel like `"unknown"`; `parse()` never throws.
- `regexes.yaml` is vendored from `uap-core` (Apache-2.0, pinned commit), consumed only by the Gradle codegen task at build time — never loaded as a runtime resource — and attributed in `library/NOTICE` with the pinned commit recorded.
- `library` targets Android, `iosArm64`/`iosSimulatorArm64`, `jvm`, and `js`, mirroring `sharedLogic/build.gradle.kts`'s plugin/target pattern (`kotlinMultiplatform` + `androidMultiplatformLibrary`), plus a new `jvm()` target (no existing local precedent) and JVM_11 compiler target consistent with other modules.
- Generated pattern strings are normalized so the identical rule table compiles under Kotlin/JS's mandatory ECMAScript `u`-flag `Regex` dialect as well as JVM/Native; rules are evaluated in file order, first match wins.

**Ask First:**
- Adding any new build-time-only dependency (e.g. a YAML parsing library) to make the codegen task read `regexes.yaml`, versus hand-rolling a minimal parser for uap-core's regular structure.

**Never:**
- Implement OS or device detection (Stories 1.2/1.3) — `os` and `device` stay `null` placeholders in this story.
- Wrap a platform-native/JVM-only UA parser at runtime.
- Modify, repurpose, or retire the existing `androidApp`/`iosApp`/`webApp`/`sharedLogic`/`sharedUI` modules — out of scope for this story.
- Load `regexes.yaml` (or any vendored data) as a runtime resource/asset.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Known desktop browser | Chrome desktop UA string | `browser`/`engine` populated as matching `Component(name, version)` | N/A |
| Known browser, different family | Firefox, Safari, or Edge UA string | `browser`/`engine` populated correctly for that family, identical result on all 4 targets | N/A |
| Unrecognized UA | Garbage/non-UA string | `browser` and `engine` are `null` | No exception thrown |
| Empty input | `""` | `browser` and `engine` are `null` | No exception thrown |

</frozen-after-approval>

## Code Map

- `settings.gradle.kts:31` -- add `include(":library")` alongside the existing `:androidApp`/`:sharedLogic`/`:sharedUI` includes.
- `sharedLogic/build.gradle.kts` -- closest existing KMP module; mirror its plugin block (`kotlinMultiplatform`, `androidMultiplatformLibrary`), `iosArm64()`/`iosSimulatorArm64()` setup, and `js { browser(); binaries.library(); ... }` block. It has **no `jvm()` target anywhere in the repo** — `library` needs a new one with no local precedent. Do not carry over its `jsMain.dependencies { implementation(libs.wrappers.browser) }` (browser-DOM dep, violates stdlib-only).
- `gradle/libs.versions.toml` -- `kotlin = "2.4.10"` already pinned (no bump needed); reuse existing `kotlinMultiplatform`/`androidMultiplatformLibrary` plugin aliases and the `kotlin-test` library alias for `commonTest`.
- `build.gradle.kts` (root) -- already declares the needed plugins `apply false`; no change needed.
- `_bmad-output/specs/spec-user-agent/SPEC.md` -- capability/license contract (CAP-1 parse; MIT end product; only MIT/Apache-2.0/BSD 3rd-party data/deps).
- `_bmad-output/planning-artifacts/architecture/architecture-kmp-user-agent-2026-09-01/ARCHITECTURE-SPINE.md` -- AD-1..7 and the structural seed, which calls for `library/NOTICE`.
- No Gradle codegen/KSP task and no vendored `uap-core`/`regexes.yaml` data exist anywhere in the repo yet — this story is the first of both.

## Tasks & Acceptance

**Execution:**
- [x] `settings.gradle.kts` -- add `include(":library")` -- registers the new module
- [x] `library/build.gradle.kts` -- new KMP module: `kotlinMultiplatform` + `androidMultiplatformLibrary` plugins; `android`/`iosArm64`/`iosSimulatorArm64`/`jvm`/`js` targets; `commonMain` stdlib-only, `commonTest` + `kotlin.test`; wire generated-source dir into `commonMain` -- stands up the module per the architecture's structural seed
- [x] `library/vendor/uap-core/regexes.yaml` -- vendor uap-core's `user_agent_parsers` rules at a pinned commit (Apache-2.0) -- source of truth for detection patterns
- [x] `library/NOTICE` -- Apache-2.0 attribution for the vendored uap-core data, recording the pinned commit -- license compliance
- [x] Gradle codegen task in `library/build.gradle.kts` -- parses `regexes.yaml`'s `user_agent_parsers` section, normalizes patterns for JS's mandatory `u`-flag dialect, writes a generated Kotlin rule table into `library/build/generated/...` as a `commonMain` source dir, wired as a dependency of compile tasks -- build-time-only vendoring, never a runtime resource
- [x] `library/src/commonMain/kotlin/site/lempert/useragent/UserAgentInfo.kt` -- `UserAgentInfo`, `Component`, `Device` data classes per the fixed model -- symmetric API shape usable by later stories without breaking changes
- [x] `library/src/commonMain/kotlin/site/lempert/useragent/UserAgentParser.kt` -- `object UserAgentParser { fun parse(userAgent: String): UserAgentInfo }`, matching the generated rule table in file order, first match wins, populating only `browser`/`engine` -- public API entry point
- [x] `library/src/commonTest/kotlin/site/lempert/useragent/UserAgentParserTest.kt` -- `kotlin.test` cases per the I/O matrix (Chrome/Firefox/Safari/Edge + unrecognized input) -- validates identical behavior across all four targets

**Acceptance Criteria:**
- Given the `library` module added as a dependency on Android, iOS, JVM, or JS, when `UserAgentParser.parse(rawUserAgentString)` is called, then the same Kotlin call site compiles and returns identical results on all four targets.
- Given `library`'s production source sets, when dependencies are inspected, then `commonMain`/`androidMain`/`iosMain`/`jvmMain`/`jsMain` depend only on the Kotlin stdlib.
- Given the vendored `regexes.yaml`, when the Gradle codegen task runs, then it generates `commonMain` Kotlin source rather than a runtime-loaded resource.

## Spec Change Log

## Design Notes

Since `library`'s production code must stay stdlib-only but the codegen task itself runs at Gradle build time (not shipped in the artifact), it may use a build-classpath-only YAML dependency if needed — but uap-core's `regexes.yaml` has a very regular structure (a flat list of maps with `regex`/`family_replacement`/`v1_replacement`/`v2_replacement` keys), so a small hand-rolled line-based parser is likely simpler and avoids adding new build tooling. Prefer the hand-rolled parser; if it proves insufficient, ask before adding a dependency (see Boundaries).

## Verification

**Commands:**
- `./gradlew :library:build` -- expected: all targets compile; codegen task runs and produces the generated rule table before compilation
- `./gradlew :library:allTests` -- expected: `commonTest` cases pass on every configured target (android host test, iosSimulatorArm64, jvm, js)

## Suggested Review Order

**Public API & data model**

- Entry point: the stateless parse API and its fixed, symmetric result shape.
  [`UserAgentInfo.kt:11`](../../library/src/commonMain/kotlin/site/lempert/useragent/UserAgentInfo.kt#L11)

- `parse()` composes browser + engine detection; `os`/`device` are hardcoded `null` per this story's scope.
  [`UserAgentParser.kt:17`](../../library/src/commonMain/kotlin/site/lempert/useragent/UserAgentParser.kt#L17)

**Browser & engine detection**

- Matches the generated rule table in file order, first match wins; family/version come from either a replacement template or raw capture groups.
  [`UserAgentParser.kt:57`](../../library/src/commonMain/kotlin/site/lempert/useragent/UserAgentParser.kt#L57)

- `$N` template substitution for `family_replacement`/`v1_replacement`/`v2_replacement`; fixed during review to consume multi-digit group indices instead of only the first digit.
  [`UserAgentParser.kt:79`](../../library/src/commonMain/kotlin/site/lempert/useragent/UserAgentParser.kt#L79)

- Engine detection is original hand-written logic (Trident/Blink/Gecko/Presto/WebKit tokens), not vendored data -- uap-core's `regexes.yaml` has no engine section.
  [`UserAgentParser.kt:128`](../../library/src/commonMain/kotlin/site/lempert/useragent/UserAgentParser.kt#L128)

**Build-time codegen (uap-core vendoring)**

- Declared as a plain `object`, not top-level script functions, so the configuration cache can serialize the task action.
  [`build.gradle.kts:27`](../../library/build.gradle.kts#L27)

- Hand-rolled line-based YAML parsing for `user_agent_parsers`, chosen over adding a YAML library dependency.
  [`build.gradle.kts:50`](../../library/build.gradle.kts#L50)

- Normalizes patterns for Kotlin/JS's mandatory `u`-flag `Regex` dialect, applied identically on every target.
  [`build.gradle.kts:106`](../../library/build.gradle.kts#L106)

- Registers the codegen task and wires its output into `commonMain`'s source dir.
  [`build.gradle.kts:181`](../../library/build.gradle.kts#L181)

- KMP target/plugin configuration (Android/iosArm64/iosSimulatorArm64/jvm/js), mirroring `sharedLogic`'s pattern plus a new `jvm()` target.
  [`build.gradle.kts:210`](../../library/build.gradle.kts#L210)

**Vendored data & license**

- Apache-2.0 attribution for the vendored uap-core data, recording the pinned commit; updated during review to point at the full license text.
  [`NOTICE`](../../library/NOTICE)

- Full Apache License 2.0 text, added during review alongside the vendored data.
  [`LICENSE`](../../library/vendor/uap-core/LICENSE)

- The vendored `user_agent_parsers` snapshot itself, unmodified upstream data.
  [`regexes.yaml:1`](../../library/vendor/uap-core/regexes.yaml#L1)

**Peripherals**

- Desktop Chrome/Firefox/Safari/Edge cases plus the null/empty-input contract.
  [`UserAgentParserTest.kt:12`](../../library/src/commonTest/kotlin/site/lempert/useragent/UserAgentParserTest.kt#L12)

- Added during review: covers the `$N`-template substitution path that no other test exercised.
  [`UserAgentParserTest.kt:86`](../../library/src/commonTest/kotlin/site/lempert/useragent/UserAgentParserTest.kt#L86)

- Registers the new module.
  [`settings.gradle.kts:33`](../../settings.gradle.kts#L33)
