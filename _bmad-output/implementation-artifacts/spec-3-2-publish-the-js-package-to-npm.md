---
title: 'Publish the JS Package to npm'
type: 'feature'
created: '2026-09-02'
status: 'review'
review_loop_iteration: 0
baseline_commit: '84eea6b'
context:
  - '{project-root}/_bmad-output/specs/spec-user-agent/SPEC.md'
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-kmp-user-agent-2026-09-01/ARCHITECTURE-SPINE.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-3-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The Kotlin/JS target builds a `library` module that JS/Node consumers can already exercise locally (Story 1.4's `webApp` sample), but nothing publishes it as a real npm package. There's no npm publish plugin configured, no scoped package name chosen, and no dependency/license audit for the npm artifact specifically.

**Approach:** Wire up `org.jetbrains.kotlin.npm-publish` (v3.7.x lineage) in `library/build.gradle.kts`, producing the npm package `@lempert/user-agent` (human-confirmed: `organization = "lempert"`, `packageName = "user-agent"`) directly from the `js` target's own dist output — no hand-authored `package.json`, no manually copied files. Bundle the same `NOTICE`/`LICENSE` attribution that Story 3.1 ships in the Maven artifacts. Verification is local-only (`npm pack`/dry-run equivalent) — this story does not execute a real `npm publish`; that's a manual step the human runs afterward with their own `NPM_TOKEN`.

## Boundaries & Constraints

**Always:**
- npm package name: scoped as `@lempert/user-agent` via `npmPublish { organization = "lempert" }` + `packages { named("js") { packageName = "user-agent" } }` (both human-confirmed, matching the Maven artifactId `user-agent` renamed in Story 3.1's amendment).
- Package version: `0.1.0`, matching the Maven Central coordinate version for this release.
- `org.jetbrains.kotlin.npm-publish` plugin, pinned within the `3.7.x` line (verify the latest patch and its exact `npmPublish { }` DSL shape against the plugin's own docs/source rather than assuming an older/newer version's syntax — the extension shape is: `organization`, `registries { npmjs { authToken = ... } }`, `packages { named("js") { version, packageName, readme, packageJson { license, homepage, description, repository { ... } } } } }`).
- Registry auth: read the publish token from the `NPM_TOKEN` environment variable (`System.getenv("NPM_TOKEN")`), consistent with the plugin's documented convention. Do not hardcode any token anywhere in the build. Note: this session found `NPM_TOKEN` already set in the user's shell environment (presence only checked, value never inspected) — do not assume it is a valid npmjs.org publish token without the human confirming; flag this to the human rather than treating it as proof npm is already configured.
- `packageJson { license = "MIT"; homepage = <GitHub repo URL>; description = <same library description as the Maven POM>; repository { type = "git"; url = <GitHub repo URL + .git> } }`.
- Bundle `library/NOTICE` and root `LICENSE` into the published npm package (e.g. via the plugin's `readme`/extra-files mechanism, or a pre-publish task copying both files into the `js` target's dist directory before packaging — verify which the plugin actually supports and use the supported path, don't fabricate an unsupported config key).
- Dependency audit specific to the npm package: confirm the published `package.json` declares no runtime npm dependencies beyond what the Kotlin/JS compiler itself emits (the library has no non-Kotlin JS dependencies), and that nothing copyleft-licensed is pulled in by the npm-publish plugin's own packaging step into the shipped files.
- This session must not run the plugin's live "publish to npmjs registry" task (whatever it's named for the exact version used, e.g. `publishJsPackageToNpmjsRegistry` or similar) — verify only via `npm pack --dry-run` on the generated package directory, or the plugin's own local/dry-run equivalent if one exists. If a task needs a valid `NPM_TOKEN` to fully succeed, report the exact command for the human to run themselves rather than attempting it.

**Ask First:**
- Any change to the chosen npm package name/scope/version once set (effectively permanent after a real publish, same as Story 3.1's Maven coordinates).

**Never:**
- Change `UserAgentParser`/`UserAgentGenerator`/`UserAgentInfo`/`Component`/`Device` behavior, or any existing test.
- Execute a real `npm publish` (or the plugin's equivalent live-publish Gradle task) from this session.
- Commit any credential value (npm token) anywhere in the repo.

</frozen-after-approval>

## Code Map

- `gradle/libs.versions.toml` -- add an `npmPublish` (or similarly named, avoiding collision with the existing Maven `mavenPublish` alias) version entry for `org.jetbrains.kotlin.npm-publish` and a plugin alias.
- `library/build.gradle.kts` -- apply the plugin, add the `npmPublish { }` block per Boundaries, and whatever task wiring is needed to bundle `NOTICE`/`LICENSE` into the packaged npm output.
- `library/NOTICE` -- extend the existing dependency-license-audit note (from Story 3.1) to cover the npm package specifically, or note it's the same audit if nothing npm-specific changes it.
- `README.md` -- replace the "(A JS/npm package is published separately -- see Story 3.2.)" placeholder with the real `npm install @lempert/user-agent` instructions once resolved.

## Tasks & Acceptance

**Execution:**
- [ ] `gradle/libs.versions.toml`, relevant `build.gradle.kts` -- register the `org.jetbrains.kotlin.npm-publish` plugin (verify exact latest 3.7.x patch version)
- [ ] `library/build.gradle.kts` -- configure `npmPublish { }`: organization `lempert`, package name `user-agent`, version `0.1.0`, registry auth via `NPM_TOKEN`, full `packageJson { }` metadata (MIT license, homepage, description, repository)
- [ ] Bundle `NOTICE`/`LICENSE` into the packaged npm output via a supported mechanism
- [ ] Document the npm-specific dependency audit (no extra runtime npm deps, no copyleft) in `library/NOTICE` or a clearly-linked location
- [ ] Run the plugin's local packaging/dry-run task and inspect the generated package contents (unpack the tarball or inspect the staged package directory) to confirm package name (`@lempert/user-agent`), version, `package.json` metadata, and bundled `NOTICE`/`LICENSE`
- [ ] Update `README.md`'s npm placeholder with real install instructions

**Acceptance Criteria:**
- Given the `org.jetbrains.kotlin.npm-publish` plugin configured in the library's build, when the local packaging task is run, then a package named `@lempert/user-agent` version `0.1.0` is produced from the `js` target's dist output, with correct `package.json` metadata (license: MIT, homepage, repository).
- Given the vendored `uap-core` Apache-2.0 snapshot, when the packaged npm output is inspected, then a `NOTICE` file preserving the Apache-2.0 attribution is present in the package contents.
- Given the npm package's own dependency footprint, when audited, then it declares no additional runtime npm dependencies and pulls in nothing copyleft-licensed, and this is documented.

## Spec Change Log

## Design Notes

The actual live publish (`npm publish` or the plugin's equivalent Gradle task, e.g. `publishJsPackageToNpmjsRegistry`) is intentionally not run by this story -- hand the exact command to the human at the end, along with a reminder to confirm `NPM_TOKEN` is a genuine npmjs.org automation/publish token before running it (this session only confirmed the env var's presence, not its validity, and the human previously indicated npm wasn't configured yet).

## Verification

**Actual real task names (discovered via `./gradlew :library:tasks --all`):**
- `packJsPackage` -- local-only: assembles the package contents and runs `npm pack` to produce a tarball under `library/build/packages/`. Depends on `assembleJsPackage` (stages the package directory) and the `js` target's production compile/dist tasks. **This is the task actually run for this story's verification.**
- `publishJsPackageToNpmjsRegistry` -- the live publish task. **Never run in this session**, per the spec's boundaries.

**Commands actually run:**
- `./gradlew :library:packJsPackage` -- succeeded; produced `library/build/packages/lempert-user-agent-0.1.0.tgz`.
- Unpacked that tarball by hand (`tar xzf ... `) and read `package/package.json` directly, rather than trusting the Gradle task's success alone (same empirical-verification standard used for Story 3.1's AAR bundling bug).
- `./gradlew :library:build` -- full build (all four targets + tests) still succeeds after the plugin/config additions.

**Actual results:**
- Tarball contents (11 files): `LICENSE`, `NOTICE`, `README.md`, `package.json`, `library.mjs` + `.map`, `library.d.mts`, `kotlin-kotlin-stdlib.mjs` + `.map`, `kotlin_org_jetbrains_kotlin_kotlin_dom_api_compat.mjs` + `.map`.
- `package.json`: `"name": "@lempert/user-agent"`, `"version": "0.1.0"`, `"license": "MIT"`, `"homepage": "https://github.com/ido-lempert/kmp-user-agent"`, `"repository": {"type": "git", "url": "https://github.com/ido-lempert/kmp-user-agent.git"}`, `"main": "library.mjs"`, `"types": "library.d.mts"`. No `dependencies`/`devDependencies`/`peerDependencies`/`optionalDependencies` key present at all.
- `types` was **not** auto-populated by the plugin (only `main` was) -- found by inspecting the first packed tarball before adding an explicit `types.set("library.d.mts")` to the `packageJson { }` block; re-verified present after the fix.
- NOTICE/LICENSE bundling confirmed by direct inspection of unpacked tarball contents, not just Gradle task success.

**Plugin API shape:** matched the assumed shape in the frozen spec almost exactly (confirmed by decompiling the resolved `npm-publish-gradle-plugin-3.7.0.jar` from the Gradle cache -- its classes still live under `dev.petuska.npm.publish.*`). The one real gap found: `types` on `packageJson { }` needs to be set explicitly; it is not inferred from `generateTypeScriptDefinitions()` the way `main` is inferred from the JS target's output module name.

**Note:** as intended, `publishJsPackageToNpmjsRegistry` (the live publish task) was never run in this session. Handing to the human: run `./gradlew :library:publishJsPackageToNpmjsRegistry` from your own terminal once you've confirmed `NPM_TOKEN` is a genuine npmjs.org publish/automation token (this session only checked that the env var is *set*, never its value or validity).
