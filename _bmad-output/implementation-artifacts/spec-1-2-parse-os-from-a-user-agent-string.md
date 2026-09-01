---
title: 'Parse OS from a User-Agent String'
type: 'feature'
created: '2026-09-01'
status: 'done'
review_loop_iteration: 0
baseline_commit: '78d5db91130b2123f1970a71ab2e07b59bc33b06'
context:
  - '{project-root}/_bmad-output/specs/spec-user-agent/SPEC.md'
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-kmp-user-agent-2026-09-01/ARCHITECTURE-SPINE.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** `UserAgentParser.parse()` (Story 1.1) already returns `browser`/`engine`, but `UserAgentInfo.os` is always `null` — there is no way yet to extract OS name/version from a User-Agent string.

**Approach:** Extend the existing `library` module's codegen and parser: vendor uap-core's `os_parsers` rules (already present in the vendored `regexes.yaml`, unused until now) into the same generated rule table, and add OS detection to `UserAgentParser` using the same matching/template-substitution mechanic already proven for browser/engine, populating `UserAgentInfo.os`.

## Boundaries & Constraints

**Always:**
- Extend the existing `generateUserAgentRules` task/generated file rather than adding a second codegen task — one vendored file, one codegen pass, per AD-1's "three ordered pattern lists" in one generated table.
- `os_parsers` rules follow the same first-match-wins, file-order evaluation and the same replacement mechanic already implemented for browser rules (literal or `$N`-templated `os_replacement`/`os_v1_replacement`/`os_v2_replacement`/`os_v3_replacement`, defaulting to positional capture groups 1–4 when a given replacement is absent).
- `os` is `Component(name, version)` — same type as `browser`/`engine`; version is `v1.v2.v3` with any missing parts dropped, joined with `.` (matching Story 1.1's version-join convention), never a sentinel.
- No `os_parsers` rule in the current vendored data uses `regex_flag` — confirmed zero occurrences in this section — so no case-insensitivity handling is required by this story (unlike `device_parsers`, which does use it and is Story 1.3's concern).
- `library`'s production source sets stay stdlib-only; the same defensive `parse()`-never-throws guarantee extends to OS detection.

**Never:**
- Implement device detection (Story 1.3) — `device` stays a `null` placeholder.
- Change `browser`/`engine` detection behavior or their existing test outcomes.
- Add `regex_flag` handling in this story — no current `os_parsers` rule needs it (defer to whichever story handles rules that do).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Windows | UA containing `Windows NT 10.0` | `os` = `Component("Windows", "10")` | N/A |
| macOS | UA containing `Mac OS X 10_15_7`-style token | `os` = `Component("Mac OS X", "10.15.7")` | N/A |
| iOS | UA containing `CPU iPhone OS 17_5` | `os` = `Component("iOS", "17.5")` | N/A |
| Android | UA containing `Android 12` | `os` = `Component("Android", "12")` | N/A |
| Linux (no version) | UA containing bare `Linux` (e.g. `X11; Linux x86_64`) | `os` = `Component("Linux", null)` | N/A |
| Unrecognized/empty | Garbage or `""` | `os` is `null` | No exception thrown |

</frozen-after-approval>

## Code Map

- `library/build.gradle.kts:27` (`UapCoreCodegen` object) -- add an `OsRule(pattern, osReplacement, v1Replacement, v2Replacement, v3Replacement)` data class and a `parseOsParsers` function mirroring `parseUserAgentParsers` (line 50), reading the `os_parsers:` section (starts at `regexes.yaml:1356`) instead of `user_agent_parsers:`, with fields `os_replacement`/`os_v1_replacement`/`os_v2_replacement`/`os_v3_replacement`.
- `library/build.gradle.kts:106` (`normalizePatternForAllTargets`) -- reuse as-is for OS patterns; already `regexes.yaml`-format-agnostic.
- `library/build.gradle.kts:155` (`generateSource`) -- extend to also emit an `internal class OsRule(...)` and `internal val osRules: List<OsRule> = listOf(...)` block in the same generated `UserAgentRuleTable.kt`, alongside the existing `browserRules`.
- `library/build.gradle.kts:181` (`generateUserAgentRules` task) -- call the new `parseOsParsers` in the same `doLast`, no new task/file needed.
- `library/src/commonMain/kotlin/site/lempert/useragent/UserAgentParser.kt:57` (`detectBrowser`) and `:79` (`applyGroupReplacement`) -- the matching + `$N`-template-substitution mechanic generalizes directly to a 4-field OS rule (name, v1, v2, v3); reuse `applyGroupReplacement`/`groupValueOrNull` as-is, add an analogous `detectOs` alongside `detectBrowser`. Whether to share a common helper between the two or keep them structurally parallel (as `detectBrowser`/`detectEngine` already are) is an implementation judgment call, not mandated.
- `library/src/commonMain/kotlin/site/lempert/useragent/UserAgentParser.kt:17` (`parse`) -- change `os = null` to `os = detectOs(userAgent)`.
- `library/vendor/uap-core/regexes.yaml:1356` -- `os_parsers:` section, 204 rules, already vendored (no re-vendoring needed; same pinned commit as Story 1.1).
- `library/src/commonTest/kotlin/site/lempert/useragent/UserAgentParserTest.kt` -- existing Chrome/Firefox/Safari/Edge tests currently assert `assertNull(info.os)`; these must be updated once `os` is populated for those same UAs (verify against the actual OS token each UA already contains, e.g. the Chrome test UA contains `Windows NT 10.0`).
- Verified concrete rule matches for the I/O matrix (file-order confirmed, no earlier `os_parsers` rule intercepts): `Windows NT 10.0` → `regexes.yaml:1586` (`os_replacement: 'Windows'`, `os_v1_replacement: '10'`); `Mac OS X\s.{1,50}\s(\d+).(\d+).(\d+)` → `regexes.yaml:1636` (explicit `$1`/`$2`/`$3`); iOS → `regexes.yaml:1697` (`os_replacement: 'iOS'`, versions default from groups 2/3/4); generic Android → `regexes.yaml:1466` (`(Android)[ \-/](\d+)(?:\.(\d+)|)...`, no replacement fields, pure positional defaults); generic Linux → `regexes.yaml:2172` (`(Linux)(?:[ /](\d+)\.(\d+)(?:\.(\d+)|)|)`).
- `_bmad-output/implementation-artifacts/deferred-work.md` -- has a note (from Story 1.1 review) about `regex_flag`; correct it in passing if convenient -- the 65 `regex_flag` occurrences are all in `device_parsers` (Story 1.3), not `os_parsers` (confirmed 0 occurrences here), so this story is unaffected.

## Tasks & Acceptance

**Execution:**
- [x] `library/build.gradle.kts` -- add `OsRule` data class + `parseOsParsers` (mirroring the existing `BrowserRule`/`parseUserAgentParsers` pattern) -- vendors the `os_parsers` section
- [x] `library/build.gradle.kts` -- extend `generateSource`/the `generateUserAgentRules` task's `doLast` to also emit `osRules` into the same generated file -- one codegen pass per AD-1
- [x] `library/src/commonMain/kotlin/site/lempert/useragent/UserAgentParser.kt` -- add `detectOs`, wire into `parse()` -- populates `UserAgentInfo.os`
- [x] `library/src/commonTest/kotlin/site/lempert/useragent/UserAgentParserTest.kt` -- update existing browser tests' `assertNull(info.os)` to the correct OS `Component` for each UA, and add cases per the I/O matrix (Windows/macOS/iOS/Android/Linux-no-version + unrecognized/empty) -- validates identical behavior across all four targets

**Acceptance Criteria:**
- Given the existing parse API from Story 1.1, when `UserAgentParser.parse(rawUserAgentString)` is called with a UA string identifying a known OS (Windows, macOS, iOS, Android, or Linux), then `UserAgentInfo.os` is populated as `Component(name, version)` matching the known OS, correct on all four MVP targets.
- Given a User-Agent string with no recognizable OS pattern, when `UserAgentParser.parse(rawUserAgentString)` is called, then `os` is `null` and no exception is thrown.
- Given the vendored `regexes.yaml`'s `os_parsers` section, when the Gradle codegen task runs, then it generates the `os_parsers` rule table in `commonMain` following the same codegen approach and JS-dialect normalization as Story 1.1.

## Spec Change Log

## Design Notes

`os_parsers` rules use the same positional-default / `$N`-template-override mechanic as `user_agent_parsers`, just with one extra optional field (`v3`) and a name field called `os_replacement` instead of `family_replacement`. Confirmed against real vendored rules: when a rule's regex has no name-capturing group (e.g. the Mac OS X rule at `regexes.yaml:1636`), its author always supplies explicit `$1`/`$2`/`$3` templates to compensate for the shifted group numbering — the codegen/parser never needs to special-case this, since the vendored data already encodes the correct group references wherever the shift happens.

## Verification

**Commands:**
- `./gradlew :library:build` -- expected: all targets compile; codegen emits both `browserRules` and `osRules`
- `./gradlew :library:allTests` -- expected: `commonTest` passes on every configured target, including updated `os` assertions on the existing browser tests

## Suggested Review Order

**OS detection**

- `parse()` now wires in OS detection alongside browser/engine.
  [`UserAgentParser.kt:18`](../../library/src/commonMain/kotlin/site/lempert/useragent/UserAgentParser.kt#L18)

- `detectOs` mirrors `detectBrowser`'s matching/first-match-wins mechanic, extended to a 4-field (name, v1, v2, v3) rule shape.
  [`UserAgentParser.kt:146`](../../library/src/commonMain/kotlin/site/lempert/useragent/UserAgentParser.kt#L146)

- Fixed during review: `v1`/`v2`/`v3` (and, for consistency, browser's `v1`/`v2`) now route through `applyGroupReplacement` instead of being used as raw strings -- 4-5 real vendored `os_parsers` rules use a `$N` template in these fields (e.g. the bare-token Windows XP/Vista/CE fallback, and several CFNetwork/Darwin iOS rules), and were previously emitting the literal text `"$1"` instead of the substituted version.
  [`UserAgentParser.kt:80`](../../library/src/commonMain/kotlin/site/lempert/useragent/UserAgentParser.kt#L80)

- Also fixed during review: the digit-parsing in `applyGroupReplacement` no longer throws `NumberFormatException` on a hypothetical oversized `$N` group index -- falls back to emitting the literal text instead, preserving the "`parse()` never throws" contract.
  [`UserAgentParser.kt:86`](../../library/src/commonMain/kotlin/site/lempert/useragent/UserAgentParser.kt#L86)

**Codegen (os_parsers vendoring)**

- New `OsRule` data class and `parseOsParsers`, mirroring Story 1.1's `BrowserRule`/`parseUserAgentParsers` pattern for the `os_parsers:` section.
  [`build.gradle.kts:36`](../../library/build.gradle.kts#L36)

- `generateSource` extended to accept and emit both rule lists into the same generated file -- one codegen task/pass per AD-1, unchanged from Story 1.1.
  [`build.gradle.kts:214`](../../library/build.gradle.kts#L214)

**Peripherals**

- Windows/macOS/iOS/Android/Linux-no-version cases per the I/O matrix.
  [`UserAgentParserTest.kt:67`](../../library/src/commonTest/kotlin/site/lempert/useragent/UserAgentParserTest.kt#L67)

- Added during review: regression test for the `$N`-template OS-version bug found above.
  [`UserAgentParserTest.kt:98`](../../library/src/commonTest/kotlin/site/lempert/useragent/UserAgentParserTest.kt#L98)
