---
id: SPEC-user-agent
companions: ["../../planning-artifacts/architecture/architecture-kmp-user-agent-2026-09-01/ARCHITECTURE-SPINE.md"]
sources: []
---

> **Canonical contract.** This SPEC and the files in `companions:` are the complete, preservation-validated contract for what to build, test, and validate. Source documents listed in frontmatter are for traceability — consult them only if you need narrative rationale or prose color this contract intentionally omits.

# KMP User-Agent Library

## Why

This is a vision to realize: an open-source, MIT-licensed Kotlin Multiplatform library that parses and generates User-Agent strings behind one common API, so KMP projects (and their Android/iOS/JVM/JS targets) stop reaching for platform-specific or JVM-only UA parsers. The owner wants a fast, pragmatic MVP shipped and publishable first, with detection coverage and features deepened afterward.

## Capabilities

- **CAP-1** Parse
  - **intent:** A consumer can parse a raw User-Agent string into structured data (browser name/version, rendering engine, OS name/version, device type/model).
  - **success:** Parsing a representative sample set covering major browsers/OS/devices returns correct structured fields on every MVP target (Android, iOS, JVM, JS).

- **CAP-2** Generate
  - **intent:** A consumer can build a valid User-Agent string from structured input data (the inverse of CAP-1).
  - **success:** Generated strings match known-good UA formats, or round-trip through CAP-1's parser, for the supported browser/OS/device combinations.

- **CAP-3** Common API
  - **intent:** A consumer calls one common KMP API for parse and generate that behaves identically from Android, iOS, JVM, and JS.
  - **success:** The same Kotlin call site compiles and returns correct parse/generate results on all four MVP targets.

- **CAP-4** Publish
  - **intent:** The library is published as an MIT-licensed multiplatform artifact consumable via standard package managers.
  - **success:** A fresh project can add the published dependency and successfully parse/generate a UA string on each MVP target.

## Constraints

- MIT license end-to-end; only permissive-licensed (MIT/Apache-2.0/BSD) 3rd-party dependencies are allowed — no copyleft (GPL/LGPL) — so downstream MIT consumers never inherit copyleft obligations.
- MVP targets are Android, iOS, JVM/Desktop, and JS(Web) only; Wasm/WasmJs is explicitly deferred and not required for v1.
- The library is implemented in common Kotlin from scratch, not by wrapping platform-native/JVM-only parser libraries at runtime — a wrapped library is rarely available identically across Android/iOS/JVM/JS and would break CAP-3's single common API. Implementation may take algorithmic/rule inspiration from existing open-source UA parsers (e.g. uap-core/uap-java's regex-based detection rules, and comparable UA parser/generator projects) as design and data reference, provided any borrowed rule data stays license-compatible (permissive) with the MIT policy; these projects are not added as runtime dependencies.
- The current repo (kmp-user-agent) is scaffolded as an application template (androidApp/iosApp/webApp + sharedLogic/sharedUI with Compose Multiplatform and a React web app), not a publishable library module — architecture must introduce a dedicated library module, plus minimal sample/test apps per MVP target (Android, iOS, JVM, JS) that exercise the library's parse/generate calls as validation harnesses only (not the product). Existing app-template modules may be repurposed or replaced for this.
- v1 favors shipping speed and pragmatic scope over exhaustive browser/OS/device coverage; broader detection coverage is explicit post-MVP iteration work.
- The Maven Central publishing namespace is the reverse-DNS of the owner's domain, `site.lempert` (domain `lempert.site`), verified via the Sonatype Central Portal's domain-ownership check.
- A JS/npm package is published for JS consumers in addition to the Maven Central Kotlin/JS artifact; it must be produced as part of the standard build's dist output (the Kotlin/JS Gradle plugin's dist folder), not a separate hand-built step.

## Non-goals

- Not building an exhaustive, continuously-updated device/browser/bot detection database competitive with mature parsers (e.g. ua-parser-js level coverage) in v1 — deferred to post-MVP iteration.
- Not a UA-string based analytics, bot-detection, or fraud-scoring product.
- Not shipping a UI or application; this is a library only, distinct from the existing app-template modules in this repo.
- Wasm/WasmJs target is out of scope for the v1 MVP.

## Success signal

v1 is done when the library is published (MIT license, permissive-only dependencies) to Maven Central and as an npm package, with a common API that parses and generates User-Agent strings correctly against a documented set of test UA strings, verified passing on Android, iOS, JVM, and JS targets.

## Open Questions

- ~~Exact library artifact id (e.g. `user-agent`, `user-agent-kmp`) under the `site.lempert` namespace not yet chosen.~~ Resolved: `kmp-user-agent` (see spec-3-1-publish-the-library-to-maven-central.md).
