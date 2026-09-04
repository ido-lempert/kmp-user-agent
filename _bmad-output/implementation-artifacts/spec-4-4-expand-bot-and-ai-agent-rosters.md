---
title: 'Expand Bot and AI-Agent Rosters'
type: 'feature'
created: '2026-09-04'
status: 'done'
review_loop_iteration: 0
baseline_commit: 'e80ddd0329beec726efda4c7798fe7e90056e2ae'
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-4-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-4-3-add-bot-and-ai-agent-detection-packs.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Story 4.3's `UserAgentBotTypes`/`UserAgentAIAgentTypes` cover only 21 entries. The human wants broader coverage, using DataDome's bot directory as a name checklist only (their site is 403-blocked to automated fetching and their data is proprietary/unusable as a source — same discipline already established in Story 4.3).

**Approach:** Extend the existing `botRules`/`aiAgentRules` tables (in `UserAgentBotTypePack.kt`/`UserAgentAIAgentTypePack.kt`) with 28 new entries below, same `BotRule`/`AiAgentRule` shape, same first-match-wins evaluation, no new files/codegen. Each entry is independently sourced — never from DataDome.

**New bot-pack entries** (name: token — citation):
AhrefsBot: `AhrefsBot/7.0` — ahrefs.com/robot · SemrushBot: `SemrushBot/7~bl` (version varies) — semrush.com/bot · AdsBot-Google: `AdsBot-Google` — google.com/adsbot.html · Mediapartners-Google: `Mediapartners-Google/2.1` — google.com/bot.html · GoogleOther: `GoogleOther` — developers.google.com · YandexAdditionalBot: `YandexAdditionalBot` — yandex.com/support/webmaster · MJ12bot: `MJ12bot/v1.4.8` — mj12bot.com · DotBot: `DotBot/2.0` — dotbot.com/about · Twitterbot: `Twitterbot` (no version) · LinkedInBot: `LinkedInBot` (no version) · Discordbot: `Discordbot` (no version) · PetalBot: `PetalBot` (no version, Huawei) · Diffbot: `Diffbot` (no version) · ImagesiftBot: `ImagesiftBot` (no version)

**New AI-agent-pack entries:**
Amazonbot: `Amazonbot/0.1` — developer.amazon.com/amazonbot · Meta-ExternalAgent: real token lowercase `meta-externalagent` — developers.facebook.com · Meta-ExternalFetcher: `meta-externalfetcher` — same · MistralAI-User: `MistralAI-User/1.0` — docs.mistral.ai/robots · DuckAssistBot: `DuckAssistBot/1.0` — duckduckgo.com/duckassistbot · FacebookBot, Google-CloudVertexBot, DeepSeekBot, Kimi-User, Timpibot, omgili/omgilibot, cohere-ai/cohere-training-data-crawler, YouBot, Amzn-User — corroborated via the `ai.robots.txt` MIT-licensed registry + operator identity confirmation; exact token to be pinned down per-entry during implementation from that operator's own docs where findable, dropped if not confidently verifiable

## Boundaries & Constraints

**Always:** Every new entry's source is the operator's own documentation, or (for the last corroborated tier) independently confirmed via multiple sources with a known operator — never DataDome. Same `BotRule`/`AiAgentRule` shape and first-match-wins table order as Story 4.3; new entries append after the existing 21 (don't reorder existing rules — could change match precedence for pathological UAs containing multiple tokens). `UserAgentAllTypes`'s generate direction (fixed in Story 4.3's review pass) must keep rendering bot/AI-agent segments correctly as the tables grow — verify, don't just assume.

**Ask First:** If, during implementation, a listed "corroborated" entry's actual token can't be confidently pinned down from the operator's own docs, drop it rather than guess (per Story 4.3's established discipline) — no need to halt and ask, just drop and note it in the Design Notes/Spec Change Log.

**Never:** Do not bulk-import from `ai.robots.txt` or any other external registry in this story — only the specific entries listed above. Do not add `Applebot-Extended`/`Google-Extended` (robots.txt opt-out tokens, never appear in a real UA string). Do not introduce a codegen task/vendored data file — still plain hand-authored Kotlin, matching Story 4.3.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| New bot entry | UA containing `AhrefsBot/7.0` parsed with `UserAgentBotTypes` | `bot = Component("AhrefsBot", "7.0")` | N/A |
| New AI-agent entry, unusual casing | UA containing `meta-externalagent` parsed with `UserAgentAIAgentTypes` | `aiAgent = Component("Meta-ExternalAgent", null-or-version)` — display name stays the documented mixed-case form even though the real token is lowercase | N/A |
| All-types generate | `UserAgentGenerator(UserAgentAllTypes)` given an `Info` with one of the new entries | Still round-trips correctly (regression check on Story 4.3's review-pass fix) | N/A |
| Existing 21 entries | Full Story 4.3 test suite | Still passes unchanged — no existing entry's behavior regresses | N/A |

</frozen-after-approval>

## Code Map

- `library/src/commonMain/kotlin/site/lempert/useragent/UserAgentBotTypePack.kt` (118 lines) — `botRules: List<BotRule>` at line ~51; append new entries after the existing 12, keep `BotRule`'s `name`/`regex`/`token`/`versionSeparator`/`versionMode` shape (see `Pingdom`'s entry for a non-default `token`/`versionSeparator`/`versionMode` example, `Bytespider`'s for `NONE`).
- `library/src/commonMain/kotlin/site/lempert/useragent/UserAgentAIAgentTypePack.kt` (94 lines) — same pattern, `aiAgentRules` list, append after existing 9.
- `library/src/commonMain/kotlin/site/lempert/useragent/UserAgentGenerator.kt` — `generateFullUserAgentString`'s bot/AI-agent fallback branches (Story 4.3's review-pass fix) — no code change expected, but the regression check in the I/O matrix should exercise this path with a new entry.
- `library/src/commonTest/kotlin/site/lempert/useragent/UserAgentParserTest.kt` — Story 4.3's table-driven test (search `Story 4.3`) is the pattern to extend with the 28 new entries.
- `library/src/commonTest/kotlin/site/lempert/useragent/UserAgentGeneratorTest.kt` — Story 4.3's round-trip tests; add a couple of spot-check round trips for new entries (not necessarily all 28 — one per `versionMode` variant touched by new entries is enough, matching Story 4.3's own test density).
- `README.md` — the built-in-packs list currently names all 21 Story 4.3 entries explicitly; decide during implementation whether to list all 49 or switch to a count + "see source" pointer (49 inline entries risks bloating the README — use judgment).

## Tasks & Acceptance

**Execution:**
- [x] `UserAgentBotTypePack.kt` — append 14 new bot entries per Intent's list
- [x] `UserAgentAIAgentTypePack.kt` — append 5 confirmed + 6 (of 9) corroborated-tier AI-agent entries (3 dropped — see Spec Change Log)
- [x] `UserAgentParserTest.kt` — extend the Story 4.3 table-driven test with the new entries actually added
- [x] `UserAgentGeneratorTest.kt` — add spot-check round-trip tests, including at least one through `UserAgentAllTypes` (the exact regression class Story 4.3's review caught)
- [x] `README.md` — update the built-in-packs section for the new count/entries
- [x] `CHANGELOG.md` — new entry (additive, non-breaking)

**Acceptance Criteria:**
- [x] Given each new entry actually added, when its documented UA token is parsed with the corresponding pack, then the field populates as `Component(name, version-or-null)` matching its citation
- [x] Given `./gradlew :library:allTests`, when run after this story, then all tests pass on every configured target, including the full pre-existing Story 4.3 suite unchanged
- [x] Given `UserAgentGenerator(UserAgentAllTypes)` with a new entry's `Component` set, when generated then re-parsed, then the identity round-trips correctly

## Design Notes

For the "corroborated" AI-agent tier (FacebookBot, Google-CloudVertexBot, DeepSeekBot, Kimi-User, Timpibot, omgili/omgilibot, cohere-ai/cohere-training-data-crawler, YouBot, Amzn-User), spend a bounded amount of verification effort per entry — if the operator's own docs aren't findable quickly, it's fine to drop rather than sink disproportionate time into one entry. Record which were dropped and why in this spec's Spec Change Log, not silently.

## Verification

**Commands:**
- `./gradlew :library:allTests` -- expected: all pass on every target, including all Story 4.3 cases unchanged

**Manual checks (if no CLI):**
- Confirm no entry's token/citation was sourced from DataDome, even indirectly — every citation in Intent traces to the operator's own docs or an explicitly-named third-party corroboration (`ai.robots.txt`).

## Suggested Review Order

**Sourcing decisions (what to trust, what was dropped)**

- The Spec Change Log above — every added/dropped entry's reasoning, most load-bearing thing to read first.
  [`spec-4-4-expand-bot-and-ai-agent-rosters.md`](spec-4-4-expand-bot-and-ai-agent-rosters.md)

**The two expanded tables**

- 14 new bot entries, appended after Story 4.3's 12.
  [`UserAgentBotTypePack.kt:55`](../../library/src/commonMain/kotlin/site/lempert/useragent/UserAgentBotTypePack.kt#L55)

- 11 new AI-agent entries, appended after Story 4.3's 9; note the new `token` field on `AiAgentRule` (mirrors `BotRule`) needed for `Meta-ExternalAgent`/`Meta-ExternalFetcher`'s lowercase real tokens.
  [`UserAgentAIAgentTypePack.kt:59`](../../library/src/commonMain/kotlin/site/lempert/useragent/UserAgentAIAgentTypePack.kt#L59)

**Regression coverage (peripheral)**

- The `UserAgentAllTypes` round-trip regression checks — the exact class of bug Story 4.3's review caught, re-verified here as the tables grow.
  [`UserAgentGeneratorTest.kt:752`](../../library/src/commonTest/kotlin/site/lempert/useragent/UserAgentGeneratorTest.kt#L752)

## Spec Change Log

- **2026-09-04, implementation:** All 14 bot-pack entries from Intent were added as listed (their tokens were already frozen in Intent, so no independent sourcing was needed for those). For the 9 "corroborated" AI-agent-tier candidates, verification effort was spent per entry against each operator's own current documentation (bounded, per Design Notes):
  - **Added (6):** `Google-CloudVertexBot` (Google's own crawler-overview docs confirm the literal substring, no version), `Kimi-User` (Moonshot AI's own `kimi.com/policies/kimi-crawlers` page documents `Kimi-User/1.0`), `Amzn-User` (Amazon's own `developer.amazon.com/amazonbot` page documents `Amzn-User/0.1`), `Timpibot` (no first-party Timpi docs page found quickly, but the `ai.robots.txt` registry's operator attribution plus an independent WebmasterWorld server-log sighting of `Timpibot/0.9` corroborate the token), `omgili` (Webz.io's own blog post documents `omgili/0.5`; the paired legacy alias `omgilibot` was *not* added — `ai.robots.txt` itself flags it as "legacy … unknown if still used", so it isn't confidently a currently-active token), `YouBot` (You.com's own `you.com/docs/youbot` page documents `YouBot/1.0`).
  - **Dropped (3):** `FacebookBot` — Meta's own crawler-docs page (`developers.facebook.com/docs/sharing/webmasters/web-crawlers/`, the same page that confirms `meta-externalagent`/`meta-externalfetcher`) does not mention a distinct `FacebookBot` token at all, only `facebookexternalhit` (already in the Story 4.3 table) and the two Meta-External* crawlers; couldn't confidently pin down a real, currently-documented `FacebookBot` string separate from those. `DeepSeekBot` — no first-party DeepSeek documentation or `robots.txt` entry could be found (DeepSeek's own `robots.txt` only has a wildcard `Allow: /`, no named user-agent); third-party sources conflict on whether DeepSeek publishes any crawler UA at all. `cohere-ai`/`cohere-training-data-crawler` — Cohere's own official page (`docs.cohere.com/docs/cohere-web-crawlers`) explicitly states "We do not use Cohere bots or user agents for the purpose of crawling or scraping web content to train generative AI foundation models at this time" and lists its crawler table as empty (`N/A`), directly contradicting these tokens as currently active/real — dropped per the "drop rather than guess" discipline.

  Net: 14 bot + 11 AI-agent = 25 new entries (of the 28 discussed in Intent), all traced to first-party operator documentation except `Timpibot`/`omgili`/`YouBot`'s corroborated tier as noted above. No frozen-section change — classified as `patch` (dropping entries per Intent's own "Ask First" clause, not a design change).

- **2026-09-04, review pass 1:** Review (blind-hunter/edge-case-hunter/verification-gap, all three run) found no blocking defects — verification-gap explicitly confirmed generate-side test density matches Story 4.3's own established convention (not a new gap), and one edge-case-hunter finding (claiming `Mediapartners-Google` used `OPTIONAL` version mode) was checked against the actual code and found factually wrong (it's `REQUIRED`, matching Intent's citation) — discounted. Patched five small, real issues: a malformed-parentheses test fixture for `Kimi-User`, two test-naming nits (a stray "s" in `mj12botsNonDefaultTokenAndSeparatorGenerateRoundTrips`, and dropping "New" from two `UserAgentAllTypes` round-trip test names — renamed to an "Added" disambiguator since Story 4.3 already owns the un-suffixed names), a README pointer line overstating what the source files' KDoc actually contains, an incomplete category summary missing `PostmanRuntime`/`Diffbot`, and a CHANGELOG heading not stating its target version. Deferred two real-but-non-blocking findings to `deferred-work.md` (several `NONE`-version-mode entries whose test fixtures hint at an unused, unverified version number; two Google token pairs that collapse distinct crawler variants) — both need operator-doc re-verification, not guessable, matching this story's own "don't guess" discipline.
