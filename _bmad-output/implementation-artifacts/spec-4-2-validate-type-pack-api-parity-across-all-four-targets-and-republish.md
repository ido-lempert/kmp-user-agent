---
title: 'Validate Type-Pack API Parity Across All Four Targets and Republish'
type: 'chore'
created: '2026-09-04'
status: 'done'
review_loop_iteration: 0
baseline_commit: '270919a2f7fd71988ece0e7fafc0459255700a44'
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-4-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-4-1-redesign-parse-generate-as-composable-type-packs-validated-via-npm-node.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Story 4.1 already shipped the composable pack-based API to all four targets — `./gradlew build` is green (JVM/Android/iOS/JS tests + sample-app compiles), and `jvmApp`/`androidApp`/`webApp`/`iosApp` all compile against the new factory-function API. But the library is still versioned `0.1.0` everywhere, and `README.md` still documents the old `UserAgentParser.parse()`/`UserAgentGenerator.generate()` shape with no migration note or CHANGELOG at all.

**Approach:** Bump the version (Maven coordinates + npm package) to reflect the breaking change, rewrite `README.md` for the real shipped API (factory functions, built-in packs, no-packs-is-empty), add a `CHANGELOG.md` with a migration note, and verify the release artifacts build correctly *locally only* — `publishToMavenLocal` and the local JS packaging task. Do not touch the pack-composition test corpus or sample-app call sites further; Story 4.1 already extended the corpus with subset/all-types/custom-pack cases across all four targets and fixed every sample app's call sites, both already verified via `./gradlew build`.

## Boundaries & Constraints

**Always:** Version bump must be identical and consistent between the Maven `coordinates(...)` call and the npm `packages { named("js") { version.set(...) } }` call. `README.md`/`CHANGELOG.md` must describe the API exactly as Story 4.1 shipped it (post-review-pass-1: `UserAgentParser(vararg packs)`, no-packs-passed = empty result, not an `UserAgentAllTypes` fallback) — verify against the actual current source, don't describe an earlier design. `./gradlew build` must stay green after the version bump.

**Ask First:** None expected — version number and doc wording are easily revised at Checkpoint 1 before anything is built or verified.

**Never:** Do not run `publishToMavenCentral`, `publishAndReleaseToMavenCentral`, `npm publish`, or any task that reaches the live Maven Central or npmjs.org registries — the human publishes both themselves. Do not implement `UserAgentBotTypes`/`UserAgentAIAgentTypes` (still deferred, tracked separately). Do not further modify sample-app call sites or the shared test corpus — both already parity-verified by Story 4.1; only touch them if this story's own verification surfaces an actual defect.

</frozen-after-approval>

## Code Map

- `library/build.gradle.kts:625` (`mavenPublishing { coordinates("site.lempert", "user-agent", "0.1.0") }`) — Maven Central version string, one of two spots to bump.
- `library/build.gradle.kts:701` (`npmPublish { packages { named("js") { version.set("0.1.0") } } }`) — npm package version string, the other spot; must match the Maven bump.
- `README.md` — documents `implementation("site.lempert:user-agent:0.1.0")` / `npm install @lempert/user-agent`, and describes the old `UserAgentParser.parse()`/`UserAgentGenerator.generate()` API in its module description. Needs: new version number, new API shape (factory functions + built-in packs `UserAgentBrowserTypes`/`EngineTypes`/`OsTypes`/`DeviceTypes`/`AllTypes`), and a short migration note from the old shape.
- `CHANGELOG.md` — does not exist yet; create it, leading with the 0.1.0 → next-version breaking change (old singleton API → pack-based factory functions).
- `spec-4-1-...md` (this story's `context:` entry) — the authoritative record of exactly what shipped and how it was verified; source the README/CHANGELOG wording from its final `Intent`/`Boundaries`/Spec Change Log, not from memory of the original (pre-review) design.

## Tasks & Acceptance

**Execution:**
- [x] `library/build.gradle.kts:625,701` — bump both version strings from `0.1.0` to `0.2.0` (pre-1.0 semver: a minor bump is the conventional way to signal a breaking change before 1.0) — keeps Maven and npm versions in lockstep
- [x] `README.md` — update the dependency version, rewrite the API description section for the real shipped shape (factory functions, built-in packs, no-packs-is-empty), add a short "Migrating from 0.1.0" note showing old-call → new-call for both parse and generate
- [x] `CHANGELOG.md` (new file) — add a `0.2.0` entry describing the breaking API change, referencing the migration note in `README.md`
- [x] Local release verification — ran `./gradlew :library:publishToMavenLocal` (POM generation succeeded for `0.2.0` across all publications; blocked at signing per the Spec Change Log's Story 3.1 precedent) and `./gradlew :library:packJsPackage` (succeeded, produced `lempert-user-agent-0.2.0.tgz`); confirmed neither `publishToMavenCentral` nor any live npm-publish task was invoked

**Acceptance Criteria:**
- [x] Given `library/build.gradle.kts`, when inspected after the bump, then both the Maven `coordinates(...)` version and the npm `version.set(...)` read `0.2.0`
- [x] Given `./gradlew build`, when run after the version bump, then it stays green (no regression introduced by this story)
- [x] Given `./gradlew :library:publishToMavenLocal`, when run, then POM generation succeeds for every publication (jvm/android/js/iosArm64/iosSimulatorArm64/kotlinMultiplatform) with `<version>0.2.0</version>`, matching Story 3.1's already-accepted precedent that signing itself ("no configured signatory") requires GPG credentials only available in the human's own interactive terminal, not this session's — with no `publishToMavenCentral`/live npm-publish command ever invoked during this story
- [x] Given `README.md`, when read, then it documents the actual current API (factory functions + built-in packs + no-packs-is-empty) and includes a migration note from the 0.1.0 shape

## Spec Change Log

- **2026-09-04, mid-implementation:** `./gradlew :library:publishToMavenLocal` reached POM generation (confirmed `<version>0.2.0</version>` for every publication) but failed at `signAndroidPublication`/`signJsPublication` with "no configured signatory" — no `ORG_GRADLE_PROJECT_signing*` env vars are set in this session. This is not a regression: Story 3.1's own spec documents hitting the identical failure for the identical reason (GPG signing requires an interactive passphrase this tooling can't provide) and already accepted POM-generation success as sufficient local verification. Amended AC3 and the Verification commands to match that already-established bar instead of requiring a full signed local-repo write this environment cannot produce. KEEP: the version-bump, README, and CHANGELOG work — none of that is implicated.

- **2026-09-04, review pass 1:** Review (blind-hunter layer; edge-case-hunter and verification-gap both came back clean) found 12 documentation-quality gaps in the new README/CHANGELOG content — none were correctness bugs. Patched 8 directly (heading hierarchy, a missing JS/TS usage example, list-marker consistency, CHANGELOG release dates, the pre-1.0 versioning convention, pack-composition precedence rules, an `applyToGenerate` custom-pack example, and a migration-note gap about `UserAgentInfo`'s new `bot`/`aiAgent` fields). Deferred 2 as genuinely out of scope (documented in `deferred-work.md`): the 0.1.0 CHANGELOG entry not mentioning the earlier Maven artifactId rename, and no automated guard against the Maven/npm version strings drifting apart on a future bump (extends an already-existing deferred-work.md item from Story 3.1). No spec amendment needed — all patches were additive documentation, no frozen content implicated.

## Verification

**Commands:**
- `./gradlew build` -- expected: green, same as Story 4.1's final state
- `./gradlew :library:publishToMavenLocal` -- expected: POM generation succeeds for every publication with `0.2.0`; fails at the signing step with "no configured signatory" (expected in this environment, matches Story 3.1's precedent) — full signed-artifact verification under `~/.m2/repository/site/lempert/user-agent/0.2.0/` is for the human to run in their own GPG-configured terminal
- `./gradlew :library:packJsPackage` (or whatever local npm-packaging task Story 3.2 established) -- expected: succeeds, produces a `0.2.0`-versioned tarball, no registry contacted

**Manual checks (if no CLI):**
- Grep the whole diff for `publishToMavenCentral`, `publishAndReleaseToMavenCentral`, and `npm publish` to confirm none were actually executed as shell commands during this story (only referenced in docs/comments, never run).

## Suggested Review Order

- Read the new API documentation top-to-bottom — this is the actual deliverable a consumer will read first.
  [`README.md:18`](../../README.md#L18)

- Pack-composition precedence rules — added on review; the one piece of behavior a multi-pack consumer most needs and previously only had in KDoc.
  [`README.md:61`](../../README.md#L61)

- Migration note, including the `bot`/`aiAgent` field-shape gap added on review.
  [`README.md:104`](../../README.md#L104)

- The two version-bump sites — the actual code change in this story, everything else is documentation.
  [`build.gradle.kts:625`](../../library/build.gradle.kts#L625), [`build.gradle.kts:701`](../../library/build.gradle.kts#L701)

- New CHANGELOG, dated and explaining the pre-1.0 versioning convention.
  [`CHANGELOG.md:1`](../../CHANGELOG.md#L1)
