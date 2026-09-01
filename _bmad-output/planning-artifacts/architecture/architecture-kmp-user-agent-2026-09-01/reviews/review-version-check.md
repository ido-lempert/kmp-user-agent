# Adversarial Review — Version/Research-Provenance Lens

**Target:** ARCHITECTURE-SPINE.md (kmp-user-agent, 2026-09-01)
**Lens:** Were committed technical/version claims actually web-researched, are named technologies real and fit-for-purpose, and is the "write regex once in commonMain, run unmodified everywhere" bet actually true?
**Method:** Live web search/fetch (Sept 2026) against kotlinlang.org, JetBrains Kotlin GitHub, Gradle Plugin Portal, Maven Central/Sonatype, vanniktech's CHANGELOG.md, Kotlin/npm-publish repo, YouTrack, and a direct `curl`+`grep`/Python analysis of the actual vendored `uap-core/regexes.yaml` (not a summarized description of it).

## Verdict

Directionally researched but not rigorously verified: two of five Stack claims have wrong supporting evidence (dates/characterization) even though the top-line version numbers happen to be right, and the spine's single biggest architectural bet — "no `expect`/`actual` split, patterns run unmodified across JVM/JS/Native" — is **empirically false today** for a non-trivial slice of the actual vendored ruleset, for a completely different reason than the one the spine flags.

---

## Finding 1 (CRITICAL — should be promoted from "Deferred" to an AD): Kotlin/JS's `Regex` hardcodes the Unicode `"u"` flag, and ~11% of uap-core's real patterns use escapes that are illegal under it

The spine's Deferred section names the risk as "uap-core regex dialect (PCRE-ish, sometimes with named groups/lookaheads)... doesn't change the shape of the contract, only which patterns need hand-adaptation." I verified both halves of that sentence directly against the actual vendored source and the actual Kotlin/JS stdlib source, and both are wrong in ways that matter:

**a) uap-core's `regexes.yaml` does not, in fact, contain named groups, lookaheads/lookbehinds, Unicode property escapes, POSIX classes, atomic groups, or Java-only anchors.** I downloaded the live file (`github.com/ua-parser/uap-core/master/regexes.yaml`, 6266 lines, all three sections `user_agent_parsers`/`os_parsers`/`device_parsers`, 1274 patterns) and grepped it directly:
- `(?P<...>` / `(?<name>...)` named groups: **0**
- `(?<=`/`(?<!` lookbehind, `(?=`/`(?!` lookahead: **0**
- `\p{...}` Unicode property escapes: **0**
- POSIX classes `[[:alpha:]]`, atomic groups `(?>`, `\Q...\E`, Java-only anchors `\A`/`\z`/`\Z`/`\G`: **0**
- In-pattern backreferences (`\1` inside the *pattern*, as opposed to `$1` in the replacement template): **0**
- Case-insensitivity is applied out-of-band via a `regex_flag: 'i'` field (65 occurrences) — this maps cleanly to `RegexOption.IGNORE_CASE`, which is genuinely safe on all three Kotlin targets.

So the specific risk the spine names does not actually manifest in the current snapshot. This reads as a generic, training-data-shaped prior about "PCRE-style regex dialects" rather than something checked against the real file — exactly the failure mode this review lens exists to catch.

**b) The risk that *does* manifest, and that the spine never mentions, is worse and is JS-target-specific.** I pulled the live Kotlin/JS stdlib source directly (`github.com/JetBrains/kotlin/blob/master/libraries/stdlib/js/src/kotlin/text/regex.kt`, master branch):

```kotlin
private val nativePattern: RegExp = RegExp(pattern, options.toFlags("gu"))
private fun initStickyPattern(): RegExp =
    nativeStickyPattern ?: RegExp(pattern, options.toFlags("yu")).also { ... }
```

Every `RegExp` Kotlin/JS ever constructs is unconditionally built with the `u` (Unicode) flag baked into the flag string (`"gu"`, `"yu"`, or bare `"u"` in `replaceFirst`) — there is no code path that omits it. Per the ECMAScript spec, under the `u` flag an `IdentityEscape` (backslash before a non-metacharacter) *outside a character class* is a `SyntaxError`, not a silently-tolerated identity escape the way it is in Java's `Pattern` or (per public reports) Kotlin/Native's from-scratch engine.

I then grepped the real vendored file for exactly this pattern and, using a small Python scan to exclude occurrences safely inside `[...]` character classes, found:

- `\-` outside a character class: **142 occurrences**, e.g. `(Obigo)\-Browser`, `(SEMC\-Browser)/(\d+)\.(\d+)`, `(IBAK\-[^;/]*)`, `(F\-\d[^;/]{1,100}?)`, `(N\-\d[^;/]{1,100}?)` — several of these are model-number patterns for real, still-relevant device families (Sony/HTC/Huawei/LG/Fujitsu/Samsung handsets).
- `\!` outside a class: 1 occurrence.
- `\ ` (escaped space) outside a class: several occurrences.

Any one of these, compiled as `Regex(pattern)` on the JS target, throws `SyntaxError: Invalid regular expression: /.../: Invalid escape` — at `Regex` construction time, not at match time. If the generated rule table is a top-level `val` list (the natural codegen shape implied by AD-1), a single bad pattern can fail JS module initialization for the whole table, not just mis-detect one browser.

**Why this belongs in an AD, not Deferred:** AD-1 currently states rule data is mechanically "converted into `commonMain` Kotlin source by a Gradle code-gen task" and the Design Paradigm section asserts "no `expect`/`actual` split exists for the matching engine itself." Given the above, the codegen step cannot be a lossless/mechanical transliteration — it must include an escape-normalization pass (e.g., strip backslashes before non-metacharacters outside character classes) to be JS-safe at all. That normalization is fortunately semantically neutral on JVM and Native too (they already treat `\-` and `-` outside a class identically), so a single universal pass can preserve "write once" — but that is a substantive addition to AD-1's Rule text, not an "implementation detail to hand-adapt later." As written, AD-1 is not achievable as stated without this normalization step, which makes it an architecture-shape issue, not a deferred implementation risk. Recommend either amending AD-1 explicitly ("codegen normalizes escape sequences for cross-target `RegExp`/`Pattern` compatibility") or adding a new AD.

Secondary, lower-priority regex-portability facts worth recording even though they don't bite the current ruleset:
- Kotlin/Native does not support named capture groups at all — confirmed open issue [KT-41890](https://youtrack.jetbrains.com/issue/KT-41890) ("Support named capture groups in Regex on Native"); a pattern with `(?<name>...)` either fails to parse or throws `UnsupportedOperationException` on group-name lookup. Irrelevant today since uap-core uses zero named groups, but should be a standing constraint on future vendored-snapshot refreshes (Deferred item "snapshot refresh cadence" should say "must re-run the escape/feature-compatibility scan on every refresh," not just "no process decided yet").
- Lookbehind assertions are JVM-safe, Native-uncertain, and JS-support-version-dependent — moot today (0 occurrences) for the same reason.

---

## Finding 2 (Medium): `com.vanniktech.maven.publish` 0.37.0's cited release date is wrong — right version, fabricated-looking provenance

The memlog states: *"verified latest 0.37.0 (released Jan 2026)."* I pulled the plugin's own `CHANGELOG.md` from GitHub directly:

| Version | Actual release date |
|---|---|
| 0.37.0 | **2026-06-21** |
| 0.36.0 | 2026-01-13 |
| 0.35.0 | 2025-11-11 |
| 0.34.0 | 2025-07-13 |

0.37.0 shipped **June 21, 2026**, not January 2026 — January 2026 is when **0.36.0** shipped. The version number and its Sonatype Central Portal support are correctly identified, and 0.37.0 does appear to still be the latest as of Sept 1, 2026 (no versions listed after it; gaps of 2–5 months between releases are typical for this project, so a ~2.3-month silence since June is unremarkable). But the specific evidentiary claim attached to "verified" — the release date — does not check out. This is the signature of a plausible-sounding number pattern-matched from training data and stamped with a "verified" label, rather than a citation actually read from the changelog. Recommend re-verifying against the live changelog/Maven Central page before treating any date in this spine as load-bearing, and dropping the incorrect date from the memlog/spine (the version number alone is sufficient and doesn't need a wrong date attached).

---

## Finding 3 (Medium): `org.jetbrains.kotlin.npm-publish`'s "too new to bind" framing conflates a new package coordinate with a new/immature project

The spine defers this plugin with: *"debuted May 2026, too new to bind now."* This undersells what's actually going on. I confirmed via the plugin's own README (`github.com/Kotlin/npm-publish`):

- The plugin is a **takeover**, not a new project: it was developed for years by community maintainer Martynas Petuška under the coordinates `dev.petuska.npm.publish` / `dev.petuska.npm-publish`.
- The last community-owned release was **v3.5.3** (Unlicense).
- JetBrains/the Kotlin team took over "by mutual agreement" and continued the version line under the new `org.jetbrains.kotlin.npm-publish` coordinates: 3.6.0-dev1 → 3.6.0 → **3.7.0 (released May 1, 2026)**.
- The functionality (auto-detecting JS/KMP targets, wiring npm publishing) is multi-year-mature; only the group ID and official backing are new as of May 2026.

"Too new to bind" is the right conclusion for the wrong reason if it's read as "unproven tooling" — the actual risk profile is closer to "a mature, widely-used community plugin recently got adopted and rebranded by JetBrains; the coordinate is young but the code isn't." That's arguably a *stronger* case for binding to it soon (once the rebrand settles) than the spine's framing suggests, and the spine should say so explicitly rather than implying immaturity of the tool itself. This doesn't change today's "hold" decision, but the stated rationale is inaccurate and should be corrected so a future re-evaluation isn't anchored on a wrong premise.

---

## Finding 4 (Low, informational — mostly checks out): Kotlin/KMP Gradle plugin 2.4.10

Confirmed 2.4.10 is the current stable Kotlin/Kotlin Multiplatform Gradle plugin version as of Sept 2026 (`kotlinlang.org/docs/releases.html`, Gradle Plugin Portal). The memlog's "2.4.20 due later this month" is also consistent with public information — 2.4.20-RC2 is out and 2.4.20 is on the published release schedule for September 2026. This is the one version claim in the Stack table that holds up cleanly under direct verification. No action needed beyond noting it as the baseline for how the *other* claims should have been checked.

---

## Finding 5 (Low): `kotlin.text.Regex` "part of the common stdlib on every MVP target" is true but the spine's confidence about behavioral equivalence is unearned

The memlog's evidence for this line is "kotlinlang.org api docs... since Kotlin 1.0" — that only establishes *API surface* availability (the class and its methods exist on all targets), which is true and uncontroversial. It does not establish *behavioral* equivalence of the compiled regex dialect, which is the actual thing the architecture's core bet depends on (see Finding 1). The Design Paradigm section's sentence "so no `expect`/`actual` split exists for the matching engine itself" reads as if this had been checked at the semantic level; it was only checked at the API-surface level. Recommend the spine distinguish these two claims explicitly: "the `Regex` type is available everywhere" (true, low-risk) vs. "an arbitrary vendored regex string compiles and matches identically everywhere" (false today for ~11% of the actual ruleset per Finding 1).

---

## Summary Table

| Claim | Verified? | Actual finding |
|---|---|---|
| Kotlin/KMP Gradle plugin 2.4.10 current stable | Yes | Confirmed current; 2.4.20 correctly flagged as imminent |
| vanniktech maven.publish 0.37.0 is latest | Partially | Version correct; cited release date (Jan 2026) is wrong — actual is June 21, 2026 |
| npm-publish plugin "debuted May 2026, too new" | Partially | Coordinate is new (May 2026); underlying plugin (`dev.petuska.npm.publish`) is a multi-year, v3.5.3-mature community project taken over by JetBrains, not a fresh/unproven tool |
| `kotlin.text.Regex` available on all MVP targets, no expect/actual needed | API surface: yes. Behavioral: no | Kotlin/JS forces the Unicode `u` RegExp flag on every `Regex`; ~142+ patterns in the real uap-core ruleset use escapes illegal under that flag and will throw `SyntaxError` on construction |
| uap-core dialect risk is "named groups/lookaheads... implementation detail, doesn't change contract shape" | No | Zero named groups/lookarounds exist in the real file; the actual, unflagged risk (JS escape strictness) does change AD-1's contract shape (codegen must normalize escapes, not just transliterate) |

## Recommendation

1. Promote the regex-portability item out of Deferred into an explicit AD (or an amendment to AD-1) that commits to a build-time escape-normalization pass in the codegen step, scoped against the real vendored file, not a generic PCRE-dialect worry.
2. Correct the vanniktech release-date citation or drop the date.
3. Rewrite the npm-publish rationale to reflect "new coordinate, mature codebase" rather than implying general immaturity.
4. Add a standing instruction to the "vendored snapshot refresh cadence" Deferred item: every refresh re-runs an escape/feature compatibility scan (the grep-based check used in this review is cheap and can be a CI step), since a future uap-core update could introduce named groups/lookarounds that don't exist today.
