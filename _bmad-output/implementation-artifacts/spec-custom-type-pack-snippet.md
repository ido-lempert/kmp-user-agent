---
title: 'Custom Type-Pack Docs Snippets'
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

**Problem:** Core Concepts' custom-pack example showed writing into `UserAgentInfo.custom` but never reading it back out, and had no example of the other extension style the user actually asked about -- populating a named field (like `browser`) directly to add or override detection, which has its own subtle pack-ordering gotcha.

**Approach:** Add a short read-back example to the existing `custom` section, then a new "Extending a named field instead of `custom`" section with a worked `acmeBrowserPack` example demonstrating first-pack-wins ordering -- verified live (via a temporary `jvmApp` swap) to produce exactly the claimed output in both pack orders before publishing.

</frozen-after-approval>

## Suggested Review Order

**The extension-style guidance**

- Entry point: when to reach for `custom` vs. a named field, and the hoisted-regex `acmeBrowserPack` example (fixed from an earlier draft that recompiled the regex per `detect()` call).
  [`core-concepts.md:153`](../../docs-site/guide/core-concepts.md#L153)

- The pack-ordering worked example -- both orderings' outputs verified live against the real library, not assumed.
  [`core-concepts.md:180`](../../docs-site/guide/core-concepts.md#L180)

**Peripherals**

- The read-back-out addition to the existing `custom` example, closing a gap tracked since Story 5.2's review.
  [`core-concepts.md:145`](../../docs-site/guide/core-concepts.md#L145)
