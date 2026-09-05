# Epic 5 Context: kmp-user-agent Docs Site

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Prospective adopters currently have no self-serve way to confirm `kmp-user-agent` works or learn how to wire it into their own platform without cloning the repo. This epic delivers a live GitHub Pages docs site: an intro page proving the library live (parsing the visitor's own browser User-Agent, and generating a plausible UA string from arbitrary/partial filters) plus a usage guide for each MVP platform (Android, iOS, JVM, Browser+Node.js) and a shared core-concepts page explaining the model once. Stories are sliced vertically — each one is an independently deployable, demoable increment of the live site, not a horizontal layer (e.g. "all guide pages" or "the demo components" alone).

## Stories

- Story 5.1: Walking Skeleton — Site Live with Browser+Node.js Guide
- Story 5.2: Core-Concepts Guide
- Story 5.3: Live Parse Demo
- Story 5.4: Live Generate Demo
- Story 5.5: Android Usage Guide
- Story 5.6: iOS Usage Guide
- Story 5.7: JVM Usage Guide

## Requirements & Constraints

- The live demo must consume the real published `@lempert/user-agent` npm package (pinned version) — never a hand-reimplemented parse/generate in site JS. Every interactive feature routes through what the published library can actually do.
- Hosting is GitHub Pages: static output only, no server-side component or backend API. Every capability must be achievable as static assets plus client-side JS.
- The docs site must never become a dependency of the library — it is a consumer only; nothing under `library/` may reference `docs-site/`.
- Guide content must cover all four MVP platforms (Android, iOS, JVM, JS) — documenting only JS/web is not acceptable, even though the interactive demos are JS-only.
- The JS guide must demonstrate real, working code for both a browser consumption path (bundler import and/or CDN script tag) and a Node.js consumption path (import/require) — one runtime alone is incomplete.
- The site must read as professional/expert-grade reference documentation (Angular-docs-like): persistent sidebar nav, a main content area, and code snippets with one-click copy. Use VitePress's default theme, which already provides this shape — not a custom or minimal theme.
- Success signal: the site is live at `https://ido-lempert.github.io/kmp-user-agent/`, redeploying automatically on every push to `master` touching `docs-site/**`. A first-time visitor sees their own browser correctly parsed and can generate a plausible UA string without leaving the intro page. A developer on any of the four MVP platforms can reach a guide that gets them from zero to a working parse/generate call.
- Non-goals for this epic: a Dokka-generated API reference; a custom domain; bot/AI-agent packs as generate-demo filters (even though the library supports them); pre-merge CI or PR-preview deployments; a "known values" API exposed by the library itself (the generate-demo matrix is hand-maintained in the docs site, not a library change).

## Technical Decisions

- **Stack:** VitePress 1.6.4 (Vite + Vue 3, Markdown-first) as the static-site generator; two Vue single-file components (`ParseDemo.vue`, `GenerateDemo.vue`) embedded in Markdown pages for the only real interactivity, hydrated client-side like the rest of the VitePress site.
- **Isolation:** `docs-site/` is a standalone Node/Vite project living entirely outside `settings.gradle.kts`, with its own `package.json`/`package-lock.json`. Its only link to the library is a normal npm dependency on the published `@lempert/user-agent` package — never in-repo Kotlin source, a Gradle project reference, or a local `file:` dependency.
- **Pinned dependency:** `docs-site/package.json` declares an exact/caret-pinned version of `@lempert/user-agent` starting at `0.2.0`, never `latest`; `docs-site/package-lock.json` is committed; the build workflow runs `npm ci`, never `npm install`. Bumping the version is a deliberate commit that must also re-check/update `generateSupportMatrix.ts` against the new release's `UserAgentGenerator.kt` in the same PR.
- **Public API being consumed:** `UserAgentInfo(browser: Component?, engine: Component?, os: Component?, device: Device?, bot: Component?, aiAgent: Component?)`, `Component(name: String?, version: String?)`, `Device(brand: String?, model: String?, name: String?)`. Entry points are two factory functions: `UserAgentParser(vararg packs: UserAgentTypePack): (String) -> UserAgentInfo` and `UserAgentGenerator(vararg packs: UserAgentTypePack): (UserAgentInfo) -> String`. Passing no packs yields an always-empty result — there is no implicit `UserAgentAllTypes` fallback. Built-in packs: `UserAgentBrowserTypes`, `UserAgentEngineTypes`, `UserAgentOsTypes`, `UserAgentDeviceTypes`, `UserAgentBotTypes`, `UserAgentAIAgentTypes`, plus the bundling `UserAgentAllTypes`.
- **ParseDemo:** calls `UserAgentParser(UserAgentAllTypes)` on `navigator.userAgent` with no manual input; renders `browser`/`engine`/`os`/`device`/`bot`/`aiAgent`. No parse logic is reimplemented in site JS.
- **GenerateDemo:** calls `UserAgentGenerator(UserAgentBrowserTypes, UserAgentEngineTypes, UserAgentOsTypes, UserAgentDeviceTypes)` — exactly these four packs, matching what the matrix covers (bot/AI-agent packs are explicitly out of scope for the generate demo in v1).
- **`generateSupportMatrix.ts`** (`docs-site/src/demo/`) is the single source of truth for GenerateDemo's filter options: named, complete `{browser, engine, os, device}` presets, each already confirmed to produce non-degraded `generate()` output — never independent per-field option arrays cross-producted by the component. Values are plain primitives (strings), never `Component`/`Device` instances; the matrix file never imports from `@lempert/user-agent`. `GenerateDemo.vue` is what constructs `Component`/`Device`/`UserAgentInfo` instances from those primitives, immediately before calling `UserAgentGenerator`. Its header comment must point back at `UserAgentGenerator.kt`'s `generateOsToken`/`unsafeCombination` check as ground truth.
- **Randomize-unset behavior:** pick a random preset, keep the visitor's own selections for fields they set, fill only the unset fields from that preset. If the visitor's selections plus a preset's remaining fields would recreate an `unsafeCombination` (confirmed: Firefox+Android, Firefox+iOS, Safari+Android — these silently drop the OS token), fall back to a different preset rather than emitting a degraded result. Leaving every filter empty must still produce a plausible, non-degraded UA string.
- **Deployment:** a GitHub Actions workflow triggers on push to `master` with a path filter covering `docs-site/**` and the workflow file; it runs `npm ci`, builds the VitePress site, packages output with `actions/upload-pages-artifact@v5`, and publishes with `actions/deploy-pages@v5` (re-verify these are still current/non-breaking before wiring — VitePress's own guide may show older major versions). Repo Pages source must be set to "GitHub Actions" (not a branch).
- **Base path:** `docs-site/.vitepress/config.ts` must set `base: '/kmp-user-agent/'` (project-page subpath) — not the VitePress default `base: '/'` — or the deployed site 404s.
- **Structural layout:** guide pages at `docs-site/guide/{android,ios,jvm,js,core-concepts}.md`; demo components at `docs-site/src/demo/`; intro/demo page at `docs-site/index.md`. No naming overlap with the library's `library/` module.
- **State:** demo components hold only local UI state (selected filters); no site-side persistence, no analytics/tracking beyond GitHub Pages defaults.
- Deferred (not in scope for this epic): a library "known values" API to derive the matrix automatically; Dokka API reference; custom domain; bot/AI-agent packs in the generate demo; pre-merge CI or PR-preview deploys; a fixed testing convention for the demo components.

## Cross-Story Dependencies

- Story 5.1 (walking skeleton) establishes the VitePress project, deploy pipeline, base path, and the first guide (JS); every later story builds on this scaffold.
- Story 5.2 (core-concepts) must exist before/alongside the platform guides (5.1's JS guide, 5.5 Android, 5.6 iOS, 5.7 JVM) since each of those links to it instead of re-explaining the shared model — do not duplicate model explanations across guide pages.
- Story 5.3 (parse demo) and Story 5.4 (generate demo) both live on the intro page (`index.md`) established in 5.1, and both depend on the pinned npm package wiring from 5.1.
- Story 5.4 depends on `generateSupportMatrix.ts` being authored correctly against `UserAgentGenerator.kt`'s `unsafeCombination` logic — this file has no other producer in this epic.
- Stories 5.5, 5.6, 5.7 (Android/iOS/JVM guides) are independent of each other and order-flexible; no explicit sequencing was specified among them.
