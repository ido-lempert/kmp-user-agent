---
title: 'Core-Concepts Guide'
type: 'feature'
created: '2026-09-05'
status: 'done'
review_loop_iteration: 0
baseline_commit: 'de5495f93cb5626106ce8421a6e85f9a88665cb5'
context:
  - '{project-root}/_bmad-output/specs/spec-docs-site/SPEC.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The `UserAgentInfo` model and type-pack composition pattern are currently explained only inline, in the JS guide's "Next steps" section — there is no shared page other platform guides (Android/iOS/JVM, Stories 5.5–5.7) can link to instead of re-explaining the same model themselves.

**Approach:** Add a platform-agnostic core-concepts guide page explaining the model and pack composition once; trim that duplicated explanation out of the JS guide and link to the new page instead; wire the new page into nav/sidebar.

## Boundaries & Constraints

**Always:**
- Core-concepts content is platform-agnostic: the `UserAgentInfo`/`Component`/`Device` shape and the canonical Kotlin `UserAgentParser(vararg packs)`/`UserAgentGenerator(vararg packs)` signature — no JS-specific binding detail (arrays instead of vararg, `.get()` on packs, real JS classes stay in `js.md`'s own "API shape in JS/TS" section, untouched).
- Every field name and behavior stated must match the real source exactly: `UserAgentInfo(browser, engine, os, device, bot, aiAgent, custom)`, `Component(name, version)`, `Device(brand, model, name)`; passing no packs returns an always-empty result — no implicit `UserAgentAllTypes` fallback.
- `js.md`'s generic model-explanation prose (the "Next steps" paragraph about fields staying `null`, the built-in pack list, the README pack-list link) moves to `core-concepts.md`; `js.md`'s "Next steps" links to the new page instead of repeating that content.
- Add `core-concepts` to `docs-site/.vitepress/config.ts` nav and sidebar, ordered before the JS guide (every platform guide depends on it conceptually).
- Write the page so it needs no edit when Stories 5.5–5.7 add Android/iOS/JVM guides — don't hardcode a "see also" list of platform guides that doesn't exist yet.

**Never:**
- Do not build the Android/iOS/JVM guides (Stories 5.5–5.7) or the demo components (Stories 5.3/5.4).
- Do not remove or alter `js.md`'s "API shape in JS/TS" section (JS-specific, stays where it is).

</frozen-after-approval>

## Code Map

- `library/src/commonMain/kotlin/site/lempert/useragent/UserAgentInfo.kt` -- confirmed exact model: `UserAgentInfo(browser, engine, os, device, bot, aiAgent, custom: Map<String, Component> = emptyMap())`, `Component(name, version)`, `Device(brand, model, name)`.
- `library/src/commonMain/kotlin/site/lempert/useragent/UserAgentParser.kt:31` -- confirmed signature `UserAgentParser(vararg packs: UserAgentTypePack): (String) -> UserAgentInfo`; no-packs case returns always-empty, no implicit fallback.
- `library/src/commonMain/kotlin/site/lempert/useragent/UserAgentTypePack.kt` -- confirmed `UserAgentTypePack(id, detect, applyToGenerate)` is the public custom-pack shape.
- `library/src/commonMain/kotlin/site/lempert/useragent/UserAgentAllTypesPack.kt` -- confirmed `UserAgentAllTypes` bundles browser/engine/os/device/bot/aiAgent.
- `docs-site/guide/js.md:115-123` ("Next steps" section) -- generic model-explanation content to move out; replace with a link to `core-concepts`.
- `docs-site/.vitepress/config.ts:17-24` (`nav`/`sidebar`) -- add the new page's entry here, before the existing `Guide` → JS item.

## Tasks & Acceptance

**Execution:**
- [x] `docs-site/guide/core-concepts.md` -- create; explain `UserAgentInfo`/`Component`/`Device` shape and the `UserAgentParser`/`UserAgentGenerator(vararg packs)` composition pattern (built-in pack names, no-packs-means-empty behavior, the `custom` extension point) against the real API.
- [x] `docs-site/guide/js.md` -- edit; replace the "Next steps" section's model-explanation prose with a short pointer to `core-concepts`, keeping the section itself.
- [x] `docs-site/.vitepress/config.ts` -- edit; add a `Core Concepts` nav/sidebar entry linking to `/guide/core-concepts`, ordered before the JS guide entry.

**Acceptance Criteria:**
- Given the core-concepts page, when a developer reads it, then it accurately explains the model and pack-composition pattern against the actual shipped API (matches Code Map's confirmed source).
- Given the JS guide, when its nav/content is inspected after this story, then it links to `core-concepts` instead of re-explaining the model, and its JS-specific "API shape in JS/TS" section is unchanged.
- Given the site's nav/sidebar, when inspected, then `Core Concepts` appears and resolves to a real page with no broken link.

## Spec Change Log

## Verification

**Commands:**
- `cd docs-site && npm ci && npm run docs:build` -- expected: build succeeds, `.vitepress/dist` includes the new `guide/core-concepts.html`, no errors.

**Manual checks (if no CLI):**
- Serve the built `dist/` locally and confirm the `Core Concepts` nav link and the JS guide's new link both resolve (200, not 404).

## Suggested Review Order

**The new Core Concepts page**

- Entry point: the model both directions of the API share, stated once.
  [`core-concepts.md:8`](../../docs-site/guide/core-concepts.md#L8)

- Pack composition and merge semantics — the most load-bearing explanation on the page, cross-checked against the real merge loop.
  [`core-concepts.md:40`](../../docs-site/guide/core-concepts.md#L40)

- The no-packs-means-empty behavior — a real API footgun worth calling out on its own.
  [`core-concepts.md:87`](../../docs-site/guide/core-concepts.md#L87)

- Custom-pack extension point, corrected during review to stop implying a parse-only example also handles generate.
  [`core-concepts.md:107`](../../docs-site/guide/core-concepts.md#L107)

**Wiring it in**

- `js.md`'s duplicated model explanation replaced with a pointer here — the actual de-duplication this story exists for.
  [`js.md:117`](../../docs-site/guide/js.md#L117)

- Nav/sidebar entry, ordered before the JS guide.
  [`config.ts:17`](../../docs-site/.vitepress/config.ts#L17)

- Homepage link, so the page is reachable without going through nav.
  [`index.md:5`](../../docs-site/index.md#L5)
