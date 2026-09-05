# Adversarial Review — kmp-user-agent-docs-site Architecture Spine

**Target:** `ARCHITECTURE-SPINE.md` (docs-site feature spine, under the kmp-user-agent initiative spine)
**Method:** For each AD, construct two engineers who each follow the AD's Rule to the letter, and see whether their outputs can still fail to interoperate. Grounded against the actual library source (`UserAgentGenerator.kt`, `UserAgentInfo.kt`, the pack files, and a Kotlin/JS `.d.ts` build artifact) rather than the spine's prose alone.

## Overall Verdict

The spine correctly identifies and closes the single-most-obvious risk (a hand-written JS reimplementation drifting from the real library, via AD-2's pinned-dependency rule and AD-3's matrix-file rule), but it stops one layer too shallow in three places: it pins *where* the shared state lives but not *its shape*, pins *that* two pieces of state must stay in sync but not *how*, and asserts a deploy-path assumption (`base: '/kmp-user-agent/'`) only in non-binding prose, never in an enforceable Rule — so a fully-compliant pair of builders can still ship a demo that silently no-ops or a site that 404s.

---

## Finding 1 — [HIGH] Matrix-to-`UserAgentInfo` data shape is unpinned; a compliant matrix and a compliant demo can disagree on primitives vs. instances

**The pair:** the engineer who writes `docs-site/src/demo/generateSupportMatrix.ts` (AD-3) vs. the engineer who wires `GenerateDemo.vue`'s call into `UserAgentGenerator(...)` (AD-2, inherited parent AD-3).

**Why both are individually compliant:**
- AD-3's Rule only requires that the matrix file be "the single source of truth for every dropdown's option list and for what the randomizer may pick," with a header comment pointing at `UserAgentGenerator.kt`. It says nothing about what *type* the matrix's exported values are.
- AD-2's Rule only requires that "all parse/generate calls in the demo go through the imported package's exported `UserAgentParser`/`UserAgentGenerator` factories."

**The actual clash:** `UserAgentInfo`, `Component`, and `Device` are `@JsExport data class`es. Confirmed against a real Kotlin/JS build artifact (`library/build/klib/.../module.d.ts`), `@JsExport` data classes compile to genuine JS/TS **classes with constructors and methods** (`copy`, `toString`, `hashCode`, `equals`) — not plain object literals:

```ts
export declare class Component {
    constructor(name: Nullable<string>, version: Nullable<string>);
    get name(): Nullable<string>;
    get version(): Nullable<string>;
    copy(name?: Nullable<string>, version?: Nullable<string>): Component;
    ...
}
```

Nothing in the spine says whether `generateSupportMatrix.ts` exports **raw strings/enums** (`browsers: ["Chrome","Firefox","Safari","Edge"]`) that `GenerateDemo.vue` is responsible for wrapping in `new Component(...)`/`new Device(...)` before calling the generator function — or whether the matrix itself exports **pre-built `Component`/`Device` instances** (importing the classes from `@lempert/user-agent` and constructing them at matrix-definition time). Both readings satisfy AD-3's Rule ("single source of truth... option list") and AD-2's Rule ("calls go through the factories") to the letter.

A team split across these two files can build:
- Engineer A (matrix): exports `{ os: { name: string; versions: string[] }[] }` — plain strings, no import of the npm package's classes at all.
- Engineer B (`GenerateDemo.vue`): expects the matrix to already hand back `Component`/`Device` instances (mirroring how they saw the parse-direction `UserAgentInfo` shape used in `ParseDemo.vue`) and passes matrix values straight into `UserAgentInfo`'s fields without wrapping.

Result: either a TypeScript compile error discovered late (when the two files are integrated, not when either is reviewed alone — `generateSupportMatrix.ts` type-checks fine in isolation against `string[]`), or, if the seam is `any`-typed to paper over it, a silent runtime failure — `generate()` receives a non-`Component` object, and because the actual generator functions catch all `Throwable`s per-pack and fall back to the bare `Mozilla/5.0` base string, the failure mode is **not an error, it's a silently wrong demo output** — exactly the failure AD-3's own "Prevents" clause says it's trying to rule out, just arriving through the shape of the bridge code instead of the option values.

**Suggested close:** Add a Rule (or extend AD-3) pinning the matrix's export shape explicitly — e.g. "the matrix exports primitive option lists only (`string`/`string[]`); `GenerateDemo.vue` owns the single mapping function from selected primitives to `Component`/`Device`/`UserAgentInfo` instances, colocated with the matrix file or in a named `toUserAgentInfo()` helper the matrix's header comment also points at."

---

## Finding 2 — [HIGH] AD-3's matrix rule enumerates single-field values, not cross-field combinations — so it doesn't actually prevent what it says it prevents

**The pair:** the same matrix author (AD-3) vs. the actual behavior of `generateOsToken` in `UserAgentGenerator.kt`.

**Why the matrix author is compliant yet still produces a broken combination:** AD-3's Prevents clause is explicit: it exists to stop "the demo offering (or randomly producing) a filter combination `generate()` silently can't render." But the Rule only asks for "every dropdown's option list" and "what the randomizer may pick when a field is left unset" — i.e., **per-field** enumerations. It never asks the matrix to encode **pairwise** constraints.

The library has real pairwise constraints that live nowhere except a code comment inside `generateOsToken`:

```kotlin
val unsafeCombination = (browserFamily == "Firefox" && (os.name == "Android" || os.name == "iOS")) ||
    (browserFamily == "Safari" && os.name == "Android")
if (unsafeCombination) return null
```

A matrix author who mirrors `UserAgentGenerator.kt` exhaustively per-field (browsers: Chrome/Firefox/Safari/Edge — all four are individually valid; OS names: Windows/Mac OS X/iOS/Android/Linux — all five are individually valid) is fully AD-3-compliant. But selecting **Firefox + Android** (or Firefox + iOS, or Safari + Android) as a combination is a filter combination `generate()` silently can't render — it returns `null` from `generateOsToken`, so `generateBrowserSegment` still returns a browser string but with **no OS token at all**, silently dropping the OS filter the visitor selected. This is precisely the failure mode AD-3 exists to prevent, and AD-3's literal Rule text does not cover it. There's no requirement anywhere (not even in Deferred) that the matrix — or the randomizer — respect this pairwise exclusion list.

**Suggested close:** Either (a) extend AD-3's Rule to require the matrix encode known-unsafe combinations (a small exclusion table, not just per-field lists) and have the randomizer/dropdown-filtering logic consult it, or (b) note this explicitly under Deferred as an accepted gap alongside the existing "Library known values API" item — right now it's neither ruled nor deferred, it's just missing.

---

## Finding 3 — [HIGH] Two independent, uncoordinated owners for "what the demo can generate": the npm version bump (AD-2) and the matrix file (AD-3)

**The pair:** whoever bumps `@lempert/user-agent` in `docs-site/package.json` (AD-2) vs. whoever edits `generateSupportMatrix.ts` (AD-3).

Both ADs are individually sound and each has its own clean Rule. But they govern the *same conceptual fact* — "what can the live demo actually generate right now" — through two separate, manually-triggered processes with no linkage between them:

- AD-2's Rule: "Bumping it is a deliberate commit made when the demo should pick up a new release." Nothing requires that commit to touch the matrix file.
- AD-3's Rule: the matrix's header comment "must point back at `UserAgentGenerator.kt`... to re-check whenever that file's supported... values change." Nothing requires this re-check to happen on the same commit/PR as an AD-2 version bump, or at all — it's a comment, not a gate.

Concretely: a contributor lands a library change that adds a new OS version or browser, publishes it, and — following AD-2 to the letter — bumps `docs-site/package.json`'s pinned version in a clean, reviewable, single-purpose commit ("upgrade to 0.3.0"). That commit satisfies AD-2 completely. It has no obligation to touch `generateSupportMatrix.ts`, and per AD-3 the matrix file is a *separate* file with its own edit history. The matrix now silently under-represents (or, worse, if a value was *removed* from the library, over-represents and can produce the Finding-2-style silent failure) what the pinned package actually supports — while both files, reviewed independently, are each fully compliant with their own AD.

This is also called out obliquely by the Deferred section ("Library 'known values' API... AD-3's hand-curated matrix is a deliberate stopgap"), but that Deferred entry frames the risk as *the matrix drifting from the library over time in general* — it doesn't name the specific mechanism (two separately-triggered manual processes, one of which — the version bump — has no reason to even glance at the other file) or flag that a version bump is the exact moment drift becomes both most likely and most consequential.

**Suggested close:** Not necessarily a new mechanism (CI to diff the matrix against the library is explicitly out of scope for this feature per the parent's stated gap) — but at minimum, tighten AD-2's Rule to require the version-bump commit to also touch (or explicitly confirm-unchanged) `generateSupportMatrix.ts`, e.g.: "A version bump commit must include a corresponding check of `generateSupportMatrix.ts` against the new version's `UserAgentGenerator.kt`, noted in the commit message even when no matrix change is needed." That converts an easily-skipped comment into a checklist item tied to the one event that actually invalidates the matrix.

---

## Finding 4 — [HIGH] `base: '/kmp-user-agent/'` is written only in non-binding prose (Structural Seed / Deferred), never promoted to an enforceable Rule

**The pair:** whoever writes `.vitepress/config.ts` vs. whoever writes the Actions workflow YAML (AD-4).

AD-1 through AD-4 govern: project location (AD-1), the npm dependency (AD-2), the matrix file (AD-3), and the deploy mechanism — Actions vs. branch, path-filtered trigger (AD-4). **None of the four ADs mentions the VitePress `base` config at all.** The only places `base: '/kmp-user-agent/'` appears are:
- a code comment in the non-normative **Structural Seed** section (`config.ts # nav/sidebar, base: '/kmp-user-agent/' (GitHub Pages project-site path)`), and
- an `[ASSUMPTION]` tag under **Deferred** ("No custom domain... so VitePress `base` is set to `/kmp-user-agent/`").

Neither the Structural Seed nor the Deferred section is a Rule — the spine's own convention (see every AD's `Rule:` line) is that binding constraints live in AD Rule text. A config.ts author who reads only AD-1–AD-4 (the section literally titled "Invariants & Rules") has zero obligation to set `base` at all; the default is `'/'`. Meanwhile the AD-4-compliant workflow author builds VitePress, uploads `dist/`, and deploys via `actions/deploy-pages@v4` exactly as specified — nothing in AD-4 checks that the built asset paths actually resolve at the served path. Both are fully AD-1–AD-4 compliant. The result is a site that deploys successfully (satisfying every literal Rule) but 404s on every asset and internal route in production, because GitHub Pages project sites serve from `/kmp-user-agent/`, not `/`.

This is a sharper version of the exact class of bug the task asked to hunt for: "conflicting assumptions about the GitHub Pages base path... that AD-1 through AD-4 don't actually pin down." It's not merely conflicting assumptions between two engineers — it's that *no* AD makes an assumption at all; the correct value exists only in sections the spine itself treats as illustrative/deferred, not binding.

**Suggested close:** Fold the `base` requirement into AD-4's Rule (it's a deploy-correctness concern, same as the Actions-vs-branch choice): "`docs-site/.vitepress/config.ts` sets `base` to match the GitHub Pages serving path (`/kmp-user-agent/` for the current project-site setup per Deferred; revisit only if a custom domain is added)." One sentence closes it.

---

## Finding 5 — [MEDIUM] Pack selection for `UserAgentGenerator(...)` is unpinned, so the matrix's option set and the demo's actual generator call can diverge on bot/AI-agent coverage

**The pair:** exactly the task's own example — the engineer wiring `GenerateDemo.vue`'s `UserAgentGenerator(...)` call vs. whoever edits the support matrix.

The Deferred section explicitly punts: "whether GenerateDemo exposes [bot/AI-agent packs] as filter options is a story-level UI-scope call, not fixed here." That's a reasonable scope deferral for *whether* to add the dropdown — but nothing then requires that whichever packs are exposed as matrix options are the same packs actually passed to `UserAgentGenerator(...)` in the component's script. The inherited parent AD-3 pins the factory call shape ("no reimplementation, no alternate entry point") but not its *arguments*.

Concretely: a matrix author adds a "persona" dropdown including a bot value (e.g. `"Googlebot"`) because the Deferred note gives explicit latitude to do this at the story level — fully compliant. Separately, whoever wires the actual `UserAgentGenerator(UserAgentBrowserTypes, UserAgentEngineTypes, UserAgentOsTypes)` call in `GenerateDemo.vue` (also compliant with AD-2/inherited-AD-3, since nothing pins the pack list) never includes `UserAgentBotTypes`. Selecting "Googlebot" from the dropdown sets `info.bot`, but the generator's pack list has no bot-aware pack, so it silently falls through to the bare-base fallback — again a silent no-op, not an error.

Note: this is a narrower risk than it could be, because the parent library's own doc comment guarantees `UserAgentAllTypes`'s generate behavior is identical to composing all six narrower packs — so *if* both engineers independently default to passing `UserAgentAllTypes`, this risk evaporates on its own. That equivalence guarantee is a good example of something the parent spine already closes adequately; it's only the *narrower, hand-picked pack list* case (plausible, since AD-2's docs-site spine never says "always pass `UserAgentAllTypes`") that reopens the gap.

**Suggested close:** A one-line addition to AD-3 (this spine's, not the inherited one): "`GenerateDemo.vue` passes `UserAgentAllTypes` to `UserAgentGenerator(...)` — not a hand-picked pack subset — so the matrix's option coverage and the generator's actual capability can never diverge by pack selection." This is a cheap, total fix given the parent's documented equivalence guarantee.

---

## Finding 6 — [LOW] Guide-page prose has no pinned relationship to the pinned demo version

**The pair:** whoever edits `docs-site/guide/*.md` vs. whoever bumps the pinned npm version (AD-2).

Guide pages describe library usage/behavior in prose; the live demo on the intro page runs against whatever version AD-2 currently pins. Nothing ties guide-page accuracy to the pinned version — a guide page could be updated to describe a newer release's behavior (or fields) while the intro page's interactive demo, still pinned to an older version per AD-2's deliberate-bump model, visibly contradicts it (e.g., a guide mentions a new device model the live GenerateDemo dropdown doesn't offer). This is a lower-severity, softer version of Finding 3 — annoying and confusing rather than build-breaking, and arguably reasonable to leave as an editorial/reviewer concern rather than an AD. Flagging for completeness; **not** recommending a new AD for this one, just noting it as a variant of the same "two owners, one fact" pattern in case it recurs.

---

## What's Already Adequately Closed (no new finding needed)

- **Hand-written reimplementation drift** — AD-2's Rule ("no parse/generate logic is duplicated in the site") plus AD-7-inherited ("real published artifact") fully forecloses the most obvious way a demo could silently diverge from the library. Good, tight closure.
- **`latest`-tag version churn** — AD-2's "never `latest`" rule is unambiguous and directly enforceable by reading `package.json`; no compliant-but-incompatible pair is possible here.
- **Legacy `gh-pages` branch coexisting with Actions** — AD-4's Rule explicitly sets Pages source to "GitHub Actions," closing the two-deploy-path race condition cleanly.
- **Kotlin/Gradle module ever depending on the docs site** — AD-1 + inherited AD-4 ("library must never gain a dependency on the docs site") jointly close the wrong-direction-dependency risk; `docs-site/` sitting fully outside `settings.gradle.kts` makes this close to physically enforced, not just declared.
- **Pack-selection equivalence for the *full* pack (`UserAgentAllTypes`) vs. composing all six narrower packs** — explicitly guaranteed by the library's own doc comment on `generateFullUserAgentString`. This is exactly the kind of cross-file consistency an AD-level rule can't reach but the library's own contract already provides — correctly leaned on rather than re-litigated in this spine.

---

## Summary Table

| # | Severity | Pair | One-line issue |
| - | - | - | - |
| 1 | HIGH | matrix author vs. GenerateDemo.vue author | Matrix export shape (primitives vs. `Component`/`Device` class instances) is unpinned; `@JsExport` data classes are real classes, not object literals. |
| 2 | HIGH | matrix author vs. `generateOsToken`'s hidden pairwise rules | AD-3 pins per-field option lists, not the cross-field unsafe-combination exclusions (Firefox/Safari × Android/iOS) that actually govern what `generate()` can render. |
| 3 | HIGH | npm-version bumper vs. matrix editor | AD-2 and AD-3 each govern half of "what the demo can generate" through separate, uncoordinated manual processes with no shared trigger. |
| 4 | HIGH | config.ts author vs. workflow author | `base: '/kmp-user-agent/'` lives only in non-binding Structural Seed/Deferred prose, never in an AD Rule — a compliant config.ts can ship with the wrong (default) base. |
| 5 | MEDIUM | GenerateDemo.vue's generator-call wiring vs. matrix's dropdown options | Pack list passed to `UserAgentGenerator(...)` is unpinned; a hand-picked subset can silently drop matrix-advertised options (e.g. bot personas). |
| 6 | LOW | guide-page authors vs. npm-version bumper | Guide prose has no pinned relationship to the pinned demo version; can describe capabilities the live demo doesn't have. |
