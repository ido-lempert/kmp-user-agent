# Adversarial Review — ARCHITECTURE-SPINE.md (kmp-user-agent)

Reviewer stance: attack the spine. For each finding I construct two units, one level down, that each
follow the letter of every AD, yet produce incompatible builds, or I show an AD's enforcement
mechanism is missing/contradicted, or a Deferred item hides a real divergence risk.

Verdict: **Not yet build-ready.** The core parse/generate split (AD-1/AD-2/AD-5) is the right shape,
but AD-3 contains a literal either/or in its Rule text, AD-1 pins *where* rule data lives but not
*what shape* the generated Kotlin API takes, AD-2 pins *that* there's one model but not its field
types, no package/namespace is fixed anywhere, and AD-5's enforcement depends entirely on a CI setup
that is deferred without acknowledging that dependency. Seven concrete findings below, roughly in
severity order.

---

## F1 — AD-3's Rule literally offers two different public APIs

**Quote:** "The public surface is `UserAgentParser.parse(String): UserAgentInfo` and
`UserAgentGenerator.generate(UserAgentInfo): String` (**or** `UserAgentInfo.toUserAgentString()`)."

**Adversarial pair:** Dev A builds the JVM sample against `UserAgentGenerator.generate(info)`. Dev B,
reading the same AD, builds the JS sample against `info.toUserAgentString()` — an extension/member
function on `UserAgentInfo`, not a call through `UserAgentGenerator` at all. Both samples compile,
both "obey AD-3," but they've bound to two different actual entry points. If the library ships only
one of the two (the common outcome, since nobody was told to ship both), whichever sample picked the
other one fails to build against the released artifact.

**Why the Rule text allows this:** the "or" reads as "pick whichever spelling you like," not as "both
must exist with a defined relationship" or "these are equivalent aliases, X delegates to Y."

**Fix:** Tighten AD-3. Either (a) pick one spelling as the sole public entry point and remove the
other from the Rule text, or (b) if both are intended to ship, state explicitly which one is primary
(e.g. `UserAgentGenerator.generate` is the implementation; `UserAgentInfo.toUserAgentString()` is a
thin convenience extension that delegates to it) so no implementer treats them as independent,
optional-to-pick surfaces.

---

## F2 — AD-1 fixes *where* rule data lives, not *what Kotlin shape* the codegen task must emit

**Adversarial pair:** Two people independently write "the Gradle codegen task for AD-1."

- Dev A emits one flat `object CompiledRules { val ALL: List<UaRule> }` where `UaRule` mixes browser,
  OS, and device patterns in one list, tagged by a `kind` enum, and expects `UserAgentParser` to filter
  at call time.
- Dev B emits three separate generated files/objects — `BrowserRuleTable`, `OsRuleTable`,
  `DeviceRuleTable` — each with its own distinct row type, and expects `UserAgentParser` to call three
  separate matchers.

Both satisfy AD-1's Rule to the letter: patterns are "vendored from uap-core... converted into
`commonMain` Kotlin source by a Gradle code-gen task at build time," and no target loads anything from
a filesystem/bundle/network at runtime. But `UserAgentParser` (a separate unit, built against
whichever shape its own author assumed) cannot compile against the other codegen output.

A second, sharper version of the same hole: uap-core's `regexes.yaml` rows aren't bare regexes — they
carry replacement/template fields (`family_replacement`, `v1_replacement`, `os_replacement`, etc.)
used to interpolate captured groups into the final family/version string. AD-1's Rule never mentions
these fields. A codegen implementer who reads only the Rule text as written may emit rows with just
`(pattern, label)` and silently drop the replacement-template mechanics — which is a large piece of
uap-core's actual matching semantics (CAP-1/CAP-2's core value) — while another implementer preserves
them. Both are "correct" per the letter of AD-1; only one is correct per uap-core's actual behavior,
and there is nothing in the spine that would catch the divergence since AD-5 only tests observable
parse/generate output, not the intermediate rule-table shape.

**Fix:** Add to AD-1's Rule (or a new AD-1b) the generated table's minimal contract: package/object
name(s), the per-rule field set (pattern + all replacement-template fields uap-core defines, not just
pattern + label), and whether it is one unified table or category-split tables. This is exactly the
kind of "shared internal contract between two independently-built units" the spine exists to fix, and
right now it's silent.

---

## F3 — AD-1 doesn't say whether generated Kotlin is committed to git or regenerated every build

**Adversarial pair:** Dev A wires the codegen task as a real Gradle task dependency
(`commonMain.kotlin.srcDir(generateUaRulesTask.get().outputDir)`), so the table regenerates from the
vendored YAML on every clean build and the generated `.kt` file is gitignored. Dev B runs the codegen
task once by hand, commits the resulting `.kt` file into `commonMain`, and never wires the task into
the actual build graph — treating "converted... by a Gradle code-gen task at build time" as satisfied
by "a Gradle task existed and ran once, at some point, to produce this checked-in source."

Both readings are literally consistent with "a Gradle code-gen task at build time." The difference is
material for CAP-4 (does `./gradlew build` from a clean checkout on a contributor's machine reproduce
the same generated source, or does it silently use stale committed code while the actual generator
script rots unused?) and for the Deferred item on "vendored uap-core snapshot refresh cadence" — that
Deferred item assumes a re-runnable generation step exists, which Dev B's reading doesn't guarantee.

**Fix:** State in AD-1 or Consistency Conventions whether generated sources are committed (diffable,
regenerated-and-diffed as a CI check) or produced fresh every build (not committed, `.gitignore`d).
This is a one-line addition that removes a real fork in how CAP-4's build reproducibility works.

---

## F4 — AD-2 pins the model's identity but not its field types; two "symmetric" models can still diverge internally

**Adversarial pair:** Dev A (building `UserAgentParser`) defines:
```kotlin
data class UserAgentInfo(val browser: Browser?, val engine: Engine?, val os: OS?, val device: Device?)
data class Browser(val name: String, val version: String?)
```
Dev B (building `UserAgentGenerator`, working from the same one-line AD-2 description — "browser,
engine, OS, device" — before either has seen the other's code) defines:
```kotlin
data class UserAgentInfo(val browserName: String?, val browserVersion: String?, ... )
```
Both believe they've implemented "a single immutable data class... both `UserAgentParser`'s output and
`UserAgentGenerator`'s input" — AD-2's actual enforceable claim is just "one class, not two," which is
trivially true in both cases even though the class shapes are incompatible with each other. This is
the single sharpest "two owners of one entity" case in the spine: `UserAgentInfo` is the one piece of
shared data every other AD depends on, and its field-level shape is never fixed.

A second-order version: is `version` a raw `String?`, or a structured `Version(major, minor, patch,
patchMinor)`? uap-core's own version fields (`v1`, `v2`, `v3`, `v4`) suggest a structured type; the
spine's one-line description doesn't say either way, and this single choice ripples into
`UserAgentGenerator`'s reconstruction logic (string-concat vs structured-format).

**Fix:** Either add the actual field-level shape of `UserAgentInfo` (including nested types for
browser/engine/OS/device and the version representation) to AD-2's Rule, or — if that's premature for
architecture altitude — explicitly note in Deferred that this class's concrete field shape is an
implementation decision *made once, by whoever lands first, and binding on the other unit*, so nobody
builds against an assumed shape in parallel. Right now nothing says who owns landing this class or
when the other side may safely start coding against it.

---

## F5 — No root package/namespace is fixed anywhere

**Observation:** The Structural Seed's directory tree lists `UserAgentParser`, `UserAgentGenerator`,
`UserAgentInfo`, and the generated rule table as flat siblings under
`library/src/commonMain/kotlin/` with no package path given. The only namespace hint anywhere in the
document is the Structural Seed's parenthetical "(name pending artifact-id decision)" under
`site.lempert` in the Deferred section — a Maven groupId, not a Kotlin package declaration, and it's
explicitly marked undecided.

**Adversarial pair:** Dev A (parser) puts `UserAgentInfo` in `site.lempert.useragent.UserAgentInfo`.
Dev B (generator) — working from the same spine, same directory tree, same "no platform suffixes on
shared names" convention — puts it in `site.lempert.useragent.model.UserAgentInfo`. Both obey every
literal AD and the naming convention table. Neither can `import` the other's class without a rename,
and the conflict surfaces only at integration time, not from anything either dev's isolated compile
would catch.

**Fix:** Add one line to Consistency Conventions (or Structural Seed) fixing the base package, e.g.
`site.lempert.useragent` as the single root package for all public types, with a stated policy on
subpackaging (flat, or `.model`/`.parser`/`.generator` — pick one). This is cheap to fix now and
expensive to fix after two people have already coded against different assumptions.

---

## F6 — AD-4's "library's only dependency is stdlib" contradicts AD-5's mandate to use `kotlin.test`

**Quote, AD-4:** "The library module's only dependency is the Kotlin stdlib."
**Quote, AD-5:** "...runs via `kotlin.test` identically on all four MVP targets."

**Adversarial pair:** Dev A reads AD-4 as scoped to the whole library module (main *and* test source
sets) and refuses to add `kotlin.test` as a `commonTest` dependency, hand-rolling assertions with
`check()`/`require()` to stay literally stdlib-only. Dev B reads AD-4 as scoped only to the
main/runtime source set (the thing that ends up in the published artifact) and adds `kotlin.test` to
`commonTest` per AD-5's explicit instruction. Both are defensible literal readings of "library
module... only dependency is the Kotlin stdlib" — the AD never says "production source set" or
"published artifact" to narrow its scope — and they produce differently-structured `build.gradle.kts`
test blocks, one of which (Dev A's) actively fights AD-5's own Rule.

Also worth a smaller note: AD-1's codegen task itself likely needs a YAML-parsing dependency
(snakeyaml/kaml or hand-rolled parsing) to read `regexes.yaml` at build time. Whether that dependency
lives in `buildSrc`/build-logic (arguably outside "library module") or is added directly to the
library module's build script is unaddressed, and AD-4's unscoped wording invites the same ambiguity.

**Fix:** Narrow AD-4's Rule to explicitly say "production/main source sets" or "the published
artifact's runtime dependencies," and explicitly carve out `commonTest`'s `kotlin.test` dependency
(and any buildSrc/codegen-only tooling dependency) as not counting against it.

---

## F7 — AD-5 doesn't fix the test-fixture *encoding*, reopening the exact cross-target resource-loading problem AD-1 was written to close

**Adversarial pair:** Dev A embeds the shared corpus as Kotlin data literals directly in
`commonTest/kotlin` (e.g. `val cases = listOf(TestCase("Mozilla/5.0 ...", expected = ...))`), which
compiles identically into every target's test binary. Dev B, treating "shared test corpus" more
literally as a *data file*, loads the uap-core fixture YAML/JSON as a bundled test resource at test
run time via each target's resource-loading mechanism. Both claim to satisfy "one `commonTest` data
set... runs via `kotlin.test` identically on all four MVP targets" — but KMP test-resource loading is
known to behave inconsistently across JVM/JS/Native test runners, so Dev B's approach risks the exact
per-target drift AD-1 explicitly closed off for production rule data, just reopened for test data,
which AD-5 never addresses.

**Fix:** Add one clause to AD-5's Rule: the shared corpus is embedded as Kotlin source (data literals
or a generated table, mirroring AD-1's own resolution), not loaded as a runtime test resource, so its
availability can't diverge by target.

---

## F8 — Deferred regex-dialect-portability risk can silently break AD-1 and AD-5 if it materializes, and the spine has no contingency

**Quote, Deferred:** "uap-core regex-dialect portability across JVM/JS/Native `kotlin.text.Regex`
backends — flagged as an implementation risk to validate early in `bmad-build`, not resolved here
since it doesn't change the shape of the contract, only which patterns need hand-adaptation."

This is stated as a non-architectural risk, but trace what happens if it's wrong: if some uap-core
regex patterns need "hand-adaptation" per target (a real possibility — JS/ECMAScript regex and JVM
regex have documented divergences, e.g. lookbehind support, possessive quantifiers, named-group
syntax), then AD-1's single "commonMain" rule table (one Kotlin source, one pattern string per rule,
shared by all targets) can no longer hold as written — you'd need either per-target pattern variants
(which is an `expect`/`actual` split the Design Paradigm explicitly says doesn't exist) or an
accepted-behavior-divergence for the affected patterns, which directly breaks AD-5's "runs identically
on all four targets" for exactly the fixture cases that exercise those patterns. Two units — "the JVM
test run" and "the JS test run" — would both be following AD-5 to the letter (running the one shared
corpus via kotlin.test) and get different pass/fail results through no fault of either implementer,
with no AD telling them which target's behavior is authoritative or how to reconcile.

**Fix:** This doesn't need to be resolved now, but the Deferred entry should say more than "flag it as
a risk" — add a decided fallback, e.g.: "if a uap-core pattern cannot be expressed identically across
`kotlin.text.Regex` backends, the JVM behavior is authoritative and the divergent pattern is either
rewritten to a portable equivalent or the affected fixture case is excluded from the shared corpus with
a recorded reason — never silently forked per target." That one sentence keeps this from becoming an
undocumented, target-specific fork of AD-1's "one table" invariant.

---

## F9 — CI is deferred without acknowledging it's the actual enforcement mechanism for AD-5 (and AD-1)

**Quote, Deferred:** "CI/CD pipeline specifics (assumed GitHub Actions, unconfirmed) — deferred to
`bmad-build`."

AD-5's entire value proposition is "one shared corpus, run identically on all four targets, so no
target quietly acquires its own pass/fail bar." That guarantee is only real if something *actually
runs all four targets' test tasks on every change*. Left fully deferred, two contributors could each
reasonably run `./gradlew jvmTest` locally, see green, and merge, while `jsTest`/`iosTest` silently
regress for weeks — which is precisely the failure mode AD-5 exists to prevent, just moved from
"per-target test data" to "per-target test execution," a gap the current wording doesn't close.

This is the one operational/environmental item in Deferred that isn't safely inert: the others
(artifact id, sample-repurposing, npm-publish plugin choice, snapshot refresh cadence, out-of-scope
targets) are genuinely deferrable without risking silent divergence between units. CI is different
because an AD's Rule already promises cross-target uniformity that only CI can actually check.

**Fix:** Either promote a minimal CI requirement to an AD ("CI runs `commonTest` — and thus the AD-5
corpus — across all four MVP target test tasks on every push/PR; a target's tests failing blocks
merge"), or at minimum revise the Deferred bullet to say pipeline *specifics* (which cloud CI, exact
YAML) are deferred but the *requirement* that CI exercises all four targets is decided now, not later.

---

## Minor / supporting observations (not standalone findings, fold into fixes above)

- No Rule addresses `generate()`'s behavior when given a `UserAgentInfo` with all four fields `null`
  (empty string? a generic base UA? throw?) — two `UserAgentGenerator` implementations could pick
  different literal answers. Worth a one-line addition wherever F4/AD-2 gets tightened.
- The Stack table's Kotlin/JS npm-publish row ("official Kotlin npm-publish tutorial flow") is looser
  than the ADs' Binds/Prevents/Rule structure and could itself be read multiple ways (legacy vs IR
  backend, `binaries.library()` vs `binaries.executable()`) — low risk since it's a single-owner
  publish config, not two independently-built units, but worth firming up before CAP-4 work starts.
- Release/version-numbering scheme for the published artifact (manual bump vs git-tag-triggered) is
  absent from both the ADs and Deferred. Low risk today (single owner), but should land in Deferred
  explicitly rather than being absent, given the operational-envelope framing of this review.

---

## Coverage check requested by the task

- **Every AD has Binds/Prevents/Rule present:** yes, all five (AD-1..AD-5) have all three fields.
  Enforceability is uneven, though: AD-3's Rule contains a literal either/or (F1); AD-1's Rule is
  enforceable for *where* data lives but silent on the generated API shape (F2/F3); AD-2's Rule is
  enforceable for "one class" but silent on that class's fields (F4); AD-4's Rule conflicts with AD-5's
  explicit tooling mandate (F6).
- **Deferred section — anything that should be an AD:** yes, F9 (CI must exercise all four targets) is
  the clearest case of a Deferred item that's actually load-bearing for an existing AD's guarantee and
  should be at least partially promoted. F8 (regex-dialect portability) is a closer call — reasonable
  to leave as a flagged risk, but its Deferred text should carry a decided fallback, not just a flag.
- **Operational/environmental envelope:** deployment (Sonatype Central Portal via vanniktech) and one
  publish flow (Kotlin/JS npm) are decided; environments are correctly N/A for a library artifact; CI
  is deferred in a way that undersells its role in enforcing AD-5 (F9); release/versioning scheme is
  silently absent rather than explicitly deferred (minor, above).
