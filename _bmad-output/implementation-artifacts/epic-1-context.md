# Epic 1 Context: Parse a User-Agent String

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Enable a consumer to add the library's `library` module to a project and call `UserAgentParser.parse(String)` to get structured browser, engine, OS, and device data — correct and identical on Android, iOS, JVM, and JS. This is the foundational epic: nothing else in the library can exist until this epic stands up the `library` module itself (repurposing/retiring the current app-template scaffolding), the `uap-core` rule-vendoring + Gradle codegen pipeline, the shared data model, the stateless parse API, the stdlib-only dependency boundary, and the shared cross-target test corpus. Epic 2 (generate) builds directly on top of everything this epic produces.

## Stories

- Story 1.1: Parse Browser & Engine from a User-Agent String
- Story 1.2: Parse OS from a User-Agent String
- Story 1.3: Parse Device Info from a User-Agent String
- Story 1.4: Validate Parsing Across All Four Targets

## Requirements & Constraints

- A consumer must be able to parse a raw UA string into structured browser/engine/OS/device data with correct results for representative major browsers/OS/devices, identically on Android, iOS, JVM, and JS (one common KMP API, same call site, same results on every target).
- Fields that a UA string doesn't carry must be `null` — never a sentinel like `"unknown"` — and unrecognized input must never throw; the result is simply a partially-populated (or fully-null) `UserAgentInfo`.
- MVP targets are exactly Android, iOS, JVM/Desktop, and JS(Web); Wasm/WasmJs is out of scope for v1.
- The parser must be original common-Kotlin logic, not a runtime wrapper around a platform-native/JVM-only UA parsing library — detection rule *data* may be adapted from existing open-source parsers, but only as build-time-vendored data, never a runtime dependency.
- v1 favors shipping speed and pragmatic coverage over exhaustive browser/OS/device support; broader coverage is explicitly post-MVP.
- Only MIT/Apache-2.0/BSD-licensed dependencies and vendored data are allowed anywhere in the library or its build — no copyleft. Vendored rule data must retain its Apache-2.0 attribution.
- The existing repo is an app template (androidApp/iosApp/webApp + sharedLogic/sharedUI); this epic restructures it, introducing a dedicated `library` module plus minimal per-target sample/harness apps. There is no fresh-scaffold requirement — repurpose or replace existing modules as needed.

## Technical Decisions

- **Data model (fixed, symmetric):** `UserAgentInfo(browser: Component?, engine: Component?, os: Component?, device: Device?)`; `Component(name: String?, version: String?)`; `Device(brand: String?, model: String?, name: String?)`. `version` is a plain nullable `String` (no semver parsing) in v1. This is the parser's only output shape — no parallel model.
- **Public API (stateless, single entry point):** exactly `UserAgentParser.parse(String): UserAgentInfo`. No alternate entry points, no mutable shared state, no caching beyond the compiled rule table.
- **Rule data pipeline:** Detection patterns are vendored from `uap-core`'s `regexes.yaml` (Apache-2.0, pinned commit at vendor time) and converted into `commonMain` Kotlin source by a Gradle codegen task at build time — never loaded as a runtime resource. The generated table is three ordered pattern lists (`user_agent_parsers`, `os_parsers`, `device_parsers`) mirroring `uap-core`'s structure, each entry holding a regex plus its replacement-template fields; entries are evaluated in file order, first match wins per category. Because Kotlin/JS's `Regex` mandates the ECMAScript `u` flag and many upstream patterns use escapes illegal under it, the codegen task strips/normalizes those non-metacharacter backslash escapes uniformly for every target (a safe no-op on JVM/Native), so all targets compile the identical pattern set.
- **Dependency boundary:** every production source set (`commonMain`, `androidMain`, `iosMain`, `jvmMain`, `jsMain`) depends only on the Kotlin stdlib. `commonTest` may additionally depend on `kotlin.test`. Sample/test apps depend on the `library` module; the library never depends on a sample app.
- **Regex engine placement:** all parse/generate logic lives in `commonMain` as pure Kotlin over an immutable compiled rule table — `kotlin.text.Regex` is common-stdlib on every MVP target, so no `expect`/`actual` split is needed for matching itself; the only per-target wrinkle (JS regex dialect) is absorbed entirely at codegen time.
- **Naming/package:** root package `site.lempert.useragent`; public types `UserAgentInfo`, `Component`, `Device`, `UserAgentParser` — no platform suffixes.
- **Cross-cutting conventions:** no logging; no exceptions for "not recognized" input.
- **Stack pins relevant here:** Kotlin/KMP Gradle plugin 2.4.10; vendored `uap-core regexes.yaml` snapshot (Apache-2.0, pinned commit); `kotlin.test` bundled with Kotlin 2.4.10.
- **Structural seed:** a new `library/` module (`src/commonMain`, `commonTest`, thin `androidMain`/`iosMain`/`jvmMain`/`jsMain`) plus `samples/android|ios|jvm|js` thin harness apps depending on `:library`. Existing `androidApp`/`iosApp`/`webApp`/`sharedLogic`/`sharedUI` are candidates to repurpose as samples or retire — this call is left to implementation, not mandated by architecture.

## Cross-Story Dependencies

- Story 1.1 establishes the `library` module, the codegen pipeline, the `UserAgentInfo`/`Component` model, and the parse API shape — Stories 1.2 and 1.3 extend the same module/API/codegen approach rather than introducing new infrastructure (1.2 adds `os_parsers` + `Component` for OS; 1.3 adds `device_parsers` + `Device`).
- Story 1.4's shared `commonTest` corpus and per-target sample apps depend on all of 1.1–1.3 being implemented, since it validates browser/engine, OS, and device parsing together across all four targets.
- This entire epic is a prerequisite for Epic 2 (generate): the generate direction reuses this epic's module, data model, codegen pipeline, and dependency boundaries rather than establishing its own.
