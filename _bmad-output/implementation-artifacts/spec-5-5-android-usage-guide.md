---
title: 'Android Usage Guide'
type: 'feature'
created: '2026-09-05'
status: 'done'
review_loop_iteration: 0
baseline_commit: '7615f112440a33b4bdf5f20ee58ccbfc9f6de4cd'
context:
  - '{project-root}/_bmad-output/specs/spec-docs-site/SPEC.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** An Android developer evaluating the library has no dedicated guide — only the JS guide and the platform-agnostic Core Concepts page exist.

**Approach:** Add `docs-site/guide/android.md`: real Maven Central coordinates, a runnable Kotlin parse+generate example verified against the repo's own working Android sample, and a link to Core Concepts instead of re-explaining the model — wired into nav/sidebar.

## Boundaries & Constraints

**Always:**
- Dependency snippet uses the real, verified Maven coordinates: `implementation("site.lempert:user-agent:0.2.0")` (confirmed in `library/build.gradle.kts`'s `mavenPublishing` block).
- The Kotlin API example matches plain Kotlin vararg syntax exactly as `androidApp/src/main/kotlin/site/lempert/kmp_user_agent/MainActivity.kt` actually calls it — `UserAgentParser(UserAgentAllTypes)(...)`/`UserAgentGenerator(UserAgentAllTypes)(...)`, no `.get()`/array-wrapping (that's JS-only, per `js.md`'s own "API shape in JS/TS" section — this guide has no equivalent section since Android needs none).
- For obtaining a real UA string to parse, show `System.getProperty("http.agent")` (the OS/device default, no permission required) rather than only a hardcoded literal like the sample app uses — a more realistic real-world starting point.
- Model/pack-composition explanation is NOT repeated here — link to Core Concepts (`/guide/core-concepts`) instead, matching `js.md`'s established pattern.
- Add `Android` to `docs-site/.vitepress/config.ts` nav and sidebar, after the existing JS guide entry.

**Never:**
- Do not claim any Android manifest permission or special setup is required — confirmed none exists (`AndroidManifest.xml` has zero `<uses-permission>` entries; parsing is pure string logic).
- Do not modify `js.md`, `core-concepts.md`, `ParseDemo.vue`, `GenerateDemo.vue`, or the deploy workflow.
- Do not build the iOS or JVM guides (Stories 5.6/5.7).

## Code Map

- `library/build.gradle.kts` (`mavenPublishing` block, ~line 625) -- confirmed coordinates `coordinates("site.lempert", "user-agent", "0.2.0")`.
- `library/build.gradle.kts` (`kotlin { android { ... } }` block, ~lines 495-568) -- confirmed `minSdk = 24`, `compileSdk = 36` (from `gradle/libs.versions.toml`) -- worth naming as the minimum supported Android API level.
- `androidApp/src/main/kotlin/site/lempert/kmp_user_agent/MainActivity.kt` -- the real, working call shape to mirror: `UserAgentParser(UserAgentAllTypes)(uaString)`, `UserAgentGenerator(UserAgentAllTypes)(UserAgentInfo(browser = Component(...), engine = Component(...), os = Component(...), device = null))`.
- `docs-site/guide/js.md` -- structural/voice pattern to follow: H1, install snippet, a runnable parse example, a runnable generate example, "Next steps" linking to Core Concepts. Skip the "API shape in JS/TS" section entirely (no platform quirks to explain for Kotlin/Android).
- `docs-site/.vitepress/config.ts:17-24` (`nav`/`sidebar`) -- add the Android entry here, after the JS guide.

## Tasks & Acceptance

**Execution:**
- [x] `docs-site/guide/android.md` -- create; Maven dependency snippet, a parse example using `System.getProperty("http.agent")`, a generate example, a link to Core Concepts instead of re-explaining the model.
- [x] `docs-site/.vitepress/config.ts` -- edit; add an `Android` nav/sidebar entry linking to `/guide/android`, after the JS guide entry.

**Acceptance Criteria:**
- Given the Android guide, when an Android developer follows its setup and usage steps, then they can add the dependency and successfully call parse/generate, verified accurate against the library's actual public API and the real sample app's call shape.
- Given the guide needs to explain the shared model or pack composition, when it does, then it links to Core Concepts rather than re-explaining it.
- Given the site's nav/sidebar, when inspected, then `Android` appears and resolves to a real page with no broken link.

## Spec Change Log

## Verification

**Commands:**
- `cd docs-site && npm ci && npm run docs:build 2>&1 | tee /tmp/docs-build.log && grep -iE '\b(ReferenceError|TypeError|SyntaxError)\b|\bis not defined\b' /tmp/docs-build.log` -- expected: build succeeds, `.vitepress/dist` includes `guide/android.html`, grep finds nothing.
- `node docs-site/scripts/verify-generate-demo.mjs` -- expected: still exits 0 (confirms this story didn't touch the generate demo's logic).

**Manual checks (if no CLI):**
- Serve the built `dist/` locally and confirm the `Android` nav link resolves (200, not 404) and Core Concepts link from the new page resolves too.

## Suggested Review Order

- The guide itself: install, minSdk-failure-mode note, and the two runnable examples.
  [`android.md:1`](../../docs-site/guide/android.md#L1)

- Nav/sidebar wiring.
  [`config.ts:20`](../../docs-site/.vitepress/config.ts#L20)

- Cross-links updated on the two pages that now need to mention both platform guides.
  [`index.md:10`](../../docs-site/index.md#L10)
  [`core-concepts.md:166`](../../docs-site/guide/core-concepts.md#L166)
