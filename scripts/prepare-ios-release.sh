#!/usr/bin/env bash
# Local helper for preparing an iOS SPM release: rebuilds the XCFramework,
# zips it, computes its checksum, and prints the exact `url`/`checksum`
# values to paste into Package.swift's `.binaryTarget(...)` call --
# reusing the exact command sequence already used for the real 0.2.0
# release (per
# _bmad-output/implementation-artifacts/spec-5-6-ios-usage-guide.md):
#   ./gradlew :library:assembleLibraryReleaseXCFramework
#   zip -r -X Library.xcframework.zip Library.xcframework
#   swift package compute-checksum Library.xcframework.zip
#
# Print-only: this script does NOT edit Package.swift and does NOT touch
# git in any way (no commit, no tag, no push) -- consistent with this
# project's "no CI/tooling auto-commits" convention already established
# for the Maven/npm release process (see release.yml). The maintainer
# pastes the printed values into Package.swift and commits/tags by hand,
# in this exact order (SPM resolves Package.swift from the tagged
# commit, so the tag must come after Package.swift is pushed, not
# before):
#   1. Paste the printed `url` and `checksum` into Package.swift's
#      .binaryTarget(...) call.
#   2. Commit Package.swift (and any other release-related changes).
#   3. Push the commit to origin.
#   4. Create the tag on that pushed commit: git tag <tag>
#   5. Push the tag: git push origin <tag>
#      This triggers .github/workflows/release.yml, which rebuilds the
#      XCFramework fresh in CI, verifies (via scripts/verify-ios-checksum.sh)
#      that its checksum and the committed url's tag both still match what
#      you just committed, and -- only if both match -- creates the
#      GitHub Release and uploads Library.xcframework.zip as its asset.
#
# Usage:
#   scripts/prepare-ios-release.sh <tag>
#
# <tag> is the release tag you intend to push, e.g. v0.3.0 (`v`-prefixed
# -- release.yml's automation only triggers on `v*` tags; the historical
# bare `0.2.0` tag is a one-time exception, not the pattern to follow
# going forward). The tag is used only to construct the printed `url`
# value; this script does not create, check, or push the tag itself.
set -euo pipefail

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 <tag>" >&2
  echo "  e.g. $0 v0.3.0" >&2
  exit 2
fi

TAG="$1"

if [ ! -x "./gradlew" ]; then
  echo "::error::./gradlew not found -- run this script from the repository root." >&2
  exit 1
fi

if ! command -v swift >/dev/null 2>&1; then
  echo "::error::'swift' command not found -- install Xcode/the Swift toolchain before running this script." >&2
  exit 1
fi

if ! command -v zip >/dev/null 2>&1; then
  echo "::error::'zip' command not found -- install it before running this script." >&2
  exit 1
fi

# Non-fatal: release.yml's automation only triggers on `v*` tags (see its
# `on: push: tags: - 'v*'` trigger). A tag that doesn't match won't fire
# the CI verify+release job -- this script still runs and prints values
# either way (it never creates/pushes the tag itself), but the maintainer
# should know before they tag and push.
case "$TAG" in
  v[0-9]*) ;;
  *)
    echo "WARNING: tag '${TAG}' does not match the 'v<digit>...' pattern (e.g. v0.3.0). release.yml's automation only triggers on 'v*' tags -- pushing this tag as-is will NOT fire the release workflow." >&2
    ;;
esac

# Non-fatal: a dirty working tree could produce a checksum here that
# doesn't match what's later actually committed and pushed (e.g.
# uncommitted source changes that affect the build output), causing CI's
# verify step to fail against a checksum this script never actually
# measured against the committed state.
if [ -n "$(git status --porcelain 2>/dev/null)" ]; then
  echo "WARNING: working tree has uncommitted changes. The checksum computed by this run may not match what ends up committed -- consider committing/stashing first, or re-run this script right before pasting values into Package.swift." >&2
fi

REPO_URL="https://github.com/ido-lempert/kmp-user-agent"
XCFRAMEWORK_DIR="library/build/XCFrameworks/release"
ZIP_NAME="Library.xcframework.zip"
ZIP_PATH="${XCFRAMEWORK_DIR}/${ZIP_NAME}"

echo "==> Building XCFramework (./gradlew :library:assembleLibraryReleaseXCFramework)..."
./gradlew :library:assembleLibraryReleaseXCFramework

if [ ! -d "${XCFRAMEWORK_DIR}/Library.xcframework" ]; then
  echo "::error::Expected ${XCFRAMEWORK_DIR}/Library.xcframework to exist after the assemble task, but it does not." >&2
  exit 1
fi

# Remove any stale zip from a previous run before re-zipping, so
# `zip -r` never appends to/reuses an old archive.
rm -f "${ZIP_PATH}"

echo "==> Zipping (zip -r -X ${ZIP_NAME} Library.xcframework)..."
# -x '*.DS_Store': a stray .DS_Store inside the XCFramework directory
# (plausible on any macOS machine) would produce a different zip/checksum
# between two otherwise-identical builds, causing a false checksum
# mismatch against CI's own rebuild.
(cd "${XCFRAMEWORK_DIR}" && zip -r -X "${ZIP_NAME}" Library.xcframework -x '*.DS_Store')

echo "==> Computing checksum (swift package compute-checksum ${ZIP_NAME})..."
CHECKSUM=$(swift package compute-checksum "${ZIP_PATH}")

if [ -z "$CHECKSUM" ]; then
  echo "::error::'swift package compute-checksum' produced empty output for ${ZIP_PATH}." >&2
  exit 1
fi

RELEASE_URL="${REPO_URL}/releases/download/${TAG}/${ZIP_NAME}"

cat <<EOF

============================================================
iOS release artifact ready for tag: ${TAG}
============================================================
Zip built at: ${ZIP_PATH}

Paste these into Package.swift's .binaryTarget(...) call:

    url: "${RELEASE_URL}",
    checksum: "${CHECKSUM}"

This script has NOT modified Package.swift and has NOT touched git.
Required order from here:
  1. Paste the values above into Package.swift.
  2. Commit Package.swift.
  3. Push the commit to origin.
  4. git tag ${TAG}
  5. git push origin ${TAG}
     (this triggers .github/workflows/release.yml, which rebuilds the
     XCFramework fresh, verifies the checksum and url tag above still
     match, and only then creates the GitHub Release and uploads
     ${ZIP_NAME}.)
============================================================
EOF
