---
title: 'Live Parse Demo'
type: 'feature'
created: '2026-09-05'
status: 'done'
review_loop_iteration: 2
baseline_commit: '746fb6f0dbc38c97bb136410a58111e49e8f451a'
context:
  - '{project-root}/_bmad-output/specs/spec-docs-site/SPEC.md'
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-kmp-user-agent-2026-09-04-docs-site/ARCHITECTURE-SPINE.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The intro page makes claims about the library but gives a visitor no live proof it actually works — nothing on the page demonstrates real parsing.

**Approach:** Add a `ParseDemo.vue` component to the intro page that parses the visitor's own `navigator.userAgent` in the browser, via the real published `@lempert/user-agent` package, and renders the result.

## Boundaries & Constraints

**Always:**
- `navigator.userAgent` (or any browser-only global) is read only inside `onMounted` — never at the top level of `<script setup>`. Confirmed by direct testing: VitePress's SSR build pass does **not** crash on a top-level `navigator.userAgent` read (Node 21+'s own built-in `navigator` polyfill silently resolves to `"Node.js/22"`), so a top-level read would silently bake a wrong static value into the page rather than failing loudly.
- The parse call is exactly `UserAgentParser([UserAgentAllTypes.get()])(navigator.userAgent)` — array-wrapped pack argument with `.get()` on the getter-object export, matching `js.md`'s documented call shape. No parse logic is reimplemented.
- Render all six `UserAgentInfo` fields (`browser`, `engine`, `os`, `device`, `bot`, `aiAgent`), each with an explicit "not detected" state when `null` — never a blank space that could read as broken.
- The component is imported directly into `index.md`'s own `<script setup>` block (confirmed working with zero config) — no custom VitePress theme.
- Show a loading/placeholder state for the pre-mount (SSR-rendered) moment, since the real result only exists after `onMounted` runs client-side.

**Never:**
- Do not build `GenerateDemo` (Story 5.4).
- Do not add a custom VitePress theme or global component registration.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Page loaded in a real browser | Any real `navigator.userAgent` | Component mounts, parses it via the real package, renders the populated fields | N/A |
| Build-time SSR render pass | `vitepress build`, no browser | Static HTML shows the loading placeholder; build output has no `ReferenceError`/"is not defined" for this component | Exit code alone does not prove this — VitePress logs per-page SSR errors to stderr without failing the build, so build output must be grepped |
| Obscure/unrecognized `navigator.userAgent` | A UA string no built-in pack recognizes | Every field shows its explicit "not detected" state | N/A |

</frozen-after-approval>

## Code Map

- `docs-site/index.md` -- current content (title, description, links to Core Concepts and the JS guide); this story adds a new section here.
- `docs-site/guide/js.md:26-37` -- the documented, working browser call shape to replicate exactly: `import { UserAgentAllTypes, UserAgentParser } from '@lempert/user-agent'`, `UserAgentParser([UserAgentAllTypes.get()])`.
- Investigation this session (empirically verified, not from memory): a trivial `.vue` SFC imported via `<script setup>` directly into a `.md` file renders with zero VitePress config changes; `onMounted`-gated `navigator.userAgent` access builds clean with no SSR error, while an ungated `window.*` access throws `ReferenceError` during SSR (build still exits 0 -- must grep output, not trust `$?`).
- `library/src/commonMain/kotlin/site/lempert/useragent/UserAgentInfo.kt` -- confirmed field set to render: `browser`, `engine`, `os`, `device`, `bot`, `aiAgent` (each `Component`/`Device`, nullable).
- `.github/workflows/docs-deploy.yml:48-56` -- the existing `Build with VitePress` step (`working-directory: docs-site`, `run: npm run docs:build`) this story's new step follows; confirmed via reproduction that GitHub Actions only fails a `run:` step on non-zero exit code, and `vitepress build` exits 0 even when it logs an SSR `ReferenceError` to stderr for a broken page.

## Tasks & Acceptance

**Execution:**
- [x] `docs-site/src/demo/ParseDemo.vue` -- create; `onMounted`-gated `navigator.userAgent` read, calls `UserAgentParser([UserAgentAllTypes.get()])`, renders all six fields with an explicit per-field "not detected" fallback, shows a loading placeholder before mount. **KEEP** (see Spec Change Log): reproduce this exactly as the prior implementation had it — the component logic, rendering, and scoped styles were already correct and review found no fault with them.
- [x] `docs-site/index.md` -- edit; add a `<script setup>` import of `ParseDemo.vue` and embed `<ParseDemo />` under a new section (e.g. "Try it live"). **KEEP**: reproduce exactly as the prior implementation had it.
- [x] `.github/workflows/docs-deploy.yml` -- edit; add TWO checks after the build step (see review_loop_iteration 2 amendment): (a) a log-text grep for crash-style SSR errors, broadened and word-boundaried; (b) a check against the actual built `docs-site/.vitepress/dist/index.html` that specifically catches the silent-fake-value case a log grep cannot.
- [x] `docs-site/src/demo/ParseDemo.vue` -- edit; wrap the `onMounted` parse call in `try`/`catch` so a thrown error surfaces an explicit error state instead of leaving the demo stuck on "Detecting your browser…"/"Loading…" forever. **KEEP** everything else in this file exactly as-is (the onMounted-gated read, the call shape, the six-field rendering, the loading placeholder, the scoped styles) — this is the only change to make here.

**Acceptance Criteria:**
- Given the intro page loaded in a real browser, when it finishes loading, then the visitor's own browser is shown correctly identified (browser/engine/os/device/bot/aiAgent), sourced from the real `UserAgentParser` call, with no manual input required.
- Given the component's implementation, when inspected, then `navigator.userAgent` is read only inside `onMounted`, never at top-level `<script setup>`.
- Given a production build, when its full output is inspected (not just its exit code), then no `ReferenceError`/"is not defined" (or other crash-style error text) appears for the demo page.
- Given `ParseDemo.vue` deliberately regressed to read `window.*` (a genuinely crashing global) at top level, when `docs-deploy.yml` runs, then the log-text check fails the job.
- Given `ParseDemo.vue` deliberately regressed to read `navigator.userAgent` at top level (the silent-fake-value case — Node's own `navigator` polyfill means this does **not** crash or log an error), when `docs-deploy.yml` runs, then the dist-content check still fails the job, because a log grep alone cannot catch this case.
- Given `UserAgentParser(...)`/`parse(...)` throws inside `onMounted`, when the demo runs in a real browser, then an explicit error state is shown instead of the loading placeholder persisting forever.

## Spec Change Log

- **Finding (verification-gap, review_loop_iteration 1):** This story's own core safety invariant — never read a browser-only global outside `onMounted` — was documented in code comments and a spec Verification command, but never enforced by CI. The reviewer proved this empirically: reading `window.location.href` at top level in `ParseDemo.vue` still builds with exit code 0 (`docs-deploy.yml` only checks exit code), silently drops the whole component from the static HTML, and would deploy to production undetected.
- **Amended:** Added a new Execution task wiring an automated grep-on-build-output check into `.github/workflows/docs-deploy.yml`, and a fourth Acceptance Criterion requiring that check to actually fail the job on a reproduced regression. This is a CI-pipeline change, not a component-logic change.
- **Known-bad state avoided:** A future edit to `ParseDemo.vue` (or a Node-version change altering which browser globals Node happens to polyfill) silently shipping a broken or data-faking demo to the live site with a fully green Actions run.
- **KEEP:** `ParseDemo.vue`'s component logic (the `onMounted`-gated read, the exact `UserAgentParser([UserAgentAllTypes.get()])` call shape, the six-field rendering with "Not detected" fallbacks, the loading placeholder, the scoped styles using `--vp-c-*` theme variables) and `index.md`'s "Try it live" wiring were not at fault and should be reproduced as before, not redesigned.

- **Finding (verification-gap, review_loop_iteration 2):** The `review_loop_iteration 1` fix itself had a gap, proven empirically: `docs-deploy.yml`'s new grep only catches SSR *crashes* (`window.*`-style `ReferenceError`s). It does not catch `ParseDemo.vue`'s actual regression class — a top-level `navigator.userAgent` read — because Node 21+'s built-in `navigator` polyfill resolves `navigator.userAgent` to `"Node.js/<version>"` without throwing at all. That silently bakes a fake value into the static HTML with a fully green build AND a fully green log-grep check. Edge-case-hunter separately flagged the grep pattern itself as unscoped (no word boundaries, narrow error-type coverage) and blind-hunter flagged the unguarded `parse()` call (info stuck `null` forever on a thrown error) for a second consecutive round.
- **Amended:** Split the CI check into two: (a) the existing log-text grep, broadened to more crash-style error types and word-boundaried to reduce false positives; (b) a new check against the actually-built `docs-site/.vitepress/dist/index.html`, asserting it contains the SSR-rendered loading-placeholder text and does **not** contain `Node.js/` (the signature Node's `navigator` polyfill would bake in) — this is the check that closes the real gap, since it inspects the artifact itself rather than inferring from log text. Also added a `try`/`catch` around the `onMounted` parse call in `ParseDemo.vue`.
- **Known-bad state avoided:** The exact regression `ParseDemo.vue`'s own header comment warns about — a fake `navigator.userAgent`-derived value silently baked into the live docs page — shipping with a fully green Actions run, including the run that was supposed to prevent it.
- **KEEP:** Everything from the `review_loop_iteration 1` KEEP note still applies. Additionally, the `review_loop_iteration 1` `docs-deploy.yml` steps ("Build with VitePress" capturing output, "Check build output for silent SSR errors") are a correct foundation -- extend them, don't discard the capture-and-tee approach.

## Verification

**Commands:**
- `cd docs-site && npm ci && npm run docs:build 2>&1 | tee /tmp/docs-build.log && grep -iE "ReferenceError|is not defined" /tmp/docs-build.log` -- expected: build succeeds AND the `grep` finds nothing (empty output means clean; a match means an SSR error slipped through).
- Reproduce BOTH regression classes locally and confirm the two CI checks each catch the one the other cannot:
  1. Change `ParseDemo.vue` to read `window.location.href` at top level (a crashing global) -- confirm the log-text check would fail.
  2. Restore, then change `ParseDemo.vue` to read `navigator.userAgent` at top level instead of inside `onMounted` -- confirm the log-text check does NOT fail (no crash, no error text) but the new dist-content check DOES fail (finds `Node.js/` baked into `dist/index.html`, or is missing the loading-placeholder text).
  3. Restore fully, rebuild, and confirm both checks pass clean.
  Do not skip step 2 -- it's the actual gap this loopback exists to close.

**Manual checks (if no CLI):**
- Serve the built `dist/` locally, load the intro page in an actual browser, and confirm the demo shows real values (not the loading placeholder, not "Node.js/..." or any server-side artifact).

## Suggested Review Order

**The core SSR-safety fix (why this story took three review rounds)**

- Why `navigator.userAgent` must stay inside `onMounted`, and what happens if it doesn't.
  [`ParseDemo.vue:6`](../../docs-site/src/demo/ParseDemo.vue#L6)

- The two-check split: log-text grep for crashes, plus a dist-content check for the silent-fake-value case a log grep structurally cannot catch.
  [`docs-deploy.yml:22`](../../.github/workflows/docs-deploy.yml#L22)

- The check that actually closes the gap — inspects the built artifact directly rather than inferring from log text.
  [`docs-deploy.yml:38`](../../.github/workflows/docs-deploy.yml#L38)

**The demo component**

- The guarded parse call: exact documented call shape, wrapped in try/catch so a thrown error surfaces instead of hanging forever.
  [`ParseDemo.vue:19`](../../docs-site/src/demo/ParseDemo.vue#L19)

- Field rendering with explicit loading/error/not-detected states.
  [`ParseDemo.vue:62`](../../docs-site/src/demo/ParseDemo.vue#L62)

**Peripherals**

- Wiring into the intro page, plus the noscript fallback.
  [`index.md:1`](../../docs-site/index.md#L1)
