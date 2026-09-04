---
title: 'Redesign Parse/Generate as Composable Type Packs, Validated via npm/Node'
type: 'refactor'
created: '2026-09-04'
status: 'done'
review_loop_iteration: 0
baseline_commit: '6556be91d5240b1cca09f44e44c5ca5a732a6e5b'
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-4-context.md'
  - '{project-root}/_bmad-output/planning-artifacts/sprint-change-proposal-2026-09-04.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The published API (`UserAgentParser.parse(String)` / `UserAgentGenerator.generate(UserAgentInfo)`, singleton objects over one monolithic always-loaded rule table) has no bundle-size control and no extensibility — confirmed by trying the npm package in a clean Node.js project, where JS consumers compile to an awkward `UserAgentParser.getInstance().parse(...)` and pay for browser+OS+device rules even when they only want one category, with no way to add new detection categories without forking.

**Approach:** Replace both entry points with factory functions — `UserAgentParser(vararg packs)` and `UserAgentGenerator(vararg packs)` — parameterized by composable type packs covering today's existing categories: browser, engine, OS, device. Reorganize the codegen'd rule tables from 3 monolithic lists into one compiled table per pack so an unreferenced pack tree-shakes out of a JS build. Ship `UserAgentBrowserTypes`/`UserAgentEngineTypes`/`UserAgentOsTypes`/`UserAgentDeviceTypes`/`UserAgentAllTypes`, and a documented `UserAgentTypePack` shape consumers can implement for their own packs. Validate directly against a clean Node.js project consuming a locally packed tarball, including a single-pack-vs-all-types bundle-size comparison. (Bot/AI-agent detection packs are net-new capability, not part of this redesign — deferred to a follow-up story; see `deferred-work.md`.)

## Boundaries & Constraints

**Always:** No packs passed = the parser/generator matches/generates nothing (an always-empty `UserAgentInfo` / empty string) — **not** an implicit fallback to `UserAgentAllTypes` (see Spec Change Log: the implicit fallback was found during implementation to force every `UserAgentParser` call site, regardless of which packs it actually passes, to retain a static reachability edge to every built-in pack under standard JS bundlers, defeating the tree-shaking this story exists to deliver). Consumers wanting everything call `UserAgentParser(UserAgentAllTypes)` explicitly. Still stateless — no mutable shared state, no caching beyond read-only compiled tables. `UserAgentInfo` keeps `browser`/`engine`/`os`/`device` unchanged; also add `bot: Component?`/`aiAgent: Component?` fields now (unused until the deferred follow-up ships their packs) so that follow-up doesn't need another data-model change. `commonMain`/target source sets stay stdlib-only (AD-4). Existing regex-matching/template-substitution logic and JS-dialect escape normalization are reused, not reinvented.

**Ask First:** The exact `UserAgentTypePack`/custom-pack contract shape (only the sketch in Design Notes is fixed) — if a materially different shape turns out necessary, confirm before locking it in, since Story 4.2 and the deferred bot/AI-agent story depend on it. ~~Whether to touch `webApp`/`androidApp`/`jvmApp`/`iosApp` call sites here — default is no, that's Story 4.2; ask if leaving them broken on `master` between stories is a problem.~~ **Resolved during review (see Spec Change Log):** yes, this is a problem — `jvmApp`/`androidApp` are on the CI path (`./gradlew build`) and this repo works directly on `master` with no feature branches, so leaving them broken would redden CI. Apply a minimal compile-only fix to all 4 sample apps' call sites (old API syntax → new API syntax, e.g. `UserAgentParser.parse(ua)` → `UserAgentParser(UserAgentAllTypes)(ua)`) — no redesign, no richer demos, that's still Story 4.2's job.

**Never:** Do not implement `UserAgentBotTypes`/`UserAgentAIAgentTypes` or any bot/AI-agent rule data here — deferred. Do not touch `stagePublishJsPackage`/`prepareNpmStagePublishConfig` in `library/build.gradle.kts` (unrelated in-progress work). Do not run a real `publishToMavenCentral`/npm publish. Do not do a full redesign of the 4 sample apps (Story 4.2's job) — only the minimal compile-fix described above.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Subset pack | `UserAgentParser(UserAgentBrowserTypes)("...")` | Only `browser` populated; `engine`/`os`/`device`/`bot`/`aiAgent` all `null` | N/A |
| All types | `UserAgentParser(UserAgentAllTypes)("...")` on a known Chrome UA | Same result as today's `UserAgentParser.parse(...)` for that UA (`bot`/`aiAgent` stay `null` — no packs for them yet) | N/A |
| No packs | `UserAgentParser()("...")` | Always returns an empty `UserAgentInfo` (all fields `null`) — does **not** fall back to `UserAgentAllTypes` | N/A |
| Custom pack | A consumer-authored pack passed alongside/instead of built-ins | Contributes to the result without any library source change | N/A |
| Generate round-trip | `UserAgentGenerator(UserAgentAllTypes)(info)` then parsed back | Matches today's `UserAgentGenerator.generate(info)` output for the 4 MVP browser families | N/A |
| Bundle size | `npm pack` tarball installed fresh in a clean Node project; import only `UserAgentBrowserTypes` vs. `UserAgentAllTypes` | Single-pack import's built size is measurably smaller | N/A |

</frozen-after-approval>

## Code Map

- `library/src/commonMain/kotlin/site/lempert/useragent/UserAgentParser.kt` — singleton `object` with `parse(String)`; becomes the `UserAgentParser(vararg packs)` factory. `detectBrowser`/`detectOs`/`detectDevice` logic (regex + `$1`/`$2` template substitution via `applyGroupReplacement`) is reused per-pack; `detectEngine` is hardcoded regexes (Trident/Blink/Gecko/Presto/WebKit tokens), not codegen'd — stays code, gets wrapped as the engine pack's detector, not a rule-table pack.
- `library/src/commonMain/kotlin/site/lempert/useragent/UserAgentGenerator.kt` — singleton `object` with `generate(UserAgentInfo)`; becomes the `UserAgentGenerator(vararg packs)` factory. Internals (`generateOsToken`, per-family `when (browser?.name)` branches) are pack-agnostic today and can stay mostly as-is inside the returned closure.
- `library/src/commonMain/kotlin/site/lempert/useragent/UserAgentInfo.kt` — add `bot: Component?`, `aiAgent: Component?` (unused fields, packs land later) and the custom-pack extension point (shape per Design Notes).
- `library/build.gradle.kts:287-355` (`UapCoreCodegen.generateSource`) — currently emits one `browserRules`/`osRules`/`deviceRules` `internal val` each into `site.lempert.useragent.generated`, already independent top-level `val`s (good starting point for tree-shaking) — no schema change needed, just consumed per-pack instead of all together.
- `library/build.gradle.kts:459-472` (`js { }` block) — `outputModuleName = "library"`, `browser()` + `binaries.library()` + `generateTypeScriptDefinitions()`, `target = "es2015"`. No dedicated Node test harness exists yet for the bundle-size AC — needs a fresh scratch verification step (not a committed 5th sample app; Story 4.2 owns the committed samples).
- `library/src/commonTest/kotlin/site/lempert/useragent/UserAgentParserTest.kt` (22 call sites), `UserAgentGeneratorTest.kt` (27 parse + 33 generate call sites), `UapCoreFixtureCorpusTest.kt` (3 call sites, data-driven loops) — all call the old singleton API directly. Cheapest safe migration: define one `private val parse = UserAgentParser(UserAgentAllTypes)` / `private val generate = UserAgentGenerator(UserAgentAllTypes)` per test class, then mechanically replace `UserAgentParser.parse(` → `parse(` and `UserAgentGenerator.generate(` → `generate(`.
- **Out of scope, do not touch:** `webApp/src/components/Greeting/Greeting.tsx`, `androidApp/.../MainActivity.kt`, `jvmApp/.../Main.kt`, `iosApp/iosApp/ContentView.swift` (Story 4.2 updates them) — `library/build.gradle.kts`'s `stagePublishJsPackage`/`prepareNpmStagePublishConfig` tasks (unrelated, uncommitted, pre-existing work) — and any bot/AI-agent rule data or packs (deferred, see `deferred-work.md`).

## Tasks & Acceptance

**Execution:**
- [x] `library/src/commonMain/kotlin/site/lempert/useragent/UserAgentInfo.kt` — add unused `bot`/`aiAgent` fields + custom-pack extension point — future-proofs the data model for the deferred follow-up without another breaking change
- [x] `library/src/commonMain/kotlin/site/lempert/useragent/UserAgentParser.kt` — replace the `object` with a `UserAgentParser(vararg packs)` factory returning `(String) -> UserAgentInfo`; define `UserAgentTypePack` and the built-in pack constants (`UserAgentBrowserTypes`, `UserAgentEngineTypes`, `UserAgentOsTypes`, `UserAgentDeviceTypes`, `UserAgentAllTypes`) — the core API redesign (AD-3). Built-in pack constants ended up split one-per-file (`UserAgentBrowserTypePack.kt`, etc.) rather than all living in `UserAgentParser.kt`, so each gets an independent Kotlin/JS lazy-init gate — required for the tree-shaking AC below. No-packs case returns an always-empty result, not a fallback to `UserAgentAllTypes` (see Spec Change Log).
- [x] `library/src/commonMain/kotlin/site/lempert/useragent/UserAgentGenerator.kt` — replace the `object` with a `UserAgentGenerator(vararg packs)` factory returning `(UserAgentInfo) -> String` — symmetric generate-side redesign, same no-packs-is-empty behavior as the parser
- [x] `library/src/commonTest/kotlin/site/lempert/useragent/*.kt` (all 3 files) — migrate call sites per the Code Map's find-replace approach; add cases for: subset-pack parsing, a custom pack, no-packs-returns-empty (amended from the original "no-packs-equals-all-types") — covers the I/O matrix
- [x] Fresh Node.js verification (scratch project, not committed as a sample) — `npm pack` the js target's dist output, install in a clean Node project, confirm `UserAgentParser(UserAgentBrowserTypes)(ua)` works and its built size is measurably smaller than `UserAgentParser(UserAgentAllTypes)(ua)` — the concrete check that motivated this story. Confirmed: 91,680 vs. 178,270 bytes minified (esbuild), 190,397 vs. 299,130 bytes unminified, 87,880 vs. 174,559 bytes via Terser — see Verification below for the full numbers.
- [x] *(Review pass 1 patch)* Minimal compile-only fix for all 4 sample apps' call sites (`jvmApp/.../Main.kt`, `androidApp/.../MainActivity.kt`, `webApp/.../Greeting.tsx`, `iosApp/iosApp/ContentView.swift`) to the new factory-function API — `jvmApp`/`androidApp` are on the CI path (`./gradlew build`) and this repo commits straight to `master`, so leaving them broken would redden CI. `webApp`'s and `iosApp`'s exact call syntax (`UserAgentParser([UserAgentAllTypes.get()])(...)` for JS/TS; `UserAgentParserKt.UserAgentParser(packs:)` taking a `KotlinArray`, not a native Swift Array, for Swift) was confirmed against the real compiled `.d.mts` and the real generated `Library.framework` header, not guessed. `webApp`'s positional `new UserAgentInfo(...)` 4-arg call was checked against the new 7-param constructor and needed no change (the 3 new fields are trailing/optional in the generated JS constructor); `iosApp`'s equivalent Swift call needed all 7 params since Objective-C/Swift interop doesn't carry Kotlin default-parameter values.
- [x] *(Review pass 1 patch)* Added one test per direction (`UserAgentParserTest.throwingCustomPackDegradesGracefullyAlongsideAWorkingPack`, `UserAgentGeneratorTest`'s same-named test) exercising a `UserAgentTypePack` whose `detect`/`applyToGenerate` throws, composed alongside `UserAgentBrowserTypes` — confirms the existing `catch (_: Throwable) { null }` around each pack call actually degrades gracefully rather than propagating.

**Acceptance Criteria:**
- [x] Given the reworked public API, when its signature is inspected, then it is exactly `UserAgentParser(vararg packs: UserAgentTypePack): (String) -> UserAgentInfo` and `UserAgentGenerator(vararg packs: UserAgentTypePack): (UserAgentInfo) -> String`, with no coexisting alternate entry point
- [x] Given `./gradlew :library:allTests`, when run after the rework, then all tests pass on every configured target (jvm, iosArm64/iosSimulatorArm64, android host test, js) — 70 tests × 4 targets, all green
- [x] Given `UserAgentInfo`, when inspected, then it has `bot`/`aiAgent` fields present (always `null` in this story's output, since no pack populates them yet)
- [x] Given a bundled call site `UserAgentParser(UserAgentBrowserTypes)(ua)` (not just a bare pack import with no factory call), when built with a standard bundler (esbuild/Terser) and compared against `UserAgentParser(UserAgentAllTypes)(ua)`, then the single-pack build is measurably smaller — this is the criterion the earlier implicit-`UserAgentAllTypes`-default design failed. Now passes: ~49% reduction (esbuild minified), ~50% reduction (Terser); raw unminified bundle confirms `detectDevice`/`deviceRules`/`detectOs`/`osRules`/`detectEngine` are fully absent (0 occurrences) from the `UserAgentBrowserTypes`-only bundle.
- [x] *(Review pass 1)* Given `./gradlew build` (full build, not just `:library:allTests`), when run after the sample-app fixes, then it succeeds — confirmed. `iosApp` verified separately via a real `xcodebuild build` against the iOS Simulator (BUILD SUCCEEDED); `webApp`'s changed call sites verified via `tsc --noEmit --strict` against the real published `.d.mts` (0 errors) — neither has a repo CI/build step, so these were best-effort manual checks per the human's direction.

## Spec Change Log

- **2026-09-04, mid-implementation:** First implementation pass satisfied "no packs = `UserAgentAllTypes`" literally, by having `UserAgentParser`'s factory body reference `UserAgentAllTypes` in its empty-args fallback branch. Verified via a real `npm pack` + esbuild/Terser build that this gives every `UserAgentParser` call site — including `UserAgentParser(UserAgentBrowserTypes)`, the primary documented pattern — a static reachability edge to every built-in pack, since standard JS bundlers tree-shake at module/top-level-binding granularity, not per-branch: `UserAgentParser(UserAgentBrowserTypes)(ua)` measured 178,339 bytes vs. `UserAgentParser(UserAgentAllTypes)(ua)`'s 178,310 bytes — no real reduction, defeating this story's core purpose. Human decided (offered three options: drop the implicit default / keep it and document the limitation / research a specializing bundler) to **drop the implicit default**: no packs now returns an always-empty result; `UserAgentAllTypes` must be passed explicitly. Amended: Boundaries' "Always" line, the "No packs" I/O matrix row, and added an explicit bundled-call-site bundle-size AC. KEEP: everything else from the original implementation pass (factory shape, built-in packs, `UserAgentTypePack` custom-pack contract, `bot`/`aiAgent` fields, test migration approach) — none of that is implicated by this change.

- **2026-09-04, review pass 1:** Review (edge-case-hunter + verification-gap layers) found `jvmApp`/`androidApp` still call the old `.parse(ua)`/`.generate(info)` member-function syntax, which no longer compiles against the new factory-function API — and, unlike `iosApp`/`webApp`, these two are built by `.github/workflows/ci.yml`'s `./gradlew build`, so `master`'s CI would go red. This resolves the "Ask First" item on sample-app call sites left open in the original Boundaries: human chose a minimal compile-only fix now (all 4 sample apps, not just the CI-covered two, for consistency) rather than accepting red CI or doing Story 4.2's full rework early. KEEP: everything else — factory shape, built-in packs, custom-pack contract, `bot`/`aiAgent` fields, no-packs-is-empty behavior, test migration — none of that is implicated. Also patched from the same review pass (both `patch`, no spec change needed): add a test per direction (parse/generate) asserting a throwing custom `UserAgentTypePack` degrades gracefully (the existing `catch (_: Throwable) { null }` around each pack's `detect`/`applyToGenerate` call was previously unexercised by any test).

## Design Notes

Sketch (implementer may refine field names): `UserAgentTypePack` needs, at minimum, a way to contribute parse-side detection and generate-side templating without the parser/generator knowing about built-in vs. custom packs differently. One workable shape:

```kotlin
class UserAgentTypePack internal constructor(
    val id: String,
    val detect: (String) -> UserAgentInfo,       // returns a partially-populated info; parser merges non-null fields across all passed packs, first-pack-wins on conflict
    val applyToGenerate: (UserAgentInfo) -> String? = { null }, // optional per-pack string segment; null = contributes nothing
)
```
`UserAgentBrowserTypes`/etc. are pre-built instances using the existing detect/generate logic; a consumer builds a custom `UserAgentTypePack(id = "myThing", detect = { ua -> ... })` the same way. Keep `detect`/`applyToGenerate` constructible from a public factory (not `internal`) if `internal` blocks JS export — verify `@JsExport` compatibility with a public-facing constructor path early, since npm consumers must be able to build one. This same shape is what the deferred bot/AI-agent story will reuse — get it right here.

## Verification

**Commands:**
- `./gradlew :library:allTests` -- expected: all pass on every target
- `./gradlew :library:packJsPackage` (or the task Story 3.2 established for local packing) -- expected: produces an installable tarball
- `npm pack` output installed fresh in a scratch Node.js project (outside the repo, e.g. `/private/tmp/...`); build+bundle (esbuild/Terser) a call site using `UserAgentParser(UserAgentBrowserTypes)` vs. one using `UserAgentParser(UserAgentAllTypes)` and compare resulting size -- expected: single-pack call site measurably smaller

**Manual checks (if no CLI):**
- Confirm `webApp`/`androidApp`/`jvmApp`/`iosApp` are left exactly as-is (still referencing the old API, expected to be broken until Story 4.2) — this story does not touch them. *(Superseded during review pass 1: all 4 got a minimal compile-only fix instead — see Spec Change Log.)*

## Suggested Review Order

**The pack contract (entry point)**

- The shape every built-in and custom pack implements — start here to understand everything else.
  [`UserAgentTypePack.kt:37`](../../library/src/commonMain/kotlin/site/lempert/useragent/UserAgentTypePack.kt#L37)

**The factory functions (core API redesign)**

- `UserAgentParser` becomes a factory returning a closure over composed packs; no-args now returns an always-empty result, not an implicit `UserAgentAllTypes` fallback (the review-pass-1 fix).
  [`UserAgentParser.kt:39`](../../library/src/commonMain/kotlin/site/lempert/useragent/UserAgentParser.kt#L39)

- Symmetric generate-side factory; same no-packs-is-empty behavior.
  [`UserAgentGenerator.kt:37`](../../library/src/commonMain/kotlin/site/lempert/useragent/UserAgentGenerator.kt#L37)

- `UserAgentInfo` gains `bot`/`aiAgent`/`custom` fields — unused until the deferred bot/AI-agent story, but the shape is locked here.
  [`UserAgentInfo.kt:30`](../../library/src/commonMain/kotlin/site/lempert/useragent/UserAgentInfo.kt#L30)

**Built-in packs (one file each, for independent JS tree-shaking)**

- `UserAgentAllTypes` — the explicit "everything" pack; no longer an implicit default (review-pass-1 change).
  [`UserAgentAllTypesPack.kt:24`](../../library/src/commonMain/kotlin/site/lempert/useragent/UserAgentAllTypesPack.kt#L24)

- `UserAgentBrowserTypes` — the pack this story's bundle-size claim is measured against.
  [`UserAgentBrowserTypePack.kt:19`](../../library/src/commonMain/kotlin/site/lempert/useragent/UserAgentBrowserTypePack.kt#L19)

- Engine, OS, and device packs — same one-file-per-pack pattern.
  [`UserAgentEngineTypePack.kt:16`](../../library/src/commonMain/kotlin/site/lempert/useragent/UserAgentEngineTypePack.kt#L16), [`UserAgentOsTypePack.kt:15`](../../library/src/commonMain/kotlin/site/lempert/useragent/UserAgentOsTypePack.kt#L15), [`UserAgentDeviceTypePack.kt:18`](../../library/src/commonMain/kotlin/site/lempert/useragent/UserAgentDeviceTypePack.kt#L18)

**Codegen (rule tables split per pack)**

- Rule-table generation split into browser/OS/device-specific functions instead of one shared emitter — the build-time half of tree-shaking.
  [`build.gradle.kts:308`](../../library/build.gradle.kts#L308)

- Output directory is cleared before regenerating the (now 3, previously 1) generated files.
  [`build.gradle.kts:409`](../../library/build.gradle.kts#L409)

**Sample-app compile fix (review pass 1 — not original scope)**

- JVM/Android call the new factory syntax directly — these two are on the CI path, so this was the blocking fix.
  [`Main.kt:19`](../../jvmApp/src/main/kotlin/site/lempert/kmp_user_agent/Main.kt#L19), [`MainActivity.kt:51`](../../androidApp/src/main/kotlin/site/lempert/kmp_user_agent/MainActivity.kt#L51)

- Web calls the actual compiled JS shape (`UserAgentParser([pack.get()])`, not a guessed signature) — grounded in the real `.d.mts`.
  [`Greeting.tsx:18`](../../webApp/src/components/Greeting/Greeting.tsx#L18)

- iOS calls the actual generated Kotlin/Native Swift header shape, including the required-params quirk from lost Kotlin default-parameter interop.
  [`ContentView.swift:24`](../../iosApp/iosApp/ContentView.swift#L24)

**Tests (peripheral)**

- New exception-isolation coverage: a throwing custom pack must not take down a composed call — added on review, previously unexercised.
  [`UserAgentParserTest.kt:441`](../../library/src/commonTest/kotlin/site/lempert/useragent/UserAgentParserTest.kt#L441), [`UserAgentGeneratorTest.kt:557`](../../library/src/commonTest/kotlin/site/lempert/useragent/UserAgentGeneratorTest.kt#L557)
