---
title: 'JVM Usage Guide'
type: 'feature'
created: '2026-09-06'
status: 'done'
review_loop_iteration: 0
route: 'one-shot'
context:
  - '{project-root}/_bmad-output/specs/spec-docs-site/SPEC.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Epic 5's docs site covered Android, iOS, and JS, but a JVM developer had no guide taking them from zero to a working parse/generate call — the last of the four MVP platforms.

**Approach:** Add `docs-site/guide/jvm.md` mirroring the Android guide's structure (Install/Parse/Generate/Next steps), but corrected for the real JVM difference verified live: `System.getProperty("http.agent")` (Android's approach) returns `null` on a stock JVM, so the guide instead points JVM developers — who are typically writing servers, not clients — at parsing an incoming request's `User-Agent` header.

</frozen-after-approval>

## Suggested Review Order

**The guide's core claims**

- Entry point: install (Gradle + Maven coordinates) and the JVM-version troubleshooting note.
  [`jvm.md:1`](../../docs-site/guide/jvm.md#L1)

- The real, verified difference from Android's guide — `http.agent` is unset on a plain JVM, so the guide points at incoming-request headers instead.
  [`jvm.md:45`](../../docs-site/guide/jvm.md#L45)

- Maven consumers need the per-target artifact id (`user-agent-jvm`), not the bare Gradle-only coordinate — confirmed via the local Maven publication output, not assumed.
  [`jvm.md:23`](../../docs-site/guide/jvm.md#L23)

**Nav wiring**

- Adds the JVM entry to nav/sidebar, and renames the ambiguous generic "Guide" label now that all four platform guides exist side by side.
  [`config.ts:15`](../../docs-site/.vitepress/config.ts#L15)

**Cross-links from sibling pages**

- Intro page now also points to the JVM guide.
  [`index.md:14`](../../docs-site/index.md#L14)

- Core Concepts' closing pointer now lists all four platform guides instead of only two.
  [`core-concepts.md:164`](../../docs-site/guide/core-concepts.md#L164)

**Peripherals**

- Sprint tracking and deferred-work log entries for this story.
  [`sprint-status.yaml:65`](../../_bmad-output/implementation-artifacts/sprint-status.yaml#L65)
