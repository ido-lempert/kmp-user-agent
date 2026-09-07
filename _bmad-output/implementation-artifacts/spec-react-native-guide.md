---
title: 'React Native Usage Guide'
type: 'feature'
created: '2026-09-07'
status: 'done'
review_loop_iteration: 0
route: 'one-shot'
context:
  - '{project-root}/_bmad-output/specs/spec-docs-site/SPEC.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Whether the published `@lempert/user-agent` npm package actually works under React Native's Metro bundler and Hermes JS engine was an open, unverified question (logged in deferred-work.md back in the 2026-09-06 split) -- a guide couldn't honestly be written without answering it first.

**Approach:** Verify empirically before writing anything: install the real published package, bundle a real entry file through the actual `metro build` CLI, and execute the bundle on the real Hermes binary shipped inside `react-native`'s own SDK -- not Node, not a simulation. It works. A second check found Metro's default config doesn't tree-shake unused packs the way webpack/esbuild do, a real caveat the guide documents. Review also caught and fixed a genuine pre-existing bug this verification surfaced: `docs-site/guide/ios.md` claimed the same iPhone UA string parses to browser "Safari" when the real published package returns "Mobile Safari" -- confirmed directly, not assumed, and corrected.

</frozen-after-approval>

## Suggested Review Order

**The verification itself (why this guide could finally be written)**

- Entry point: the compatibility claim, explicitly scoped (standalone Metro+Hermes execution, not a full RN app shell) to avoid overclaiming.
  [`react-native.md:21`](../../docs-site/guide/react-native.md#L21)

- The corrected pre-existing bug this verification surfaced -- `ios.md` claimed "Safari" for a UA string the real package returns "Mobile Safari" for, confirmed via a direct Node run of the published package.
  [`ios.md:102`](../../docs-site/guide/ios.md#L102)

- The Metro tree-shaking caveat, with exact measured numbers (214,286 vs 215,363 bytes minified), correcting an unscoped claim on two other pages.
  [`react-native.md:97`](../../docs-site/guide/react-native.md#L97)

**Wiring and consistency**

- Cross-links and the "not a fifth platform" clarification added to `index.md` and `core-concepts.md`, plus nav/sidebar wiring.
  [`config.ts:16`](../../docs-site/.vitepress/config.ts#L16)

**Peripherals**

- `react-native-device-info` UA-source guidance, corrected mid-session from an initially wrong iOS-availability claim after checking that package's real docs.
  [`react-native.md:78`](../../docs-site/guide/react-native.md#L78)
