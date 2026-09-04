# Epic 4 Context: Composable, Tree-Shakeable Type-Pack API

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Replace the fixed-shape `UserAgentParser.parse(String)` / `UserAgentGenerator.generate(UserAgentInfo)` singleton API (shipped and already published in Epics 1–3) with two factory functions composed from pluggable "type packs," so a JS/npm consumer who only wants browser detection isn't forced to load OS/device/bot/AI-agent rule tables, and so any consumer can add their own detection categories without forking the library. This was triggered by hands-on validation of the published npm package in a clean Node.js project, which surfaced two gaps in the old API: no bundle-size control (one monolithic rule table always loads) and no extensibility (no bot/AI-agent detection, no way to add categories). The regex-matching engine, uap-core vendoring/codegen mechanics, and the Maven Central + npm publish pipelines built in Epics 1–3 are reused as-is — this epic reworks the public API surface and how rule data is organized for loading, not the underlying detection logic or publishing infrastructure.

## Stories

- Story 4.1: Redesign parse/generate as composable type packs, validated via npm/Node
- Story 4.2: Validate type-pack API parity across all four targets and republish

## Requirements & Constraints

- The public API is a pair of factory functions — one for parse, one for generate — each taking a variadic list of type packs and returning the callable parse/generate function.
- **Passing no packs returns an always-empty result** (all `UserAgentInfo` fields `null`, or an empty generated string) — there is no implicit fallback to an "all types" bundle. A consumer wanting everything must pass the all-types pack explicitly.
- Built-in packs must be individually importable so the JS/npm build can dead-code-eliminate packs a consumer doesn't reference — verified by comparing built JS bundle size for a single-pack import vs. the all-types bundle, via a fresh install in a clean Node.js project (this mirrors the manual check that surfaced the redesign).
- Consumers may author their own packs following the same documented shape to add detection categories without forking the library; a custom pack must contribute to parse/generate results without any library source change, and must not be able to take down a composed call if it throws.
- Built-in packs required: browser, engine, OS, device, bot, AI-agent, plus a convenience bundle of all of them. Bot and AI-agent detection are new capabilities not present before this epic (hand-seeded rules — no vendored uap-core source exists for them) and are explicitly deferred out of this epic's two stories to a later follow-up; `UserAgentInfo` still gains `bot`/`aiAgent` fields now so that follow-up needs no further data-model break.
- Parsing with a subset of packs must populate only the fields those packs cover, leaving other result fields `null` — never a sentinel value, never an exception.
- Same Kotlin call site must compile and return identical results on all four MVP targets (Android, iOS, JVM, JS); this must be verified on Android/iOS/JVM too, not assumed from JS parity, before republishing.
- This is a breaking change: the old fixed-shape API must not be present in the new release. The release is a new major version to both Maven Central and npm, reusing the existing publish pipelines (no new publish infrastructure). README/CHANGELOG must document the new API and a migration note from the old `parse()`/`generate()` shape.
- Per-target sample apps (from Epics 1–2) must be updated to call the new factory-function API and continue to compile/run as manual validation harnesses; the sample-depends-on-library dependency direction is unchanged.
- Only MIT/Apache-2.0/BSD-licensed dependencies and vendored data anywhere in the library or build; no copyleft. Common Kotlin only — no wrapping platform-native/JVM-only parsers.

## Technical Decisions

- **Rule data (AD-1, amended):** compiled tables move from 3 monolithic ordered lists (user_agent/os/device) to **one compiled table per type pack** — browser, engine, os, device, bot, aiAgent — each its own top-level `commonMain` value, generated at build time (never a runtime resource), so unused packs tree-shake out of a JS build. Within a table: first match wins, file order preserved, using uap-core's replacement-template fields for the vendored categories (browser/OS/device). Bot and AI-agent tables are hand-seeded (not vendored from uap-core, which has no such sections) — a small, explicitly non-exhaustive starter list, extensible via custom packs. The existing JS-dialect regex normalization in codegen still applies uniformly to all targets.
- **Data model (AD-2, amended):** `UserAgentInfo` gains `bot: Component?` and `aiAgent: Component?` as first-class fields alongside existing `browser`/`engine`/`os`/`device`, plus a small extension point for data a custom pack contributes beyond the built-in fields (exact shape is a dev-story-level decision, not fixed by architecture). `Component`/`Device` are unchanged. Version stays a plain nullable `String` (no parsed semver).
- **Public API (AD-3, replaced and then refined):** `UserAgentParser(vararg packs: UserAgentTypePack): (String) -> UserAgentInfo` and `UserAgentGenerator(vararg packs: UserAgentTypePack): (UserAgentInfo) -> String`. **No packs passed returns an always-empty result — not** an implicit fallback to the all-types bundle. This was refined mid-implementation: a factory body that internally references the all-types pack as a fallback gives every call site a static reachability edge to every built-in pack under standard JS bundlers' module-level tree-shaking, which defeats AD-1's per-pack tree-shaking goal regardless of which packs the call site actually passes. Consumers wanting everything call the parser/generator with the all-types pack explicitly. Still stateless — no mutable shared state, no caching beyond the compiled read-only tables. Still exactly one entry point per direction (no coexisting alternate spelling, e.g. no `toUserAgentString()`).
- **Built-in packs to ship:** browser, engine, OS, device, bot, AI-agent type packs, plus an all-types bundling pack. The type-pack shape must be documented so consumers can author custom packs against it.
- **Naming/package:** root package `site.lempert.useragent`, no platform suffixes on shared names. The generator-side type is named symmetrically with the parser-side type per the existing naming convention.
- **Dependency boundary (AD-4, unchanged):** `commonMain`/`androidMain`/`iosMain`/`jvmMain`/`jsMain` depend only on the Kotlin stdlib; `commonTest` may depend on `kotlin.test`. Sample apps depend on the library, never the reverse.
- **Test corpus (AD-5, unchanged mechanism, extended content):** the existing shared `commonTest` corpus (kotlin.test, all four targets, CI gate) is extended with pack-composition cases — subset packs, the all-types pack, and at least one custom pack.
- **Reused unchanged:** the regex-matching engine itself, the uap-core vendoring/codegen pipeline mechanics (AD-1's normalization logic), the Maven Central and npm publish pipelines from Epic 3, and the Apache-2.0 `NOTICE` file requirement (AD-6).

## Cross-Story Dependencies

- Story 4.2 depends on Story 4.1: the composable type-pack API, built-in packs, and codegen rework must exist and be validated on JS/npm before parity is checked on Android/iOS/JVM and a new major version is republished. Story 4.1 is complete.
- Both stories depend on Epics 1–3's already-published infrastructure: the regex/codegen mechanics (Epic 1), the generate-direction logic (Epic 2), and the Maven Central + npm publish pipelines (Epic 3) are reused as-is, not rebuilt.
- Republishing (Story 4.2) supersedes the live Epic 3 artifacts on Maven Central and npm — the old fixed-shape API must not remain as the latest published version after this epic.
- Bot/AI-agent detection packs are out of scope for both stories in this epic and tracked as a separate deferred follow-up; that follow-up will build on the `bot`/`aiAgent` fields and the custom-pack extension point Story 4.1 already put in place.
