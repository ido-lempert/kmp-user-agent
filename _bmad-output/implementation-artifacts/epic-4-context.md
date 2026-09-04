# Epic 4 Context: Composable, Tree-Shakeable Type-Pack API

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Replace the fixed-shape `UserAgentParser.parse(String)` / `UserAgentGenerator.generate(UserAgentInfo)` singleton API (shipped and already published in Epics 1–3) with two factory functions composed from pluggable "type packs," so a JS/npm consumer who only wants browser detection isn't forced to load OS/device/bot/AI-agent rule tables, and so any consumer can add their own detection categories without forking the library. This was triggered by hands-on validation of the published npm package in a clean Node.js project, which surfaced two gaps in the old API: no bundle-size control and no extensibility. Stories 4.1 and 4.2 have already redesigned and shipped the factory-function/type-pack API (parity-verified on all four targets, republished as `0.2.0`); Story 4.3 was added afterward to fill in the one built-in capability that redesign deliberately deferred: actual bot and AI-agent detection packs. The regex-matching engine, uap-core vendoring/codegen mechanics, and the Maven Central + npm publish pipelines from Epics 1–3 are reused as-is throughout.

## Stories

- Story 4.1: Redesign parse/generate as composable type packs, validated via npm/Node — **done**
- Story 4.2: Validate type-pack API parity across all four targets and republish — **done**
- Story 4.3: Add Bot and AI-Agent Detection Packs

## Requirements & Constraints

- The public API is a pair of factory functions — one for parse, one for generate — each taking a variadic list of type packs and returning the callable parse/generate function. This is already shipped; Story 4.3 does not change the factory signatures.
- Passing no packs returns an always-empty result; there is no implicit fallback to an "all types" bundle. `UserAgentBotTypes`/`UserAgentAIAgentTypes` must be individually importable/composable exactly like the existing built-in packs, and `UserAgentAllTypes` must include both once they exist.
- Bot/AI-agent rule data must be a small, explicitly non-exhaustive, hand-authored rule set sourced only from each bot/crawler operator's own public documentation (e.g. Google's, Microsoft's, OpenAI's, Anthropic's, Perplexity's own docs) — never copied from a third-party commercial bot-detection dataset. This is a license-compatibility constraint (only MIT/Apache-2.0/BSD-licensed data anywhere in the library; no copyleft), since there is no uap-core section to vendor from for these two categories.
- Parsing a UA string matching a documented bot/crawler with the corresponding pack included must populate `bot` or `aiAgent` as `Component(name, version)`; a UA string matching nothing must leave the field `null` (never a sentinel value) and never throw.
- Same Kotlin call site must compile and return identical results on all four MVP targets (Android, iOS, JVM, JS).
- The shared `commonTest` corpus (kotlin.test, all four targets, CI gate) must be extended with bot/AI-agent detection cases and continue passing identically everywhere.
- Only MIT/Apache-2.0/BSD-licensed dependencies and vendored/hand-authored data anywhere in the library or build; no copyleft. Common Kotlin only — no wrapping platform-native/JVM-only parsers.

## Technical Decisions

- **Already-shipped API shape (context for 4.3, not to be changed):** `UserAgentParser(vararg packs: UserAgentTypePack): (String) -> UserAgentInfo` and `UserAgentGenerator(vararg packs: UserAgentTypePack): (UserAgentInfo) -> String`, both stateless factories with no mutable shared state and no caching beyond the compiled read-only rule tables.
- **`UserAgentTypePack` contract (already shipped, reuse as-is):** a plain class (not an `object`, so referencing the class doesn't force-initialize every pack under Kotlin/JS's module-level init gating) with `id: String`, `detect: (String) -> UserAgentInfo` (returns a partially-populated `UserAgentInfo` covering only the fields this pack owns), and `applyToGenerate: (UserAgentInfo) -> String? = { null }` (returns a string segment to contribute, or `null` for "nothing to add"). The constructor is public so consumer-authored and JS/npm-authored custom packs can build one too. `UserAgentParser` merges every passed pack's `detect` result field-by-field, first non-null wins in pack-argument order (first-key-wins for the `custom` map); a pack whose `detect`/`applyToGenerate` throws is caught and treated as contributing nothing, so one broken pack can't take down a composed call.
- **`UserAgentInfo` (already shipped, gains no new fields for 4.3):** already has `bot: Component?` and `aiAgent: Component?` fields (both always `null` today, since no pack populates them yet) alongside `browser`/`engine`/`os`/`device`, plus `custom: Map<String, Component> = emptyMap()` as the extension point for data outside the named fields. Story 4.3 only needs to populate `bot`/`aiAgent` — the data model itself needs no further change.
- **Built-in packs are one-per-file** in `commonMain` (e.g. `UserAgentBrowserTypePack.kt`, `UserAgentEngineTypePack.kt`, `UserAgentOsTypePack.kt`, `UserAgentDeviceTypePack.kt`), each an independent top-level `val` — this is what makes per-pack JS tree-shaking work, since each file gets its own Kotlin/JS lazy-init gate. `UserAgentBotTypes`/`UserAgentAIAgentTypes` must follow the same one-file-per-pack pattern, and `UserAgentAllTypesPack.kt` must be updated to include both.
- **Rule data organization (AD-1, already amended):** compiled tables are one per type pack (`browserRules`, `engineRules`, `osRules`, `deviceRules`, and now `botRules`, `aiAgentRules`), each its own top-level `commonMain` value generated at build time (never a runtime resource). Within a table, first match wins, file order preserved. The browser/OS/device tables use uap-core's regex + replacement-template format and codegen pipeline; `botRules`/`aiAgentRules` have no uap-core source to vendor from, so they're a small hand-seeded starter list (e.g. Googlebot/Bingbot/curl-class bots; GPTBot/ClaudeBot/PerplexityBot-class AI agents), explicitly not exhaustive and meant to be extended by consumers via custom packs rather than by growing this list indefinitely.
- **Reused unchanged:** the regex-matching/template-substitution engine, the existing JS-dialect regex escape normalization in codegen (a safe no-op on JVM/Native), the shared `commonTest` corpus mechanism, and the Maven Central + npm publish pipelines from Epic 3. Story 4.3 does not require a new major-version republish by itself unless the maintainer chooses to bundle it with one — no requirement in the epic mandates an immediate republish.
- **Naming/package:** root package `site.lempert.useragent`, no platform suffixes on shared names. `NOTICE`/Apache-2.0 attribution requirements (AD-6) apply only to the uap-core-vendored categories; the hand-authored bot/AI-agent data has no such attribution requirement but must still be permissively sourced.

## Cross-Story Dependencies

- Story 4.3 builds directly on Story 4.1's `UserAgentTypePack` contract and the `bot`/`aiAgent` fields already present in `UserAgentInfo` — both were put in place specifically so this follow-up needs no further data-model or API-shape change.
- Story 4.3 follows the same one-file-per-pack and per-category-rule-table patterns Stories 4.1/4.2 established; it does not touch the factory functions, the four existing built-in packs, or the Maven Central/npm publish pipelines except to add the two new packs (and update `UserAgentAllTypes` to include them).
- No dependency in the other direction: Stories 4.1/4.2 are complete and do not require anything from Story 4.3.
