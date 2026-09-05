---
id: SPEC-docs-site
companions: ['../../planning-artifacts/architecture/architecture-kmp-user-agent-2026-09-04-docs-site/ARCHITECTURE-SPINE.md']
sources: []
---

> **Canonical contract.** This SPEC and the files in `companions:` are the complete, preservation-validated contract for what to build, test, and validate. Source documents listed in frontmatter are for traceability — consult them only if you need narrative rationale or prose color this contract intentionally omits.

# Spec — kmp-user-agent Docs Site

## Why

A vision to realize: `kmp-user-agent` ships parse/generate for four platforms behind one API, but a prospective adopter has no self-serve way to see it work or learn how to wire it into their own platform before committing. A GitHub Pages site closes that gap — an intro page that proves the library live (parses the visitor's own browser, generates a User-Agent from arbitrary filters) plus a usage guide per MVP platform (Android, iOS, JVM, JS) so "does this work, and how do I use it" is answered without cloning the repo.

## Capabilities

Sliced vertically per the Vertical Development method: each capability is a thin, complete, independently-deployable increment of the live site — never a horizontal layer like "all guide pages" or "the demo components" or "deployment" on its own.

- **CAP-1** Walking skeleton
  - **intent:** A visitor can reach a real, live docs site with working navigation/search and one correct, complete platform guide: Browser + Node.js usage of the JS package, covering both runtime consumption contexts.
  - **success:** After a push to `master`, `https://ido-lempert.github.io/kmp-user-agent/` is live, shows nav/sidebar/search, and its guide page demonstrates real, working code for both a browser consumption path and a Node.js consumption path — not a placeholder, and not just one runtime.

- **CAP-2** Live parse demo
  - **intent:** A visitor can see their own browser's User-Agent parsed in real time on the intro page, using the actual published library.
  - **success:** Loading the intro page in a real browser shows that browser correctly identified (browser/engine/os/device/bot/aiAgent fields), sourced from `@lempert/user-agent`, not a reimplementation.

- **CAP-3** Live generate demo
  - **intent:** A visitor can pick browser/engine/os/device filters and generate a User-Agent string via the real published library, with any filter left unset or empty randomized to a valid result rather than left blank or broken.
  - **success:** Leaving every filter empty still produces a plausible, non-degraded generated UA string on demand; pinning any subset of filters produces a valid result that reflects those choices.

- **CAP-4** Android usage guide
  - **intent:** An Android developer can go from zero to a working parse/generate call using only this guide.
  - **success:** The page is live, linked from nav, and its setup/usage steps are accurate against the library's actual public API.

- **CAP-5** iOS usage guide
  - **intent:** An iOS developer can go from zero to a working parse/generate call using only this guide.
  - **success:** Same bar as CAP-4, for iOS.

- **CAP-6** JVM usage guide
  - **intent:** A JVM developer can go from zero to a working parse/generate call using only this guide.
  - **success:** Same bar as CAP-4, for JVM.

- **CAP-7** Core-concepts guide
  - **intent:** A developer can understand the `UserAgentInfo` model and type-pack composition (`UserAgentParser`/`UserAgentGenerator(vararg packs)`) once, from one shared page linked by every platform guide, instead of each guide re-explaining it.
  - **success:** The page is live and linked from every platform guide's nav; a developer reading only this page plus one platform guide has everything needed to call parse/generate correctly.

## Constraints

- The live demo must consume the real published `@lempert/user-agent` npm package (pinned version) — never a hand-reimplemented parse/generate in site JS. Every interactive feature routes through what the published library can actually do, not what's convenient to fake.
- Hosting is GitHub Pages: static output only, no server-side component, no backend API. Every capability must be achievable as static assets plus client-side JS.
- The docs site must never become a dependency of the library — it is a consumer, one direction only; nothing in `library/` may reference `docs-site/`.
- Guide content must cover all four MVP platforms the library ships for (Android, iOS, JVM, JS) — a v1 documenting only JS/web is not acceptable even though the interactive demos are JS-only.
- The JS guide (CAP-1) must demonstrate real, working code for both a browser consumption path and a Node.js consumption path — a guide showing only one runtime is incomplete.
- The site must present as professional/expert-grade reference documentation (Angular-docs-like): persistent sidebar navigation, a main content area, and code snippets with one-click copy. Guide pages use VitePress's default theme — which already provides exactly this shape — not a custom or minimal one.

## Non-goals

- A Dokka-generated API reference (hand-written guides only for v1).
- A custom domain for GitHub Pages (serves at the default project-page URL).
- Bot/AI-agent packs as generate-demo filters, even though the library supports detecting/generating them (CAP-3 covers browser/engine/os/device only).
- Pre-merge CI (lint/typecheck) or a PR-preview deployment environment for the docs site (only publish-on-push-to-master exists for v1).
- A "known values" API exposed by the library itself. CAP-3's filter/randomizer options come from a hand-maintained matrix in the docs site, not a library change — exposing it from the library is future work against the library's own architecture, not this feature.

## Success signal

The site is live at `https://ido-lempert.github.io/kmp-user-agent/`, redeploying automatically on every push to `master` that touches `docs-site/**`. Without leaving the intro page, a first-time visitor sees their own browser correctly parsed and can generate a plausible User-Agent string from arbitrary or partial filters. A developer on any of the four MVP platforms can find a guide that gets them from zero to a working parse/generate call.

## Assumptions

- JS is the first platform guide written, as part of CAP-1's walking skeleton, because it's also what the live demos run on. No explicit ordering was given for the other three platform guides (CAP-4/5/6); assumed independent and order-flexible.
