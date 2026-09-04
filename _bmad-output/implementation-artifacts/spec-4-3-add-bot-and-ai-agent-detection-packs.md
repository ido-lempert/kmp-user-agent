---
title: 'Add Bot and AI-Agent Detection Packs'
type: 'feature'
created: '2026-09-04'
status: 'done'
review_loop_iteration: 0
baseline_commit: '2dc40c089b3ade09fcce3bb956a5621f65b1e7ec'
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-4-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-4-1-redesign-parse-generate-as-composable-type-packs-validated-via-npm-node.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** `UserAgentInfo.bot`/`.aiAgent` and the `UserAgentTypePack` extensibility contract shipped in Story 4.1, but always return `null` — no built-in pack populates them. Consumers can't detect well-known bots or AI/LLM crawlers without writing their own pack from scratch.

**Approach:** Add `UserAgentBotTypes` and `UserAgentAIAgentTypes`, one file each (matching `UserAgentBrowserTypePack.kt`'s pattern), each a small hand-authored table of (token/regex → name) entries — no codegen needed, this list is short enough to write directly as Kotlin. Every entry is sourced from the bot/crawler operator's own public documentation, listed verbatim below; none is copied from any third-party commercial bot-detection dataset.

**Bot pack entries** — name: UA token/pattern:
Googlebot: `Googlebot/([0-9.]+)` · Bingbot: `bingbot/([0-9.]+)` · DuckDuckBot: `DuckDuckBot/([0-9.]+)` · YandexBot: `YandexBot(?:/([0-9.]+))?` · Baiduspider: `Baiduspider/([0-9.]+)` · Bytespider: `Bytespider` (no version) · UptimeRobot: `UptimeRobot/([0-9.]+)` · Pingdom: `Pingdom\.com_bot_version_([0-9.]+)` · StatusCake: `StatusCake` (no version) · facebookexternalhit: `facebookexternalhit/([0-9.]+)` · Slackbot: `Slackbot-LinkExpanding` (no version) · PostmanRuntime: `PostmanRuntime/([0-9.]+)`

**AI-agent pack entries:**
GPTBot: `GPTBot/([0-9.]+)` · ChatGPT-User: `ChatGPT-User/([0-9.]+)` · OAI-SearchBot: `OAI-SearchBot/([0-9.]+)` · PerplexityBot: `PerplexityBot/([0-9.]+)` · Perplexity-User: `Perplexity-User/([0-9.]+)` · ClaudeBot: `ClaudeBot` (no version) · Claude-User: `Claude-User` (no version) · Claude-SearchBot: `Claude-SearchBot` (no version) · CCBot: `CCBot/([0-9.]+)`

## Boundaries & Constraints

**Always:** Every rule entry's source is one of the operators listed above (Google, Microsoft, DuckDuckGo, Baidu, ByteDance, UptimeRobot, Pingdom, StatusCake, Meta, Slack, Postman, OpenAI, Perplexity, Anthropic, Common Crawl) — never DataDome or any other third-party bot-detection vendor's compiled data. First-match-wins within each pack's own table, same evaluation shape as the existing browser/OS/device packs. `detect` never throws (matches every other pack). Both new packs follow the exact `UserAgentTypePack` shape from `UserAgentTypePack.kt` — no contract change.

**Ask First:** None expected.

**Never:** Do not add `Google-Extended` (no distinct UA string per Google's own docs — undetectable this way) or `anthropic-ai`/`Claude-Web` (deprecated). Do not introduce a new codegen task/vendored YAML file for this — the list is short enough to be plain Kotlin, matching this story's "small, non-exhaustive starter list" scope (AD-1). Do not touch `UserAgentBrowserTypes`/`EngineTypes`/`OsTypes`/`DeviceTypes`/`UserAgentParser`/`UserAgentGenerator`'s existing logic — only add to `UserAgentAllTypes`'s pack list and add the two new files.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Known bot, versioned | UA containing `Googlebot/2.1` parsed with `UserAgentBotTypes` | `bot = Component("Googlebot", "2.1")` | N/A |
| Known bot, no documented version | UA containing `ClaudeBot` parsed with `UserAgentAIAgentTypes` | `aiAgent = Component("ClaudeBot", null)` | N/A |
| Unknown UA | A browser UA string parsed with both new packs | `bot`/`aiAgent` both `null` | N/A |
| All types | `UserAgentParser(UserAgentAllTypes)(ua)` for a bot/AI-agent UA | `bot`/`aiAgent` populated alongside existing fields | N/A |
| Generate round-trip | `UserAgentGenerator(UserAgentBotTypes)(UserAgentInfo(bot = Component("Googlebot","2.1")))` then parsed back | Recovers the same `bot` value | N/A |

</frozen-after-approval>

## Code Map

- `library/src/commonMain/kotlin/site/lempert/useragent/UserAgentBrowserTypePack.kt` — the exact pattern to mirror: one file, a `private data class` rule shape, a `private val` compiled/lazy rule list, an `internal fun detectX` used by both `UserAgentXTypes` and (indirectly, via `UserAgentInfo` merge) `UserAgentAllTypes`, and `@JsExport val UserAgentXTypes = UserAgentTypePack(id = ..., detect = ..., applyToGenerate = ...)`.
- `library/src/commonMain/kotlin/site/lempert/useragent/UserAgentTypePack.kt` — the contract both new packs implement; no change needed here.
- `library/src/commonMain/kotlin/site/lempert/useragent/UserAgentAllTypesPack.kt` — add `UserAgentBotTypes`/`UserAgentAIAgentTypes` to whatever list/composition `UserAgentAllTypes` currently builds from (read this file first to match its exact current shape).
- `library/src/commonMain/kotlin/site/lempert/useragent/UserAgentInfo.kt` — `bot: Component?`/`aiAgent: Component?` already exist (Story 4.1); no change needed, just populate them.
- `library/src/commonTest/kotlin/site/lempert/useragent/UserAgentParserTest.kt`/`UserAgentGeneratorTest.kt` — existing test-helper pattern (`private val parse = UserAgentParser(UserAgentAllTypes)`, per-pack subset tests) to extend with bot/AI-agent cases.
- New files to create: `UserAgentBotTypePack.kt`, `UserAgentAIAgentTypePack.kt` (naming to match the sibling files' `UserAgentXTypePack.kt` convention).

## Tasks & Acceptance

**Execution:**
- [x] `library/src/commonMain/kotlin/site/lempert/useragent/UserAgentBotTypePack.kt` (new) — 12-entry bot table + `UserAgentBotTypes` pack. Tracks each rule's literal `token`/`versionSeparator` separately from its display `name` (e.g. Bingbot's real token is lowercase `bingbot`, Pingdom has no `/` separator) so `applyToGenerate` round-trips correctly.
- [x] `library/src/commonMain/kotlin/site/lempert/useragent/UserAgentAIAgentTypePack.kt` (new) — 9-entry AI-agent table + `UserAgentAIAgentTypes` pack, same structure; doc comment notes the excluded `Google-Extended`/`anthropic-ai`/`Claude-Web`
- [x] `library/src/commonMain/kotlin/site/lempert/useragent/UserAgentAllTypesPack.kt` — `detect` now also calls `detectBot`/`detectAiAgent`
- [x] `library/src/commonTest/kotlin/site/lempert/useragent/UserAgentParserTest.kt` — table-driven test covering all 21 tokens, Yandex-without-version edge case, unknown-UA case, `UserAgentAllTypes` composition case
- [x] `library/src/commonTest/kotlin/site/lempert/useragent/UserAgentGeneratorTest.kt` — round-trip cases (versioned Googlebot/GPTBot, versionless StatusCake/ClaudeBot), plus unrecognized-bot-name-contributes-nothing
- [x] `README.md` — added both packs to the built-in list with entries and sourcing note; fixed the stale "always null" note on `bot`/`aiAgent`
- [x] `CHANGELOG.md` — new `0.3.0` entry, additive/non-breaking (no version bump applied to `build.gradle.kts` — that's a separate republish decision, not required by this story)

**Acceptance Criteria:**
- [x] Given `UserAgentParser(UserAgentBotTypes)` and `UserAgentParser(UserAgentAIAgentTypes)`, when each of the 21 documented UA tokens above is parsed, then the corresponding field is populated as `Component(name, version-or-null)` matching the table in Intent
- [x] Given `./gradlew :library:allTests`, when run after this story, then all tests pass on every configured target (jvm, iosArm64/iosSimulatorArm64, android host test, js)
- [x] Given `UserAgentAllTypes`, when a bot/AI-agent UA is parsed with it, then `bot`/`aiAgent` populate alongside `browser`/`engine`/`os`/`device` as applicable

## Design Notes

Version-optional entries (`ClaudeBot`, `Claude-User`, `Claude-SearchBot`, `Bytespider`, `StatusCake`, `Slackbot-LinkExpanding`) have no documented version token — their regex has no capture group, so `version` is always `null` for those, same null-when-absent convention as every other pack.

`applyToGenerate` for both packs: render the minimal string that round-trips through `detect` — e.g. `"${name}/${version}"` when a version exists, else just `"${name}"`. Don't fabricate a full `"Mozilla/5.0 (compatible; ...; +https://...)"` wrapper with an invented URL; these are typically the actual literal substrings the operator's real crawler sends. Keep the shared match/replace helper (`applyGroupReplacement`, `groupValueOrNull` from `UserAgentRuleMatching.kt`) in mind — reuse rather than reinvent if it fits this simpler (single capture group, no template) case; a plain `Regex.find` + `groupValues.getOrNull(1)` is likely simpler here than the uap-core-oriented template substitution machinery.

## Verification

**Commands:**
- `./gradlew :library:allTests` -- expected: all pass on every target
- Manually construct one UA string per table entry (21 total) and confirm each parses to the expected `Component` — either as the commonTest cases above or a scratch check during development

**Manual checks (if no CLI):**
- Confirm no new dependency on `vendor/uap-core` or any new vendored data file was introduced for this story — it's meant to stay plain hand-authored Kotlin.

## Spec Change Log

- **2026-09-04, review pass 1:** Review (blind-hunter + verification-gap layers, corroborating independently) found `UserAgentAllTypes`'s generate direction silently dropped `bot`/`aiAgent` identity — its `applyToGenerate` never called the new `generateBotSegment`/`generateAiAgentSegment`, contradicting its own doc comment and the README's documented round-trip idiom. Patched: `generateFullUserAgentString` now tries bot then AI-agent segments after the existing browser/engine/OS chain; added round-trip tests through `UserAgentAllTypes` for both. Also patched (test-coverage gaps, no design change): a generate test for YandexBot's only `OPTIONAL`-version-mode rule, null-required-version-returns-null tests for both packs, and a missing `unrecognizedAiAgentNameContributesNothingToGenerate` test for symmetry with the existing bot-pack test. Two findings deferred (not caused by a design flaw, low-priority, matching an already-accepted pattern) — Slackbot's second undocumented UA form, and generate-side version-string sanitization — see `deferred-work.md`. No frozen-section change needed; entirely a `patch` classification.

## Suggested Review Order

**The verified data tables (what to trust, what's excluded)**

- Bot rules, each annotated with its real literal token vs. display name — the actual data this story exists to add.
  [`UserAgentBotTypePack.kt:51`](../../library/src/commonMain/kotlin/site/lempert/useragent/UserAgentBotTypePack.kt#L51)

- AI-agent rules, including the doc comment explaining why `Google-Extended`/`anthropic-ai`/`Claude-Web` are deliberately absent.
  [`UserAgentAIAgentTypePack.kt:1`](../../library/src/commonMain/kotlin/site/lempert/useragent/UserAgentAIAgentTypePack.kt#L1)

**The review-pass-1 fix (not in the original scope, but the most important correction)**

- `UserAgentAllTypes`'s generate chain now includes bot/AI-agent segments — previously silently dropped them.
  [`UserAgentGenerator.kt`](../../library/src/commonMain/kotlin/site/lempert/useragent/UserAgentGenerator.kt) (`generateFullUserAgentString`)

- The round-trip tests that would have caught it, added on review.
  [`UserAgentGeneratorTest.kt:621`](../../library/src/commonTest/kotlin/site/lempert/useragent/UserAgentGeneratorTest.kt#L621)

**Integration and detection (peripheral)**

- `UserAgentAllTypes.detect` now calls both new detectors.
  [`UserAgentAllTypesPack.kt:22`](../../library/src/commonMain/kotlin/site/lempert/useragent/UserAgentAllTypesPack.kt#L22)

- Table-driven test covering all 21 documented tokens.
  [`UserAgentParserTest.kt:457`](../../library/src/commonTest/kotlin/site/lempert/useragent/UserAgentParserTest.kt#L457)
