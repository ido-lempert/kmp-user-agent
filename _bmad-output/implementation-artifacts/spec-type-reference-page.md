---
title: 'Docs-Site Type Reference Page'
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

**Problem:** No page listed every name the library's six built-in type packs actually recognize, and the six packs aren't uniform: two (bot/AI-agent) are small hand-authored tables, one (engine) is a tiny derived table, and three (browser/os/device) are generated from hundreds of vendored regex rules -- a naive "list everything" approach would either dump unreadable raw regexes or hide that non-uniformity.

**Approach:** Build all six lists programmatically from the real compiled/generated source (never hand-transcribed) to avoid drift, at `docs-site/reference.md`. Mid-implementation, the review layer caught a serious factual error this intent summary itself must record: the bot/AI-agent tables document the library's current *source*, which is ahead of what's actually published (`0.2.0`, which has no bot/AI-agent detection at all) -- the page was rewritten with a prominent freshness caveat rather than silently shipping that error.

</frozen-after-approval>

## Suggested Review Order

**The freshness/accuracy caveat (the load-bearing fix from this round's review)**

- Entry point: the page intro, including the corrected claim that Bots/AI-agents document unreleased source, not the published `0.2.0` package every other section (and this site's own live demos) actually runs.
  [`reference.md:1`](../../docs-site/reference.md#L1)

- The matching correction on the homepage's own bot/AI-agent bullet, which had the same overclaim.
  [`index.md:29`](../../docs-site/index.md#L29)

**The six reference lists (verified against real source, not hand-typed)**

- Bots (26) and AI agents (20) -- full tables cross-checked row-by-row against `UserAgentBotTypePack.kt`/`UserAgentAIAgentTypePack.kt`, including the corrected OS-rule "fixed name" claim's sibling accuracy fix.
  [`reference.md:38`](../../docs-site/reference.md#L38)

- Browsers (191/433), Operating systems (36/204), Device brands (227/633) -- extracted programmatically from the build-generated rule tables, not transcribed, with reproduction steps for refreshing them later.
  [`reference.md:130`](../../docs-site/reference.md#L130)

**Wiring**

- Nav/sidebar: new "Reference" entry, moved into its own sidebar group (not nested under "Guide", since `/reference` isn't a `/guide/*` page).
  [`config.ts:15`](../../docs-site/.vitepress/config.ts#L15)

- Cross-links added from Core Concepts and every platform guide's "Next steps".
  [`core-concepts.md:74`](../../docs-site/guide/core-concepts.md#L74)
