#!/usr/bin/env bash
# Fixture-based test for scripts/verify-release-version.sh -- the version
# check that release.yml runs on every real `vX.Y.Z` tag push. That
# real-tag path only ever exercises the "exact match" case in practice, so
# this drives the script against fixture build.gradle.kts-style files
# covering match, mismatch, and the multi-match ambiguity guard, and wires
# into ci.yml so a regression here is caught on every push/PR, not only
# when a release tag happens to be pushed. Mirrors the
# check()/PASS//FAIL/ALL CHECKS PASSED assertion style of
# docs-site/scripts/verify-generate-demo.mjs and
# docs-site/scripts/verify-consent-decision.mjs.
#
# Run directly: bash scripts/verify-release-version.test.sh
# Exits non-zero if any assertion fails.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VERIFY_SCRIPT="${SCRIPT_DIR}/verify-release-version.sh"
FIXTURE_DIR="$(mktemp -d)"
trap 'rm -rf "$FIXTURE_DIR"' EXIT

FAILURES=0

check() {
  local label="$1"
  local condition="$2"
  if [ "$condition" -eq 0 ]; then
    echo "PASS: ${label}"
  else
    FAILURES=$((FAILURES + 1))
    echo "FAIL: ${label}"
  fi
}

# (a) Exact match: tag version equals the single coordinates(...) version
# -> exit 0, prints a pass message.
cat > "${FIXTURE_DIR}/match.gradle.kts" <<'EOF'
mavenPublishing {
  coordinates("site.lempert", "user-agent", "1.2.3")
}
EOF
OUTPUT_A=$("$VERIFY_SCRIPT" "v1.2.3" "${FIXTURE_DIR}/match.gradle.kts" 2>&1)
STATUS_A=$?
check "(a) exact match: exits 0" "$([ "$STATUS_A" -eq 0 ] && echo 0 || echo 1)"
check "(a) exact match: prints pass message" "$(echo "$OUTPUT_A" | grep -q 'Version check passed' && echo 0 || echo 1)"

# (b) Mismatch: tag version differs from the single coordinates(...)
# version -> exit 1, ::error:: naming both versions.
cat > "${FIXTURE_DIR}/mismatch.gradle.kts" <<'EOF'
mavenPublishing {
  coordinates("site.lempert", "user-agent", "1.2.3")
}
EOF
OUTPUT_B=$("$VERIFY_SCRIPT" "v9.9.9" "${FIXTURE_DIR}/mismatch.gradle.kts" 2>&1)
STATUS_B=$?
check "(b) mismatch: exits non-zero" "$([ "$STATUS_B" -ne 0 ] && echo 0 || echo 1)"
check "(b) mismatch: ::error:: names both versions" "$(echo "$OUTPUT_B" | grep -q "::error::.*'9.9.9'.*'1.2.3'" && echo 0 || echo 1)"

# (c) Multi-match guard: two coordinates(...) lines (e.g. a stray comment
# repeating the real call) -> exit 1, ::error:: naming the ambiguity,
# instead of silently comparing a multi-line value.
cat > "${FIXTURE_DIR}/multi-match.gradle.kts" <<'EOF'
mavenPublishing {
  coordinates("site.lempert", "user-agent", "1.2.3")
  // coordinates("site.lempert", "user-agent", "1.2.3")
}
EOF
OUTPUT_C=$("$VERIFY_SCRIPT" "v1.2.3" "${FIXTURE_DIR}/multi-match.gradle.kts" 2>&1)
STATUS_C=$?
check "(c) multi-match guard: exits non-zero" "$([ "$STATUS_C" -ne 0 ] && echo 0 || echo 1)"
check "(c) multi-match guard: ::error:: names the ambiguity" "$(echo "$OUTPUT_C" | grep -q '::error::.*[Ff]ound 2 lines' && echo 0 || echo 1)"
check "(c) multi-match guard: does not print a pass message" "$(echo "$OUTPUT_C" | grep -q 'Version check passed' && echo 1 || echo 0)"

# (d) No match: no coordinates(...) call in the file -> exit 1, ::error::
# saying no version could be extracted.
cat > "${FIXTURE_DIR}/no-match.gradle.kts" <<'EOF'
mavenPublishing {
  // no coordinates() call here
}
EOF
OUTPUT_D=$("$VERIFY_SCRIPT" "v1.2.3" "${FIXTURE_DIR}/no-match.gradle.kts" 2>&1)
STATUS_D=$?
check "(d) no match: exits non-zero" "$([ "$STATUS_D" -ne 0 ] && echo 0 || echo 1)"
check "(d) no match: ::error:: says no version could be extracted" "$(echo "$OUTPUT_D" | grep -q '::error::Could not extract a version' && echo 0 || echo 1)"

echo ""
if [ "$FAILURES" -gt 0 ]; then
  echo "${FAILURES} CHECK(S) FAILED"
  exit 1
else
  echo "ALL CHECKS PASSED"
  exit 0
fi
