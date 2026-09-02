# Epic 3 Context: Publish the Library

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

A consumer can add the library as a real dependency — from Maven Central for Kotlin/Android/iOS/JVM, or from npm for JS — in a brand-new project and have parsing/generation work immediately. This epic turns the library built in Epics 1–2 into a genuinely consumable, trustworthy artifact: a verified publishing namespace, correct license metadata and attribution, a build-produced (not hand-maintained) npm package, and end-to-end proof that a fresh, unrelated project can pull the published artifact and get correct results on all four MVP targets. No PRD/UX documents exist for this project (library with no UI); this context is drawn from the epics breakdown and the architecture spine only.

## Stories

- Story 3.1: Publish the Library to Maven Central
- Story 3.2: Publish the JS Package to npm
- Story 3.3: Verify Fresh-Project Consumption of the Published Library

## Requirements & Constraints

- The library must be published as an MIT-licensed multiplatform artifact consumable via standard package managers: Maven Central for Kotlin/JVM/Android/iOS, npm for JS.
- Only permissive-licensed (MIT/Apache-2.0/BSD) dependencies are allowed anywhere in the library or its build — no copyleft (GPL/LGPL) anywhere in the dependency graph. This must be verifiable by audit, not just assumed.
- The Maven Central publishing namespace is the reverse-DNS of the owner's domain: `site.lempert` (domain `lempert.site`), and must be verified via the Sonatype Central Portal's domain-ownership check before publishing is possible.
- The npm package must be produced as part of the standard build's dist output (the Kotlin/JS Gradle plugin's dist folder) — never a separate hand-built or hand-copied step.
- Success for this epic is proven only by a fresh, unrelated project on each of the four MVP targets (Android, iOS, JVM, JS/Node) successfully adding the published dependency and getting correct parse/generate results using only the publicly published artifact — this is the SPEC's overall v1 success signal.
- Exact library artifact id under `site.lempert` is an open question not yet resolved by planning; it must be decided during implementation of this epic.

## Technical Decisions

- **Namespace & verification:** Publishing namespace is `site.lempert` (reverse-DNS of `lempert.site`), approved via Sonatype Central Portal domain-ownership verification.
- **Maven Central publishing:** Use `com.vanniktech.maven.publish` plugin, pinned to version 0.37.0, targeting the Sonatype Central Portal host. Published POM metadata must declare the MIT license. Covers Kotlin/Android/iOS/JVM artifacts.
- **npm publishing (AD-7):** The npm package must be produced directly from the `js(IR)` target's own Gradle dist output via `org.jetbrains.kotlin.npm-publish` (v3.7.x lineage, the continuation of `dev.petuska.npm.publish`), wired into the same Gradle build. No hand-authored `package.json` and no manually copied files — the published package name should follow the `site.lempert` namespace convention.
- **License/provenance (AD-6):** Because `uap-core`'s `regexes.yaml` is vendored (Apache-2.0) into the library's rule data, a `NOTICE` file preserving that Apache-2.0 attribution must be shipped alongside every published artifact (both the Maven Central artifact and the npm package).
- **Dependency audit (NFR1):** The full dependency graph of the library and its build must be audited to confirm every dependency is MIT/Apache-2.0/BSD-licensed, with no copyleft dependency present anywhere — this is a checkable acceptance condition, not a one-time assumption.
- **Stack pins relevant to this epic:** `com.vanniktech.maven.publish` 0.37.0; `org.jetbrains.kotlin.npm-publish` v3.7.x lineage; Kotlin/KMP Gradle plugin 2.4.10 (underlying build).
- **Structural expectation:** Publishing configuration (both Maven Central and npm) lives in the `library` module's `build.gradle.kts` plus root-level publishing config; the `NOTICE` file lives alongside the library module and ships with the published artifact.
- This epic builds entirely on top of the `library` module, data model, and stateless parse/generate API established in Epics 1–2 — it adds no new production API surface, only build/publish tooling.

## Cross-Story Dependencies

- Story 3.1 (Maven Central) and Story 3.2 (npm) are independent of each other but both require the `library` module's parse and generate implementations (Epics 1–2) to be complete and buildable first.
- Story 3.3 (fresh-project verification) depends on both Story 3.1 and Story 3.2 having successfully published real artifacts — it validates the actual published output, not a local build, so it must run after both publish stories land.
- The exact library artifact id (open question, not resolved in planning) must be settled before or during Story 3.1/3.2, since both publish targets need a concrete, consistent name under the `site.lempert` namespace.
