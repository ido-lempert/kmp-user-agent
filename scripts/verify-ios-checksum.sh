#!/usr/bin/env bash
# Verifies two things about the iOS XCFramework release for a given tag,
# against what's committed in Package.swift's
# `.binaryTarget(name: "Library", ...)` call:
#   1. The freshly-computed checksum (passed in as an argument -- this
#      script does not rebuild/zip/compute anything itself) matches the
#      checksum committed in Package.swift.
#   2. The committed url's release-tag segment (between
#      `/releases/download/` and `/Library.xcframework.zip`) matches the
#      tag actually being released.
#
# Extracted out of `.github/workflows/release.yml`'s "Verify iOS
# XCFramework checksum matches Package.swift" step so this logic is
# covered by scripts/verify-ios-checksum.test.sh (run on every push/PR via
# ci.yml) instead of only ever executing -- untested -- when a real
# `vX.Y.Z` release tag is pushed. Mirrors
# scripts/verify-release-version.sh's structure and error-handling
# discipline (three-tier: not found / ambiguous / mismatch), including its
# fix for the same class of bug: the original inline CI step's checksum
# extraction was a blind whole-file
# `sed -n 's/.*checksum: "\([a-f0-9]*\)".*/\1/p' Package.swift`, which
# would match ANY `checksum: "<hex>"`-shaped line anywhere in the file, not
# just the one inside the .binaryTarget(name: "Library", ...) call. This
# version isolates that call first (by paren-depth counting, mirroring how
# verify-release-version.sh isolates the npmPublish block by brace-depth
# counting), then only extracts checksum/url from within it.
#
# It also did not previously verify the committed url's tag segment at
# all -- only the checksum. A maintainer could paste a stale url (wrong
# tag) alongside a correctly-recomputed checksum and CI would still pass,
# leaving Package.swift pointing consumers at the wrong release's asset.
#
# Usage:
#   verify-ios-checksum.sh <tag> <computed-checksum> <path-to-Package.swift>
#
# <computed-checksum> is the checksum the caller already computed via
# `swift package compute-checksum <zip>` against a freshly rebuilt zip --
# this script only compares it against what's committed, it never builds
# or computes anything.
#
# Exits 0 and prints a pass message when exactly one
# `.binaryTarget(name: "Library", ...)` call is found in the file, its
# committed checksum equals <computed-checksum>, AND its committed url's
# tag segment equals <tag>.
# Exits 1 with an `::error::`-prefixed message (GitHub Actions annotation
# format) for each failure case:
#   - file not found
#   - no .binaryTarget(name: "Library", ...) call found
#   - more than one .binaryTarget(name: "Library", ...) call found (ambiguous)
#   - no checksum found inside the call
#   - more than one checksum found inside the call (ambiguous)
#   - computed checksum does not match the committed checksum
#   - no url found inside the call
#   - more than one url found inside the call (ambiguous)
#   - committed url does not have the expected
#     ".../releases/download/<tag>/Library.xcframework.zip" shape
#   - committed url's tag segment does not match <tag>
set -euo pipefail

if [ "$#" -ne 3 ]; then
  echo "Usage: $0 <tag> <computed-checksum> <path-to-Package.swift>" >&2
  exit 2
fi

TAG="$1"
COMPUTED_CHECKSUM="$2"
PACKAGE_FILE="$3"

if [ ! -f "$PACKAGE_FILE" ]; then
  echo "::error::File not found: ${PACKAGE_FILE}"
  exit 1
fi

if [ -z "$COMPUTED_CHECKSUM" ]; then
  echo "::error::Computed checksum argument is empty. 'swift package compute-checksum' must have produced empty/malformed output -- check the build/zip step that ran before this script."
  exit 1
fi

# Isolate each .binaryTarget(...) call by paren-depth counting (handles
# the call spanning multiple lines), keeping only the ones that declare
# name: "Library" -- the target this project actually distributes. A
# decoy checksum-shaped or url-shaped string elsewhere in the file (e.g.
# a comment, or some other binaryTarget call) must never be picked up.
BLOCKS=$(awk '
  BEGIN { depth = 0; capturing = 0 }
  {
    line = $0
    if (!capturing) {
      if (line ~ /\.binaryTarget[ \t]*\(/) {
        capturing = 1
        depth = 0
        buf = ""
      } else {
        next
      }
    }
    buf = buf line "\n"
    opens = gsub(/\(/, "(", line)
    closes = gsub(/\)/, ")", line)
    depth += opens - closes
    if (capturing && depth == 0) {
      if (buf ~ /name:[ \t]*"Library"/) {
        print "@@@BLOCK-START@@@"
        printf "%s", buf
        print "@@@BLOCK-END@@@"
      }
      capturing = 0
    }
  }
' "$PACKAGE_FILE")

BLOCK_COUNT=$(printf '%s\n' "$BLOCKS" | grep -c '@@@BLOCK-START@@@' || true)

if [ "$BLOCK_COUNT" -eq 0 ]; then
  echo "::error::Could not find a .binaryTarget(name: \"Library\", ...) call in ${PACKAGE_FILE}."
  exit 1
fi

if [ "$BLOCK_COUNT" -gt 1 ]; then
  echo "::error::Found ${BLOCK_COUNT} .binaryTarget(name: \"Library\", ...) calls in ${PACKAGE_FILE}, expected exactly one. Remove the extra occurrence(s) so the checksum/url are unambiguous before releasing."
  exit 1
fi

BLOCK=$(printf '%s\n' "$BLOCKS" | sed -n '/@@@BLOCK-START@@@/,/@@@BLOCK-END@@@/p' | sed '1d;$d')

COMMITTED_CHECKSUM=$(printf '%s\n' "$BLOCK" | sed -n 's/.*checksum: "\([a-f0-9]*\)".*/\1/p' | sed '/^$/d')
COMMITTED_CHECKSUM_COUNT=0
if [ -n "$COMMITTED_CHECKSUM" ]; then
  COMMITTED_CHECKSUM_COUNT=$(printf '%s\n' "$COMMITTED_CHECKSUM" | wc -l | tr -d ' ')
fi

if [ "$COMMITTED_CHECKSUM_COUNT" -eq 0 ]; then
  echo "::error::Could not extract a checksum from ${PACKAGE_FILE}'s .binaryTarget(name: \"Library\", ...) call."
  exit 1
fi

if [ "$COMMITTED_CHECKSUM_COUNT" -gt 1 ]; then
  echo "::error::Found ${COMMITTED_CHECKSUM_COUNT} checksum: \"...\" occurrences inside ${PACKAGE_FILE}'s .binaryTarget(name: \"Library\", ...) call, expected exactly one."
  exit 1
fi

if [ "$COMPUTED_CHECKSUM" != "$COMMITTED_CHECKSUM" ]; then
  echo "::error::iOS XCFramework checksum mismatch: freshly rebuilt checksum '${COMPUTED_CHECKSUM}' does not match the checksum committed in ${PACKAGE_FILE} ('${COMMITTED_CHECKSUM}'). Recompute via scripts/prepare-ios-release.sh, update Package.swift's checksum, commit, push, and re-tag -- CI never modifies Package.swift itself."
  exit 1
fi

COMMITTED_URL=$(printf '%s\n' "$BLOCK" | sed -n 's/.*url: "\([^"]*\)".*/\1/p' | sed '/^$/d')
COMMITTED_URL_COUNT=0
if [ -n "$COMMITTED_URL" ]; then
  COMMITTED_URL_COUNT=$(printf '%s\n' "$COMMITTED_URL" | wc -l | tr -d ' ')
fi

if [ "$COMMITTED_URL_COUNT" -eq 0 ]; then
  echo "::error::Could not extract a url from ${PACKAGE_FILE}'s .binaryTarget(name: \"Library\", ...) call."
  exit 1
fi

if [ "$COMMITTED_URL_COUNT" -gt 1 ]; then
  echo "::error::Found ${COMMITTED_URL_COUNT} url: \"...\" occurrences inside ${PACKAGE_FILE}'s .binaryTarget(name: \"Library\", ...) call, expected exactly one."
  exit 1
fi

COMMITTED_URL_TAG=$(printf '%s' "$COMMITTED_URL" | sed -n 's#.*/releases/download/\([^/]*\)/Library\.xcframework\.zip$#\1#p')

if [ -z "$COMMITTED_URL_TAG" ]; then
  echo "::error::Could not extract a release-tag segment from ${PACKAGE_FILE}'s committed url ('${COMMITTED_URL}'); expected the shape '.../releases/download/<tag>/Library.xcframework.zip'."
  exit 1
fi

if [ "$TAG" != "$COMMITTED_URL_TAG" ]; then
  echo "::error::iOS release url tag mismatch: workflow tag '${TAG}' does not match the tag segment '${COMMITTED_URL_TAG}' in ${PACKAGE_FILE}'s committed url ('${COMMITTED_URL}'). Update Package.swift's .binaryTarget(...) url to point at tag '${TAG}' before releasing."
  exit 1
fi

echo "iOS XCFramework verification passed: checksum ${COMMITTED_CHECKSUM} and url tag '${COMMITTED_URL_TAG}' both match tag ${TAG} in ${PACKAGE_FILE}."
