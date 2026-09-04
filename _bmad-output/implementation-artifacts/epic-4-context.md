# Epic 4 Context: Composable, Tree-Shakeable Type-Pack API

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Replace the fixed-shape `UserAgentParser.parse(String)` / `UserAgentGenerator.generate(UserAgentInfo)` singleton API (shipped and already published in Epics 1–3) with two factory functions composed from pluggable "type packs," so a JS/npm consumer who only wants browser detection isn't forced to load OS/device/bot/AI-agent rule tables, and so any consumer can add their own detection categories without forking the library. This was triggered by hands-on validation of the published npm package in a clean Node.js project, which surfaced two gaps in the old API: no bundle-size control and no extensibility. Stories 4.1–4.3 have already redesigned and shipped the factory-function/type-pack API (parity-verified on all four targets, republished as `0.2.0`) and added real bot/AI-agent detection with a 21-entry starter roster (published as an additive `0.3.0`-worthy change). Story 4.4 was added afterward, at the human's explicit direction, to broaden that roster beyond the initial 21 entries. The regex-matching engine, uap-core vendoring/codegen mechanics, and the Maven Central + npm publish pipelines from Epics 1–3 are reused as-is throughout.

## Stories

- Story 4.1: Redesign parse/generate as composable type packs, validated via npm/Node — **done**
- Story 4.2: Validate type-pack API parity across all four targets and republish — **done**
- Story 4.3: Add Bot and AI-Agent Detection Packs — **done**
- Story 4.4: Expand Bot and AI-Agent Rosters — **done**

## Requirements & Constraints

- The public API is a pair of factory functions — one for parse, one for generate — each taking a variadic list of type packs and returning the callable parse/generate function. This is already shipped; Story 4.4 does not change the factory signatures, the `UserAgentTypePack` contract, or `UserAgentInfo`'s shape.
- `UserAgentBotTypes`/`UserAgentAIAgentTypes` already exist (Story 4.3) with a 21-entry combined starter roster (12 bot, 9 AI-agent) and are already wired into `UserAgentAllTypes`. Story 4.4 only adds entries to these two existing tables — it does not add new packs.
- New entries must follow the same hand-authored, per-operator-cited sourcing discipline as Story 4.3: sourced from that bot/crawler operator's own public documentation, or — only where no first-party page exists — from multiple independent, clearly-attributed corroborating sources. Never sourced from any third-party commercial bot-detection dataset. This is a license-compatibility constraint (only MIT/Apache-2.0/BSD-licensed data anywhere in the library; no copyleft), since there is no uap-core section to vendor from for these two categories.
- DataDome's site may be used only as a checklist of well-known bot/AI-agent *names* to consider adding — never as the source of the actual detection tokens/regexes for any entry (same discipline Story 4.3 already applied to other third-party sources).
- Parsing a UA string matching a documented bot/crawler with the corresponding pack included must populate `bot` or `aiAgent` as `Component(name, version)` (version `null` when the operator documents no version token); a UA string matching nothing must leave the field `null` and never throw.
- Same Kotlin call site must compile and return identical results on all four MVP targets (Android, iOS, JVM, JS).
- The shared `commonTest` corpus (kotlin.test, all four targets, CI gate) must be extended with cases covering the newly added entries and continue passing identically everywhere.
- Only MIT/Apache-2.0/BSD-licensed dependencies and vendored/hand-authored data anywhere in the library or build; no copyleft. Common Kotlin only — no wrapping platform-native/JVM-only parsers.

## Technical Decisions

- **Already-shipped API shape (context, not to be changed):** `UserAgentParser(vararg packs: UserAgentTypePack): (String) -> UserAgentInfo` and `UserAgentGenerator(vararg packs: UserAgentTypePack): (UserAgentInfo) -> String`, both stateless factories with no mutable shared state and no caching beyond the compiled read-only rule tables.
- **`UserAgentTypePack` contract (already shipped, reuse as-is):** a plain class with `id: String`, `detect: (String) -> UserAgentInfo`, and `applyToGenerate: (UserAgentInfo) -> String? = { null }`. `UserAgentParser` merges every passed pack's `detect` result field-by-field, first non-null wins in pack-argument order; a pack whose `detect`/`applyToGenerate` throws is caught and treated as contributing nothing.
- **Real shipped bot/AI-agent table shape (`UserAgentBotTypePack.kt`, mirrored by `UserAgentAIAgentTypePack.kt`):** a private `enum class ...VersionMode { NONE, OPTIONAL, REQUIRED }` and a private `data class ...Rule(name: String, regex: Regex, token: String, versionSeparator: String = "/", versionMode: ...VersionMode)`, held in a private `List<...Rule>` evaluated first-match-wins in table order via a plain `Regex.find(...)` + `groupValues.getOrNull(1)` (not the uap-core template-substitution machinery, since every rule has at most one capture group). `token`/`versionSeparator` capture the bot's real literal UA substring (which can differ in case/shape from the display `name`, e.g. Bingbot's real token is lowercase `bingbot`) so `applyToGenerate` renders a string that round-trips back through `detect`. Story 4.4's new entries must follow this exact same rule shape and append to the existing `botRules`/`aiAgentRules` lists — not introduce a parallel structure.
- **One-file-per-pack pattern (already established, unchanged):** built-in packs each live in their own `commonMain` file (`UserAgentBotTypePack.kt`, `UserAgentAIAgentTypePack.kt`, etc.) as an independent top-level `val`, which is what makes per-pack JS tree-shaking work. `UserAgentAllTypesPack.kt` already calls both `detectBot`/`detectAiAgent` on the parse side and both generate segments on the generate side (a review-pass gap from Story 4.3 that is now fixed) — Story 4.4 does not need to touch this wiring, only the rule tables themselves.
- **Explicitly excluded entries (from Story 4.3, still binding unless a new first-party source contradicts it):** `Google-Extended` (no distinct UA string per Google's own docs), `anthropic-ai`/`Claude-Web` (deprecated tokens). Story 4.4 should not silently reverse these exclusions without a documented reason.
- **No codegen for this category:** unlike browser/OS/device, bot/AI-agent tables are plain hand-authored Kotlin lists, not vendored/generated from an external YAML file — this stays true for Story 4.4's additions too (no new vendored data file, no new codegen task).
- **Reused unchanged:** the regex-matching/template-substitution engine, the JS-dialect regex escape normalization in codegen (irrelevant to this hand-authored category but present elsewhere), the shared `commonTest` corpus mechanism, and the Maven Central + npm publish pipelines from Epic 3. No requirement in the epic mandates an immediate republish for Story 4.4 by itself.
- **Naming/package:** root package `site.lempert.useragent`, no platform suffixes on shared names.

## Cross-Story Dependencies

- Story 4.4 builds directly on Story 4.3's `botRules`/`aiAgentRules` tables and rule shape (`BotRule`/`AiAgentRule` with `name`/`regex`/`token`/`versionSeparator`/`versionMode`) — it extends those existing lists rather than introducing new packs, files, or contracts.
- Story 4.4 depends on Story 4.1's `UserAgentTypePack` contract and `UserAgentInfo.bot`/`.aiAgent` fields only transitively, through Story 4.3; it does not touch the factory functions or the four original browser/engine/os/device packs.
- No dependency in the other direction: Stories 4.1–4.3 are complete and do not require anything from Story 4.4.
