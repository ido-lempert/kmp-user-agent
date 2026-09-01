# Epic 2 Context: Generate a User-Agent String

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Enable a consumer to call `UserAgentGenerator.generate(UserAgentInfo)` to build a valid User-Agent string from structured browser, engine, OS, and device data — the inverse of Epic 1's parse direction — using the same data model and API shape, correct and identical on Android, iOS, JVM, and JS. This epic does not stand up new infrastructure: it reuses Epic 1's `library` module, `UserAgentInfo`/`Component`/`Device` model, codegen pipeline, and stdlib-only dependency boundary, adding only generate-direction rule templates, the generate API itself, and generate test cases layered onto the existing shared test corpus. Per-target sample apps are extended to also exercise generate.

## Stories

- Story 2.1: Generate a User-Agent String from Browser & Engine Data
- Story 2.2: Generate a User-Agent String Including OS & Device Data
- Story 2.3: Validate Generation Across All Four Targets

## Requirements & Constraints

- A consumer must be able to build a valid UA string from structured input data such that generated strings either match known-good UA formats or round-trip through the parser to recover the same structured data, for supported browser/OS/device combinations — identically on Android, iOS, JVM, and JS via the one common KMP API.
- When an input field (`browser`, `engine`, `os`, or `device`) is `null`, the corresponding segment must simply be omitted from the generated string — never a placeholder or sentinel value — and no exception may be thrown for any input.
- A fully-populated `UserAgentInfo` must round-trip: generating a string and then parsing it back must recover the original values for supported combinations.
- MVP targets are exactly Android, iOS, JVM/Desktop, and JS(Web); Wasm/WasmJs is out of scope for v1.
- v1 favors shipping speed and pragmatic coverage over exhaustive browser/OS/device support; broader coverage is explicitly post-MVP.
- Only MIT/Apache-2.0/BSD-licensed dependencies and vendored data are allowed anywhere in the library or its build — no copyleft.

## Technical Decisions

- **Public API (stateless, single entry point):** exactly `UserAgentGenerator.generate(UserAgentInfo): String` — no coexisting `toUserAgentString()` extension or other second entry point, no mutable shared state, no caching beyond the compiled rule table already established in Epic 1.
- **Data model (reused, not reinvented):** generate consumes the same `UserAgentInfo(browser: Component?, engine: Component?, os: Component?, device: Device?)` / `Component(name: String?, version: String?)` / `Device(brand: String?, model: String?, name: String?)` model that Epic 1's parser produces — one symmetric model for both directions, no parallel generate-only shape.
- **Rule/template reuse:** generate-direction logic lives in `commonMain` alongside the parser, over the same build-time-compiled rule table approach from Epic 1 (rules vendored from `uap-core`, codegen'd at build time, never loaded as a runtime resource). No `expect`/`actual` split is needed for generation itself, consistent with the parser.
- **Dependency boundary (unchanged):** generate code lives in the same production source sets (`commonMain`, `androidMain`, `iosMain`, `jvmMain`, `jsMain`) which depend only on the Kotlin stdlib; `commonTest` may additionally depend on `kotlin.test`. Sample apps depend on the `library` module, never the reverse.
- **Naming/package (unchanged):** root package `site.lempert.useragent`; public type `UserAgentGenerator` added alongside the existing `UserAgentInfo`, `Component`, `Device`, `UserAgentParser` — no platform suffixes.
- **Cross-cutting conventions (unchanged):** no logging; no exceptions for unrepresentable or partial input — omission of the segment is the result, not an error.
- **Stack pins relevant here:** Kotlin/KMP Gradle plugin 2.4.10; `kotlin.test` bundled with Kotlin 2.4.10 (for the extended shared corpus).

## Cross-Story Dependencies

- This entire epic depends on Epic 1 having established the `library` module, the `UserAgentInfo`/`Component`/`Device` model, the codegen pipeline, and the dependency boundaries — generate reuses all of it rather than introducing new infrastructure.
- Story 2.1 establishes the generate API shape and browser/engine templating; Story 2.2 extends the same API and approach to add OS and device segments rather than introducing a new mechanism.
- Story 2.3's shared-corpus additions and sample-app extensions depend on both 2.1 and 2.2 being implemented, since it validates browser+engine, OS, and device generation together, and exercises full round-trips against Epic 1's parser, across all four targets.
