# Reconciliation Review — SPEC-user-agent vs ARCHITECTURE-SPINE (kmp-user-agent, 2026-09-01)

Scope: compare `SPEC.md` + `.memlog.md` (the WHAT / decision trail) against `ARCHITECTURE-SPINE.md` (the HOW / invariants) for anything load-bearing that was quietly dropped, weakened, or contradicted when compressed into AD-1..AD-5.

Verdict: **4 gaps found** (2 real drops, 1 drift risk, 1 cross-document inconsistency worth surfacing). Sample-apps requirement, MVP target set, from-scratch/uap-core-inspiration policy, and the `site.lempert` namespace decision were all checked and are faithfully carried forward — no issue there.

---

## Finding 1 (primary) — MIT / no-copyleft license policy has no explicit invariant in the spine

**Spec/memlog:** The very first constraint in SPEC.md, and explicitly tagged `(user decision)` in the memlog:

> "MIT license end-to-end; only permissive-licensed (MIT/Apache-2.0/BSD) 3rd-party dependencies are allowed — no copyleft (GPL/LGPL) — so downstream MIT consumers never inherit copyleft obligations."

This is the headline promise of the "Why" section too ("an open-source, MIT-licensed Kotlin Multiplatform library").

**Spine:** The string "MIT" does not appear anywhere in ARCHITECTURE-SPINE.md. There is no AD, no Constraints table, and no Consistency Convention row stating the library ships under MIT or that any dependency must clear a permissive-license gate. The policy is only *accidentally* satisfied — AD-4 ("Library depends on nothing but the Kotlin stdlib") happens to make the no-copyleft-dependency question moot for v1, and AD-1 mentions "Apache-2.0, attribution retained" for the vendored uap-core data.

**Why this matters:** AD-4's stdlib-only rule is stricter than the spec's actual constraint (spec permits MIT/Apache-2.0/BSD 3rd-party deps; the spine forbids all 3rd-party deps). That's fine for now, but it means the license *policy itself* — the thing a future contributor needs to re-check the moment AD-4 is ever relaxed to add one dependency (a very likely v1.1 event) — isn't written down anywhere as a rule to consult. There's also no invariant covering the mechanics of shipping an MIT release that embeds vendored Apache-2.0 data: no NOTICE file, no LICENSE-APACHE-alongside-LICENSE-MIT convention, nothing that says where "attribution retained" (AD-1's phrase) is supposed to live. If AD-4 is loosened later without anyone re-deriving the license constraint from SPEC.md, a copyleft dependency could slip in with nothing in the architecture to catch it.

**Suggested fix:** Add a short Constraint/AD (or a Consistency Conventions row) stating: "Any dependency, now or later, must be MIT/Apache-2.0/BSD; no copyleft. Vendored Apache-2.0 content (uap-core) requires a NOTICE file alongside the MIT LICENSE at publish time." This costs one row and closes the gap.

---

## Finding 2 — npm "must be dist-output, not hand-built" requirement is only a Stack-table aside, not an invariant

**Spec/memlog (Q2 resolution, explicit and deliberate):**

> "The npm-publishable package must be produced as part of the standard build's dist output (the Kotlin/JS Gradle plugin's dist folder), not a separate hand-built step."

This was a specific, negotiated answer to an open question — i.e. load-bearing, not incidental phrasing.

**Spine:** The only place this shows up is a Stack-table cell: "official Kotlin npm-publish tutorial flow (js(IR) `binaries.library()`); `org.jetbrains.kotlin.npm-publish` plugin held for later re-evaluation." The Capability → Architecture Map row for CAP-4 just says "Governed by: Stack (vanniktech plugin, npm flow)" — it doesn't carry the "not a separate hand-built step" language forward.

**Why this matters:** `binaries.library()` does produce a package.json + dist folder as part of the standard Kotlin/JS build, so the current plan is *probably* compliant — but "official tutorial flow" is exactly the kind of phrase that, in practice, gets implemented with a bolted-on script that manually patches package.json fields or runs `npm publish` against a hand-assembled directory (both common in real Kotlin/JS npm-publishing writeups). Because the spec's specific constraint ("not hand-built") isn't restated as a rule anywhere a builder would check it against, there's nothing in the spine that would flag a hand-rolled packaging step as a violation when bmad-build implements CAP-4.

**Suggested fix:** Either promote this to its own AD ("CAP-4's JS artifact is whatever `js(IR) binaries.library()` emits into `build/dist/js/...` — no post-processing of that folder's contents before `npm publish`") or add the "not a separate hand-built step" sentence verbatim into the Stack row / CAP-4 map row so it survives into bmad-build's checklist.

---

## Finding 3 — Cross-document inconsistency the spine silently resolved without flagging it

**SPEC.md Success signal (line 52)** still reads:

> "...published (MIT license, permissive-only dependencies) to Maven Central — and to npm too, **if a separate JS artifact turns out to be needed**..."

This is a stale hedge. The memlog's Q2 resolution (`.memlog.md` line 30) already closed this unconditionally: "yes, publish an npm package for JS consumers in addition to the Maven Central Kotlin/JS artifact." SPEC.md's Capabilities/Constraints sections were updated to reflect Q2 (CAP-4, the npm/dist constraint), but the Success signal prose sentence was never revised and still carries the old conditional framing — SPEC.md is internally inconsistent between its own Constraints section and its Success signal.

**Spine:** Treats npm publishing as unconditional and required (Stack row states it as a settled decision; Deferred only defers the *plugin choice*, not whether to publish npm at all). This is the *correct* resolution per the memlog, but the spine doesn't note anywhere that it's resolving a stale hedge in its stated source of truth (SPEC.md) — a future reader diffing only SPEC.md against the spine could plausibly conclude the spine over-committed to npm publishing that the spec framed as conditional.

**Suggested fix:** Not the spine's bug to fix architecturally, but worth a one-line fix to SPEC.md's Success signal (drop the "if...needed" hedge) so the canonical WHAT document stops contradicting its own Constraints section and the memlog. Flagging here because a reconciliation pass is exactly the place this kind of drift surfaces.

---

## Finding 4 (minor, related to Finding 1) — Attribution mechanics for vendored uap-core data are named but not specified

AD-1 says the vendored `regexes.yaml` is "Apache-2.0, attribution retained" — this phrase asserts a fact (attribution *will be* retained) without the spine saying where or how (NOTICE file? header comment in the generated Kotlin? a THIRD_PARTY_LICENSES doc?). Given the spec's emphasis on "downstream MIT consumers never inherit copyleft obligations," the actual attribution artifact matters for real-world license compliance once published to Maven Central/npm. Folding a one-line answer into Finding 1's suggested fix would close this too.

---

## Checked and confirmed intact (no gap)

- **Sample/test apps per MVP target as validation harnesses only** — faithfully carried into AD-4, the Structural Seed (`samples/android/ios/jvm/js`), and the Capability → Architecture Map. Existing app-template modules are correctly left as "candidates to repurpose... left to bmad-build," matching the spec's "may be repurposed or replaced" language.
- **`site.lempert` / `lempert.site` domain-based Maven namespace** — preserved in the Deferred section ("Exact library artifact id under the `site.lempert` namespace") and consistent with the Stack table's choice of the vanniktech plugin against the Sonatype Central Portal (matching the spec's domain-ownership-check mechanism). Only the artifact id itself remains open, matching SPEC.md's own Open Questions.
- **From-scratch implementation + uap-core as "data reference"** — the spec's own language explicitly sanctions borrowing "rule data" (not just algorithmic inspiration) provided it's license-compatible and not a runtime dependency ("borrowed rule data stays license-compatible... these projects are not added as runtime dependencies"). AD-1's build-time-vendor-and-codegen approach is compiled in, not a runtime dependency, so it is consistent with the spec rather than exceeding it — flagged here as checked, not as a finding.
- **MVP target set / Wasm deferral** — consistent throughout; no Wasm reference introduced anywhere in the spine.
- **Non-goals (no analytics/bot-detection/fraud-scoring product, not exhaustive coverage, not shipping UI)** — all explicitly re-stated in the spine's Deferred section.

## Not flagged as a finding, but worth a bmad-build note

CAP-4's success criterion is about a fresh project consuming the *published* artifact; the spine's samples (AD-4) only validate against the local `:library` module, not the artifact actually published to Maven Central/npm. This is a reasonable thing to leave to CI/release tooling rather than the architecture spine, but nothing currently guarantees a post-publish smoke test happens — worth a line in bmad-build's plan, not a spine gap.
