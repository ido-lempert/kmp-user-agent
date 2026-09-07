---
title: 'Automate iOS/SPM release verification and GitHub Release creation'
type: 'feature'
created: '2026-09-07'
status: 'done'
review_loop_iteration: 0
context: []
baseline_commit: '67c8eab53739b731df96d6a3b1384c7b1af2dd4e'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Cutting an SPM release is entirely manual and undocumented today (`deferred-work.md`, "No documented maintainer process exists"). The only real release (`0.2.0`) used a bare (no `v`) git tag — but `release.yml`'s Maven/npm automation (already shipped) only triggers on `v*` tags. Human decision this session: standardize on `v`-prefixed tags going forward (confirmed SPM itself accepts both formats for consumer version resolution).

**Approach:** Extend `release.yml` (same job, same tag trigger, no new file) with a "verify + release" step for iOS: rebuild the XCFramework fresh, verify its checksum matches what the maintainer already committed in `Package.swift` for this tag, then create the GitHub Release and upload the zip asset. The maintainer still computes the checksum and commits `Package.swift` locally before tagging (SPM requires this — a tag is immutable, so CI can't fix it after the fact) — but gets a new local helper script instead of the fully ad hoc process used for `0.2.0`.

## Boundaries & Constraints

**Always:**
- Reuse the exact process already used for the real `0.2.0` release (per `spec-5-6-ios-usage-guide.md`): `./gradlew :library:assembleLibraryReleaseXCFramework` → `zip -r -X Library.xcframework.zip Library.xcframework` (output at `library/build/XCFrameworks/release/Library.xcframework`) → `swift package compute-checksum Library.xcframework.zip`.
- New local helper script (e.g. `scripts/prepare-ios-release.sh`) runs that same sequence and prints the exact `url`/`checksum` values to paste into `Package.swift`, plus a reminder of the required ordering: commit `Package.swift` → push to `origin` → tag → push tag. Does not edit `Package.swift` itself or touch git — print-only, maintainer still pastes and commits by hand (consistent with "no CI/tooling auto-commits" already established for Maven/npm).
- CI step (same `release.yml` job, after the existing Maven/npm steps or interleaved sensibly): rebuild the XCFramework fresh (never trust a local artifact), zip it the same way, compute its checksum, and extract the checksum currently committed in `Package.swift` — fail loudly (`::error::`) if they disagree, naming both values. This is the safety net; it never modifies `Package.swift`.
- If (and only if) the checksum matches, create a GitHub Release for the exact tag that triggered the workflow (`${{ github.ref_name }}`, e.g. `v0.3.0` — same tag, no stripping/renaming) if one doesn't already exist, and upload the zip as `Library.xcframework.zip`, matching what `Package.swift`'s URL expects. Use `gh release create`/`gh release upload` (preinstalled on GitHub-hosted runners) rather than a third-party action.
- The job's `permissions: contents: read` must be widened to `contents: write` (needed for `gh release create`) — scope this narrowly and note why in a comment.
- Leave the existing `0.2.0` release/tag untouched — this is a one-time historical exception, not something to migrate.
- Add a short note to `docs-site/guide/ios.md` (only if something there would now read as stale/misleading given the `v`-prefix convention change) — read it first, don't rewrite content that's already accurate (SPM's own `from: "0.2.0"` version-requirement syntax in consumer code is unaffected by the tag's `v` prefix).
- CI automation only — no live release cut this session.

**Ask First:** None — trigger convention (`v`-prefixed going forward) and the verify-only design were already decided by the human this session.

**Never:**
- Never have CI commit `Package.swift` or move/recreate a tag.
- Never touch the existing `0.2.0` tag or release.
- Never trust a pre-built/cached XCFramework in the verify step — always rebuild fresh in CI.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Checksum matches | Maintainer correctly computed and committed the checksum before tagging | CI verify passes; GitHub Release created with the zip asset | N/A |
| Checksum mismatch | Maintainer forgot to update `Package.swift`, or computed it wrong | CI fails loudly before any release is created, naming both checksums | Non-zero exit, no release created |
| Release already exists for this tag | Re-run after a transient earlier failure | `gh release create` step doesn't fail/duplicate — checks existence first | Skips creation, still verifies+uploads the asset (or reports clearly if the asset already exists) |

</frozen-after-approval>

## Code Map

- `.github/workflows/release.yml` -- edit: widen `permissions.contents` to `write`; add XCFramework rebuild + zip + checksum-verify step; add GitHub Release creation/asset-upload step.
- `scripts/prepare-ios-release.sh` -- NEW. Local helper: build, zip, compute checksum, print `Package.swift` values + ordering reminder.
- `library/build.gradle.kts:503-517` -- read-only; confirms `XCFramework("Library")` config and the `assembleLibraryReleaseXCFramework` task this spec invokes.
- `Package.swift` -- read-only in this spec (maintainer edits it manually per the established process); CI only reads its committed checksum to verify against.
- `_bmad-output/implementation-artifacts/spec-5-6-ios-usage-guide.md:43,55-58` -- read-only; the exact manual process this spec automates the verification/release half of.
- `docs-site/guide/ios.md` -- read-only unless the `v`-prefix convention change makes something there stale.

## Tasks & Acceptance

**Execution:**
- [x] `scripts/prepare-ios-release.sh` -- create: build, zip, compute-checksum, print values + ordering reminder
- [x] `.github/workflows/release.yml` -- widen permissions to `contents: write`; add rebuild+zip+checksum-verify step (fails loudly on mismatch, never modifies `Package.swift`)
- [x] `.github/workflows/release.yml` -- add GitHub Release creation (idempotent) + asset upload step, gated on the checksum verification passing
- [x] Check `docs-site/guide/ios.md` for now-stale wording given the `v`-prefix tag convention; update only if needed

**Acceptance Criteria:**
- Given `Package.swift`'s committed checksum matches a fresh CI rebuild, when the tag-triggered workflow runs, then a GitHub Release is created at that exact tag with `Library.xcframework.zip` attached.
- Given the checksums disagree, when the workflow runs, then it fails before creating any release, naming both checksum values.
- Given the workflow re-runs against a tag that already has a release, when it runs, then it doesn't fail/duplicate — it handles the already-exists case cleanly.
- Given the local helper script runs, when it finishes, then it has not modified `Package.swift` or touched git — output only.

## Spec Change Log

## Verification

**Commands:**
- `./gradlew :library:assembleLibraryReleaseXCFramework` -- expected: succeeds, produces `library/build/XCFrameworks/release/Library.xcframework`.
- `bash scripts/prepare-ios-release.sh` (run locally) -- expected: prints a real, valid checksum and the correctly-formed `Package.swift` URL for a test tag.
- `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/release.yml'))"` -- expected: parses cleanly.
- `actionlint .github/workflows/release.yml` -- expected: no errors, if available.

**Manual checks (if no CLI):**
- Read the finished workflow: the release-creation step never runs unless the checksum-verify step passed; `permissions` is scoped no wider than needed.

## Suggested Review Order

**Verification logic**

- Entry point: the scoped checksum/url verification script — protects against the exact unscoped-regex bug already found and fixed on the npm side, plus a real gap this round's review caught that the original design missed (the URL's tag segment was never checked, only the checksum).
  [`verify-ios-checksum.sh`](../../scripts/verify-ios-checksum.sh)

- Fixture coverage for both checks, plus the ambiguity/decoy/empty-input guards, wired into CI on every push/PR.
  [`verify-ios-checksum.test.sh`](../../scripts/verify-ios-checksum.test.sh)

**Release pipeline**

- CI step: rebuilds fresh, guards against `.DS_Store` zip pollution and empty checksum output, then delegates comparison to the testable script.
  [`release.yml`](../../.github/workflows/release.yml)

- GitHub Release creation, idempotent on re-run, gated implicitly on the verify step's success.
  [`release.yml`](../../.github/workflows/release.yml)

**Local maintainer tooling**

- `prepare-ios-release.sh`: same build/zip/checksum sequence as CI, plus non-fatal warnings for a dirty working tree or a non-`v`-prefixed tag — print-only, never touches `Package.swift` or git.
  [`prepare-ios-release.sh`](../../scripts/prepare-ios-release.sh)

**Documentation**

- Tag-convention note explaining the `0.2.0` (historical) vs `v0.3.0`-going-forward split, and that SPM resolves both.
  [`ios.md`](../../docs-site/guide/ios.md)
