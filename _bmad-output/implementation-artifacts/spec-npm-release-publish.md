---
title: 'Extend release workflow with npm publishing'
type: 'feature'
created: '2026-09-07'
status: 'done'
review_loop_iteration: 0
context: []
baseline_commit: '97ab9027da441f555f0a5feb847ecc7c00188b67'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** `.github/workflows/release.yml` (commit `3713cdc`) automates Maven Central publishing only — npm was deliberately split off as a named follow-up (`deferred-work.md`, entry "Add the npm publish step"). npm has never been published from CI.

**Approach:** Extend the same workflow/job (not a new file) to also publish to npm on the same tag push, reusing its existing checkout/setup-gradle/concurrency/timeout infrastructure. Verify-then-publish, same discipline as the Maven half: no live publish this session.

## Boundaries & Constraints

**Always:**
- Extend `scripts/verify-release-version.sh` (and its fixture test) to also extract `npmPublish { packages { named("js") { version.set("X.Y.Z") } } }`'s version from `library/build.gradle.kts` and fail loudly if it disagrees with either the tag or the Maven coordinates version — one release must ship the same version number everywhere.
- Verify the exact Gradle task name via `./gradlew :library:tasks --group publishing` before using it — expected to be `publishJsPackageToNpmjsRegistry` per the existing code comment, but confirm, don't assume.
- Do NOT use `stagePublishJsPackage` — known-buggy (see `deferred-work.md`), out of scope to fix here.
- Extend the existing "Verify required secrets are present" step to also check `NPM_TOKEN`.
- `NPM_TOKEN` is read the same way `library/build.gradle.kts` already expects (`providers.environmentVariable("NPM_TOKEN")`) — pass it as a plain `NPM_TOKEN` env var (not the `ORG_GRADLE_PROJECT_*` prefix convention used for Maven, which is specific to Gradle properties).
- Update the workflow's header comment: rename away from "(Maven Central)"-only framing, list `NPM_TOKEN` alongside the 5 existing secrets, and document that unattended CI publish requires an npm **Automation**-type token (exempt from OTP/2FA) — a 2FA-tied token fails in CI.
- npm publish step runs after the version check and secrets check, same gating discipline as Maven's step.

**Ask First:** None — same safe defaults as the Maven half (verify real task name, no live publish, document the token requirement rather than guess around it).

**Never:**
- Never wire `stagePublishJsPackage` into this workflow.
- Never hardcode `NPM_TOKEN` or any secret value — `${{ secrets.* }}` only.
- Never attempt a real publish this session (no working npm token available to test against).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Tag matches all three versions | Tag, Maven coords, npm version all `0.2.0` | Version check passes; both publish steps proceed | N/A |
| npm version disagrees | Maven `0.2.0`, npm still `0.1.0`, tag `v0.2.0` | Version check fails before either publish step, naming the npm mismatch specifically | Non-zero exit |
| `NPM_TOKEN` missing | Tag pushed, secret not configured | Secrets-check step fails, naming `NPM_TOKEN` specifically | Non-zero exit, no publish attempted |

</frozen-after-approval>

## Code Map

- `.github/workflows/release.yml` -- edit: add npm version to the secrets check and a new "Publish to npm" step after the Maven step; update header comment.
- `scripts/verify-release-version.sh` -- edit: add npm-version extraction/comparison (mirrors the existing `coordinates(...)` regex pattern at a new anchor).
- `scripts/verify-release-version.test.sh` -- edit: extend fixtures to cover the new npm-version comparison (match/mismatch cases).
- `library/build.gradle.kts:699,707` -- read-only; confirms `NPM_TOKEN` env var name and the `version.set("0.2.0")` literal this reads.
- `_bmad-output/implementation-artifacts/deferred-work.md` -- read-only; this spec closes the "Add the npm publish step" entry.

## Tasks & Acceptance

**Execution:**
- [x] Run `./gradlew :library:tasks --group publishing`, confirm the npm publish task name
- [x] `scripts/verify-release-version.sh` + test -- add npm-version extraction/comparison
- [x] `.github/workflows/release.yml` -- add `NPM_TOKEN` to secrets check, add "Publish to npm" step, update header comment

**Acceptance Criteria:**
- Given a tag where Maven and npm versions both match the tag, when the workflow runs, then both publish steps run.
- Given npm's version disagrees with the tag/Maven version, when the workflow runs, then it fails at the version-check step, before any publish step, naming the npm mismatch.
- Given `NPM_TOKEN` is missing, when the workflow runs, then the secrets-check step fails naming it specifically.

## Spec Change Log

## Verification

**Commands:**
- `bash scripts/verify-release-version.test.sh` -- expected: all fixtures pass, including new npm-version cases.
- `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/release.yml'))"` -- expected: parses cleanly.
- `actionlint .github/workflows/release.yml` -- expected: no errors, if available.

**Manual checks (if no CLI):**
- Read the finished workflow: npm step is gated behind both checks, `NPM_TOKEN` never appears as a literal.

## Suggested Review Order

**Version consistency**

- Entry point: `verify-release-version.sh`'s npm-version extraction, scoped to the `npmPublish { ... }` block via brace-depth counting rather than a blind whole-file regex (a real gap found by all three review layers and fixed during review).
  [`verify-release-version.sh:79`](../../scripts/verify-release-version.sh#L79)

- Fixture proving the scoping fix: an unrelated `version.set(...)` elsewhere in the file no longer causes a false ambiguity or misattribution.
  [`verify-release-version.test.sh`](../../scripts/verify-release-version.test.sh)

**npm publish correctness**

- `publishConfig.access: "public"` added to the generated package.json — verified by decompiling the actual Gradle plugin bytecode to confirm scoped packages default to private without it.
  [`library/build.gradle.kts:746`](../../library/build.gradle.kts#L746)

- Corrected OTP/Automation-token claim in the workflow header, now consistent with the more nuanced reality already documented in `build.gradle.kts`, with a manual fallback path noted.
  [`release.yml`](../../.github/workflows/release.yml)

**CI protection (new)**

- Push/PR-time check that `publishJsPackageToNpmjsRegistry` still exists as a Gradle task, catching a future plugin rename before a real release.
  [`ci.yml`](../../.github/workflows/ci.yml)

- Push/PR-time run of the real version-check script against the real `library/build.gradle.kts`, not only synthetic fixtures.
  [`ci.yml`](../../.github/workflows/ci.yml)
