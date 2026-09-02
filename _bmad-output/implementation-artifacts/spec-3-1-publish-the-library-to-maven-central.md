---
title: 'Publish the Library to Maven Central'
type: 'feature'
created: '2026-09-02'
status: 'done'
review_loop_iteration: 0
baseline_commit: '9ffcf0f97ec50a5c70b17f59774b40c1302cfba7'
context:
  - '{project-root}/_bmad-output/specs/spec-user-agent/SPEC.md'
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-kmp-user-agent-2026-09-01/ARCHITECTURE-SPINE.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-3-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The library isn't publishable — no Maven publish plugin is configured, no root `LICENSE` file exists despite the project claiming MIT throughout, and the dependency graph has never been formally audited for license compliance.

**Approach:** Wire up `com.vanniktech.maven.publish` 0.37.0 targeting the already-verified `site.lempert` Sonatype Central Portal namespace, add the missing MIT `LICENSE` file, bundle `library/NOTICE`'s Apache-2.0 attribution into published JVM/Android artifacts, and audit the dependency graph. Verification is local-only (`publishToMavenLocal`) — this story does not execute an actual live publish to Maven Central; that's a manual step the human runs afterward with their own GPG-unlockable terminal (this session's tooling can't complete an interactive passphrase prompt).

## Boundaries & Constraints

**Always:**
- Maven coordinates: `groupId = "site.lempert"`, `artifactId = "kmp-user-agent"`, `version = "0.1.0"` (both human-confirmed).
- Add a root-level `LICENSE` file: standard MIT license text, copyright holder `Ido Lempert`, current year.
- `com.vanniktech.maven.publish` plugin, pinned to exactly `0.37.0`, configured in `library/build.gradle.kts` via its `mavenPublishing { }` DSL: Central Portal as the publish target, `signAllPublications()` (reads `signingInMemoryKey`/`signingInMemoryKeyPassword`/`signingInMemoryKeyId` from Gradle properties/env automatically — already configured on this machine as `ORG_GRADLE_PROJECT_*` env vars, do not hardcode credentials anywhere in the build), `coordinates(...)` per above, and a `pom { }` block: name, description, MIT license (`name`/`url` = the standard MIT SPDX identifier/URL), a `url` pointing at the project's GitHub repo, `developers { developer { name/email } }` using `Ido Lempert` / `il.mrbit@gmail.com`, and `scm { }` pointing at the same repo. Verify the exact 0.37.0 API shape against the plugin's own docs/source rather than assuming an older/newer version's syntax.
- `library/NOTICE`'s existing content ships bundled into the JVM and Android published artifacts (e.g. via a `Jar` task configuration adding it under `META-INF/`). For the iOS klib/framework artifact, embedding a NOTICE file inside the compiled binary isn't standard practice — rely on the published POM's license/SCM metadata pointing back to the source repo (which contains `NOTICE`) for that target; note this explicitly rather than silently skipping it.
- Dependency audit (document findings, e.g. appended to `library/NOTICE` or a short new section): confirm every actual dependency — Kotlin stdlib (implicit, Apache-2.0), `kotlin.test` (commonTest only, Apache-2.0), the vendored `uap-core` data (Apache-2.0, already attributed) — is MIT/Apache-2.0/BSD, with no copyleft anywhere. Scope this to dependencies that actually ship with or are needed to build the published artifact; an exhaustive line-by-line audit of every Gradle plugin's own transitive tooling dependencies is not required for v1.
- This session must not run `./gradlew publishToMavenCentral`/`publishAndReleaseToMavenCentral` (or any task that actually uploads to Sonatype) — verify only via `./gradlew publishToMavenLocal`. If a Gradle command needs the GPG passphrase to succeed, it will only work in the human's own terminal, not this session's tooling; report that command for them to run rather than attempting it.

**Ask First:**
- Any change to the chosen coordinates/version once set (they're effectively permanent after a real publish).

**Never:**
- Change `UserAgentParser`/`UserAgentGenerator`/`UserAgentInfo`/`Component`/`Device` behavior, or any existing test.
- Execute a real publish to Sonatype/Maven Central from this session.
- Commit any credential value (username, password, signing key, passphrase) anywhere in the repo.

</frozen-after-approval>

## Code Map

- `LICENSE` (new, repo root) -- standard MIT text, `Copyright (c) 2026 Ido Lempert`.
- `gradle/libs.versions.toml` -- add a `mavenPublish` version (`0.37.0`) and plugin alias (`com.vanniktech.maven.publish`).
- `build.gradle.kts` (root) -- add `alias(libs.plugins.mavenPublish) apply false`, matching the existing pattern for other plugins declared here.
- `library/build.gradle.kts` -- apply the plugin (`alias(libs.plugins.mavenPublish)`), add the `mavenPublishing { }` block per Boundaries, and a `tasks.withType<Jar>()` (or equivalent) configuration bundling `NOTICE` into JVM/Android jars.
- `library/NOTICE` -- append the dependency-audit findings (or note where they're recorded, if placed elsewhere).
- `_bmad-output/specs/spec-user-agent/SPEC.md:56` -- has an "Open Questions" line about the unresolved artifact id; this is now resolved (`kmp-user-agent`) -- update or strike it if convenient, not required.

## Tasks & Acceptance

**Execution:**
- [ ] `LICENSE` -- add MIT license text
- [ ] `gradle/libs.versions.toml`, `build.gradle.kts` (root) -- register the `com.vanniktech.maven.publish` 0.37.0 plugin
- [ ] `library/build.gradle.kts` -- apply the plugin, configure `mavenPublishing { }` (coordinates, signing, Central Portal target, full POM), bundle `NOTICE` into JVM/Android jars
- [ ] `library/NOTICE` (or a clearly-linked location) -- record the dependency-license-audit findings
- [ ] Run `./gradlew publishToMavenLocal` and inspect the local output (`~/.m2/repository/site/lempert/kmp-user-agent/0.1.0/`) to confirm coordinates, POM content, and signatures (`.asc` files) are all correct

**Acceptance Criteria:**
- Given the `com.vanniktech.maven.publish` plugin configured in the library's build, when `./gradlew publishToMavenLocal` is run, then Kotlin/Android/iOS/JVM artifacts are produced under `site.lempert:kmp-user-agent:0.1.0` with correct POM metadata (license: MIT) and valid `.asc` signatures for each artifact.
- Given the vendored `uap-core` Apache-2.0 snapshot, when the locally-published JVM/Android artifacts are inspected, then a `NOTICE` file preserving the Apache-2.0 attribution is present (e.g. under `META-INF/`).
- Given the full dependency graph of the library and its build, when audited, then every dependency is confirmed MIT/Apache-2.0/BSD-licensed with no copyleft dependency present, and this is documented.

## Spec Change Log

- 2026-09-02: Human-directed amendment — Maven artifactId changed from `kmp-user-agent` to `user-agent` (coordinates now `site.lempert:user-agent:0.1.0`), to align with the npm package name chosen for Story 3.2 (`@lempert/user-agent`). Group id and version unchanged. No real publish had occurred, so this is a pre-release correction, not a breaking change to a shipped artifact.

## Design Notes

The actual live publish (`./gradlew publishAndReleaseToMavenCentral` or the plugin's equivalent task name for 0.37.0) is intentionally not run by this story — hand the exact command to the human at the end, along with a reminder that it needs their own terminal (the GPG signing key setup in this environment requires an interactive passphrase prompt this session's tooling can't satisfy).

## Verification

**Commands:**
- `./gradlew :library:publishToMavenLocal` -- expected: succeeds, produces signed artifacts in the local Maven repository under the configured coordinates
- Manual inspection of `~/.m2/repository/site/lempert/kmp-user-agent/0.1.0/` -- expected: `.jar`/`.klib`/`.aar` artifacts as applicable, a `.pom` with correct license/coordinates, and `.asc` signature files for each

**Note:** as anticipated, `publishToMavenLocal` fails specifically at the GPG-signing step (`no configured signatory`) in this session, since `ORG_GRADLE_PROJECT_signingInMemoryKey` requires an interactive passphrase prompt this tooling can't satisfy. Everything up to signing was verified directly: `generatePomFileForXXXPublication` succeeds for every target, and artifact contents were inspected by hand (see Suggested Review Order) rather than via the local repository, since publishing never completes. **The user's own terminal also hit a second, real issue while retrying this: `ORG_GRADLE_PROJECT_signingInMemoryKeyId` appears to be set to something like "Ido..." rather than a valid hex GPG key ID (e.g. `EFGH5678`) — this needs fixing in their local `.zshrc` regardless of the TTY limitation before a live publish can work.**

## Suggested Review Order

**Bug found and fixed during review: NOTICE/LICENSE silently dropped from the published AAR**

- `packaging.resources.excludes -= "/META-INF/NOTICE"` (kept, but confirmed insufficient on its own) -- verified by literally unzipping the built `.aar`: NOTICE reached the intermediate merged-resources jar but was still missing from the final artifact, because `com.android.kotlin.multiplatform.library`'s AAR-bundling step doesn't fully respect that DSL. Fixed by also hooking `bundleAndroidMainAar` (itself a `Zip` task) directly.
  [`build.gradle.kts:566`](../../library/build.gradle.kts#L566)

- Also added during review: the root `LICENSE` file itself wasn't bundled at all, only `NOTICE` -- now both travel together into `META-INF/` for JVM and Android artifacts.
  [`build.gradle.kts:415`](../../library/build.gradle.kts#L415), [`build.gradle.kts:417`](../../library/build.gradle.kts#L417)

**POM correctness fixes made during review**

- `licenses.license.distribution` was set to a URL; Maven's `<distribution>` element means `repo`/`manual`, not a link -- fixed to `"repo"`.
  [`build.gradle.kts:601`](../../library/build.gradle.kts#L601)

- `scm.connection` used the `git://` protocol, which GitHub disabled for anonymous access in 2022 -- fixed to `https://` (`developerConnection`'s `ssh://` was already correct).
  [`build.gradle.kts:620`](../../library/build.gradle.kts#L620)

- Added the conventional `developer.id` (GitHub username), previously missing.

**Core publish configuration**

- `mavenPublishing { }`: Central Portal target, signing, coordinates, full POM.
  [`build.gradle.kts:587`](../../library/build.gradle.kts#L587)
