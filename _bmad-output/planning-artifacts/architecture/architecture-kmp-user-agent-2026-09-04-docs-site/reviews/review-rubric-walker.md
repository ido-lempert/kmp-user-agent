# Review — kmp-user-agent-docs-site Architecture Spine (rubric walk)

Reviewed: `_bmad-output/planning-artifacts/architecture/architecture-kmp-user-agent-2026-09-04-docs-site/ARCHITECTURE-SPINE.md`
Parent (context only): `_bmad-output/planning-artifacts/architecture/architecture-kmp-user-agent-2026-09-01/ARCHITECTURE-SPINE.md`

## Overall verdict

Solid, well-scoped feature spine — the four ADs correctly target the real divergence risks for a small docs site with a live demo, and the inherited-invariants citations are accurate. It has one real gap that should be closed before epics/stories: it inherits parent AD-3's factory API but never pins down *which packs* the demo passes into it, and parent AD-3 explicitly warns that the empty-packs case silently returns nothing — as written, a story could build a fully "correct" (per this spine) demo that is permanently blank. A few secondary gaps (lockfile discipline under AD-2, no stated environments/preview or testing convention) are worth a look but are not blocking.

## Checklist walk

### 1. Does it fix the real divergence points for the level below, missing none?

Covered well:
- **Where the site lives / how it depends on the library** (AD-1) — prevents the classic mistake of a local Gradle/`file:` dependency that would let the demo diverge from what a real npm consumer sees.
- **Version drift of the npm dependency** (AD-2) — prevents silent behavior changes on every publish.
- **Demo option list vs. actual generator support** (AD-3) — prevents the demo offering/producing values `generate()` can't render, with an honest acknowledgment that today's stopgap is a hand-maintained file, not enforced.
- **Deploy path** (AD-4) — prevents a legacy branch-deploy path coexisting with Actions, and unrelated library commits triggering a site deploy.

**Missed (the one significant gap):** nothing in this spine states which `UserAgentTypePack`s the demo's `UserAgentParser(...)`/`UserAgentGenerator(...)` calls actually pass. The inherited-invariants table (row 1) correctly cites parent AD-3's factory signature, but parent AD-3 also carries an explicit, hard-won gotcha: *"Passing no packs returns an always-empty result (not an implicit `UserAgentAllTypes` fallback)"* — this was found the hard way during Story 4.1 because it's non-obvious. This spine's AD-2 says only "All parse/generate calls in the demo go through the imported package's exported `UserAgentParser`/`UserAgentGenerator` factories" — that's silent on the packs argument. A story implementer who writes `UserAgentParser()` with no args (the most naturally-discoverable call, since it type-checks and compiles) gets a demo that is the site's entire reason for existing and never shows anything, with no error and nothing in this spine to catch it. This is exactly the kind of "next unit built independently diverges silently" case the spine is supposed to close off, and it's a one-line fix (a Rule sentence: demo components call `UserAgentParser(UserAgentAllTypes)` / `UserAgentGenerator(UserAgentAllTypes)`, or whatever explicit pack set is intended given the bot/AI-agent deferral).

Everything else checked (guide-page structure, matrix file location, naming, Pages source config) is either adequately fixed by an AD or is genuinely low-risk enough to leave to story level.

### 2. Is every AD's Rule enforceable and does it actually prevent its stated divergence?

- **AD-1**: enforceable by inspection (`settings.gradle.kts` doesn't list `docs-site`; `package.json` has no `file:`/`link:` dependency). Effective.
- **AD-2**: enforceable by inspection (no `"latest"` in `package.json`) — but see finding below on caret-pinning + lockfile.
- **AD-3**: enforceable only as a documentation convention (header comment pointing at `UserAgentGenerator.kt`) — the spine itself is honest that this is a stopgap with no automated check, and correctly logs the real fix under Deferred rather than overclaiming enforcement here. Acceptable.
- **AD-4**: mostly enforceable via workflow-file inspection (build/upload-pages-artifact/deploy-pages steps, path filter). The "Pages source is set to GitHub Actions, not a branch" clause is a manual repo setting no code can pin down — a normal, acceptable limitation at this altitude, not a defect in the Rule.

**Finding (Medium): AD-2's caret-pinning allowance is weaker than its own stated goal.** AD-2's Prevents clause is "the demo silently changing behavior on every npm publish with no reviewable site-side change," and the Rule says bumps must be "a deliberate commit." But the Rule permits `^0.2.0`-style caret pins, and for a 0.x package, npm's caret semantics (`>=0.2.0 <0.3.0`) still allow `npm install` to silently resolve a newer patch/minor within range on any machine or CI run where the lockfile isn't authoritative. The Rule never mandates committing `docs-site/package-lock.json`, so the exact "diverges without a reviewable commit" scenario AD-2 exists to prevent is still reachable through the lockfile gap. Fix: either require exact pinning (no `^`), or explicitly require a committed, CI-respected lockfile (`npm ci`) alongside the caret pin.

### 3. Could anything under Deferred let two independently-built units diverge unsafely?

- **Library "known values" API**, **Dokka reference**, **custom domain**: all safe to defer — each has an explicit stopgap or fallback already in force (AD-3's hand matrix; guides-only v1; the `[ASSUMPTION]` + concrete `base: '/kmp-user-agent/'` fallback plan).
- **Bot/AI-agent packs in the generate demo**: safe to defer — it's a scope choice, not a shape choice; AD-3 already governs how the matrix must be kept honest whatever the scope decision turns out to be.
- **CI for the docs-site build (lint/typecheck on PRs)**: borderline, but on balance safe to defer *as stated*. AD-4's deploy workflow already gates on a successful `vitepress build` before `upload-pages-artifact` runs, so a broken build can't reach production — it just fails to deploy (site stays on last good version), which is a tolerable failure mode for a docs site. What a missing lint/typecheck gate does *not* catch is a build that succeeds but is logically broken at runtime (e.g., a `GenerateDemo` regression that only surfaces in the browser). That risk is real but modest, and the spine already names the gap explicitly in Deferred rather than hiding it — so this is a "consider tightening" note, not a rubric failure.

### 4. Named tech versions — verified vs. dubious?

- **VitePress 1.6.4** — plausible and consistent with the known trajectory (1.x stable line, 2.0 as an alpha built on Vite 6/Rolldown) as of my training data, but I cannot independently confirm the exact patch number `1.6.4` specifically as current at the spine's stated date (2026-09-04), which is after my knowledge cutoff. Low-confidence flag, not a correctness objection — worth a 30-second registry check (`npm view vitepress version`) at implementation time, since Stack tables are exactly the kind of thing that goes stale between spine-authoring and `npm install`.
- **actions/upload-pages-artifact v4** and **actions/deploy-pages v4** — plausible given the ecosystem-wide v4 bump many GitHub Actions went through together (`actions/checkout@v4`, `actions/setup-node@v4`, `actions/upload-artifact@v4`, tied to the Node 20 runner move), but I have lower confidence specifically on `upload-pages-artifact` having reached v4 (my training data recalls v1–v3 more solidly for that particular action). Recommend a quick Marketplace check before wiring the workflow — if it's still on v3, that's a trivial substitution, not an architectural issue either way.

None of these are "dubious" in the sense of contradicting something I'm confident about; they're flagged as unverified given the date is past my knowledge cutoff, per the instruction to flag anything not independently confirmable.

### 5. Does any new AD weaken or contradict inherited AD-3, AD-4, or AD-7 of the parent?

No contradictions found, and the citations are accurate:
- Inherited-invariants row for parent **AD-3** correctly quotes the current (post-supersession) factory signature, not the original `.parse()`/`.generate()` shape that AD-3 superseded — the authors clearly read the amended text, not a stale copy.
- Row for parent **AD-4** (stdlib-only library, consumer-never-reverse) is faithfully generalized from "samples" to "consumers" without changing its substance; AD-1 of this spine (npm-only dependency, no in-repo Kotlin coupling) actively reinforces it rather than weakening it.
- Row for parent **AD-7** (npm packaging from the build's own dist output) is correctly used to justify why AD-2 here matters (pinning only means something if what's published is the real dist output).

The one place inheritance is *accurately cited but under-operationalized* is the AD-3 packs-argument gotcha discussed in finding 1 above — the citation is correct, but this spine doesn't turn the gotcha into a binding Rule of its own the way it should, given the gotcha is specific to *how a consumer calls the factories*, which is squarely this spine's business.

### 6. Is every dimension this altitude owns decided, deferred, or an open question?

Mostly yes. Two dimensions are left effectively silent rather than decided/deferred/flagged:

**Finding (Medium): "Environments" beyond production is unaddressed.** The stack/deploy story here is single-environment: push to `master` → build → deploy to the one public GitHub Pages URL (AD-4). There's no mention of how a docs/demo change gets visually verified before it reaches that single production environment — no preview deployment, no documented `npm run docs:dev` expectation, nothing. The existing Deferred item about CI covers lint/typecheck only, which is a different concern (build correctness) from environments (where can a reviewer actually see the rendered change before merge). For a small docs site this may well be an acceptable "just run it locally" answer, but that answer isn't written down anywhere, so it's a silent dimension rather than a deliberately deferred one. Recommend either a one-line Rule ("PRs are reviewed via local `vitepress dev`; no hosted preview environment exists") or a one-line Deferred entry.

**Finding (Low): No testing convention for the demo components.** Parent AD-5 mandates a shared test corpus for the *library*, but this spine has no equivalent statement for the two Vue components that carry real logic (filter state, randomize-unset-fields behavior, wiring to the imported factories). This is application logic, not static content, and it's plausible for two stories (ParseDemo vs. GenerateDemo) to independently land with unit tests, no tests, or incompatible test tooling with no rule to converge them. Worth at least a Deferred line acknowledging the gap the way the CI item does, even if the answer is "manual verification only for v1."

Everything else expected at this altitude — infra/provider strategy (GitHub Pages via Actions), operations/tracking posture (explicitly "no analytics beyond GitHub Pages defaults" in Consistency Conventions), naming, data formats — is either decided by an AD or a Consistency Convention. Deployment *mechanics* specifically (AD-4) are well covered; it's the surrounding environment/testing envelope that has the two gaps above.

## Summary of findings by severity

- **High** — AD-2/inherited-AD-3 gap: no Rule states which type packs the demo passes to `UserAgentParser`/`UserAgentGenerator`; per parent AD-3, an empty-packs call silently returns nothing, so a spine-compliant implementation could ship a permanently blank demo.
- **Medium** — AD-2 allows caret-pinning without mandating a committed/`npm ci`-enforced lockfile, leaving the exact "silent behavior change without a reviewable commit" scenario it exists to prevent still reachable.
- **Medium** — "Environments" (pre-merge preview/visual review) is silent rather than decided or deferred; distinct from the already-acknowledged lint/typecheck CI gap.
- **Low** — No testing convention (unit/e2e) for the two Vue demo components, which carry real logic, not just static content.
- **Low / Info** — `actions/upload-pages-artifact@v4` and `actions/deploy-pages@v4`, and `VitePress 1.6.4`, are plausible but not independently verifiable from training data given the stated date is past my knowledge cutoff; spot-check at implementation time.
