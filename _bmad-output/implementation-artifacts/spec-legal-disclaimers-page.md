---
title: 'Legal disclaimers page (trademarks, A11Y, privacy) for docs-site'
type: 'feature'
created: '2026-09-07'
status: 'done'
review_loop_iteration: 0
context: []
baseline_commit: '3713cdc0cff6d4e5e9e51fd65b5eb164b6da5ec5'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** `docs-site` has no page addressing trademark/logo attribution beyond `license.md`'s narrow "Simple Icons" section, no accessibility disclaimer, and no privacy/analytics disclosure — despite the site now running Google Analytics (`spec-google-analytics-consent-mode.md`, commit `7761a0a`) whose consent banner has no link explaining what's collected. This is a real, if modest, liability-exposure gap for a public, trademark-referencing, analytics-running site.

**Approach:** Add `docs-site/legal.md`, a single disclaimers page covering trademark/nominative-use, accessibility, privacy/analytics, and general no-warranty liability — all boilerplate, all traceable to facts already true in this repo, explicitly framed as not legal advice. Add it to the "Legal" nav/sidebar section alongside the existing License entry, cross-link both pages both ways, and update `ConsentBanner.vue`'s text to link here.

## Boundaries & Constraints

**Always:**
- Every factual claim on the page must trace to something actually true in this repo/session: the library parses/detects third-party browser/OS/device/bot/AI-agent identifiers (nominative use only, no affiliation/endorsement implied); the docs site is a standard VitePress default-theme site with no accessibility audit or WCAG-conformance certification performed; Google Analytics runs consent-gated (Consent Mode v2, all four signals default denied) per the already-shipped `ConsentBanner.vue`, and no other tracking exists on the site; Consent Mode v2 runs in "Advanced" mode — `gtag.js` loads and Google may receive cookieless conversion-modeling signals even pre-consent (per the already-logged deferred-work.md entry).
- The page opens with a clear, prominent statement that it is general boilerplate, not legal advice, and that the maintainer/reader should consult their own counsel for anything compliance-critical — matching `license.md`'s existing "not a legal opinion" framing/tone.
- The docs-site content's own license status is stated as an open/unstated question (per the human's explicit decision), not asserted as MIT, CC-BY, or any other license.
- Cross-link `license.md` <-> the new page in both directions (the new page references `license.md`'s existing trademark/logo section rather than duplicating it; `license.md`'s "Next steps" section, or an equivalent spot, gains a pointer to the new page).
- Add the new page to `docs-site/.vitepress/config.ts`'s existing "Legal" sidebar section (alongside "License") and to the main nav if `license.md` is itself nav-linked (mirror whatever `license.md` currently gets).
- Update `docs-site/src/theme/ConsentBanner.vue`'s banner text to link to the new page's privacy section, closing the exact gap the deferred-work.md entry names.
- No fabricated compliance claims: never state or imply GDPR/CCPA/ADA/WCAG "compliance" as an achieved, certified status — only good-faith, honest, proportionate statements about what's actually implemented.

**Ask First:** None — the one open decision (docs content licensing) was already resolved by the human before this spec was written: leave it explicitly unstated/flagged as open, not asserted.

**Never:**
- Never invent a docs-content license (MIT, CC-BY, or otherwise) — the human explicitly chose to leave this unstated.
- Never claim a specific WCAG conformance level (A/AA/AAA) that hasn't been tested/verified.
- Never claim GDPR/CCPA "compliance" as a certified/guaranteed status — describe actual behavior (consent-gated analytics) instead of asserting a compliance label.
- Never duplicate `license.md`'s existing MIT/Apache-2.0/Simple-Icons content wholesale — link to it instead.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Visitor reads the new page | Navigates to `/legal` | Sees trademark, A11Y, privacy, and liability sections, each with only claims traceable to real repo facts, and a "not legal advice" framing at the top | N/A |
| Visitor clicks the privacy link from the consent banner | Clicks the link added to `ConsentBanner.vue`'s text | Lands on the new page's privacy/analytics section specifically (anchor link) | N/A |
| Visitor checks docs content licensing | Reads the new page or `license.md` | Finds an honest "not yet decided" statement, not an invented license claim | N/A |

</frozen-after-approval>

## Code Map

- `docs-site/legal.md` -- NEW. The disclaimers page itself: trademark/nominative-use, accessibility, privacy/analytics (with an anchor for the consent-banner deep link), general liability, and the open docs-content-license note.
- `docs-site/license.md` -- read/edit: add a short cross-link to the new page (e.g. in "Next steps" or a new one-line pointer near the top), without duplicating its existing trademark/Simple-Icons section — that section stays the canonical source, the new page links to it.
- `docs-site/.vitepress/config.ts:19-72` -- edit: `themeConfig.nav` and `themeConfig.sidebar`'s existing `{ text: 'Legal', items: [{ text: 'License', link: '/license' }] }` block gains a second entry for `/legal`; mirror whatever nav treatment `license` already has (currently present in both `nav` and the `Legal` sidebar group).
- `docs-site/src/theme/ConsentBanner.vue:96-99` -- edit: the banner's `<p class="consent-banner__text">` currently reads "This site uses Google Analytics to understand how visitors use the docs. Analytics is off by default until you accept." with no link — add a link to the new page's privacy anchor (e.g. "See our [privacy notice](/legal#privacy) for details.").
- `_bmad-output/implementation-artifacts/deferred-work.md` -- read-only reference; this spec directly closes the entry beginning "Link the consent banner's text to the site's privacy/analytics disclosure page once it exists".

## Tasks & Acceptance

**Execution:**
- [x] `docs-site/legal.md` -- create with four sections (trademarks, accessibility, privacy/analytics, general liability) plus the not-legal-advice framing and the open docs-content-license note -- delivers the core disclaimers content
- [x] `docs-site/license.md` -- add a cross-link to the new page -- keeps the two legal pages discoverable from each other
- [x] `docs-site/.vitepress/config.ts` -- add `/legal` to nav and the `Legal` sidebar group -- makes the page reachable through the site's normal navigation
- [x] `docs-site/src/theme/ConsentBanner.vue` -- add a privacy-notice link to the banner text, pointing at `/legal#privacy` -- closes the exact deferred-work.md gap this spec was scoped to address

**Acceptance Criteria:**
- Given a visitor opens `/legal`, when they read it, then every factual claim (trademark nominative use, no A11Y certification, consent-gated analytics only, Advanced Consent Mode behavior) matches what's actually implemented in this repo, with no fabricated compliance claims.
- Given a visitor opens the consent banner on any page, when they read its text, then a working link to the new page's privacy section is present.
- Given a visitor is on `license.md`, when they look for related legal content, then a link to `/legal` is present; the reverse link exists from `/legal` to `license.md`'s trademark section rather than repeating it.
- Given the new page addresses docs-content licensing, when read, then it states the status as open/undecided rather than asserting a specific license.

## Spec Change Log

## Verification

**Commands:**
- `cd docs-site && npm run docs:build` -- expected: production build succeeds with the new page, nav/sidebar entries, and updated banner text included in `dist/`.

**Manual checks (if no CLI):**
- Read `docs-site/legal.md`'s built output and manually cross-check every factual claim against this session's actual changes (ConsentBanner.vue's real signal-default behavior, the actual absence of any A11Y audit, the actual trademark/nominative-use facts already stated in `license.md`) -- confirm nothing asserts more than what's true today.
- Click through the nav/sidebar to confirm `/legal` is reachable, and confirm the consent banner's new link resolves to the correct in-page anchor.

## Suggested Review Order

**Disclosure accuracy**

- Entry point: the Privacy & analytics section, including the fixed-clarified ad-signal disclosure (Consent Mode v2's four signals are granted as a package, not because the site runs ads).
  [`legal.md:52`](../../docs-site/legal.md#L52)

- General liability section's UA-spoofability caveat -- the one disclaimer specific to what this library actually does.
  [`legal.md:102`](../../docs-site/legal.md#L102)

- Accessibility section, reworded to acknowledge the real a11y work already in `ConsentBanner.vue` without overclaiming certification.
  [`legal.md:32`](../../docs-site/legal.md#L32)

- Trademarks section, cross-linking to `license.md`'s existing attribution rather than duplicating it.
  [`legal.md:16`](../../docs-site/legal.md#L16)

**Link integrity**

- Pinned anchor id on `license.md`'s Simple Icons heading, protecting the cross-link from a future heading reword.
  [`license.md:89`](../../docs-site/license.md#L89)

- New link/anchor validator, mirroring this repo's established `verify-*.mjs` CI pattern -- checks every internal markdown anchor and every nav/sidebar link resolves.
  [`verify-links-and-anchors.mjs:1`](../../docs-site/scripts/verify-links-and-anchors.mjs#L1)

**Site wiring**

- Consent banner's link text corrected to match its actual destination.
  [`ConsentBanner.vue`](../../docs-site/src/theme/ConsentBanner.vue)

- Nav's "Legal" entries grouped into a dropdown, matching the sidebar's existing grouping.
  [`config.ts:66`](../../docs-site/.vitepress/config.ts#L66)
