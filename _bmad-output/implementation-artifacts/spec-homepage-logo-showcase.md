---
title: 'Homepage Brand Logo Showcase'
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

**Problem:** The docs site had no visual showcase of the real browsers, OSes, bots, and AI agents the library detects -- text-only lists (Type Reference) don't give a prospective adopter the quick, scannable "oh, it knows about X" confirmation a logo row does.

**Approach:** Add a "A sample of what it detects" section to the homepage referencing brand icons from Simple Icons (CC0-1.0) via a version-pinned jsdelivr CDN URL -- explicitly chosen over vendoring icon files into the repo, per direct human instruction weighing this against the project's standing aversion to vendoring third-party assets. Two review rounds (the first stalled mid-run and was relaunched with a lighter prompt) caught and fixed real defects: icons would have been invisible in dark mode (default-black SVGs on a near-black theme chip background), the specific detected-entity name was never visible to a sighted user, and two operators' bot/AI-agent table entries were confusingly spliced into one alt text.

</frozen-after-approval>

## Suggested Review Order

**Legibility and accuracy fixes (the load-bearing corrections from review)**

- Entry point: the fixed light chip background (`#f6f6f7`, not the theme-adaptive `--vp-c-bg-soft`) -- confirmed via the built CSS that dark mode's value (`#202127`) would have made every default-black icon invisible.
  [`index.md:62`](../../docs-site/index.md#L62)

- The bots/AI-agents split into two separate labeled rows (was one combined row), each chip now mapping to exactly one Type Reference table entry instead of splicing two together.
  [`index.md:80`](../../docs-site/index.md#L80)

- The missing-logo callouts (Windows, Microsoft Edge) -- verified absent from Simple Icons directly, not just omitted silently.
  [`index.md:76`](../../docs-site/index.md#L76)

**Wiring and attribution**

- `preconnect` hint for the CDN, and the new Simple Icons/CC0 attribution section on the License page (License previously covered only the uap-core attribution).
  [`license.md:89`](../../docs-site/license.md#L89)

**Peripherals**

- Accessibility: `role="list"`/`aria-label` grouping and `loading="lazy"`/`onerror` graceful-degradation on all 21 image references.
  [`index.md:62`](../../docs-site/index.md#L62)
