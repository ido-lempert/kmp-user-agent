#!/usr/bin/env bash
# Verifies that a release tag's version matches the Maven coordinates
# version declared in a build.gradle.kts-style file's
# `coordinates("site.lempert", "user-agent", "<version>")` call.
#
# Extracted out of `.github/workflows/release.yml`'s "Verify tag version
# matches library/build.gradle.kts coordinates" step so this logic is
# covered by scripts/verify-release-version.test.sh (run on every push/PR
# via ci.yml) instead of only ever executing -- untested -- when a real
# `vX.Y.Z` release tag is pushed.
#
# Usage:
#   verify-release-version.sh <tag> <path-to-gradle-file>
#
# Exits 0 and prints a pass message when the tag's version (with a leading
# "v" stripped) matches exactly one coordinates(...) version in the file.
# Exits 1 with an `::error::`-prefixed message (GitHub Actions annotation
# format) for each failure case:
#   - file not found
#   - no coordinates(...) version found
#   - more than one coordinates(...) version found (ambiguous match)
#   - tag version does not match the extracted gradle version
set -euo pipefail

if [ "$#" -ne 2 ]; then
  echo "Usage: $0 <tag> <path-to-build.gradle.kts>" >&2
  exit 2
fi

TAG="$1"
GRADLE_FILE="$2"
TAG_VERSION="${TAG#v}"

if [ ! -f "$GRADLE_FILE" ]; then
  echo "::error::File not found: ${GRADLE_FILE}"
  exit 1
fi

MATCHES=$(sed -n 's/.*coordinates("site\.lempert", "user-agent", "\([0-9.]*\)").*/\1/p' "$GRADLE_FILE")

MATCH_COUNT=0
if [ -n "$MATCHES" ]; then
  MATCH_COUNT=$(printf '%s\n' "$MATCHES" | sed '/^$/d' | wc -l | tr -d ' ')
fi

if [ "$MATCH_COUNT" -eq 0 ]; then
  echo "::error::Could not extract a version from ${GRADLE_FILE}'s coordinates(\"site.lempert\", \"user-agent\", ...) call."
  exit 1
fi

if [ "$MATCH_COUNT" -gt 1 ]; then
  echo "::error::Found ${MATCH_COUNT} lines matching coordinates(\"site.lempert\", \"user-agent\", ...) in ${GRADLE_FILE}, expected exactly one. Remove the extra occurrence(s) (e.g. a stray comment referencing the same coordinates call) so the version is unambiguous before releasing."
  exit 1
fi

GRADLE_VERSION="$MATCHES"

if [ "$TAG_VERSION" != "$GRADLE_VERSION" ]; then
  echo "::error::Tag '${TAG}' (version '${TAG_VERSION}') does not match ${GRADLE_FILE}'s Maven coordinates version '${GRADLE_VERSION}'. Update coordinates(...) to '${TAG_VERSION}' and commit before pushing this tag, or push the correct tag."
  exit 1
fi

echo "Version check passed: tag ${TAG} matches ${GRADLE_FILE} coordinates version ${GRADLE_VERSION}."
