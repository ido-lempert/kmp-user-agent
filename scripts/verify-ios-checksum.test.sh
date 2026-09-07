#!/usr/bin/env bash
# Fixture-based test for scripts/verify-ios-checksum.sh -- the iOS
# checksum/url check that release.yml runs on every real `vX.Y.Z` tag
# push. That real-tag path only ever exercises the "exact match" case in
# practice, so this drives the script against fixture Package.swift-style
# files covering match, mismatch, and the ambiguity/scoping guards --
# wired into ci.yml so a regression is caught on every push/PR, not only
# when a real release tag happens to be pushed. Mirrors the
# check()/PASS//FAIL/ALL CHECKS PASSED assertion style of
# scripts/verify-release-version.test.sh.
#
# Run directly: bash scripts/verify-ios-checksum.test.sh
# Exits non-zero if any assertion fails.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VERIFY_SCRIPT="${SCRIPT_DIR}/verify-ios-checksum.sh"
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

# (a) Checksum and url tag both match: given checksum equals the
# committed checksum, and the committed url's tag segment equals the tag
# argument -> exit 0, prints a pass message.
cat > "${FIXTURE_DIR}/match.swift" <<'EOF'
let package = Package(
    name: "Library",
    targets: [
        .binaryTarget(
            name: "Library",
            url: "https://github.com/ido-lempert/kmp-user-agent/releases/download/v1.2.3/Library.xcframework.zip",
            checksum: "aaaa111122223333444455556666777788889999000011112222333344445555"
        )
    ]
)
EOF
OUTPUT_A=$("$VERIFY_SCRIPT" "v1.2.3" "aaaa111122223333444455556666777788889999000011112222333344445555" "${FIXTURE_DIR}/match.swift" 2>&1)
STATUS_A=$?
check "(a) match: exits 0" "$([ "$STATUS_A" -eq 0 ] && echo 0 || echo 1)"
check "(a) match: prints pass message" "$(echo "$OUTPUT_A" | grep -q 'verification passed' && echo 0 || echo 1)"

# (b) Checksum mismatch: computed checksum differs from the committed
# checksum, url tag is fine -> exit 1, ::error:: naming both checksums.
cat > "${FIXTURE_DIR}/checksum-mismatch.swift" <<'EOF'
let package = Package(
    name: "Library",
    targets: [
        .binaryTarget(
            name: "Library",
            url: "https://github.com/ido-lempert/kmp-user-agent/releases/download/v1.2.3/Library.xcframework.zip",
            checksum: "aaaa111122223333444455556666777788889999000011112222333344445555"
        )
    ]
)
EOF
OUTPUT_B=$("$VERIFY_SCRIPT" "v1.2.3" "ffff999988887777666655554444333322221111000099998888777766665555" "${FIXTURE_DIR}/checksum-mismatch.swift" 2>&1)
STATUS_B=$?
check "(b) checksum mismatch: exits non-zero" "$([ "$STATUS_B" -ne 0 ] && echo 0 || echo 1)"
check "(b) checksum mismatch: ::error:: names both checksums" "$(echo "$OUTPUT_B" | grep -q "::error::.*'ffff999988887777666655554444333322221111000099998888777766665555'.*'aaaa111122223333444455556666777788889999000011112222333344445555'" && echo 0 || echo 1)"
check "(b) checksum mismatch: does not print a pass message" "$(echo "$OUTPUT_B" | grep -q 'verification passed' && echo 1 || echo 0)"

# (c) URL tag mismatch: checksum matches, but the committed url's tag
# segment differs from the tag argument -> exit 1, ::error:: naming both
# tags.
cat > "${FIXTURE_DIR}/url-tag-mismatch.swift" <<'EOF'
let package = Package(
    name: "Library",
    targets: [
        .binaryTarget(
            name: "Library",
            url: "https://github.com/ido-lempert/kmp-user-agent/releases/download/v0.9.0/Library.xcframework.zip",
            checksum: "aaaa111122223333444455556666777788889999000011112222333344445555"
        )
    ]
)
EOF
OUTPUT_C=$("$VERIFY_SCRIPT" "v1.2.3" "aaaa111122223333444455556666777788889999000011112222333344445555" "${FIXTURE_DIR}/url-tag-mismatch.swift" 2>&1)
STATUS_C=$?
check "(c) url tag mismatch: exits non-zero" "$([ "$STATUS_C" -ne 0 ] && echo 0 || echo 1)"
check "(c) url tag mismatch: ::error:: names both tags" "$(echo "$OUTPUT_C" | grep -q "::error::.*'v1.2.3'.*'v0.9.0'" && echo 0 || echo 1)"
check "(c) url tag mismatch: does not print a pass message" "$(echo "$OUTPUT_C" | grep -q 'verification passed' && echo 1 || echo 0)"

# (d) Ambiguity guard: two .binaryTarget(name: "Library", ...) blocks with
# differing checksums -> exit 1, ::error:: naming the ambiguity, instead
# of silently comparing against whichever one a naive extraction happened
# to match first/last.
cat > "${FIXTURE_DIR}/two-blocks.swift" <<'EOF'
let package = Package(
    name: "Library",
    targets: [
        .binaryTarget(
            name: "Library",
            url: "https://github.com/ido-lempert/kmp-user-agent/releases/download/v1.2.3/Library.xcframework.zip",
            checksum: "aaaa111122223333444455556666777788889999000011112222333344445555"
        ),
        .binaryTarget(
            name: "Library",
            url: "https://github.com/ido-lempert/kmp-user-agent/releases/download/v1.2.3/Library.xcframework.zip",
            checksum: "bbbb111122223333444455556666777788889999000011112222333344445555"
        )
    ]
)
EOF
OUTPUT_D=$("$VERIFY_SCRIPT" "v1.2.3" "aaaa111122223333444455556666777788889999000011112222333344445555" "${FIXTURE_DIR}/two-blocks.swift" 2>&1)
STATUS_D=$?
check "(d) ambiguity guard: exits non-zero" "$([ "$STATUS_D" -ne 0 ] && echo 0 || echo 1)"
check "(d) ambiguity guard: ::error:: names the ambiguity" "$(echo "$OUTPUT_D" | grep -q '::error::.*[Ff]ound 2 \.binaryTarget' && echo 0 || echo 1)"
check "(d) ambiguity guard: does not print a pass message" "$(echo "$OUTPUT_D" | grep -q 'verification passed' && echo 1 || echo 0)"

# (e) Decoy checksum-shaped string outside any .binaryTarget block: must
# be ignored, not counted -- extraction must be scoped to the
# .binaryTarget(name: "Library", ...) call, not a whole-file grep. Also
# includes a decoy .binaryTarget(...) call for a DIFFERENT target name, to
# confirm the name: "Library" scoping filters it out too.
cat > "${FIXTURE_DIR}/decoy.swift" <<'EOF'
// Old value, kept here for reference only (NOT inside any binaryTarget
// call): checksum: "deadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdead"
let package = Package(
    name: "Library",
    targets: [
        .binaryTarget(
            name: "OtherFramework",
            url: "https://github.com/ido-lempert/kmp-user-agent/releases/download/v1.2.3/Other.xcframework.zip",
            checksum: "cccc111122223333444455556666777788889999000011112222333344445555"
        ),
        .binaryTarget(
            name: "Library",
            url: "https://github.com/ido-lempert/kmp-user-agent/releases/download/v1.2.3/Library.xcframework.zip",
            checksum: "aaaa111122223333444455556666777788889999000011112222333344445555"
        )
    ]
)
EOF
OUTPUT_E=$("$VERIFY_SCRIPT" "v1.2.3" "aaaa111122223333444455556666777788889999000011112222333344445555" "${FIXTURE_DIR}/decoy.swift" 2>&1)
STATUS_E=$?
check "(e) decoy outside block: exits 0" "$([ "$STATUS_E" -eq 0 ] && echo 0 || echo 1)"
check "(e) decoy outside block: prints pass message" "$(echo "$OUTPUT_E" | grep -q 'verification passed' && echo 0 || echo 1)"
check "(e) decoy outside block: does not report ambiguity" "$(echo "$OUTPUT_E" | grep -q 'expected exactly one' && echo 1 || echo 0)"

# (f) File not found -> exit 1, ::error:: naming the missing file.
OUTPUT_F=$("$VERIFY_SCRIPT" "v1.2.3" "aaaa111122223333444455556666777788889999000011112222333344445555" "${FIXTURE_DIR}/does-not-exist.swift" 2>&1)
STATUS_F=$?
check "(f) file not found: exits non-zero" "$([ "$STATUS_F" -ne 0 ] && echo 0 || echo 1)"
check "(f) file not found: ::error:: names the missing file" "$(echo "$OUTPUT_F" | grep -q "::error::File not found" && echo 0 || echo 1)"

# (g) No .binaryTarget(name: "Library", ...) call at all -> exit 1,
# ::error:: saying none could be found.
cat > "${FIXTURE_DIR}/no-block.swift" <<'EOF'
let package = Package(
    name: "Library",
    targets: [
        .target(name: "Library")
    ]
)
EOF
OUTPUT_G=$("$VERIFY_SCRIPT" "v1.2.3" "aaaa111122223333444455556666777788889999000011112222333344445555" "${FIXTURE_DIR}/no-block.swift" 2>&1)
STATUS_G=$?
check "(g) no block found: exits non-zero" "$([ "$STATUS_G" -ne 0 ] && echo 0 || echo 1)"
check "(g) no block found: ::error:: says none could be found" "$(echo "$OUTPUT_G" | grep -q '::error::Could not find a \.binaryTarget' && echo 0 || echo 1)"

# (h) Empty computed-checksum argument (e.g. compute-checksum produced
# empty/malformed output upstream) -> exit 1, ::error:: before comparing
# anything against Package.swift.
OUTPUT_H=$("$VERIFY_SCRIPT" "v1.2.3" "" "${FIXTURE_DIR}/match.swift" 2>&1)
STATUS_H=$?
check "(h) empty computed checksum: exits non-zero" "$([ "$STATUS_H" -ne 0 ] && echo 0 || echo 1)"
check "(h) empty computed checksum: ::error:: flags it" "$(echo "$OUTPUT_H" | grep -q '::error::Computed checksum argument is empty' && echo 0 || echo 1)"

echo ""
if [ "$FAILURES" -gt 0 ]; then
  echo "${FAILURES} CHECK(S) FAILED"
  exit 1
else
  echo "ALL CHECKS PASSED"
  exit 0
fi
