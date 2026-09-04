# Sprint Change Proposal — kmp-user-agent

**Date:** 2026-09-04
**Trigger:** Hands-on validation of the published npm package in a clean Node.js project (the manual-verification work Story 3.3 calls for), surfacing dissatisfaction with the shipped public API shape.
**Mode:** Batch (single pass; user asked for speed over ceremony)

## 1. Issue Summary

Epics 1–3 are built and already published: `UserAgentParser.parse(String): UserAgentInfo` and `UserAgentGenerator.generate(UserAgentInfo): String` are live on Maven Central and npm (commits `78d5db9`…`6556be9`). Trying the npm package cold, in a real Node project, surfaced two real problems with that shape (AD-2/AD-3):

1. **No bundle-size control.** The whole rule table (browser + OS + device, one monolithic compiled object) always loads together. A JS consumer who only wants browser detection still pays for everything.
2. **No extensibility.** Bot detection and AI-agent detection don't exist in the library at all, and there's no way for a consumer to add their own detection categories short of forking.

Desired replacement, in the user's own words: `UserAgentParser(...types)` / a generate equivalent — factory functions that take a spread of **type packs** and return the actual parse/generate function. Ship a few built-in packs (`UserAgentBrowserTypes`, `UserAgentEngineTypes`, `UserAgentBotTypes`, `UserAgentAIAgentTypes`), a convenience `UserAgentAllTypes` bundling them, and let consumers write their own packs to extend it. A consumer imports only what they need → smaller JS bundle; anyone can add new detection categories without forking.

Explicit process instruction from the user: skip a heavy story-planning cycle. Get a small, fast version working for the npm/Node consumption path first (since that's the concrete path just exercised), then extend depth and re-validate parity across the other targets/registries.

## 2. Impact Analysis

**Epic impact**

- **Epic 1 (Parse) & Epic 2 (Generate)** — done and shipped, but invalidated *at the API surface*. The actual matching engine (regex + uap-core replacement templates, first-match-wins) is sound and stays. What breaks: AD-3's singleton-object entry points, and AD-2's closed 4-field `UserAgentInfo`.
- **Epic 3 (Publish)** — Story 3.1 (Maven Central) and 3.2 (npm) already published artifacts under the old API; they'll need a new **major-version** release once the new API lands (breaking change). Story 3.3 (fresh-project verification) is what surfaced this and should be re-run against the new API rather than the old one.
- No epic becomes obsolete and no rollback is needed — this is a redesign of the public API contract, not a change in what the library does (still parse + generate, still one common Kotlin API across 4 targets).

**Artifact conflicts**

| Artifact | Conflict | Fix |
|---|---|---|
| `SPEC.md` (CAP-3, Constraints) | Implies one fixed-shape entry point; says nothing about tree-shakeability or extensibility | Add pack-based composability to CAP-3's intent; add a constraint requiring the JS build to support per-pack dead-code elimination |
| `ARCHITECTURE-SPINE.md` AD-1 | Rule data compiled as 3 inseparable ordered lists (`user_agent_parsers`/`os_parsers`/`device_parsers`), always loaded together; no bot/AI-agent categories exist | Compile rule data **per category**, each an independently referenceable top-level table so unused ones tree-shake out; add small hand-seeded Bot and AI-Agent tables (uap-core has no such sections to vendor from) |
| `ARCHITECTURE-SPINE.md` AD-2 | `UserAgentInfo` is a closed 4-field data class | Add `bot`/`aiAgent` as first-class optional `Component` fields; add a small extension point for custom-pack data |
| `ARCHITECTURE-SPINE.md` AD-3 | "Stateless singleton, one canonical entry point" was written against the old fixed-shape API | Re-state as: stateless **factory functions** (`UserAgentParser(vararg packs)`, `UserAgentGenerator(vararg packs)`) returning closures — still no mutable shared state, still exactly one entry point per direction |
| `UserAgentParser.kt` / `UserAgentGenerator.kt` / `UserAgentInfo.kt` / Gradle codegen task | Built against the old shape | Rework (implementation, not architecture — goes to the dev story) |
| Published Maven Central / npm artifacts | Old API is live | Next release is a new major version; note in README/CHANGELOG |

**Technical impact:** contained to the `library` module. No new infra — Stories 3.1/3.2 already built the Maven Central + npm publish pipelines; they get reused, not rebuilt.

## 3. Recommended Approach

**Option 1 — Direct Adjustment**, not rollback, not MVP scope reduction.

- Update `SPEC.md` and `ARCHITECTURE-SPINE.md` directly (small, targeted diffs — Section 4 below), rather than re-deriving them from scratch.
- Replace the remaining unstarted/rework scope with **one small new epic (Epic 4), two stories** instead of reopening and individually rewriting Stories 1.1–2.3 and 3.1–3.3. This is the "small fast solution" the user asked for.
- Effort: **Medium**. Risk: **Low** — breaking the API now, pre-wide-adoption, is cheap; doing it after more consumers depend on the old shape would not be.

**Rationale:** The fix is genuinely scoped to the public API surface and how rule data is organized for loading — the regex/codegen paradigm, the 4-target common-Kotlin model, and the publish pipelines all stay intact. A full re-plan (new PRD, new epic breakdown ceremony) would cost more process than the change itself warrants, which is exactly what the user flagged.

## 4. Detailed Change Proposals

### 4.1 `SPEC.md`

**CAP-3 — OLD:**
> **intent:** A consumer calls one common KMP API for parse and generate that behaves identically from Android, iOS, JVM, and JS.

**CAP-3 — NEW:**
> **intent:** A consumer calls one common KMP API for parse and generate, composed from pluggable type packs (built-in: browser, engine, OS, device, bot, AI-agent; user-defined: custom packs), that behaves identically from Android, iOS, JVM, and JS. A consumer who imports only the packs they need gets a smaller JS bundle.

**New constraint, appended to Constraints:**
> The public API is a pair of factory functions — one for parse, one for generate — each taking a variadic list of type packs and returning the callable parse/generate function. Built-in packs are individually importable so the JS/npm build can dead-code-eliminate packs a consumer doesn't reference. Consumers may author their own packs following the same shape to add detection categories without forking the library.

### 4.2 `ARCHITECTURE-SPINE.md`

**AD-1 — amend:** rule tables move from 3 monolithic ordered lists to **one compiled table per pack** (browser, engine, os, device, bot, aiAgent — each its own top-level `commonMain` value), still generated at build time, still first-match-wins within a table. Bot and AI-Agent tables are **hand-seeded, not vendored** (uap-core has no such sections) — a small starter list (e.g. Googlebot/Bingbot/curl-class bots; GPTBot/ClaudeBot/PerplexityBot-class AI agents), explicitly not exhaustive, extensible via custom packs.

**AD-2 — amend:** `UserAgentInfo` gains `bot: Component?` and `aiAgent: Component?` as first-class fields alongside the existing `browser`/`engine`/`os`/`device`, plus a small extension point for data a custom pack contributes beyond the built-in fields. Exact shape of the extension point is a dev-story-level decision, not an architecture-level one.

**AD-3 — replace:**
> The public surface is two factory functions: `UserAgentParser(vararg packs: UserAgentTypePack): (String) -> UserAgentInfo` and `UserAgentGenerator(vararg packs: UserAgentTypePack): (UserAgentInfo) -> String`. Passing no packs is equivalent to passing `UserAgentAllTypes`. Still stateless: no mutable shared state, no caching beyond the read-only compiled tables from AD-1. Still exactly one entry point per direction — no coexisting alternate spelling.

**Naming note (recommendation, not a hard requirement):** the user's own phrasing was `UserAgentGenerate`; this proposal uses `UserAgentGenerator` to stay symmetric with `UserAgentParser` (both nouns, both factories) and with the existing naming convention table. Flagging for explicit sign-off below rather than silently deciding it.

### 4.3 `epics.md` — new Epic 4

**Epic 4: Composable, Tree-Shakeable Type-Pack API**
Supersedes the fixed-shape API from Epics 1–2. A consumer calls `UserAgentParser(...packs)` / `UserAgentGenerator(...packs)` with any combination of built-in or custom type packs and gets back the parse/generate function, with unused packs excluded from the JS bundle.

- **Story 4.1 — Redesign parse/generate as composable type packs, validated via npm/Node.**
  Rework `UserAgentInfo`/`UserAgentParser`/`UserAgentGenerator`/codegen per AD-1–AD-3 above; ship `UserAgentBrowserTypes`, `UserAgentEngineTypes`, `UserAgentOsTypes`, `UserAgentDeviceTypes`, `UserAgentBotTypes`, `UserAgentAIAgentTypes`, `UserAgentAllTypes`, and document the custom-pack shape. Validate directly against a clean Node.js project consuming the npm package (mirroring the check that surfaced this issue), including confirming an import of a single pack measurably shrinks the built JS bundle vs. importing `UserAgentAllTypes`.

- **Story 4.2 — Validate parity across all four targets and republish.**
  Confirm the same pack-based API compiles and behaves identically on Android, iOS, and JVM (common Kotlin — expected to be near-automatic, but must be verified, not assumed). Bump to a new major version and republish to Maven Central and npm, superseding the old API's artifacts. Update README/CHANGELOG to document the breaking change and migration from the old `parse()`/`generate()` shape.

Everything else in Epics 1–3 (the regex-matching engine, the uap-core vendoring/codegen mechanics, the Maven Central + npm publish pipelines) is reused as-is — no other story is reopened.

## 5. Implementation Handoff

**Scope classification: Major** at the architecture-invariant level (AD-1/AD-2/AD-3 change), but deliberately kept to a **2-story implementation footprint** per the user's explicit request to avoid a heavyweight re-plan.

- **This session (now, on approval):** apply the `SPEC.md` and `ARCHITECTURE-SPINE.md` edits above directly, and add Epic 4 to `epics.md` — no separate PM/Architect handoff round-trip, since the edits are small and already fully specified above.
- **Developer (`bmad-build` or direct implementation):** implement Story 4.1, then Story 4.2, in that order.
- **Success criteria:** `UserAgentParser(UserAgentBrowserTypes)` and friends compile and run correctly from a clean Node.js project against the npm package; a single-pack import produces a visibly smaller bundle than `UserAgentAllTypes`; all four MVP targets pass the shared test corpus under the new API; Maven Central + npm carry a new major version with no old-API artifacts left as the latest.
