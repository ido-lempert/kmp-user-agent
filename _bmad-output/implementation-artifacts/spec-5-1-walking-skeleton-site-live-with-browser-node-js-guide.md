---
title: 'Walking Skeleton — Site Live with Browser+Node.js Guide'
type: 'feature'
created: '2026-09-05'
status: 'done'
review_loop_iteration: 0
baseline_commit: '0980755754dad5aa6846f715592fd7736362dff7'
context:
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-kmp-user-agent-2026-09-04-docs-site/ARCHITECTURE-SPINE.md'
  - '{project-root}/_bmad-output/specs/spec-docs-site/SPEC.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** `kmp-user-agent` has no live docs site — a prospective adopter can't self-serve confirmation the library works or how to use it without cloning the repo, and `docs-site/` doesn't exist yet, so none of the later demo/guide stories have a scaffold to build on.

**Approach:** Stand up a standalone VitePress project in `docs-site/`, deploy it to GitHub Pages via a new GitHub Actions workflow, and ship one real content page (a Browser+Node.js usage guide) so the skeleton is genuinely live and demoable, not a placeholder.

## Boundaries & Constraints

**Always:**
- `docs-site/` is a standalone Node/Vite project entirely outside `settings.gradle.kts` — never add it as a Gradle module.
- `docs-site/package.json` pins `@lempert/user-agent` at an exact/caret version starting `^0.2.0` — never `latest`; `package-lock.json` is committed; the deploy workflow runs `npm ci`, never `npm install`.
- `docs-site/.vitepress/config.ts` sets `base: '/kmp-user-agent/'` (project-page subpath) — the VitePress default `base: '/'` 404s every asset on GitHub Pages.
- Guide pages use VitePress's default theme (sidebar nav + main content + copyable code blocks) — no custom/minimal theme.
- The JS guide page demonstrates real, working code for both a browser consumption path (bundler import and/or CDN script tag) and a Node.js path (import/require) against the real `@lempert/user-agent` package — one runtime alone is incomplete.
- New deploy workflow is a separate file from the existing `.github/workflows/ci.yml` (Gradle build/test) — do not modify that file.

**Ask First:**
- Enabling GitHub Pages "Source: GitHub Actions" in the repo's Settings is a one-time manual step outside git that this agent cannot perform — flag it as a prerequisite rather than assuming it's already set.
- Adding a docs-site link to the root `README.md` — not required by this story's acceptance criteria; ask before touching a file outside `docs-site/`.

**Never:**
- Do not build `ParseDemo`/`GenerateDemo` (Stories 5.3/5.4) or the core-concepts/Android/iOS/JVM guides (Stories 5.2/5.5–5.7) — this story is the skeleton + JS guide only.
- Do not give the `library` module any dependency on `docs-site/`.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Push touching `docs-site/**` | Commit on `master` modifies `docs-site/index.md` | Deploy workflow triggers, builds, publishes; live site updates | N/A |
| Push NOT touching `docs-site/**` | Commit only modifies `library/` | Deploy workflow does not trigger (path filter) | N/A |
| Site root loaded | `GET https://ido-lempert.github.io/kmp-user-agent/` | 200; JS/CSS assets resolve under `/kmp-user-agent/` base, no 404s | Misconfigured `base` → asset 404s; must verify against a real deploy |

</frozen-after-approval>

## Code Map

- `settings.gradle.kts` (repo root) -- includes only `:androidApp`, `:library`, `:jvmApp`; confirms `docs-site/` must NOT be added here.
- `.github/workflows/ci.yml` -- existing Gradle build/test workflow (push+PR, `macos-latest`, `./gradlew build`); new deploy workflow must be a separate file and must not touch this one.
- `.gitignore` (repo root) -- already ignores `node_modules/`; needs `docs-site/.vitepress/dist` and `docs-site/.vitepress/cache` added.
- `README.md` (repo root, 191 lines) -- no existing docs/website link; do not edit without asking (Ask First).
- `library/build.gradle.kts:679-731` (`npmPublish` block) -- `organization.set("lempert")`, `packageName.set("user-agent")` → published as `@lempert/user-agent`, `version.set("0.2.0")`.
- `ARCHITECTURE-SPINE.md` (docs-site companion, `context:` above) AD-1/AD-2/AD-4 -- governs the isolation boundary, npm pin, and deploy pipeline rules restated in Boundaries above.

## Tasks & Acceptance

**Execution:**
- [x] `docs-site/package.json` -- create; pin `@lempert/user-agent` at `^0.2.0`, add VitePress `^1.6.4` devDependency -- establishes the standalone Node project.
- [x] `docs-site/package-lock.json` -- generate via `npm install` and commit -- required for `npm ci` in the deploy workflow.
- [x] `docs-site/.vitepress/config.ts` -- create; `base: '/kmp-user-agent/'`, nav/sidebar entries, default theme -- required for the deployed site's assets and nav to work.
- [x] `docs-site/guide/js.md` -- create; Browser+Node.js usage guide with two real, working examples -- this story's required first real content page.
- [x] `docs-site/index.md` -- create; minimal intro page shell (title + short description) -- placeholder only; Stories 5.3/5.4 add the demo components.
- [x] `.github/workflows/docs-deploy.yml` -- create; trigger on push to `master` path-filtered to `docs-site/**` + this file; checkout, setup Node, `npm ci` in `docs-site/`, VitePress build, `actions/upload-pages-artifact@v5`, `actions/deploy-pages@v5`.
- [x] `docs-site/.gitignore` -- create; ignore `.vitepress/dist`, `.vitepress/cache`, `node_modules`.

**Acceptance Criteria:**
- Given a push to `master` touching `docs-site/**`, when the workflow runs, then it builds via `npm ci` + VitePress build and publishes via `upload-pages-artifact@v5`/`deploy-pages@v5`.
- Given the deployed site, when a visitor loads `https://ido-lempert.github.io/kmp-user-agent/`, then it is live with the correct `base`, no asset 404s, and shows the default theme's nav/sidebar/search.
- Given the JS guide, when a JS/web developer reads it, then it shows real, working code for both a browser path and a Node.js path against `@lempert/user-agent`.
- Given `settings.gradle.kts`, when inspected after this story, then it is unchanged.

## Spec Change Log

## Design Notes

Enabling GitHub Pages with "Source: GitHub Actions" in the repo's Settings UI is a one-time manual step outside git/CI scope — this agent cannot perform it. Note it clearly to the human as a deploy prerequisite; the workflow itself will fail to publish (though it can still build) until that setting is made.

`actions/upload-pages-artifact@v5` / `actions/deploy-pages@v5` were the verified-current major versions when the architecture spine was authored (Sept 2026); the spine itself flags VitePress's own official guide as showing older majors (v3/v4) — re-check both are still current/non-breaking before finalizing the workflow file.

## Verification

**Commands:**
- `cd docs-site && npm ci && npm run docs:build` -- expected: build succeeds, `.vitepress/dist` produced with no errors.

**Manual checks (if no CLI):**
- After a real push (or a manual `workflow_dispatch` run), visit the live URL and confirm the JS guide's code snippets render correctly and are copyable.
- Confirm repo Settings → Pages → Source is set to "GitHub Actions" (see Design Notes) before expecting the workflow to actually publish.

## Suggested Review Order

**Deploy pipeline**

- Entry point: the whole reason this story exists — a live, automated path from a commit to a published site.
  [`docs-deploy.yml:7`](../../.github/workflows/docs-deploy.yml#L7)

- `workflow_dispatch` has no branch filter of its own; this guard stops a manual run on a non-master branch from publishing to production.
  [`docs-deploy.yml:31`](../../.github/workflows/docs-deploy.yml#L31)

- `npm ci` (never `npm install`) is what makes the pinned dependency (AD-2) actually enforced at build time.
  [`docs-deploy.yml:50`](../../.github/workflows/docs-deploy.yml#L50)

**Base path correctness**

- The one config line that decides whether the deployed site 404s on every asset; kept dev-mode-safe via the `command` check.
  [`config.ts:13`](../../docs-site/.vitepress/config.ts#L13)

**Guide content — API shape and both runtimes**

- States the JS-boundary API shape (array args, `.get()`, real classes) once, up front, so every example below reads consistently.
  [`js.md:15`](../../docs-site/guide/js.md#L15)

- Browser bundler path — the example most JS/TS consumers will actually use.
  [`js.md:28`](../../docs-site/guide/js.md#L28)

- CDN `<script type="module">` path — the no-bundler alternative, with a pinned version that needs its own upkeep note.
  [`js.md:46`](../../docs-site/guide/js.md#L46)

- Node.js `import` example — verified against the real published `0.2.0` package's actual output, not a guessed string.
  [`js.md:67`](../../docs-site/guide/js.md#L67)

- CommonJS `require()` path, version-gated correctly against Node's real `require(esm)` support history.
  [`js.md:86`](../../docs-site/guide/js.md#L86)

**Peripherals**

- Minimal intro shell — deliberately thin; Stories 5.3/5.4 build the actual demo here.
  [`index.md:1`](../../docs-site/index.md#L1)

- Standalone project boundary: pins `@lempert/user-agent`, kept outside the Gradle build entirely.
  [`package.json:11`](../../docs-site/package.json#L11)

- Keeps VitePress's build output and cache out of git.
  [`.gitignore:1`](../../docs-site/.gitignore#L1)
