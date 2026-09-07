#!/usr/bin/env bash
# Verifies that a release tag's version matches both the Maven coordinates
# version and the npm package version declared in a build.gradle.kts-style
# file -- `coordinates("site.lempert", "user-agent", "<version>")` for
# Maven, and `npmPublish { packages { named("js") { version.set("<version>") } } }`
# for npm. One release must ship the same version number everywhere.
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
# "v" stripped) matches exactly one coordinates(...) version AND exactly one
# npm packages { named("js") { version.set(...) } } version in the file.
# Exits 1 with an `::error::`-prefixed message (GitHub Actions annotation
# format) for each failure case:
#   - file not found
#   - no coordinates(...) version found
#   - more than one coordinates(...) version found (ambiguous match)
#   - tag version does not match the extracted Maven coordinates version
#   - no npm package version found
#   - more than one npm package version found (ambiguous match)
#   - tag version does not match the extracted npm package version
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

# Scope the npm-version extraction to the npmPublish { ... } block only --
# a blind whole-file grep for `version.set("N.N.N")` would also match any
# unrelated `version.set(...)` call elsewhere in the file (Gradle's
# Property-API `.set(...)` idiom is used pervasively throughout
# build.gradle.kts), which could falsely trip the ambiguity guard below or,
# worse, silently misattribute an unrelated version as the npm version.
# Isolate the npmPublish block first by brace-depth counting (handles the
# nested registries {}/packages {}/named("js") {}/packageJson {} blocks
# inside it), then only apply the version.set(...) capture within that
# isolated text.
NPM_BLOCK=$(awk '
  BEGIN { depth = 0; found = 0 }
  {
    if (!found) {
      if ($0 ~ /npmPublish[ \t]*\{/) {
        found = 1
      } else {
        next
      }
    }
    print
    line = $0
    opens = gsub(/\{/, "{", line)
    closes = gsub(/\}/, "}", line)
    depth += opens - closes
    if (found && depth == 0) exit
  }
' "$GRADLE_FILE")

NPM_MATCHES=$(printf '%s\n' "$NPM_BLOCK" | sed -n 's/.*version\.set("\([0-9.]*\)").*/\1/p')

NPM_MATCH_COUNT=0
if [ -n "$NPM_MATCHES" ]; then
  NPM_MATCH_COUNT=$(printf '%s\n' "$NPM_MATCHES" | sed '/^$/d' | wc -l | tr -d ' ')
fi

if [ "$NPM_MATCH_COUNT" -eq 0 ]; then
  echo "::error::Could not extract a version from ${GRADLE_FILE}'s npmPublish { packages { named(\"js\") { version.set(...) } } } call."
  exit 1
fi

if [ "$NPM_MATCH_COUNT" -gt 1 ]; then
  echo "::error::Found ${NPM_MATCH_COUNT} lines matching version.set(...) in ${GRADLE_FILE}, expected exactly one. Remove the extra occurrence(s) so the npm package version is unambiguous before releasing."
  exit 1
fi

NPM_VERSION="$NPM_MATCHES"

if [ "$TAG_VERSION" != "$NPM_VERSION" ]; then
  echo "::error::Tag '${TAG}' (version '${TAG_VERSION}') does not match ${GRADLE_FILE}'s npm package version '${NPM_VERSION}'. Update npmPublish { packages { named(\"js\") { version.set(...) } } } to '${TAG_VERSION}' and commit before pushing this tag, or push the correct tag."
  exit 1
fi

echo "Version check passed: tag ${TAG} matches ${GRADLE_FILE} coordinates version ${GRADLE_VERSION} and npm package version ${NPM_VERSION}."
