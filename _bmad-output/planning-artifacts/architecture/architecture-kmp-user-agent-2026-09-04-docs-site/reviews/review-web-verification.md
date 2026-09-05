# Web Verification Review — kmp-user-agent-docs-site Architecture Spine

**Reviewed doc:** `ARCHITECTURE-SPINE.md` (docs-site feature spine, created 2026-09-04)
**Review date:** 2026-09-04
**Method:** Live queries against the npm registry API, the GitHub REST API (releases/tags for `actions/deploy-pages` and `actions/upload-pages-artifact`), the live `vitepress.dev/guide/deploy` page, and general web search — not recalled from training data.

## Overall Verdict

The doc's headline framing — VitePress 1.6.4 stable / 2.0 alpha-only — is correct and genuinely reality-checked; every specific version number pinned for the two GitHub Actions (`upload-pages-artifact@v4`, `deploy-pages@v4`) is stale by one full major version as of Sept 2026, which reads as training-data recall rather than a live check, though the pins remain functional and match the versions VitePress's own official deploy guide currently recommends.

## Claim-by-Claim Findings

### 1. VitePress 1.6.4 (stable) — CONFIRMED ACCURATE

Queried the npm registry directly (`registry.npmjs.org/vitepress`):
- `dist-tags.latest` = `1.6.4` (published 2025-08-05, still the latest tag as of today, 2026-09-04 — over a year with no newer 1.x patch).
- `dist-tags.next` = `2.0.0-alpha.20`, published **2026-09-04** (i.e., literally the same day this architecture doc was authored).

This confirms both halves of the Stack-table entry: 1.6.4 is genuinely current-stable, and 2.0 genuinely exists only as an active alpha channel (alpha.7 through alpha.20 span July 2025 → Sept 2026, no beta/RC yet). No correction needed. This is the one claim in the doc that shows clear evidence of an actual check rather than recall — the "2.0 exists only as alpha, not bound" caveat is oddly specific and turned out to be exactly right down to the day.

**Severity: None (confirmed correct).**

### 2. Vue 3.x (bundled via VitePress) — ACCURATE BUT UNVERIFIABLE AS WRITTEN

npm registry confirms Vue's current latest is `3.5.42` (with `3.6.0-rc.6` in pre-release, not yet stable). The spine only commits to "3.x," which is true and appropriately non-committal since VitePress pins its own Vue dependency internally. No finding — this line doesn't over-claim.

**Severity: None.**

### 3. `actions/upload-pages-artifact@v4` — STALE, ONE MAJOR BEHIND

GitHub API (`api.github.com/repos/actions/upload-pages-artifact/releases`) shows:
- Latest release: **v5.0.0**, published **2026-04-10** (5 months before this doc was written).
- v4.0.0 was published 2025-08-14 and is itself a **breaking-change release** (dotfiles/hidden files excluded from the artifact by default) — the kind of detail that suggests whoever picked "v4" was recalling a training-data-era "current major," not reading the live releases page.
- v5.0.0's actual change: bumps the underlying `actions/upload-artifact` dependency to v7 and adds an `include-hidden-files` opt-in — not a breaking change for this use case, just newer.

Functionally, pinning the `v4` major tag is **not broken** — GitHub does not appear to have deprecated it — but it is not "current" as the Stack table implies, and no newer-alternative was flagged as a Deferred/known-tradeoff item the way VitePress 2.0 was.

**Severity: Medium.** Not a correctness bug, but the specific version number was asserted without the live check that the neighboring VitePress row clearly got. Recommend either bumping to v5 or adding the same kind of explicit "vN is current, vN+1 exists but not required" caveat used for VitePress.

### 4. `actions/deploy-pages@v4` — STALE, ONE MAJOR BEHIND (and more so)

GitHub API (`api.github.com/repos/actions/deploy-pages/releases`) shows:
- Latest release: **v5.0.1**, published **2026-09-01** — three days before this architecture doc's creation date.
- v5.0.0 (the major bump) was published 2026-03-25, ~5.5 months before this doc.
- The last v4 patch, v4.0.5, dates to **2024-03-18** — meaning the pinned major is over two years stale relative to today, and v4 never received the Node.js 24 runtime upgrade that shipped in v5.0.0 or the deployment-polling reliability fix in v5.0.1.

**Severity: Medium.** Same pattern as #3, slightly worse given the age gap on the v4 line specifically. Note this is a real risk vector over time: GitHub periodically deprecates Actions runtime versions (Node 16, then Node 20) on hosted runners; an action major that stopped receiving the Node 24 bump is a candidate for future breakage that a newer major already addresses. Not urgent today, but worth calling out as a maintenance debt rather than a settled fact.

### 5. Context that partially excuses #3/#4: VitePress's own official guide also lags

Fetched the live `https://vitepress.dev/guide/deploy` page directly (not cached/summarized from an old crawl) and grepped the actual action pins it recommends today:
- `actions/checkout@v5`, `actions/setup-node@v6`, `actions/configure-pages@v4`, **`actions/upload-pages-artifact@v3`**, **`actions/deploy-pages@v4`**.

So VitePress's own current official docs recommend `upload-pages-artifact@v3` — one major *behind* the spine's v4 — and `deploy-pages@v4`, matching the spine exactly. This means the spine's choices are defensible as "matches the framework vendor's own current guidance," even though neither the vendor's guide nor the spine reflects the actual latest GitHub-published majors (v5/v5). If the goal was "internally consistent with VitePress's official recipe," the spine succeeds. If the goal was "verified against the actual current release," it falls short on both actions. This is worth resolving explicitly in the doc rather than leaving ambiguous — as written, a reader can't tell whether "v4" was chosen because it's the true latest or because it matches VitePress's docs.

**Severity: Low** (mitigating context for #3/#4, not a new independent defect).

### 6. Design-paradigm terminology: "embedded interactive islands" — MINOR MISCHARACTERIZATION

The spine's Design Paradigm section describes the architecture as "Static-site-generator with embedded interactive islands" and the mermaid diagram frames `ParseDemo`/`GenerateDemo` as islands within the VitePress build. Technically, VitePress does not use an islands architecture (independently-hydrated interactive components within an otherwise static page, à la Astro's actual "islands" model) — it compiles Markdown into Vue components and ships a single client-side Vue SPA with `vue-router`-driven navigation; every page hydrates as part of one Vue application, not as isolated islands. "Islands" is a load-bearing term of art most associated with Astro specifically, and using it to describe VitePress is imprecise (though the *practical* result — two genuinely interactive components living inside a mostly-static docs site — is accurately described in spirit).

This doesn't change the technology choice's soundness (see #7), just the label. Not something that required a web search to catch, but it's the kind of claim a research pass should have caught if VitePress's actual rendering model had been checked against the term being used to describe it.

**Severity: Low.** Cosmetic/terminology issue, not an architectural defect.

### 7. Is VitePress + GitHub Actions Pages still a reasonable, current fit? — YES, CONFIRMED

- VitePress is actively maintained (alpha releases as recently as today; 1.x still receiving no-op patch cadence because it's stable, not abandoned — no gap of concern).
- It's Vue-native, which is the right fit given the spine explicitly wants Vue components (`ParseDemo.vue`, `GenerateDemo.vue`) for the two interactive pieces — no framework-bridging needed.
- GitHub's Actions-based Pages pipeline (`configure-pages` → build → `upload-pages-artifact` → `deploy-pages`) is unchanged as the current recommended deployment model in 2026; general web search turned up no newer replacement mechanism, only the ordinary major-version churn already covered above.

**Severity: None — confirmed a good, current fit.**

### 8. Alternatives not considered

A genuinely stronger structural fit for the stated "static pages + islands of interactivity" paradigm would be **Astro** (optionally with the Starlight docs theme), since Astro is the framework that actually implements islands architecture and ships zero client JS by default except for explicitly-hydrated components (via `@astrojs/vue`) — which would better serve the goal of "mostly static docs, two small interactive demos" than VitePress's whole-page Vue SPA hydration. That said, this is a marginal call, not a clear miss:
- Astro + Starlight + `@astrojs/vue` adds an extra integration layer and a second templating system (Astro's `.astro` files) on top of the Vue components, versus VitePress's zero-config "just write Markdown + Vue components" model.
- For a small single-library docs site (five guide pages + one interactive intro), VitePress's lower setup cost plausibly outweighs Astro's better-matched rendering model.
- Docusaurus, the other major docs-SSG, is React-based and would be a clear mismatch against the Vue-component requirement — correctly not a contender.

**Severity: Low / informational.** Worth one line in the spine's Deferred section (e.g., "Astro+Starlight considered and passed over for lower setup cost; revisit if page-weight becomes a concern") rather than silence, but VitePress is not a wrong call.

## Summary Table

| # | Claim | Verified against | Result | Severity |
|---|---|---|---|---|
| 1 | VitePress 1.6.4 stable / 2.0 alpha-only | npm registry (live) | Confirmed accurate, evidently actually checked | None |
| 2 | Vue 3.x | npm registry (live) | Accurate, appropriately non-specific | None |
| 3 | `upload-pages-artifact@v4` | GitHub Releases API | Stale — v5.0.0 has been out since 2026-04-10 | Medium |
| 4 | `deploy-pages@v4` | GitHub Releases API | Stale — v5.0.1 out 2026-09-01, v4's last patch is from 2024-03-18 | Medium |
| 5 | (context) v3/v4 matches VitePress's own official guide | Live fetch of vitepress.dev/guide/deploy | True, partially mitigates #3/#4 | Low (mitigating) |
| 6 | "embedded interactive islands" framing | VitePress's actual rendering model (SPA hydration, not islands) | Imprecise terminology | Low |
| 7 | VitePress + Actions Pages still fit for purpose | General web search, ecosystem activity | Confirmed | None |
| 8 | Alternatives considered | Own analysis (Astro+Starlight, Docusaurus) | Astro was a plausible unconsidered alternative; VitePress still defensible | Low/informational |

## Recommendation

Before this spine is treated as locked, bump the Stack table's two Actions pins to the currently-published majors (`actions/upload-pages-artifact@v5`, `actions/deploy-pages@v5`) or add an explicit note mirroring the VitePress 2.0 treatment (e.g., "v4 matches VitePress's own official guide as of doc date; v5 exists for both actions and is a safe, low-risk bump — revisit before first real deploy"). That keeps the one demonstrably-researched claim (VitePress version) from being the outlier against two claims that read as asserted from training-era knowledge.
