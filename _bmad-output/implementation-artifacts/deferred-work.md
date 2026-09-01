- source_spec: `_bmad-output/implementation-artifacts/spec-1-3-parse-device-info-from-a-user-agent-string.md`
  summary: `regexFlag`/`RegexOption.IGNORE_CASE` handling (added by Story 1.3) only special-cases the literal value `"i"`; any other `regex_flag` value would silently compile with no flags at all rather than failing loudly.
  evidence: Confirmed the only value present anywhere in the currently vendored `regexes.yaml` (all three sections) is `'i'` (65 occurrences, all in `device_parsers`), so this isn't demonstrable as a live bug today — but a future re-vendor introducing a different flag value would silently change matching semantics with no build-time or test signal. (This entry originally tracked "regex_flag isn't implemented at all," which Story 1.3 resolved.)

- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-parse-browser-engine-from-a-user-agent-string.md`
  summary: The hand-rolled YAML parsers in `library/build.gradle.kts` (`UapCoreCodegen.parseUserAgentParsers`/`parseOsParsers`/`parseDeviceParsers`/`extractSingleQuotedValue`) assume every scalar is single-quoted on one line, have no fallback for double-quoted values or an inline comment after the closing quote, and don't sanity-check that a plausible number of rules were parsed (e.g. by comparing the parsed count to the number of `- regex:` lines in the source section).
  evidence: Not demonstrated against the currently vendored file (which is well-formed and yields 434 browser rules, 204 OS rules, and 633 device rules), but a future re-vendor from upstream could introduce a differently formatted line and silently produce a truncated or near-empty rule table with no build-time signal. Story 1.3 completed the third near-identical section-parsing loop (predicted in this note after Story 1.2) — now a good point to extract a shared helper rather than duplicating a fourth time.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-parse-browser-engine-from-a-user-agent-string.md`
  summary: `UserAgentParser` catches `Throwable` in every rule-compiling/matching site (browser, OS, device, engine), which also swallows `OutOfMemoryError`/`StackOverflowError` from pathological regex backtracking instead of surfacing them.
  evidence: Intentional today per the spec's "parse() never throws" requirement, but broad `Throwable` catches mask non-recoverable JVM errors rather than just expected regex-compile/match failures.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-parse-browser-engine-from-a-user-agent-string.md`
  summary: `UserAgentParser.parse()` has no length cap or timeout guard before running an untrusted input string through hundreds of vendored regexes sequentially.
  evidence: Not required by this story's acceptance criteria, but `parse()` is a public API intended to eventually process real HTTP `User-Agent` headers, which are attacker-controlled; uap-core-derived rule sets have a history of catastrophic-backtracking patterns. Story 1.2 added a second full linear scan (`os_parsers`, 204 rules) alongside the browser scan; Story 1.3 added a third (`device_parsers`, 633 rules) — `parse()` now runs well over 1,000 regexes per call in the worst case.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-3-parse-device-info-from-a-user-agent-string.md`
  summary: `detectDevice` can in principle return a non-null `Device(brand = null, model = null, name = null)` if a matching rule's replacement fields all resolve to null (e.g. an optional capture group didn't participate in the match), which is indistinguishable from "no device detected" but is surfaced as a match rather than falling through to the next rule.
  evidence: Not demonstrable against the current vendored data — confirmed via a script over all 633 `device_parsers` rules that none omits all three replacement fields — but worth a defensive `continue` if it's ever observed, mirroring how `detectBrowser`/`detectOs` already `continue` when their (mandatory) name field can't be resolved.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-3-parse-device-info-from-a-user-agent-string.md`
  summary: A malformed `- regex:` line in any of the three vendored sections (one whose value fails single-quote extraction) is silently dropped from the generated rule table with no build-time warning, rather than failing the codegen task loudly.
  evidence: Not demonstrated against the current vendored file (well-formed), but a future re-vendor with an unexpected line format would silently shrink a rule table instead of surfacing the problem.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-parse-browser-engine-from-a-user-agent-string.md`
  summary: Version composition (`listOfNotNull(v1, v2, v3).joinToString(".")` in both `detectBrowser` and `detectOs`) doesn't special-case a null segment followed by a non-null one (e.g. `v1 == null && v2 != null`), which would render the later segment(s) alone as if they were a complete version from position 1.
  evidence: Not demonstrated against any currently vendored rule's actual capture-group ordering in either `user_agent_parsers` or `os_parsers`; theoretical edge case worth a regression test if it's ever observed in practice.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-parse-browser-engine-from-a-user-agent-string.md`
  summary: There is no documented process or script for refreshing the vendored `vendor/uap-core/regexes.yaml` snapshot, beyond recording the pinned commit SHA in `library/NOTICE`.
  evidence: Maintaining a security/compatibility-relevant third-party dataset by hand invites silent drift between the pinned commit and the actual vendored content.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-parse-browser-engine-from-a-user-agent-string.md`
  summary: Consider enabling Kotlin's `explicitApi()` mode on the `library` module to compiler-enforce the intended public API surface (`UserAgentInfo`, `Component`, `Device`, `UserAgentParser`, now also `UserAgentGenerator`) as the module grows in later stories.
  evidence: Not required for Story 1.1, which has a small, already-correct public surface, but becomes more valuable as more stories add code to `commonMain`.

- source_spec: `_bmad-output/implementation-artifacts/spec-2-1-generate-a-user-agent-string-from-browser-engine-data.md`
  summary: `UserAgentGenerator`'s four family templates only recognize exact-match names `"Chrome"`/`"Firefox"`/`"Safari"`/`"Edge"`; real-world `UserAgentParser` output for variant families (e.g. `"Chrome Mobile"`, `"Mobile Safari"`, `"Chromium"`, `"CriOS"`, `"Edge Mobile"`) silently falls through to the bare base string, dropping the browser segment entirely.
  evidence: Consistent with this story's stated v1 scope (the same four representative desktop families used throughout parsing), and not demonstrated as a defect since generate never claims to cover mobile-variant families — but worth widening once real-world round-trip coverage (Story 2.3 and beyond) surfaces which variants matter most.

- source_spec: `_bmad-output/implementation-artifacts/spec-2-1-generate-a-user-agent-string-from-browser-engine-data.md`
  summary: `UserAgentGenerator.generate` does not sanitize `browser.version`/`engine.version` content — a version string containing delimiter or structural characters (spaces, slashes, parentheses, newlines) would splice unintended tokens into the generated UA string or fail to round-trip through `UserAgentParser.parse` (whose version-capturing regexes are digit/dot-only).
  evidence: Not reachable via any real `UserAgentParser.parse()` output (which only ever produces digit/dot version strings), so only exploitable via a directly hand-constructed `UserAgentInfo` with malformed data; the API's own callers are trusted (not attacker-controlled HTTP input the way `parse()`'s UA string is), so this is low priority for v1.

- source_spec: `_bmad-output/implementation-artifacts/spec-2-1-generate-a-user-agent-string-from-browser-engine-data.md`
  summary: Each of `UserAgentGenerator`'s four per-family templates hardcodes its parenthetical/segment structure (e.g. `(KHTML, like Gecko)`, `(rv:$bv)`) with no seam for inserting an OS token into the same parenthetical, which real UA strings for these families normally include.
  evidence: Story 2.2 (OS/device generation) will need to extend or restructure these exact templates rather than being able to purely append new segments — worth keeping in mind when planning that story, not a defect in this one.
