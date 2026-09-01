---
stepsCompleted: [step-01-validate-prerequisites, step-02-design-epics, step-03-create-stories]
inputDocuments:
  - _bmad-output/specs/spec-user-agent/SPEC.md
  - _bmad-output/planning-artifacts/architecture/architecture-kmp-user-agent-2026-09-01/ARCHITECTURE-SPINE.md
---

# kmp-user-agent - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for kmp-user-agent, decomposing the requirements from SPEC-user-agent (in place of a PRD; this project's requirements were distilled via bmad-spec) and the ARCHITECTURE-SPINE into implementable stories. No UX design document exists — this is a library with no UI.

## Requirements Inventory

### Functional Requirements

FR1: A consumer can parse a raw User-Agent string into structured data (browser name/version, rendering engine, OS name/version, device type/model), returning correct fields for a representative sample set covering major browsers/OS/devices on every MVP target (Android, iOS, JVM, JS). (CAP-1)

FR2: A consumer can build a valid User-Agent string from structured input data — the inverse of FR1 — such that generated strings match known-good UA formats or round-trip through the parser for supported browser/OS/device combinations. (CAP-2)

FR3: A consumer calls one common Kotlin Multiplatform API for parse and generate that behaves identically from Android, iOS, JVM, and JS — the same call site compiles and returns correct results on all four MVP targets. (CAP-3)

FR4: The library is published as an MIT-licensed multiplatform artifact consumable via standard package managers (Maven Central for Kotlin/JVM/Android/iOS consumers, npm for JS consumers), such that a fresh project can add the dependency and successfully parse/generate a UA string on each MVP target. (CAP-4)

### NonFunctional Requirements

NFR1: MIT license end-to-end; only permissive-licensed (MIT/Apache-2.0/BSD) third-party dependencies are allowed anywhere in the library or its build — no copyleft (GPL/LGPL) — so downstream MIT consumers never inherit copyleft obligations.

NFR2: MVP targets are limited to Android, iOS, JVM/Desktop, and JS(Web); Wasm/WasmJs is explicitly deferred and not required for v1.

NFR3: The library is implemented in common Kotlin from scratch, not by wrapping platform-native/JVM-only parser libraries at runtime, since a wrapped library is rarely available identically across Android/iOS/JVM/JS and would break FR3's single common API. Rule/detection logic may take algorithmic inspiration from existing open-source UA parsers (e.g. uap-core/uap-java), provided any borrowed rule data stays license-compatible (permissive) with NFR1; these projects are not added as runtime dependencies.

NFR4: v1 favors shipping speed and pragmatic scope over exhaustive browser/OS/device coverage; broader detection coverage is explicit post-MVP iteration work.

NFR5: The Maven Central publishing namespace is the reverse-DNS of the owner's domain, `site.lempert` (domain `lempert.site`), verified via the Sonatype Central Portal's domain-ownership check.

NFR6: A JS/npm package is published for JS consumers in addition to the Maven Central Kotlin/JS artifact; it must be produced as part of the standard build's dist output (the Kotlin/JS Gradle plugin's dist folder), not a separate hand-built step.

### Additional Requirements

- The current repo (kmp-user-agent) is scaffolded as an application template (androidApp/iosApp/webApp + sharedLogic/sharedUI with Compose Multiplatform and a React web app), not a publishable library module — a dedicated `library` module must be introduced, plus minimal sample/test apps per MVP target (Android, iOS, JVM, JS) that exercise the library's parse/generate calls as validation harnesses only (not the product). Existing app-template modules may be repurposed or replaced for this. **No starter/greenfield template is specified by Architecture — this is a restructuring of an existing repo, not a fresh scaffold.**
- **AD-1 (Rule data compilation):** Browser/OS/device detection patterns are vendored from `uap-core`'s `regexes.yaml` (Apache-2.0, attribution per AD-6) and converted into `commonMain` Kotlin source by a Gradle code-gen task at build time — no target loads the rule set as a runtime resource. The generated table is three ordered pattern lists (`user_agent_parsers`, `os_parsers`, `device_parsers`) mirroring `uap-core`'s structure; each entry holds a regex plus replacement-template fields, evaluated in file order, first match wins per category. The codegen task strips/normalizes non-metacharacter backslash escapes illegal under Kotlin/JS's mandatory ECMAScript `u`-flag regex dialect, uniformly for all targets (a safe no-op on JVM/Native).
- **AD-2 (Data model):** One symmetric data model for parse and generate: `UserAgentInfo(browser: Component?, engine: Component?, os: Component?, device: Device?)`, `Component(name: String?, version: String?)`, `Device(brand: String?, model: String?, name: String?)`. Version stays a plain `String?` (no parsed semver) for v1. This one class is both the parser's output and the generator's input; no parallel model.
- **AD-3 (Stateless API surface):** The public surface is exactly `UserAgentParser.parse(String): UserAgentInfo` and `UserAgentGenerator.generate(UserAgentInfo): String` — no coexisting `toUserAgentString()` extension or second entry point. No mutable shared state and no caching beyond the read-only compiled rule table.
- **AD-4 (Dependency direction):** Every production source set (`commonMain`, `androidMain`, `iosMain`, `jvmMain`, `jsMain`) depends only on the Kotlin stdlib (`commonTest` may depend on `kotlin.test`). Each MVP target's sample/test app depends on the library module; the library module never depends on a sample app.
- **AD-5 (Shared test corpus):** One `commonTest` data set (seeded from a subset of `uap-core`'s own Apache-2.0 test fixtures, plus hand-picked cases for generate) runs via `kotlin.test` identically on all four MVP targets, as a CI gate on every change.
- **AD-6 (License/provenance):** Only MIT/Apache-2.0/BSD-licensed dependencies and vendored data are permitted anywhere in the library or its build. The vendored `uap-core` snapshot's Apache-2.0 notice is preserved in a `NOTICE` file shipped alongside every published artifact.
- **AD-7 (npm packaging):** The npm package is produced from the `js(IR)` target's own Gradle dist output via `org.jetbrains.kotlin.npm-publish` (v3.7.x lineage) wired into the same build — no hand-authored `package.json`, no manually copied files.
- **Stack pins:** Kotlin/KMP Gradle plugin 2.4.10; `com.vanniktech.maven.publish` 0.37.0 (Sonatype Central Portal host); `uap-core regexes.yaml` vendored Apache-2.0 snapshot (pinned commit at vendor time); `kotlin.test` bundled with Kotlin 2.4.10; `org.jetbrains.kotlin.npm-publish` v3.7.x lineage.
- **Naming convention:** Public types `UserAgentInfo`, `Component`, `Device`, `UserAgentParser`, `UserAgentGenerator`; root package `site.lempert.useragent`; no platform suffixes on shared names.
- **Data convention:** Fields nullable where a UA string doesn't carry that data; never a sentinel string like `"unknown"`.
- **Cross-cutting convention:** No logging, no exceptions for "not recognized" data — a partially-populated `UserAgentInfo` is the result, not an error.
- **Deferred (explicitly out of scope for this epic/story breakdown):** exact library artifact id under `site.lempert` (open question); whether to repurpose vs. rebuild `androidApp`/`iosApp`/`webApp`/`sharedLogic`/`sharedUI` as `samples/` (a build-time call); vendored `uap-core` refresh cadence; CI/CD pipeline specifics beyond "run the shared corpus on all four targets."

### UX Design Requirements

N/A — no UX design document exists for this project. kmp-user-agent is a library with no UI; the existing app-template modules (androidApp/iosApp/webApp/sharedLogic/sharedUI) are candidates for repurposing as sample/validation harnesses only, not product UI.

### FR Coverage Map

FR1: Epic 1 - Parse a User-Agent string into structured data
FR2: Epic 2 - Generate a User-Agent string from structured data
FR3: Epic 1 (parse side) + Epic 2 (generate side) - One common API behaving identically across Android/iOS/JVM/JS
FR4: Epic 3 - Publish as MIT-licensed multiplatform artifact via Maven Central + npm

## Epic List

### Epic 1: Parse a User-Agent String
A consumer can add the library's `library` module to a project and call `UserAgentParser.parse(String)` to get structured browser/engine/OS/device data — correct on Android, iOS, JVM, and JS alike. This epic does the foundational build-out since parsing can't exist without it: the new `library` module (repurposing/retiring the existing app-template scaffolding), the `uap-core` rule vendoring + Gradle codegen pipeline (AD-1), the shared `UserAgentInfo`/`Component`/`Device` model (AD-2), the stateless parse API (AD-3), the stdlib-only dependency boundary (AD-4), and the shared `commonTest` corpus running on all 4 targets (AD-5) — seeded with parse cases. Includes thin per-target sample apps that call parse.
**FRs covered:** FR1, FR3 (parse side)

### Epic 2: Generate a User-Agent String
A consumer can call `UserAgentGenerator.generate(UserAgentInfo)` to build a valid UA string from structured data — the inverse of Epic 1 — using the same data model and API shape, on the same 4 targets. Builds on Epic 1's module/model/codegen but stands alone as new capability: generate-direction rule templates, the generate API, and generate test cases added to the shared corpus. Sample apps extended to exercise generate too.
**FRs covered:** FR2, FR3 (generate side)

### Epic 3: Publish the Library
A consumer can add the library as a real dependency from Maven Central (Kotlin/Android/iOS/JVM) or npm (JS) in a fresh project and have it work. Covers the `site.lempert` Maven Central namespace + domain verification, the `com.vanniktech.maven.publish` setup, the npm package produced from the JS dist output (AD-7) with no hand-authored `package.json`, the Apache-2.0 `NOTICE` file (AD-6), and license/dependency compliance (NFR1) verified end-to-end.
**FRs covered:** FR4

## Epic 1: Parse a User-Agent String

A consumer can add the library's `library` module to a project and call `UserAgentParser.parse(String)` to get structured browser/engine/OS/device data — correct on Android, iOS, JVM, and JS alike.

### Story 1.1: Parse Browser & Engine from a User-Agent String

As a KMP developer integrating the library,
I want to call a common parse API that returns browser and rendering-engine name/version from a raw User-Agent string,
So that I can build browser/engine-aware behavior without writing platform-specific UA parsing code.

**Acceptance Criteria:**

**Given** the `library` module is added as a dependency on Android, iOS, JVM, or JS
**When** I call `UserAgentParser.parse(rawUserAgentString)`
**Then** it returns a `UserAgentInfo` with `browser` and `engine` populated as `Component(name, version)` matching the known browser/engine for representative UA strings from major browsers (Chrome, Firefox, Safari, Edge)
**And** the same Kotlin call site compiles and returns identical results on all four MVP targets

**Given** a User-Agent string with no recognizable browser/engine pattern
**When** I call `UserAgentParser.parse(rawUserAgentString)`
**Then** `browser` and/or `engine` are `null` rather than a sentinel string like "unknown", and no exception is thrown

**Given** the library module's production source sets
**When** dependencies are inspected
**Then** `commonMain`/`androidMain`/`iosMain`/`jvmMain`/`jsMain` depend only on the Kotlin stdlib (AD-4), and the root package is `site.lempert.useragent` with public types named `UserAgentInfo`, `Component`, `UserAgentParser`

**Given** the vendored `uap-core` `regexes.yaml` snapshot for `user_agent_parsers`
**When** the Gradle codegen task runs at build time
**Then** it generates `commonMain` Kotlin source (not a runtime-loaded resource) with patterns normalized for Kotlin/JS's mandatory ECMAScript `u`-flag dialect, applied uniformly on all targets, evaluated in file order with first match winning

### Story 1.2: Parse OS from a User-Agent String

As a KMP developer integrating the library,
I want the parse API to also return OS name/version from a raw User-Agent string,
So that I can build OS-aware behavior alongside browser detection using the same call.

**Acceptance Criteria:**

**Given** the existing parse API from Story 1.1
**When** I call `UserAgentParser.parse(rawUserAgentString)` with a UA string identifying a known OS (e.g. Windows, macOS, iOS, Android, Linux)
**Then** the returned `UserAgentInfo.os` is populated as `Component(name, version)` matching the known OS, correct on all four MVP targets

**Given** a User-Agent string with no recognizable OS pattern
**When** I call `UserAgentParser.parse(rawUserAgentString)`
**Then** `os` is `null` rather than a sentinel value, and no exception is thrown

**Given** the vendored `uap-core` `regexes.yaml` snapshot for `os_parsers`
**When** the Gradle codegen task runs at build time
**Then** it generates the `os_parsers` rule table in `commonMain` following the same codegen approach and JS-dialect normalization as Story 1.1

### Story 1.3: Parse Device Info from a User-Agent String

As a KMP developer integrating the library,
I want the parse API to also return device brand/model/name from a raw User-Agent string,
So that I can build device-aware behavior using the same call.

**Acceptance Criteria:**

**Given** the existing parse API from Stories 1.1/1.2
**When** I call `UserAgentParser.parse(rawUserAgentString)` with a UA string identifying a known device (e.g. an iPhone, a specific Android device model)
**Then** the returned `UserAgentInfo.device` is populated as `Device(brand, model, name)` matching the known device, correct on all four MVP targets

**Given** a User-Agent string with no recognizable device pattern (e.g. a desktop browser UA)
**When** I call `UserAgentParser.parse(rawUserAgentString)`
**Then** `device` is `null` rather than a sentinel value, and no exception is thrown

**Given** the vendored `uap-core` `regexes.yaml` snapshot for `device_parsers`
**When** the Gradle codegen task runs at build time
**Then** it generates the `device_parsers` rule table in `commonMain` following the same codegen approach as Stories 1.1/1.2

### Story 1.4: Validate Parsing Across All Four Targets

As a KMP developer relying on the library,
I want confidence that parsing behaves identically and correctly across Android, iOS, JVM, and JS, verified automatically,
So that I can trust the library without re-testing platform-specific UA parsing myself.

**Acceptance Criteria:**

**Given** a shared `commonTest` data set seeded from a subset of `uap-core`'s own Apache-2.0 test fixtures, covering major browsers/OS/devices
**When** the test suite runs via `kotlin.test`
**Then** it executes identically on all four MVP targets and passes for every fixture case

**Given** the shared test corpus
**When** a change is pushed to the repository
**Then** it runs as a CI gate on all four targets, failing the build if any target diverges from another for the same input

**Given** thin per-target sample apps (Android, iOS, JVM, JS) that depend on the `library` module
**When** each sample app calls `UserAgentParser.parse()` with a representative UA string
**Then** it displays/logs the parsed `UserAgentInfo` correctly as a manual validation harness (not the product itself), and the sample app depends on the library — never the reverse (AD-4)

## Epic 2: Generate a User-Agent String

A consumer can call `UserAgentGenerator.generate(UserAgentInfo)` to build a valid UA string from structured data — the inverse of Epic 1 — using the same data model and API shape, on the same 4 targets.

### Story 2.1: Generate a User-Agent String from Browser & Engine Data

As a KMP developer integrating the library,
I want to call a common generate API that builds a User-Agent string from browser and engine data,
So that I can construct valid UA strings for testing or synthetic requests without hand-writing UA format rules.

**Acceptance Criteria:**

**Given** a `UserAgentInfo` with `browser` and `engine` populated as `Component(name, version)`
**When** I call `UserAgentGenerator.generate(userAgentInfo)`
**Then** it returns a UA string that either matches a known-good UA format for that browser/engine combination or round-trips through `UserAgentParser.parse()` to recover the same `browser`/`engine` values, correct on all four MVP targets

**Given** a `UserAgentInfo` with `browser` or `engine` as `null`
**When** I call `UserAgentGenerator.generate(userAgentInfo)`
**Then** it omits the corresponding segment from the generated string rather than inserting a placeholder, and no exception is thrown

**Given** the public generate API
**When** its signature is inspected
**Then** it is exactly `UserAgentGenerator.generate(UserAgentInfo): String` — no coexisting `toUserAgentString()` extension or second entry point (AD-3), stateless with no caching beyond the compiled rule table

### Story 2.2: Generate a User-Agent String Including OS & Device Data

As a KMP developer integrating the library,
I want the generate API to also incorporate OS and device data into the output string,
So that I can construct a fully-specified UA string in one call.

**Acceptance Criteria:**

**Given** a `UserAgentInfo` with `os` populated as `Component(name, version)`
**When** I call `UserAgentGenerator.generate(userAgentInfo)`
**Then** the returned UA string includes the OS segment matching known-good format for that OS, correct on all four MVP targets

**Given** a `UserAgentInfo` with `device` populated as `Device(brand, model, name)`
**When** I call `UserAgentGenerator.generate(userAgentInfo)`
**Then** the returned UA string includes the device segment matching known-good format for that device, correct on all four MVP targets

**Given** a fully-populated `UserAgentInfo` (browser, engine, os, device all non-null)
**When** I call `UserAgentGenerator.generate(userAgentInfo)` and then `UserAgentParser.parse()` on the result
**Then** the parsed result matches the original `UserAgentInfo` for the supported browser/OS/device combination (round-trip)

### Story 2.3: Validate Generation Across All Four Targets

As a KMP developer relying on the library,
I want confidence that generation behaves identically and correctly across Android, iOS, JVM, and JS, verified automatically,
So that I can trust the library's generate direction without re-testing it myself per platform.

**Acceptance Criteria:**

**Given** the shared `commonTest` corpus from Story 1.4
**When** hand-picked generate test cases are added covering browser+engine, OS, and device combinations
**Then** the suite runs via `kotlin.test` identically on all four MVP targets and passes for every case, as part of the existing CI gate

**Given** the per-target sample apps from Story 1.4
**When** each sample app is extended to also call `UserAgentGenerator.generate()` with representative structured data
**Then** it displays/logs the generated UA string correctly as a manual validation harness, with no change to the sample-depends-on-library dependency direction (AD-4)

## Epic 3: Publish the Library

A consumer can add the library as a real dependency from Maven Central (Kotlin/Android/iOS/JVM) or npm (JS) in a fresh project and have it work.

### Story 3.1: Publish the Library to Maven Central

As a KMP developer maintaining the library,
I want to publish the library's Kotlin/Android/iOS/JVM artifacts to Maven Central under a verified namespace,
So that consumers can add it as a standard dependency from a trusted, permanent registry.

**Acceptance Criteria:**

**Given** the `site.lempert` reverse-DNS namespace and the `lempert.site` domain
**When** domain ownership is verified via the Sonatype Central Portal's domain-ownership check
**Then** the namespace is approved for publishing under `site.lempert`

**Given** the `com.vanniktech.maven.publish` plugin (0.37.0) configured in the library's build
**When** a release build is published
**Then** the Kotlin/Android/iOS/JVM artifacts are published to Maven Central under the `site.lempert` namespace with correct POM metadata (license: MIT)

**Given** the vendored `uap-core` Apache-2.0 snapshot
**When** the published artifact is inspected
**Then** a `NOTICE` file preserving the Apache-2.0 attribution is included alongside the published artifact (AD-6)

**Given** the full dependency graph of the library and its build
**When** dependencies are audited
**Then** every dependency is MIT/Apache-2.0/BSD-licensed with no copyleft (GPL/LGPL) dependency present (NFR1)

### Story 3.2: Publish the JS Package to npm

As a KMP developer maintaining the library,
I want to publish a JS/npm package built directly from the Kotlin/JS build's own dist output,
So that JS consumers can install the library via npm without a separately maintained artifact that could drift from the real build.

**Acceptance Criteria:**

**Given** the `js(IR)` target's Gradle dist output for the library
**When** `org.jetbrains.kotlin.npm-publish` (v3.7.x lineage) is wired into the same build
**Then** the npm package is produced from that dist output directly — no hand-authored `package.json`, no manually copied files (AD-7)

**Given** a standard `npm publish` (or CI-equivalent) run
**When** the package is published
**Then** it appears on the npm registry under a name consistent with the `site.lempert` namespace convention, installable via a standard `npm install`

### Story 3.3: Verify Fresh-Project Consumption of the Published Library

As a KMP developer evaluating the library,
I want to add the published dependency to a brand-new project on each MVP target and have parsing/generation work immediately,
So that I know the library is genuinely ready for real-world use, not just successfully uploaded.

**Acceptance Criteria:**

**Given** a fresh Android project with no prior relationship to this repository
**When** the published Maven Central dependency is added and `UserAgentParser.parse()` / `UserAgentGenerator.generate()` are called
**Then** both calls succeed and return correct results, using only the publicly published artifact

**Given** a fresh iOS project
**When** the published Maven Central (Kotlin/Native) dependency is added and parse/generate are called
**Then** both calls succeed and return correct results

**Given** a fresh JVM project
**When** the published Maven Central dependency is added and parse/generate are called
**Then** both calls succeed and return correct results

**Given** a fresh JS/Node project
**When** the published npm package is installed and parse/generate are called
**Then** both calls succeed and return correct results, completing the SPEC's v1 success signal across all four MVP targets
