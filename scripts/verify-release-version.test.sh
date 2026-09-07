#!/usr/bin/env bash
# Fixture-based test for scripts/verify-release-version.sh -- the version
# check that release.yml runs on every real `vX.Y.Z` tag push. That
# real-tag path only ever exercises the "exact match" case in practice, so
# this drives the script against fixture build.gradle.kts-style files
# covering match, mismatch, and the multi-match ambiguity guard -- for both
# the Maven coordinates(...) version and the npm packages { named("js") {
# version.set(...) } } version -- and wires into ci.yml so a regression
# here is caught on every push/PR, not only when a release tag happens to
# be pushed. Mirrors the check()/PASS//FAIL/ALL CHECKS PASSED assertion
# style of docs-site/scripts/verify-generate-demo.mjs and
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

# (a) Exact match: tag version equals both the single coordinates(...)
# version and the single npm version.set(...) version -> exit 0, prints a
# pass message.
cat > "${FIXTURE_DIR}/match.gradle.kts" <<'EOF'
mavenPublishing {
  coordinates("site.lempert", "user-agent", "1.2.3")
}
npmPublish {
  packages {
    named("js") {
      version.set("1.2.3")
    }
  }
}
EOF
OUTPUT_A=$("$VERIFY_SCRIPT" "v1.2.3" "${FIXTURE_DIR}/match.gradle.kts" 2>&1)
STATUS_A=$?
check "(a) exact match: exits 0" "$([ "$STATUS_A" -eq 0 ] && echo 0 || echo 1)"
check "(a) exact match: prints pass message" "$(echo "$OUTPUT_A" | grep -q 'Version check passed' && echo 0 || echo 1)"

# (b) Maven mismatch: tag version differs from the single coordinates(...)
# version -> exit 1, ::error:: naming both versions, before the npm check
# ever runs.
cat > "${FIXTURE_DIR}/mismatch.gradle.kts" <<'EOF'
mavenPublishing {
  coordinates("site.lempert", "user-agent", "1.2.3")
}
npmPublish {
  packages {
    named("js") {
      version.set("1.2.3")
    }
  }
}
EOF
OUTPUT_B=$("$VERIFY_SCRIPT" "v9.9.9" "${FIXTURE_DIR}/mismatch.gradle.kts" 2>&1)
STATUS_B=$?
check "(b) Maven mismatch: exits non-zero" "$([ "$STATUS_B" -ne 0 ] && echo 0 || echo 1)"
check "(b) Maven mismatch: ::error:: names both versions" "$(echo "$OUTPUT_B" | grep -q "::error::.*'9.9.9'.*'1.2.3'" && echo 0 || echo 1)"

# (c) Multi-match guard: two coordinates(...) lines (e.g. a stray comment
# repeating the real call) -> exit 1, ::error:: naming the ambiguity,
# instead of silently comparing a multi-line value.
cat > "${FIXTURE_DIR}/multi-match.gradle.kts" <<'EOF'
mavenPublishing {
  coordinates("site.lempert", "user-agent", "1.2.3")
  // coordinates("site.lempert", "user-agent", "1.2.3")
}
npmPublish {
  packages {
    named("js") {
      version.set("1.2.3")
    }
  }
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
npmPublish {
  packages {
    named("js") {
      version.set("1.2.3")
    }
  }
}
EOF
OUTPUT_D=$("$VERIFY_SCRIPT" "v1.2.3" "${FIXTURE_DIR}/no-match.gradle.kts" 2>&1)
STATUS_D=$?
check "(d) no match: exits non-zero" "$([ "$STATUS_D" -ne 0 ] && echo 0 || echo 1)"
check "(d) no match: ::error:: says no version could be extracted" "$(echo "$OUTPUT_D" | grep -q '::error::Could not extract a version' && echo 0 || echo 1)"

# (e) npm mismatch: coordinates(...) matches the tag, but the npm
# version.set(...) version disagrees -> exit 1, ::error:: naming the npm
# mismatch specifically, after the Maven check already passed.
cat > "${FIXTURE_DIR}/npm-mismatch.gradle.kts" <<'EOF'
mavenPublishing {
  coordinates("site.lempert", "user-agent", "1.2.3")
}
npmPublish {
  packages {
    named("js") {
      version.set("0.1.0")
    }
  }
}
EOF
OUTPUT_E=$("$VERIFY_SCRIPT" "v1.2.3" "${FIXTURE_DIR}/npm-mismatch.gradle.kts" 2>&1)
STATUS_E=$?
check "(e) npm mismatch: exits non-zero" "$([ "$STATUS_E" -ne 0 ] && echo 0 || echo 1)"
check "(e) npm mismatch: ::error:: names the npm version and mentions npm" "$(echo "$OUTPUT_E" | grep -q "::error::.*'1.2.3'.*npm package version '0.1.0'" && echo 0 || echo 1)"
check "(e) npm mismatch: does not print a pass message" "$(echo "$OUTPUT_E" | grep -q 'Version check passed' && echo 1 || echo 0)"

# (f) npm multi-match guard: two version.set(...) lines -> exit 1,
# ::error:: naming the ambiguity, instead of silently comparing a
# multi-line value.
cat > "${FIXTURE_DIR}/npm-multi-match.gradle.kts" <<'EOF'
mavenPublishing {
  coordinates("site.lempert", "user-agent", "1.2.3")
}
npmPublish {
  packages {
    named("js") {
      version.set("1.2.3")
      // version.set("1.2.3")
    }
  }
}
EOF
OUTPUT_F=$("$VERIFY_SCRIPT" "v1.2.3" "${FIXTURE_DIR}/npm-multi-match.gradle.kts" 2>&1)
STATUS_F=$?
check "(f) npm multi-match guard: exits non-zero" "$([ "$STATUS_F" -ne 0 ] && echo 0 || echo 1)"
check "(f) npm multi-match guard: ::error:: names the ambiguity" "$(echo "$OUTPUT_F" | grep -q '::error::.*[Ff]ound 2 lines' && echo 0 || echo 1)"
check "(f) npm multi-match guard: does not print a pass message" "$(echo "$OUTPUT_F" | grep -q 'Version check passed' && echo 1 || echo 0)"

# (g) npm no-match: coordinates(...) matches the tag, but no
# version.set(...) call exists in the file -> exit 1, ::error:: saying no
# npm version could be extracted.
cat > "${FIXTURE_DIR}/npm-no-match.gradle.kts" <<'EOF'
mavenPublishing {
  coordinates("site.lempert", "user-agent", "1.2.3")
}
npmPublish {
  packages {
    named("js") {
      // no version.set(...) call here
    }
  }
}
EOF
OUTPUT_G=$("$VERIFY_SCRIPT" "v1.2.3" "${FIXTURE_DIR}/npm-no-match.gradle.kts" 2>&1)
STATUS_G=$?
check "(g) npm no match: exits non-zero" "$([ "$STATUS_G" -ne 0 ] && echo 0 || echo 1)"
check "(g) npm no match: ::error:: says no npm version could be extracted" "$(echo "$OUTPUT_G" | grep -q '::error::Could not extract a version from .*npmPublish' && echo 0 || echo 1)"

# (h) Unrelated version.set(...) outside the npmPublish block: some other
# plugin config (e.g. a hypothetical unrelated Gradle plugin's own
# Property-API `.set(...)` call, simulating how pervasively that idiom is
# used elsewhere in a real build.gradle.kts) declares its own
# `version.set("N.N.N")` well outside npmPublish { packages { named("js")
# { ... } } } -- the extraction must be scoped to the npmPublish block, so
# this stray line must neither trip the ambiguity guard nor get
# misattributed as the npm version -> exit 0, still finds exactly the real
# npm version and passes.
cat > "${FIXTURE_DIR}/npm-unrelated-outside-block.gradle.kts" <<'EOF'
someOtherPlugin {
  // Unrelated plugin config that happens to use the same Property-API
  // `.set(...)` idiom -- must NOT be picked up by the npm version
  // extraction, which is scoped to the npmPublish block only.
  version.set("9.9.9")
}

mavenPublishing {
  coordinates("site.lempert", "user-agent", "1.2.3")
}

npmPublish {
  packages {
    named("js") {
      version.set("1.2.3")
    }
  }
}
EOF
OUTPUT_H=$("$VERIFY_SCRIPT" "v1.2.3" "${FIXTURE_DIR}/npm-unrelated-outside-block.gradle.kts" 2>&1)
STATUS_H=$?
check "(h) unrelated version.set(...) outside npmPublish block: exits 0" "$([ "$STATUS_H" -eq 0 ] && echo 0 || echo 1)"
check "(h) unrelated version.set(...) outside npmPublish block: prints pass message" "$(echo "$OUTPUT_H" | grep -q 'Version check passed' && echo 0 || echo 1)"
check "(h) unrelated version.set(...) outside npmPublish block: does not report ambiguity" "$(echo "$OUTPUT_H" | grep -q 'expected exactly one' && echo 1 || echo 0)"

echo ""
if [ "$FAILURES" -gt 0 ]; then
  echo "${FAILURES} CHECK(S) FAILED"
  exit 1
else
  echo "ALL CHECKS PASSED"
  exit 0
fi
