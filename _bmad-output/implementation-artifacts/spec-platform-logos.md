---
title: 'Platform logos: sidebar guide icons + homepage "Supported platforms" cards'
type: 'feature'
created: '2026-09-07'
status: 'done'
review_loop_iteration: 0
context: []
baseline_commit: 'd61386f4b02c2be241cbd8611c039ec7b11ef28b'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The docs-site's Guide sidebar entries (Core Concepts, Browser & Node.js (JS), React Native, Android, iOS, JVM) and homepage are text-only — no visual platform identity, unlike the existing detection-showcase icons further down the homepage.

**Approach:** Add a small platform icon to each per-platform sidebar link (JS, React Native, Android, iOS, JVM — not Core Concepts, which isn't platform-specific), and add a new, more prominent "Supported platforms" homepage section with bigger icon+text cards linking to each guide. Reuse this repo's already-established pattern exactly: version-pinned Simple Icons (CC0-1.0) served from `cdn.jsdelivr.net`, `title`/`alt` for accessibility, `onerror` hide-on-fail, no vendored icon files (see `spec-homepage-logo-showcase.md` and `docs-site/license.md`'s existing Simple Icons attribution). Top-nav icons are explicitly out of scope (human decision — VitePress nav items are plain-text-only and would need a custom component re-implementing active-link highlighting; not worth the regression risk for this ask).

## Boundaries & Constraints

**Always:**
- Icon source stays Simple Icons via `cdn.jsdelivr.net/npm/simple-icons@<pinned-version>/icons/<slug>.svg`, pinned to the same version already used elsewhere on this page (`13.21.0`) unless a slug genuinely requires a newer pin — verify each slug actually resolves (fetch/check) before using it, don't guess.
- Sidebar icons: VitePress's own `VPSidebarItem.vue` renders `item.text` via `v-html` (confirmed by reading `node_modules/vitepress/dist/client/theme-default/components/VPSidebarItem.vue`) — this means an inline `<img>` tag embedded directly in a `SidebarItem`'s `text` string in `config.ts` renders natively, with zero custom Vue component and zero risk to VitePress's existing active-page-highlighting logic (which operates on `link`, untouched). Use this mechanism; do not build a custom sidebar-item component.
- Suggested icon slugs (verify each resolves before use, adjust only if a slug 404s): Browser & Node.js (JS) → `javascript`; React Native → `react`; Android → `android`; iOS → `apple` (matches this site's existing iOS/macOS icon choice); JVM → `kotlin` (this is a Kotlin Multiplatform library; the JVM guide's audience is Kotlin/JVM consumers).
- Homepage "Supported platforms" section: bigger cards than the existing compact detection-icon chips (visually distinct — these represent what the library *runs on*, not what it *detects*), each with icon + platform name + one-line description + link to that guide. Place it prominently (e.g. right after "Why kmp-user-agent", before the detection showcase) since it's about the library's own identity, not a detection-result sample.
- Every icon keeps a `title`/`alt` naming the actual platform (accessibility parity with the existing homepage showcase).
- No icon files vendored into the repo — CDN-only, per this project's established supply-chain stance.
- Verify in a real browser (dev server) that sidebar icons render correctly, don't break active-page highlighting or collapse/expand behavior, and look acceptable in both light and dark mode, before considering this done.

**Ask First:** None — top-nav icons already excluded by explicit human decision; icon-slug choices are ordinary implementation judgment (verify resolution, don't guess), consistent with how the prior logo-showcase spec made similar calls (e.g. omitting Windows/Edge for missing marks).

**Never:**
- Never add icons to the top nav bar (`themeConfig.nav`) — out of scope per human decision.
- Never vendor icon files into the repository.
- Never use a slug without confirming it actually resolves against the pinned Simple Icons CDN version.
- Never add an icon to "Core Concepts" (not platform-specific) or duplicate/replace the existing detection-showcase section.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Sidebar renders normally | Visitor opens any guide page | Each platform-specific Guide sidebar item shows its icon before the text; Core Concepts has none | N/A |
| An icon URL 404s | CDN slug unresolvable (shouldn't happen if verified pre-ship, but as a safety net) | `onerror` hides just that image, text label still fully readable | Graceful degradation, matches existing homepage pattern |
| Visitor opens homepage | Loads `/` | Sees the new "Supported platforms" cards section with 5 platform cards, each linking to its guide | N/A |
| Dark mode | Visitor has dark theme active | Icon chip backgrounds/contrast remain legible (matches the fixed-light-chip-background fix already applied to the existing detection showcase for this exact reason) | N/A |

</frozen-after-approval>

## Code Map

- `docs-site/.vitepress/config.ts` -- edit: the `sidebar`'s `Guide` group items (`JS`/`React Native`/`Android`/`iOS`/`JVM`, not `Core Concepts`) each get an inline `<img>`-embedded `text` string instead of a plain string, per the `v-html` mechanism confirmed above.
- `docs-site/index.md` -- edit: add a new `## Supported platforms` section with 5 larger icon+text+link cards, placed after "Why kmp-user-agent" and before "A sample of what it detects". Reuse the existing inline-styled `<span>`/`<div role="list">` pattern from that same file (lines 65-77 for the visual convention) but sized up for card treatment and wrapped in `<a>` so each card links to its guide.
- `docs-site/license.md` -- read-only reference; the existing Simple Icons/CC0-1.0 attribution section already covers any Simple Icons usage site-wide, no new attribution text needed unless a genuinely new fact requires it.
- `_bmad-output/implementation-artifacts/spec-homepage-logo-showcase.md` -- read-only reference for the established pattern/conventions (CDN URL shape, dark-mode chip-background fix, accessibility approach) this spec must match.

## Tasks & Acceptance

**Execution:**
- [x] Verify each of the 5 suggested Simple Icons slugs resolves against `cdn.jsdelivr.net/npm/simple-icons@13.21.0/` (or the pinned version actually in use) -- confirms icon choices before wiring them in
- [x] `docs-site/.vitepress/config.ts` -- embed an icon `<img>` into each platform-specific Guide sidebar item's `text` -- delivers the sidebar icon ask
- [x] `docs-site/index.md` -- add the "Supported platforms" cards section -- delivers the homepage cards ask
- [x] Run the dev server and visually verify (light + dark mode) sidebar icons and homepage cards render correctly, active-page highlighting still works, and no layout regression -- required per this spec's Boundaries

**Acceptance Criteria:**
- Given a visitor is on any guide page, when they look at the sidebar, then each platform-specific entry (not Core Concepts) shows a small platform icon before its label.
- Given a visitor opens the homepage, when they scroll past "Why kmp-user-agent", then they see a "Supported platforms" section with 5 cards (JS, React Native, Android, iOS, JVM), each linking to the correct guide.
- Given the site is viewed in dark mode, when either the sidebar icons or the new cards render, then they remain legible (no invisible-dark-icon-on-dark-background regression).
- Given the sidebar's active-page highlighting, when a visitor navigates between guide pages, then the current page is still correctly highlighted (no regression from the `v-html` text change).

## Spec Change Log

## Verification

**Commands:**
- `cd docs-site && npm run docs:build` -- expected: production build succeeds with the updated config and homepage.
- `node scripts/verify-links-and-anchors.mjs` -- expected: passes (no new broken links/nav entries introduced).

**Manual checks (if no CLI):**
- `npm run docs:dev`, open the site in a real browser, click through each guide page to confirm sidebar icons render and active-page highlighting still works; toggle dark mode; view the homepage's new cards section at both desktop and narrow-viewport widths.

## Suggested Review Order

**Sidebar icons**

- Entry point: the `sidebarIcon()` helper and its wiring into the Guide sidebar's 5 platform entries.
  [`config.ts`](../../docs-site/.vitepress/config.ts)

- Confirmed via `node_modules` source inspection: renders through VitePress's own `v-html` sidebar-text mechanism, so active-page highlighting (which operates on `link`, untouched) can't regress.
  [`config.ts`](../../docs-site/.vitepress/config.ts)

**Homepage cards**

- The new "Supported platforms" section, placed before the existing detection showcase.
  [`index.md:48`](../../docs-site/index.md#L48)

- Accessibility fix: `role="listitem"` moved off the `<a>` onto a wrapping `<span>`, preserving each card's native link role.
  [`index.md:54`](../../docs-site/index.md#L54)

**Link and icon integrity (new CI protection)**

- Raw `href="..."` attributes in markdown are now scanned alongside markdown-syntax links — closes the gap that let the new cards' links bypass link-checking entirely.
  [`verify-links-and-anchors.mjs`](../../docs-site/scripts/verify-links-and-anchors.mjs)

- Icon-slug-to-platform-link assertion, protecting against a copy-paste icon/platform mismatch.
  [`verify-links-and-anchors.mjs`](../../docs-site/scripts/verify-links-and-anchors.mjs)

- Live CDN URL resolution check, covering every Simple Icons reference site-wide (new and pre-existing).
  [`verify-links-and-anchors.mjs`](../../docs-site/scripts/verify-links-and-anchors.mjs)

**Site wiring**

- `cdn.jsdelivr.net` preconnect hint moved from homepage-only to site-wide, since sidebar icons now load on every page.
  [`config.ts`](../../docs-site/.vitepress/config.ts)

- `license.md`'s Simple Icons attribution updated to name all three current usage locations.
  [`license.md`](../../docs-site/license.md)
