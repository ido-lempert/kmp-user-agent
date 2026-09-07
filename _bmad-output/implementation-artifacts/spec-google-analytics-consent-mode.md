---
title: 'Google Analytics with Consent Mode v2 on docs-site'
type: 'feature'
created: '2026-09-07'
status: 'done'
review_loop_iteration: 0
context: []
baseline_commit: '28ad9e3a5ec470077bca257d9cde8529e65a80d7'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** `docs-site` (VitePress) has no analytics, so there is no visibility into traffic or which guide pages visitors actually use. The site's architecture spine (`_bmad-output/planning-artifacts/architecture/architecture-kmp-user-agent-2026-09-04-docs-site/ARCHITECTURE-SPINE.md`, Consistency Conventions) states "no analytics/tracking beyond whatever GitHub Pages provides by default" — this spec deliberately supersedes that line per explicit human request; the spine doc's convention row must be updated to match, not left contradicting the code.

**Approach:** Load Google's `gtag.js` (measurement ID `G-1TGW366KHQ`) site-wide via VitePress's `head` config, defaulting all four Consent Mode v2 signals (`ad_storage`, `ad_user_data`, `ad_personalization`, `analytics_storage`) to `denied` before `gtag('config', ...)` fires. Add a small accept/reject consent banner (a Vue component in a custom minimal theme extension) that calls `gtag('consent', 'update', ...)` on the visitor's choice and persists it in `localStorage` so the banner doesn't reappear once decided.

## Boundaries & Constraints

**Always:**
- Consent defaults are set (`gtag('consent', 'default', {...})`) in an inline `<head>` script that executes *before* the `gtag.js` loader script and before any `gtag('config', ...)` call — ordering is what makes Consent Mode v2 actually gate collection.
- All four signals (`ad_storage`, `ad_user_data`, `ad_personalization`, `analytics_storage`) default to `'denied'`. Nothing is granted until the visitor explicitly accepts.
- The banner shows on first visit (no stored choice), offers exactly two actions (Accept, Reject), and never reappears once a choice is stored in `localStorage` (read/write wrapped in try/catch — see `artifact`-grade guidance: storage can throw or be unavailable, must not break page render).
- On Accept: call `gtag('consent', 'update', { ad_storage: 'granted', ad_user_data: 'granted', ad_personalization: 'granted', analytics_storage: 'granted' })`. On Reject: leave all signals denied (no `consent` update call needed, but store the choice so the banner doesn't re-show).
- Implementation stays VitePress-idiomatic: `head` entries in `docs-site/.vitepress/config.ts` for the two script tags; a custom `docs-site/.vitepress/theme/index.ts` extending `DefaultTheme` to inject the banner component into the default theme's `layout-bottom` slot. No hand-edited generated HTML.
- Update `ARCHITECTURE-SPINE.md`'s "State & cross-cutting" Consistency Conventions row to reflect that analytics now runs deliberately, consent-gated.

**Ask First:** None — scope and consent behavior are fully specified above.

**Never:**
- Do not load `gtag.js` unconditionally without the consent-default script running first (defeats Consent Mode).
- Do not build a full CMP (multi-purpose category toggles, per-vendor consent, etc.) — two-button accept/reject is sufficient for this site's ad-hoc analytics use.
- Do not gate the banner's own rendering on consent (the banner itself must always be able to show, independent of prior tracking state).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| First visit | No `localStorage` consent key | Banner shows; all 4 consent signals denied; `gtag.js` still loads (queues events) but nothing is sent until consent updates | N/A |
| Visitor accepts | Click "Accept" | All 4 signals set to `granted` via `gtag('consent', 'update', ...)`; choice stored; banner hides | N/A |
| Visitor rejects | Click "Reject" | Signals remain denied; choice stored; banner hides | N/A |
| Returning visitor | `localStorage` has a stored choice | Banner does not render; if choice was "accept", consent update fires again on load (idempotent) so mode stays granted across sessions | N/A |
| `localStorage` unavailable/throws | Private browsing, blocked storage | Banner still renders correctly every visit (denied-by-default is safe); no thrown error breaks page render | try/catch around all storage reads/writes |

</frozen-after-approval>

## Code Map

- `docs-site/.vitepress/config.ts` -- add `head:` array with (1) inline consent-default script, (2) async `gtag.js` loader script tag, (3) inline `gtag('js', ...)` / `gtag('config', 'G-1TGW366KHQ')` script. No existing `head` key today — safe to add.
- `docs-site/.vitepress/theme/index.ts` -- NEW. Does not exist yet (site currently uses VitePress's default theme with no override). Extends `DefaultTheme`, registers `ConsentBanner.vue` into the `layout-bottom` slot via a wrapped `Layout` render function (per VitePress's documented default-theme-extension pattern).
- `docs-site/src/theme/ConsentBanner.vue` -- NEW. Consent banner UI + `localStorage` read/write + `window.gtag('consent', 'update', ...)` calls on Accept.
- `_bmad-output/planning-artifacts/architecture/architecture-kmp-user-agent-2026-09-04-docs-site/ARCHITECTURE-SPINE.md` -- update the "State & cross-cutting" row in Consistency Conventions to note analytics is now present, consent-gated (Consent Mode v2), superseding the prior "no analytics" line.

## Tasks & Acceptance

**Execution:**
- [x] `docs-site/.vitepress/config.ts` -- add `head` array with consent-default + gtag loader + gtag config inline/external scripts, in that order -- makes Consent Mode v2 defaults apply before any tracking call
- [x] `docs-site/.vitepress/theme/index.ts` -- create, extending `DefaultTheme.Layout` with `ConsentBanner` in `layout-bottom` slot -- VitePress-idiomatic way to inject site-wide UI without touching generated HTML
- [x] `docs-site/src/theme/ConsentBanner.vue` -- create: on-mount localStorage check, Accept/Reject buttons, `gtag('consent', 'update', ...)` call, persisted choice -- delivers the actual consent gating UX
- [x] `ARCHITECTURE-SPINE.md` -- update Consistency Conventions "State & cross-cutting" row -- keeps the spine doc coherent with the new deliberate analytics decision

**Acceptance Criteria:**
- Given a first-time visitor with no stored consent choice, when any docs-site page loads, then the consent banner is visible and no `gtag('consent', 'update', ...)` call with granted signals has fired.
- Given a visitor clicks Accept, when the click handler runs, then `gtag('consent', 'update', ...)` is called with all four signals `'granted'`, the choice is persisted, and the banner hides.
- Given a visitor clicks Reject, when the click handler runs, then no signals are granted, the choice is persisted, and the banner hides.
- Given a returning visitor who previously accepted, when a new page loads, then the banner does not render and consent is re-affirmed as granted (no re-prompt).

## Spec Change Log

## Verification

**Commands:**
- `cd docs-site && npm run docs:dev` -- expected: dev server starts; loading any page in a browser shows the consent banner on first load (clear `localStorage` between checks), and the page `<head>` contains the `gtag.js` script tag plus the consent-default inline script preceding it (verify via browser devtools "View Source" / Elements, and confirm script order in the raw HTML response).
- `cd docs-site && npm run docs:build` -- expected: production build succeeds with the new theme override and head scripts included in `dist/`.

**Manual checks (if no CLI):**
- In browser devtools Network tab, confirm `gtag/js?id=G-1TGW366KHQ` is requested on every page load (queuing is fine pre-consent), and that no `collect`/`g/collect` analytics-hit request fires until after clicking Accept.

## Suggested Review Order

**Consent-gated script loading**

- Entry point: production-only `head` array injecting the consent-default, `gtag.js` loader, and `gtag('config', ...)` scripts in load-bearing order.
  [`config.ts:24`](../../docs-site/.vitepress/config.ts#L24)

- Single source of truth for the measurement ID, interpolated into both script bodies to avoid drift.
  [`config.ts:11`](../../docs-site/.vitepress/config.ts#L11)

- `NODE_ENV`-gated `src` attribute keeps dev/preview builds from firing real hits at the production GA property.
  [`config.ts:39`](../../docs-site/.vitepress/config.ts#L39)

**Consent decision logic**

- Framework-free decision function: given a stored choice, what the banner should show and what `gtag` call (if any) fires — this is what CI actually verifies.
  [`consentDecision.ts:49`](../../docs-site/src/theme/consentDecision.ts#L49)

- `ConsentBanner.vue` calls the decision function on mount rather than re-encoding the branching inline.
  [`ConsentBanner.vue:71`](../../docs-site/src/theme/ConsentBanner.vue#L71)

- `gtag` call wrapped so a throw can never block persisting the visitor's choice or hiding the banner.
  [`ConsentBanner.vue:13`](../../docs-site/src/theme/ConsentBanner.vue#L13)

**Banner UI & accessibility**

- Accurate `aria-label` (this persists via `localStorage`, not cookies).
  [`ConsentBanner.vue:119`](../../docs-site/src/theme/ConsentBanner.vue#L119)

- Focus moves to the banner when it becomes visible, for keyboard/screen-reader visibility.
  [`ConsentBanner.vue:88`](../../docs-site/src/theme/ConsentBanner.vue#L88)

- Accept/Reject buttons given equal visual weight — no default-choice dark pattern.
  [`ConsentBanner.vue:192`](../../docs-site/src/theme/ConsentBanner.vue#L192)

**Site-wide wiring**

- Injects `ConsentBanner` into every page via VitePress's documented `layout-bottom` slot — no hand-edited generated HTML.
  [`theme/index.ts:13`](../../docs-site/.vitepress/theme/index.ts#L13)

**CI protection**

- Byte-offset check that the consent-default script precedes the `gtag.js` loader in the built `dist/index.html` — protects the ordering the code comments call load-bearing.
  [`docs-deploy.yml:114`](../../.github/workflows/docs-deploy.yml#L114)

- Runs the new decision-logic verification script alongside the repo's existing `verify-generate-demo.mjs` pattern.
  [`docs-deploy.yml:152`](../../.github/workflows/docs-deploy.yml#L152)

**Architecture record**

- Updates the docs-site spine's "no analytics" convention to reflect this deliberate, consent-gated change.
  [`ARCHITECTURE-SPINE.md:87`](../../_bmad-output/planning-artifacts/architecture/architecture-kmp-user-agent-2026-09-04-docs-site/ARCHITECTURE-SPINE.md#L87)
