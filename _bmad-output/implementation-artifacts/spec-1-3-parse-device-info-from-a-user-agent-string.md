---
title: 'Parse Device Info from a User-Agent String'
type: 'feature'
created: '2026-09-01'
status: 'done'
review_loop_iteration: 0
baseline_commit: '6a8fafca4aefb59da32f370888f8ba66bf4c2d62'
context:
  - '{project-root}/_bmad-output/specs/spec-user-agent/SPEC.md'
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-kmp-user-agent-2026-09-01/ARCHITECTURE-SPINE.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** `UserAgentInfo.device` is always `null` — there is no way yet to extract device brand/model/name from a User-Agent string.

**Approach:** Extend the existing codegen and parser once more: vendor uap-core's `device_parsers` rules (already present in the vendored `regexes.yaml`, unused until now) into the same generated rule table, and add device detection to `UserAgentParser`, populating `UserAgentInfo.device`. Unlike browser/OS, this section's replacement fields never rely on positional capture-group defaults (confirmed against all 633 rules), and ~65 rules require case-insensitive matching via a new `regex_flag` field this story must read and apply for the first time.

## Boundaries & Constraints

**Always:**
- Extend the existing `generateUserAgentRules` task/generated file rather than adding a second codegen task — same "three ordered pattern lists, one generated table" approach as Stories 1.1/1.2.
- `device_parsers` rules evaluate first-match-wins in file order, same as browser/OS.
- Map uap-core's `device_replacement`/`brand_replacement`/`model_replacement` to `Device.name`/`Device.brand`/`Device.model` respectively; each field is computed independently as `replacement?.let { applyGroupReplacement(it, match) }` — `null` when the replacement field is absent, with **no positional-group fallback** (unlike browser's `family`/OS's `name`, which default to group 1 when absent). This is a deliberate divergence from the Story 1.1/1.2 mechanic, confirmed correct against real data: 0 of 633 `device_parsers` rules omit all three replacement fields, and rules that omit one field always intend that field to be `null` (e.g. a rule giving only `device_replacement` never also relies on a default for `brand`/`model`).
- Read the `regex_flag` field (only value seen in this data: `'i'`) from each `device_parsers` rule and compile its regex with `RegexOption.IGNORE_CASE` when present — this is the first vendored section to use it (confirmed 0 uses in `user_agent_parsers`/`os_parsers`, 65 in `device_parsers`).
- Reuse `normalizePatternForAllTargets` and `applyGroupReplacement` as-is; no changes to browser/OS detection behavior or existing tests.

**Never:**
- Add a positional-group default for any device field (`name`/`brand`/`model`) — the vendored data doesn't need it and adding one risks masking a real parsing bug behind a plausible-looking fallback value.
- Change browser/engine/OS detection behavior.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Known iPhone | UA containing an `iPhone;`-style token | `device` = `Device(brand="Apple", model="iPhone", name="iPhone")` (or the specific matching rule's actual output — verify against the compiled table) | N/A |
| Known Android device | UA containing `Pixel 6` in the bare-metal Android device position | `device` = `Device(brand="Google", model=<matched>, name=<matched>)` (verify exact matched rule) | N/A |
| Case-insensitive match | A UA hitting one of the ~65 `regex_flag: 'i'` rules with non-canonical casing | `device` populated the same as the canonically-cased input would produce | N/A |
| Desktop browser (no device signal) | Plain desktop Chrome/Firefox UA (no mobile/embedded device token) | `device` is `null` | No exception thrown |
| Unrecognized/empty | Garbage or `""` | `device` is `null` | No exception thrown |

</frozen-after-approval>

## Code Map

- `library/build.gradle.kts:27` (`UapCoreCodegen` object) -- add a `DeviceRule(pattern, regexFlag, deviceReplacement, brandReplacement, modelReplacement)` data class and a `parseDeviceParsers` function mirroring `parseUserAgentParsers`/`parseOsParsers`, reading the `device_parsers:` section (starts at `regexes.yaml:2203`), fields `regex_flag`/`device_replacement`/`brand_replacement`/`model_replacement`. Per the Story 1.2 deferred-work note, consider (not required) factoring the now-three-times-duplicated section-parsing loop into a shared helper.
- `library/build.gradle.kts` (`generateSource`) -- extend to accept `deviceRules: List<DeviceRule>` and emit a third `internal class DeviceRule(...)` / `internal val deviceRules: List<DeviceRule>` block in the same generated file, alongside `browserRules`/`osRules`.
- `library/build.gradle.kts` (`generateUserAgentRules` task `doLast`) -- call `parseDeviceParsers` on the same already-read `yamlText`, pass to `generateSource`.
- `library/src/commonMain/kotlin/site/lempert/useragent/UserAgentParser.kt` -- add `CompiledDeviceRule` (regex compiled with `RegexOption.IGNORE_CASE` when `regexFlag == "i"`), `compiledDeviceRules`, and `detectDevice`, structurally parallel to `detectBrowser`/`detectOs` but **without** the positional-group-default step (see Boundaries). Wire `device = detectDevice(userAgent)` into `parse()` (`:18`); update the class KDoc, which currently states `device` is always `null`.
- `library/vendor/uap-core/regexes.yaml:2203` -- `device_parsers:` section, 633 rules, already vendored (no re-vendoring needed).
- Verified concrete rule matches for the I/O matrix (confirmed via direct inspection, not exhaustive trace through all 633 rules in file order -- implementer must confirm actual first-match against the compiled table, adjusting the test UA if an earlier rule intercepts): a bare `iPhone;` token → a rule near `regexes.yaml:5730` (`regex: '(iPhone)(?:;| Simulator;)'`, `device_replacement: '$1'`, `brand_replacement: 'Apple'`, `model_replacement: '$1'`); `; Pixel 6) AppleWebKit` → `regexes.yaml:3160` (`regex: '; {0,2}([g|G]oogle)? (Pixel.{0,200}?)(?: Build|\) AppleWebKit)'`, `device_replacement: '$2'`, `brand_replacement: 'Google'`, `model_replacement: '$2'`).
- `_bmad-output/implementation-artifacts/spec-1-2-parse-os-from-a-user-agent-string.md` -- Suggested Review Order and Design Notes show the exact browser/OS detection pattern this story extends a third time.

## Tasks & Acceptance

**Execution:**
- [x] `library/build.gradle.kts` -- add `DeviceRule` + `parseDeviceParsers` (reads `regex_flag` too) -- vendors the `device_parsers` section
- [x] `library/build.gradle.kts` -- extend `generateSource`/the codegen task to also emit `deviceRules` into the same generated file
- [x] `library/src/commonMain/kotlin/site/lempert/useragent/UserAgentParser.kt` -- add `detectDevice` (regex compiled with `RegexOption.IGNORE_CASE` per-rule based on `regexFlag`, each of name/brand/model resolved independently with no positional default), wire into `parse()`, update KDoc
- [x] `library/src/commonTest/kotlin/site/lempert/useragent/UserAgentParserTest.kt` -- verify the exact first-matching rule for each I/O-matrix UA against the compiled table (adjust UA/expected values if a different rule matches first), add those cases plus a case-insensitive-match case and an unrecognized/empty case for `device`

**Acceptance Criteria:**
- Given the existing parse API from Stories 1.1/1.2, when `UserAgentParser.parse(rawUserAgentString)` is called with a UA string identifying a known device, then `UserAgentInfo.device` is populated as `Device(brand, model, name)` matching the known device, correct on all four MVP targets.
- Given a User-Agent string with no recognizable device pattern (e.g. a desktop browser UA), when `UserAgentParser.parse(rawUserAgentString)` is called, then `device` is `null` and no exception is thrown.
- Given the vendored `regexes.yaml`'s `device_parsers` section, when the Gradle codegen task runs, then it generates the `device_parsers` rule table in `commonMain` following the same codegen approach as Stories 1.1/1.2, including `regex_flag`-driven case-insensitive compilation.

## Spec Change Log

- Implementation note (not a change to frozen Intent): `UserAgentParserTest.safariDesktop` previously asserted `assertNull(info.device)` (a Story 1.1/1.2 placeholder, since device was always null before this story). With device detection now implemented, that UA's `Macintosh`/`Mac OS X` tokens correctly match `device_parsers`'s catch-all `'Mac OS'` rule (regexes.yaml ~line 6263), so the assertion was updated to `Device(brand = "Apple", model = "Mac", name = "Mac")` -- this is `detectDevice` working as intended, not a regression.
- Implementation note: one vendored `device_parsers` pattern (regexes.yaml ~line 5850, an HTC rule using `HTC[ _\-;]?`) fails to compile under `java.util.regex` (JVM/Android) after `normalizePatternForAllTargets`'s backslash-stripping turns `\-` into a bare `-` inside a character class, forming an invalid character range (`_-;`). This is a latent interaction in the reused-as-is `normalizePatternForAllTargets` (safe outside character classes, not inside them) surfaced for the first time by `device_parsers` data; per this story's boundaries that function was reused unmodified. `detectDevice`'s existing try/catch-and-skip compilation (same pattern as browser/OS) silently drops this one rule at runtime rather than crashing; it is not exercised by this story's acceptance criteria. Flagged here for a future story to consider fixing the codegen's character-class handling.

## Design Notes

`device_parsers` differs structurally from `user_agent_parsers`/`os_parsers`: empirically confirmed (via a script over all 633 rules) that 0 rules omit all three replacement fields, and no rule relies on a positional capture-group default for `name`/`brand`/`model` — each field is either given an explicit literal/`$N`-template or is genuinely absent (meaning `null`, not "derive from group N"). Do not port the browser/OS `?: groupValueOrNull(match, N)` fallback pattern here; it would fabricate values uap-core's own data never intends.

`regex_flag: 'i'` is the only flag value present anywhere in the vendored file (confirmed), and it's scoped to `device_parsers` only (0 occurrences in the other two sections) — so this is purely additive to the codegen/rule-compilation path and touches nothing in browser/OS detection.

## Verification

**Commands:**
- `./gradlew :library:build` -- expected: all targets compile; codegen emits `browserRules`, `osRules`, and `deviceRules`
- `./gradlew :library:allTests` -- expected: `commonTest` passes on every configured target, including new `device` assertions

## Suggested Review Order

**Normalization bug found and fixed during review**

- `normalizePatternForAllTargets` unconditionally stripped `\-` even inside a character class, where it's already valid syntax and stripping it can silently widen the class into an unintended range (e.g. `[ \-\.]` → `[ -\.]`, an ascending range from space through `.`). This was live and shipping (the vendored BIRD device rule), not just a future risk -- fixed by tracking character-class context so `\-` is only stripped outside one.
  [`build.gradle.kts:224`](../../library/build.gradle.kts#L224)

- Regression test pinning the fix's exact boundary behavior against the real vendored BIRD rule.
  [`UserAgentParserTest.kt:265`](../../library/src/commonTest/kotlin/site/lempert/useragent/UserAgentParserTest.kt#L265)

- The same root cause was also the sole reason one vendored HTC rule failed to compile (an invalid descending character range once `\-` was stripped); fixing the root cause resolves that too, so the device rule-table test no longer needs a compile-failure carve-out.
  [`UserAgentParserTest.kt:293`](../../library/src/commonTest/kotlin/site/lempert/useragent/UserAgentParserTest.kt#L293)

**Device detection**

- `detectDevice`'s deliberate divergence from browser/OS: no positional-group fallback for name/brand/model.
  [`UserAgentParser.kt:202`](../../library/src/commonMain/kotlin/site/lempert/useragent/UserAgentParser.kt#L202)

- Regression test (added during review, per a verification-gap finding) proving that divergence against a real rule that omits `device_replacement`.
  [`UserAgentParserTest.kt:247`](../../library/src/commonTest/kotlin/site/lempert/useragent/UserAgentParserTest.kt#L247)

- `regex_flag`-driven `RegexOption.IGNORE_CASE` compilation, the first use of per-rule regex options in this codebase.
  [`UserAgentParser.kt:184`](../../library/src/commonMain/kotlin/site/lempert/useragent/UserAgentParser.kt#L184)

**Codegen (device_parsers vendoring)**

- `DeviceRule` + `parseDeviceParsers`, the third near-identical section parser (browser/OS/device) -- a refactor candidate noted in `deferred-work.md`.
  [`build.gradle.kts:44`](../../library/build.gradle.kts#L44), [`build.gradle.kts:164`](../../library/build.gradle.kts#L164)

- `generateSource` extended to emit all three rule tables from one codegen pass, unchanged in approach since Story 1.1.
  [`build.gradle.kts:285`](../../library/build.gradle.kts#L285)

**Peripherals**

- iPhone/Pixel device cases per the I/O matrix, each verified against the actual compiled table rather than assumed.
  [`UserAgentParserTest.kt:193`](../../library/src/commonTest/kotlin/site/lempert/useragent/UserAgentParserTest.kt#L193)
