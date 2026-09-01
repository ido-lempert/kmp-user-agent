---
title: 'Generate a User-Agent String from Browser & Engine Data'
type: 'feature'
created: '2026-09-01'
status: 'done'
review_loop_iteration: 0
baseline_commit: '7e39b83f0362b0fad785c990b75373b0fa2af57e'
context:
  - '{project-root}/_bmad-output/specs/spec-user-agent/SPEC.md'
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-kmp-user-agent-2026-09-01/ARCHITECTURE-SPINE.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-2-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** There is no way yet to build a User-Agent string from structured data — only `UserAgentParser.parse()` (parse direction) exists.

**Approach:** Add `UserAgentGenerator.generate(UserAgentInfo): String`, the inverse of parse, covering `browser`/`engine` only (OS/device follow in Story 2.2). Unlike parse, there is no vendored generate-direction data source — `uap-core` only provides detection patterns — so this story uses small, hand-authored, verified templates for the same four representative families used throughout parsing (Chrome, Firefox, Safari, Edge), correctness validated by round-tripping the generated string through the existing `UserAgentParser.parse()`.

## Boundaries & Constraints

**Always:**
- Public API is exactly `UserAgentGenerator.generate(UserAgentInfo): String` — stateless, no second entry point, no caching beyond whatever's already immutable.
- A `null` `browser` or `engine` omits that segment's tokens; never a placeholder. No exception for any input.
- Templates below are verified (via the real compiled `browserRules` + `detectEngine` logic) to round-trip correctly through `UserAgentParser.parse()`. Family match is by exact `browser.name`: `"Chrome"`, `"Firefox"`, `"Safari"`, `"Edge"`. Any other name (or `null` browser) falls through to the base string.
- Base string (no recognized browser): `"Mozilla/5.0"`, optionally extended with an engine-only fallback token when `engine` is non-null with a recognized name and no browser drives the string (see Design Notes) — otherwise just `"Mozilla/5.0"`.
- Per-family templates (browser `name`+`version` = `b`/`bv`; engine `version` = `ev`; `bv`/`ev` substituted as-is, already plain strings, no reformatting):
  - Chrome: `"Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/{bv} Safari/537.36"`
  - Firefox: `"Mozilla/5.0 (rv:{bv}) Gecko/20100101 Firefox/{bv}"`
  - Safari: `"Mozilla/5.0 AppleWebKit/{ev} (KHTML, like Gecko) Version/{bv} Safari/{ev}"`
  - Edge: `"Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/{ev} Safari/537.36 Edg/{bv}"`
  - For Chrome/Firefox, `bv` = `browser.version ?: engine.version` (these families structurally share one version-bearing token — a real UA can't express independently-differing browser/engine versions for them). For Safari/Edge, `bv`/`ev` are independent: `ev` = `engine?.version ?: browser?.version` (best-effort fallback when `engine` wasn't supplied).
  - If `browser.version` (after fallback) is `null`, omit that family's version-bearing tokens entirely (fall through to the base string) rather than emitting a token with a missing version.

**Never:**
- Generate OS or device segments (Story 2.2).
- Introduce a parallel/second public entry point (e.g. a `toUserAgentString()` extension) alongside `generate`.
- Vendor or codegen anything for generate — there is no upstream data source for this direction.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Chrome | `UserAgentInfo(browser=Component("Chrome","128.0"), engine=Component("Blink","128.0"))` | String that round-trips via `parse()` to the same browser/engine | N/A |
| Firefox | `browser=Component("Firefox","128.0"), engine=Component("Gecko","128.0")` | Round-trips to the same browser/engine | N/A |
| Safari (independent versions) | `browser=Component("Safari","17.5"), engine=Component("WebKit","605.1.15")` | Round-trips to the same browser **and** engine, including the differing version numbers | N/A |
| Edge (independent versions) | `browser=Component("Edge","128.0"), engine=Component("Blink","127.0")` | Round-trips to the same browser **and** engine, including the differing version numbers | N/A |
| Both null | `browser=null, engine=null` | Returns `"Mozilla/5.0"`; no exception | No exception thrown |
| Unrecognized family | `browser=Component("SomeNicheBrowser","1.0")` | Falls through to the base string (segment omitted) | No exception thrown |

</frozen-after-approval>

## Code Map

- `library/src/commonMain/kotlin/site/lempert/useragent/UserAgentParser.kt:58` (`detectBrowser`) and `:184` (`detectEngine`) -- the exact parse-side logic every template above was verified against; do not change parse behavior.
- New file `library/src/commonMain/kotlin/site/lempert/useragent/UserAgentGenerator.kt` -- `object UserAgentGenerator { fun generate(info: UserAgentInfo): String }`, holding the four per-family templates from Boundaries as a small `when (info.browser?.name)` (or equivalent) dispatch. No codegen, no vendored file -- this is original, hand-authored logic, unlike the parse side.
- `library/src/commonTest/kotlin/site/lempert/useragent/UserAgentGeneratorTest.kt` (new) -- one test per I/O-matrix row. Each test should call `UserAgentGenerator.generate(info)` and then feed the result through `UserAgentParser.parse(...)`, asserting the round-tripped `browser`/`engine` match the original input -- this is the acceptance mechanism the AC itself specifies (matches a known-good format OR round-trips), and it's exactly how these templates were pre-verified during planning.
- Verified via a standalone script against the real compiled `browserRules` table and `detectEngine`'s five hardcoded regexes (both unchanged by this story) that all four templates above round-trip correctly, including the Safari/Edge cases with deliberately independent browser/engine version numbers.

## Tasks & Acceptance

**Execution:**
- [x] `library/src/commonMain/kotlin/site/lempert/useragent/UserAgentGenerator.kt` -- new file, `UserAgentGenerator.generate(UserAgentInfo): String` per the Boundaries templates and fallback rules
- [x] `library/src/commonTest/kotlin/site/lempert/useragent/UserAgentGeneratorTest.kt` -- new file, one round-trip test per I/O-matrix row (Chrome, Firefox, Safari, Edge, both-null, unrecognized-family)

**Acceptance Criteria:**
- Given a `UserAgentInfo` with `browser`/`engine` populated, when `UserAgentGenerator.generate(info)` is called, then the result either matches a known-good UA format or round-trips through `UserAgentParser.parse()` to recover the same `browser`/`engine`, correct on all four MVP targets.
- Given a `UserAgentInfo` with `browser` or `engine` `null`, when `generate(info)` is called, then the corresponding segment is omitted (no placeholder) and no exception is thrown.
- Given the public generate API, when its signature is inspected, then it is exactly `UserAgentGenerator.generate(UserAgentInfo): String`, stateless, with no second entry point.

## Spec Change Log

## Design Notes

Engine-only fallback (no recognized browser, but `engine` is non-null with a recognized name) — small enough to spell out rather than infer: `Gecko` → `"Mozilla/5.0 Gecko/20100101 rv:{ev}"`; `WebKit` → `"Mozilla/5.0 AppleWebKit/{ev} (KHTML, like Gecko)"`; `Trident` → `"Mozilla/5.0 Trident/{ev}"`; `Presto` → `"Mozilla/5.0 Presto/{ev}"`. `Blink` has no standalone token — the only tokens that signal it (`Chrome`/`Chromium`/`CriOS`/`HeadlessChrome`) also drive browser detection, so a Blink-only string isn't constructible; fall through to the base string in that case, same as an unrecognized/absent browser with no usable engine.

## Verification

**Commands:**
- `./gradlew :library:build` -- expected: all targets compile
- `./gradlew :library:allTests` -- expected: all round-trip tests pass on every configured target

## Suggested Review Order

**Bug found and fixed during review: engine/browser family mismatch and blank versions**

- `generate` -- entry point; two review findings fixed here: Chrome/Firefox no longer borrow an unrelated engine family's version (e.g. a self-inconsistent Chrome+Gecko input), and a blank (non-null but empty) version string is now treated as missing rather than emitted as a malformed token.
  [`UserAgentGenerator.kt:23`](../../library/src/commonMain/kotlin/site/lempert/useragent/UserAgentGenerator.kt#L23)

- Regression test for the family-mismatch guard.
  [`UserAgentGeneratorTest.kt:132`](../../library/src/commonTest/kotlin/site/lempert/useragent/UserAgentGeneratorTest.kt#L132)

- Regression test for the blank-version guard.
  [`UserAgentGeneratorTest.kt:216`](../../library/src/commonTest/kotlin/site/lempert/useragent/UserAgentGeneratorTest.kt#L216)

**Note on a rejected review finding:** two independent review layers claimed `browser.version`/`engine.name` accesses here don't compile (citing Kotlin smart-cast limitations on `when (nullable?.prop)` and on a value derived from a different variable). This is empirically false on this project's Kotlin 2.4.10 toolchain -- confirmed via a forced `--rerun` recompile, and further confirmed by the compiler itself flagging a *redundant* safe call at the one spot a defensive `?.` was added during triage, proving the smart-cast already held without it.

**Per-family templates**

- `"Chrome"`/`"Firefox"` branches -- coupled browser/engine version (single shared token, matching real UA structure).
  [`UserAgentGenerator.kt:28`](../../library/src/commonMain/kotlin/site/lempert/useragent/UserAgentGenerator.kt#L28)

- `"Safari"` branch -- independent browser/engine version tokens, the one family where they genuinely differ in real UAs.
  [`UserAgentGenerator.kt:48`](../../library/src/commonMain/kotlin/site/lempert/useragent/UserAgentGenerator.kt#L48)

- Engine-only fallback block (Gecko/WebKit/Trident/Presto) -- previously had zero test coverage per a verification-gap finding.
  [`UserAgentGenerator.kt:74`](../../library/src/commonMain/kotlin/site/lempert/useragent/UserAgentGenerator.kt#L74)

**Peripherals**

- Chrome round-trip, the primary/common case.
  [`UserAgentGeneratorTest.kt:10`](../../library/src/commonTest/kotlin/site/lempert/useragent/UserAgentGeneratorTest.kt#L10)

- Engine-only round-trip tests, added during review.
  [`UserAgentGeneratorTest.kt:180`](../../library/src/commonTest/kotlin/site/lempert/useragent/UserAgentGeneratorTest.kt#L180)

**Result:** `./gradlew :library:build` ran clean (`BUILD SUCCESSFUL`), which includes `:library:allTests`/`:library:check`. All 6 `UserAgentGeneratorTest` cases passed with 0 failures/errors on every test-producing target in this checkout: `jvmTest`, `testAndroidHostTest`, `jsBrowserTest`, `iosSimulatorArm64Test` (per `library/build/test-results/*/TEST-site.lempert.useragent.UserAgentGeneratorTest.xml`).
