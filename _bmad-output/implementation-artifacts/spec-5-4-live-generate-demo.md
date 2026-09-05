---
title: 'Live Generate Demo'
type: 'feature'
created: '2026-09-05'
status: 'done'
review_loop_iteration: 1
baseline_commit: 'a66edc0c94f2149a1ab0860acf819de35e7f64ac'
context:
  - '{project-root}/_bmad-output/specs/spec-docs-site/SPEC.md'
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-kmp-user-agent-2026-09-04-docs-site/ARCHITECTURE-SPINE.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The intro page proves parsing works (Story 5.3) but gives no way to see the generate direction — a visitor can't pick filters and get a plausible User-Agent string back.

**Approach:** Add a `GenerateDemo.vue` component: four filter selects (Browser/Engine/OS/Device), a Generate button, and a `generateSupportMatrix.ts` of named, complete presets that supplies values for whatever the visitor leaves unset — via the real published `@lempert/user-agent` package.

## Boundaries & Constraints

**Always:**
- `generateSupportMatrix.ts` exports **named, complete presets** — each a full `{browser, engine, os, device}` tuple already confirmed to produce a non-degraded `generate()` output (see the exact table in Design Notes) — never independent per-field arrays the component would cross-product itself (spine AD-3).
- Preset field values are **plain primitives** (strings, or `null`), never instances of the library's `Component`/`Device` classes; `generateSupportMatrix.ts` never imports from `@lempert/user-agent`. `GenerateDemo.vue` is what constructs `Component`/`Device`/`UserAgentInfo` instances from those primitives, immediately before calling `UserAgentGenerator`.
- Generate merge rule: for each of the 4 fields, use the visitor's explicit selection if set, otherwise the field from a randomly-chosen preset. If the resulting `(browser.name, os.name)` pair is one of the three `unsafeCombination`s (`Firefox`+`Android`, `Firefox`+`iOS`, `Safari`+`Android` — confirmed in `UserAgentGenerator.kt`'s `generateOsToken`), retry with a different preset for the fields still unset by the visitor. If the visitor's **own** two explicit selections (not the preset) already form an unsafe pair, do not retry forever — generate anyway (see Never; this is expected, not a bug).
- Calls exactly `UserAgentGenerator(UserAgentBrowserTypes, UserAgentEngineTypes, UserAgentOsTypes, UserAgentDeviceTypes)` — the four packs the matrix covers, matching `ParseDemo.vue`'s established `UserAgentAllTypes` precedent is NOT used here (spine AD-2 pins this exact four-pack list for the generate demo).
- The initial page-load result (fully randomized, no visitor filters yet) is generated **inside `onMounted`**, never during setup/top-level render. `Math.random()` itself is not browser-only and would not crash SSR, but eagerly generating a result during SSR would bake one random value into the static HTML while client-side hydration computes a *different* one — a Vue hydration mismatch. Deferring to `onMounted` avoids this entirely (this component has no `navigator`/`window` dependency at all, so it does not share `ParseDemo.vue`'s SSR-crash concern — this hydration-mismatch risk is a different, `Math.random()`-specific hazard).
- Dropdown option lists are **derived from `presets`** (unique browser/engine/os/device values across the table), not hand-duplicated separately — keeps the visitor's selectable options and the presets' data from drifting apart.

**Never:**
- Do not silently override a visitor's own explicit filter selections to avoid an unsafe combination — respect their choice even if it produces a terser (OS-token-less) but still valid string. `generateOsToken` never throws for this case; it just omits the OS parenthetical.
- Do not add bot/AI-agent packs or fields to this demo (out of scope for v1, per spec-docs-site Non-goals).
- Do not modify `ParseDemo.vue`, `index.md`'s existing "Try it live" section content, or the deploy workflow.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Page loads, no filters touched | Initial `onMounted` | A fully randomized, valid UA string appears (not blank, not the SSR placeholder) | N/A |
| All four filters left unset, Generate clicked | No selections | A random preset supplies every field; valid, non-degraded string | N/A |
| Visitor sets Browser=Firefox, leaves OS unset | Partial selection | Randomizer must never resolve OS to Android/iOS for this pick — retries presets until a safe OS is found | N/A |
| Visitor explicitly sets Browser=Firefox AND OS=Android | Both fields visitor-locked, forming an unsafe pair | Generates anyway; output has no OS parenthetical (terser, still valid) — not treated as an error | N/A |
| Visitor sets OS=Windows only | Partial selection | Any preset supplying Windows always pairs it with version "10" (the only version `generateOsToken` accepts) | N/A |

</frozen-after-approval>

## Code Map

- `library/src/commonMain/kotlin/site/lempert/useragent/UserAgentGenerator.kt` (`generateOsToken`, lines ~75-107) -- confirmed exact rules: Windows only accepts `version == "10"`; Mac OS X/iOS/Android require non-blank version; Linux ignores version entirely; `unsafeCombination` is exactly `(Firefox, Android/iOS)` or `(Safari, Android)`; iOS device token is `"iPad"` only when `device?.model == "iPad"`, else defaults to iPhone.
- `library/src/commonMain/kotlin/site/lempert/useragent/UserAgentGenerator.kt` (`generateBrowserSegment`, lines ~117-171) -- confirmed: Chrome/Firefox/Edge require non-blank `browser.version`; Safari's `AppleWebKit/` token comes from `engine.version` directly (not just a fallback), so Safari presets need a real, distinct engine version (e.g. WebKit's own numbering, not Safari's version number).
- `docs-site/src/demo/ParseDemo.vue` -- the established pattern this story follows: `onMounted`-gated generation, plain-primitive-to-class construction at the call site, scoped styles using `--vp-c-*` tokens. `GenerateDemo.vue` has no `navigator`/`window` dependency, so it does not need `ParseDemo`'s SSR-crash guards — only the hydration-mismatch guard (Boundaries, above).
- `docs-site/index.md` -- current content (intro, Core Concepts/JS guide links, `ParseDemo.vue`'s "Try it live" section) -- this story adds a new section below it.

### Confirmed-safe preset table (exact data to encode in `generateSupportMatrix.ts`)

| id | browser | engine | os | device |
|---|---|---|---|---|
| chrome-windows | Chrome 128.0 | Blink 128.0 | Windows 10 | — |
| chrome-mac | Chrome 128.0 | Blink 128.0 | Mac OS X 14.5 | — |
| chrome-android | Chrome 128.0 | Blink 128.0 | Android 14 | Google Pixel 8 (`model: "Pixel 8"`) |
| chrome-linux | Chrome 128.0 | Blink 128.0 | Linux (version ignored by generator; use `""`) | — |
| firefox-windows | Firefox 130.0 | Gecko 130.0 | Windows 10 | — |
| firefox-mac | Firefox 130.0 | Gecko 130.0 | Mac OS X 14.5 | — |
| firefox-linux | Firefox 130.0 | Gecko 130.0 | Linux (`""`) | — |
| safari-mac | Safari 17.5 | WebKit 605.1.15 | Mac OS X 14.5 | — |
| safari-iphone | Safari 17.5 | WebKit 605.1.15 | iOS 17.5 | — (defaults to iPhone token) |
| safari-ipad | Safari 17.5 | WebKit 605.1.15 | iOS 17.5 | `model: "iPad"` |
| edge-windows | Edge 128.0 | Blink 128.0 | Windows 10 | — |
| edge-mac | Edge 128.0 | Blink 128.0 | Mac OS X 14.5 | — |

Deliberately excluded (would hit `unsafeCombination` or look confusing): Firefox+Android, Firefox+iOS, Safari+Android, Chrome/Edge+iOS (technically safe per the generator but not a realistic pairing for a credible demo).

## Tasks & Acceptance

**Execution:**
- [x] `docs-site/src/demo/generateSupportMatrix.ts` -- create; export the 12-row preset table above as plain-primitive objects, plus the `unsafeCombinations` list. No import from `@lempert/user-agent`.
- [x] `docs-site/src/demo/GenerateDemo.vue` -- create; four filter `<select>`s (options derived from `presets`), a Generate button, `onMounted`-triggered initial randomized generate, the merge-and-retry-on-unsafe-combination logic, construction of real `Component`/`Device`/`UserAgentInfo` instances from primitives, and the `UserAgentGenerator(UserAgentBrowserTypes, UserAgentEngineTypes, UserAgentOsTypes, UserAgentDeviceTypes)` call.
- [x] `docs-site/index.md` -- edit; import and embed `<GenerateDemo />` in a new section below the existing "Try it live" (`ParseDemo`) section.
- [x] `docs-site/src/demo/generateSupportMatrix.ts` -- edit; extract the existing merge-and-retry logic out of `GenerateDemo.vue` into an exported pure function here (e.g. `resolveGenerateFields(explicit, candidatesInRandomOrder)` returning the merged `{browser, engine, os, device}`) so it can be exercised outside a Vue component, with no behavior change.
- [x] `docs-site/src/demo/GenerateDemo.vue` -- edit; call the extracted `resolveGenerateFields` instead of inlining the merge logic. **KEEP** everything else exactly as-is (dropdowns, option derivation, `onMounted` trigger, instance construction, styles) — this is a pure refactor, not a redesign.
- [x] `docs-site/scripts/verify-generate-demo.mjs` -- create (or under a `docs-site/` subfolder of your choice); a plain Node script (no new test framework/dependency) that imports `resolveGenerateFields`/`presets` and the real `@lempert/user-agent`, and asserts: (a) every preset produces non-degraded output; (b) forcing an explicit `browser` actually appears in the generated string's browser token across many resolutions (the specific check that catches an operand-order regression like `preset.browser ?? explicit` instead of `explicit ?? preset.browser`); (c) forcing `browser=Firefox` never resolves to an Android/iOS `os` across many trials; (d) the forced Firefox+Android case still produces a non-empty, OS-token-less string. Exit non-zero on any failed assertion.
- [x] `.github/workflows/docs-deploy.yml` -- edit; add a step after the existing build/check steps that runs the new verify script against the built/installed dependencies, failing the job on a non-zero exit.

**Acceptance Criteria:**
- Given the intro page loads, when `onMounted` runs, then a valid, non-degraded generated UA string appears without any visitor action.
- Given every filter left empty and Generate clicked repeatedly, when observed over several clicks, then every result is valid and non-degraded (never hits an excluded/unsafe combination).
- Given the visitor sets a subset of filters (e.g. Browser only) and clicks Generate, when the result is inspected, then the visitor's chosen field(s) are reflected exactly, and any unset field is filled from a preset that keeps the result safe.
- Given the visitor explicitly sets both Browser=Firefox and OS=Android and clicks Generate, when the result is inspected, then it is still a valid, non-throwing UA string (just without the OS parenthetical) — not blocked or silently overridden.
- Given `generateSupportMatrix.ts`, when inspected, then it contains only named complete presets (no independent per-field arrays) and never imports `@lempert/user-agent`.
- Given the new verify script, when it runs against a build with the merge logic's `??` operands deliberately swapped (simulating the regression this loopback exists to catch), then it fails (non-zero exit) — verified by actually reproducing this and confirming the failure, then restoring.
- Given `docs-deploy.yml`, when it runs on a clean build, then the new verify step passes; when run against the swapped-operand regression, it fails the job.

## Spec Change Log

- **Finding (verification-gap, review_loop_iteration 1):** No CI check (or test of any kind) exercises `GenerateDemo.vue`'s merge-and-retry logic, since it only runs client-side inside `onMounted` and never during the SSR build pass the two existing CI checks inspect. A plausible one-line regression (swapping `explicitBrowser.value ?? preset.browser` to `preset.browser ?? explicitBrowser.value`) would silently disable the demo's entire advertised feature — the visitor's filter choice — while still producing a valid, non-crashing string. Neither existing CI check would notice.
- **Amended:** Extract the merge logic into a plain, framework-free exported function (`resolveGenerateFields`) so it can be exercised by a plain Node verification script wired into `docs-deploy.yml` — no test framework/dependency introduced, consistent with the architecture spine's standing "testing convention not fixed here" deferral (this closes a specific, demonstrated regression path, not the general testing-convention gap).
- **Known-bad state avoided:** The demo's core "your filters win" behavior silently breaking and shipping to the live site with a fully green Actions run.
- **KEEP:** `GenerateDemo.vue`'s dropdowns, option derivation from `presets`, `onMounted` trigger, instance construction, and styles are correct as built — only the merge logic's *location* changes (extracted, not rewritten). `generateSupportMatrix.ts`'s preset table and `isUnsafeCombination` are correct as built, unchanged by this amendment beyond adding the extracted function.

## Design Notes

The retry-on-unsafe-combination logic only needs to vary the fields the visitor left unset — it should not attempt to change a field the visitor explicitly chose. A simple bounded retry (e.g. shuffle `presets` and try each until one produces a safe merged `(browser, os)` pair, or all are exhausted) is sufficient; there are always multiple safe presets per browser family among the 12 above, so this converges quickly in the normal (not-visitor-forced-unsafe) case.

## Verification

**Commands:**
- `cd docs-site && npm ci && npm run docs:build 2>&1 | tee /tmp/docs-build.log && grep -iE '\b(ReferenceError|TypeError|SyntaxError)\b|\bis not defined\b' /tmp/docs-build.log` -- expected: build succeeds, grep finds nothing.
- Run the new `docs-site/scripts/verify-generate-demo.mjs` directly -- expected: exits 0, all assertions pass.
- Reproduce the regression this loopback exists to catch: temporarily swap the `??` operand order in `resolveGenerateFields` (so a preset's field wins over the visitor's explicit selection), run the verify script, and confirm it fails. Restore, rerun, confirm it passes again. Do not skip this -- it is the actual acceptance test for this loopback.

**Manual checks (if no CLI):**
- Serve the built `dist/` locally, load the intro page, confirm an initial result appears without interaction, then try several filter combinations (including an intentionally unsafe one) and confirm the button always produces a plausible string.

## Suggested Review Order

**The merge logic and why it's testable (why this story needed a loopback)**

- The extracted, pure merge-and-retry function — read this first to understand the whole demo's core behavior.
  [`generateSupportMatrix.ts:235`](../../docs-site/src/demo/generateSupportMatrix.ts#L235)

- The plain Node script that exercises it against the real published library, specifically designed to catch a silent visitor-choice-gets-overridden regression.
  [`verify-generate-demo.mjs:93`](../../docs-site/scripts/verify-generate-demo.mjs#L93)

- Wired into the deploy pipeline, after the two SSR-safety checks from Story 5.3.
  [`docs-deploy.yml:106`](../../.github/workflows/docs-deploy.yml#L106)

**The preset table**

- The 12 confirmed-safe presets, verified line-by-line against the real generator.
  [`generateSupportMatrix.ts:82`](../../docs-site/src/demo/generateSupportMatrix.ts#L82)

- The three unsafe (browser, os) pairs the retry logic guards against.
  [`generateSupportMatrix.ts:76`](../../docs-site/src/demo/generateSupportMatrix.ts#L76)

**The component**

- `generate()`: shuffles presets, delegates to the pure merge function, constructs real library instances, calls the four-pack `UserAgentGenerator`, with error handling matching `ParseDemo.vue`.
  [`GenerateDemo.vue:103`](../../docs-site/src/demo/GenerateDemo.vue#L103)

- The four filter dropdowns, options derived from the preset table rather than hand-duplicated.
  [`GenerateDemo.vue:165`](../../docs-site/src/demo/GenerateDemo.vue#L165)

**Peripherals**

- Wiring into the intro page, below the existing parse demo.
  [`index.md:20`](../../docs-site/index.md#L20)
