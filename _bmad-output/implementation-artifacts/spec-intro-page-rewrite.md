---
title: 'Docs-Site Intro Page Rewrite'
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

**Problem:** The docs-site homepage's intro was a single flat paragraph with no value-prop framing before the live demos, and the "how do I install this" links were the very first thing on the page rather than earning attention with a hook first.

**Approach:** Add a stronger hook paragraph and a "Why kmp-user-agent" value-prop section, reorder the page (hook -> value props -> live demos -> install links), and rename the demo section headings to match the platform guides' own "Parse"/"Generate" convention -- every new claim checked against the real library source and the other guide pages rather than written as marketing copy.

</frozen-after-approval>

## Suggested Review Order

**The value-prop claims (checked against source, not asserted)**

- Entry point: the hook paragraph and "Why kmp-user-agent" -- note the corrected "same logic and results" framing (not "the same call... compiles," which overclaimed cross-platform binding uniformity contradicted by `js.md`/`ios.md`'s own documented call-shape differences).
  [`index.md:6`](../../docs-site/index.md#L6)

- The bot/AI-agent bullet's added non-exhaustive-starter-list caveat, matching `core-concepts.md`'s existing language.
  [`index.md:29`](../../docs-site/index.md#L29)

- The "actually published" bullet, corrected to claim only what's true: the demos are backed by the npm package specifically, not "every demo" by Maven Central too.
  [`index.md:37`](../../docs-site/index.md#L37)

**Structure**

- Reordered sections and renamed demo headings to match the platform guides' "Parse"/"Generate" convention; install links moved to a scannable list at the bottom, with a same-page jump link added near the top for readers who came only to install.
  [`index.md:42`](../../docs-site/index.md#L42)

**Peripherals**

- Site-wide meta description updated to match the new hook framing.
  [`config.ts:8`](../../docs-site/.vitepress/config.ts#L8)
