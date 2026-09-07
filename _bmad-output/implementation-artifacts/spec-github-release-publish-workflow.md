---
title: 'GitHub Actions release workflow for Maven Central publishing'
type: 'feature'
created: '2026-09-07'
status: 'done'
review_loop_iteration: 0
context: []
baseline_commit: '7761a0a5d44b5dc94c66aa588830cdbf5b319339'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The library has been published to Maven Central exactly once (`0.2.0`), entirely by hand from the maintainer's terminal — no GitHub Actions workflow exists to publish new releases (confirmed: `library/build.gradle.kts`'s own comments say `publishToMavenCentral()`/`publishAndReleaseToMavenCentral` was deliberately never run in CI as a Story 3.1 boundary). The Maven coordinate version is also a hardcoded literal with no tag-driven source of truth, even though `CHANGELOG.md` already documents unreleased `0.3.0`/`0.4.0` work sitting in `commonMain`.

**Approach:** Add a new GitHub Actions workflow, triggered on pushing a version tag (e.g. `v0.3.0`), that verifies the tag matches the version already configured in `library/build.gradle.kts`'s Maven coordinates (failing loudly on mismatch), then runs the Maven Central publish using GitHub Secrets the maintainer provisions separately. CI automation only — no live publish happens until a maintainer both adds the secrets and pushes a tag. npm publishing is a separate, already-logged follow-up (`deferred-work.md`), not part of this spec.

## Boundaries & Constraints

**Always:**
- New workflow file `.github/workflows/release.yml`, separate from `ci.yml` (build/test on every push/PR) and `docs-deploy.yml` (docs site only) — neither existing workflow is modified.
- Trigger is `on: push: tags: ['v*']` only — never runs on a normal push or PR.
- Before the publish step, verify the pushed tag's version (stripping a leading `v`) exactly matches `library/build.gradle.kts`'s `coordinates("site.lempert", "user-agent", "X.Y.Z")` version. Fail the workflow immediately (non-zero exit, clear error message naming both values) on any mismatch — never publish a version that doesn't match what was tagged.
- Maven Central publish uses `library/build.gradle.kts`'s existing `mavenPublishing { publishToMavenCentral(); signAllPublications() }` configuration as-is — do not change its `automaticRelease` behavior. Verify via `./gradlew :library:tasks --group publishing` (or equivalent) which concrete Gradle task this configuration produces, and use that exact task in the workflow; do not guess a task name. Signing/credentials come from `ORG_GRADLE_PROJECT_signingInMemoryKey`, `ORG_GRADLE_PROJECT_signingInMemoryKeyPassword`, `ORG_GRADLE_PROJECT_signingInMemoryKeyId`, and Sonatype Central Portal credential env vars (`mavenCentralUsername`/`mavenCentralPassword` as `ORG_GRADLE_PROJECT_*`, or whatever the vanniktech plugin's own docs specify) — sourced from GitHub Secrets of the same names, never hardcoded.
- Document in the workflow file itself (comments) and in the final report to the human: (1) whether the Maven Central task this workflow runs auto-releases immediately or only stages (requiring a manual click in Sonatype's Central Portal); (2) the exact list of GitHub Secrets the maintainer must add before the first real release.
- No live publish, no `git tag`/`git push` of any tag, from this session — this spec only adds the workflow file.

**Ask First:** None — the safe default (verify-then-publish using the project's existing, already-configured Maven Central publish task, gated on secrets the maintainer controls) is specified above. If `./gradlew :library:tasks` reveals the task auto-releases immediately and irreversibly on success with no staging step, note this prominently in the final report rather than silently proceeding — the human should consciously accept that before ever pushing a real release tag, but it does not block writing the workflow itself.

**Never:**
- Never touch npm publishing, `npmPublish {}` config, or the `stagePublishJsPackage` task in this spec — tracked separately in `deferred-work.md`.
- Never hardcode any credential, token, or key material in the workflow file — every secret is referenced via `${{ secrets.NAME }}` / GitHub Actions secret injection only.
- Never attempt to actually push a tag or trigger a real publish during this session — no real credentials exist to test against.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Tag matches Gradle coordinate version | Push tag `v0.2.0`; `coordinates(...)` version is `0.2.0` | Version-check step passes; publish step proceeds | N/A |
| Tag doesn't match Gradle coordinate version | Push tag `v0.3.0`; `coordinates(...)` version still `0.2.0` | Workflow fails at the version-check step with a clear message naming both values | Non-zero exit, publish step never runs |
| Non-tag push (normal commit/PR) | Push to `master`, no tag | Workflow does not trigger at all | N/A |
| Required secret missing | Tag pushed, but e.g. a signing secret not configured in repo secrets | The publish step fails with Gradle/plugin's own credential error, clearly attributable to the missing secret | Non-zero exit |

</frozen-after-approval>

## Code Map

- `.github/workflows/release.yml` -- NEW. Tag-triggered workflow: checkout, setup-gradle, version-check step, Maven Central publish step. Modeled on `.github/workflows/ci.yml`'s existing structure (macos-latest runner, `gradle/actions/setup-gradle@v4`) for consistency, since Maven Central publish needs the same JVM/Android toolchain build already proven there.
- `library/build.gradle.kts:635-644` -- `mavenPublishing { publishToMavenCentral(); signAllPublications(); coordinates("site.lempert", "user-agent", "0.2.0") ... }` -- read-only; the version-check step parses `"0.2.0"` out of this exact `coordinates(...)` call. Do not modify this file's publish configuration itself — only the new workflow reads from it.
- `_bmad-output/implementation-artifacts/deferred-work.md` -- read-only reference; records the npm-publish follow-up this spec deliberately excludes.
- `.github/workflows/ci.yml` -- read-only reference for existing workflow conventions (runner, `setup-gradle` action version) to match; not modified by this spec.

## Tasks & Acceptance

**Execution:**
- [x] Run `./gradlew :library:tasks --group publishing` (or equivalent) locally -- record the exact Maven Central publish task name, and whether it auto-releases or stages -- informs the next task
- [x] `.github/workflows/release.yml` -- create: `on: push: tags: ['v*']` trigger, checkout + setup-gradle steps matching `ci.yml`'s conventions -- establishes the tag-gated entry point
- [x] `.github/workflows/release.yml` -- add a version-check step (shell script) comparing the pushed tag (stripped of leading `v`) against `coordinates(...)`'s version, extracted from `library/build.gradle.kts` via grep/sed -- fails the job loudly on mismatch
- [x] `.github/workflows/release.yml` -- add the Maven Central publish step using the task name found above, with `ORG_GRADLE_PROJECT_signingInMemoryKey`/`signingInMemoryKeyPassword`/`signingInMemoryKeyId` and Sonatype credential env vars sourced from `${{ secrets.* }}` -- performs the actual publish, gated on the version check passing
- [x] `.github/workflows/release.yml` -- add header comments documenting: the full list of required GitHub Secrets, and the release semantics finding (staged vs. auto-release) -- so the maintainer has a self-contained runbook in the file itself

**Acceptance Criteria:**
- Given a tag `vX.Y.Z` is pushed where `library/build.gradle.kts`'s Maven coordinates version and the tag agree, when the workflow runs, then it proceeds past the version-check step to the publish step.
- Given a tag is pushed where the two values disagree, when the workflow runs, then it fails at the version-check step before the publish step runs, with an error message naming the mismatch.
- Given a normal commit or PR with no tag, when it's pushed, then the release workflow does not trigger at all.
- Given the workflow file, when inspected, then no credential or token value appears literally anywhere in it — only `${{ secrets.* }}` references.

## Spec Change Log

- **2026-09-07, token-count split (not a review loopback):** Original combined spec (Maven Central + npm publishing) measured ~3,200 tokens, roughly double the 1,600-token scope target. Human chose [S] Split. npm publishing (and its half of the version check) was carved out to a deferred-work.md entry for a follow-up spec; this file was regenerated to cover only the Maven Central publish path. KEEP: the tag-triggered, verify-then-publish design; the "never guess the Gradle task name, confirm via `./gradlew :library:tasks`" rule; the "no live publish this session" boundary — all carry forward unchanged to the npm follow-up spec when it's picked up.

## Design Notes

This session cannot obtain real Sonatype Central Portal credentials, so the publish step itself cannot be exercised end-to-end. Verification instead focuses on: the workflow YAML being syntactically valid, the version-check shell logic being provably correct via local dry runs against the actual current `library/build.gradle.kts` content (both the matching and mismatching cases), and the publish step being visibly gated behind `secrets.*` references rather than literals. The maintainer's remaining manual steps (adding secrets, confirming Sonatype namespace ownership) belong in the workflow's own header comment and the final report — not something this session can complete on their behalf.

## Verification

**Commands:**
- `./gradlew :library:tasks --group publishing` -- expected: confirms the exact Maven Central publish task name actually used in the workflow, and its staged-vs-auto-release behavior (inspect the vanniktech plugin's own task description/docs if the name alone doesn't make this clear).
- A local dry run of just the version-check shell logic (e.g. extracted into a standalone script invocation, or manual simulation) against the real current `library/build.gradle.kts` -- expected: passes when given tag `v0.2.0` (matches the current literal), fails with a clear message when given a mismatched tag like `v0.3.0`.
- `actionlint .github/workflows/release.yml` (or equivalent YAML/Actions lint, if available) -- expected: no syntax errors. If unavailable, manually verify the YAML parses (`python3 -c "import yaml; yaml.safe_load(open('.github/workflows/release.yml'))"` or similar) and that step gating (`if:` conditions, secret references) reads correctly.

**Manual checks (if no CLI):**
- Read the finished workflow file end-to-end and confirm: (1) the trigger is tag-only, (2) the version-check step runs before the publish step and blocks it on failure, (3) every credential reference is `${{ secrets.* }}`, never a literal.

## Suggested Review Order

**Release-gating logic**

- Entry point: tag-triggered publish job — concurrency-guarded and time-bounded before any secret is touched.
  [`release.yml:49`](../../.github/workflows/release.yml#L49)

- Tag-vs-Gradle-coordinate version check, extracted into a standalone, independently testable script rather than inlined.
  [`verify-release-version.sh:23`](../../scripts/verify-release-version.sh#L23)

- Ambiguity guard: fails loudly if the extraction pattern ever matches more than one line, instead of silently comparing a multi-line value.
  [`verify-release-version.sh:51`](../../scripts/verify-release-version.sh#L51)

- Fixture-based coverage for the match/mismatch/ambiguous/no-match cases, wired into `ci.yml` so a regression is caught on every push/PR, not only at tag-push time.
  [`verify-release-version.test.sh`](../../scripts/verify-release-version.test.sh#L1)

**Secrets & supply-chain hardening**

- Pre-flight check that all five required secrets are present before Gradle ever runs, naming each missing one explicitly.
  [`release.yml:70`](../../.github/workflows/release.yml#L70)

- Actions pinned to verified commit SHAs (not floating major-version tags), given this workflow's direct access to signing/publish secrets.
  [`release.yml:61`](../../.github/workflows/release.yml#L61)

- Gradle Wrapper checksum validated before any Gradle invocation runs with release secrets in scope.
  [`release.yml:63`](../../.github/workflows/release.yml#L63)

**Publish step**

- Runs the project's existing, unmodified `publishToMavenCentral` Gradle task — deliberately stages rather than auto-releases (see header comment for the verified reasoning).
  [`release.yml:100`](../../.github/workflows/release.yml#L100)
