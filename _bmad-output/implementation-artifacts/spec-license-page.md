---
title: 'Docs-Site License Page'
type: 'feature'
created: '2026-09-06'
status: 'done'
review_loop_iteration: 0
route: 'one-shot'
context:
  - '{project-root}/_bmad-output/specs/spec-docs-site/SPEC.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The docs site never surfaced the library's own MIT license or its Apache-2.0 attribution for vendored uap-core data — a prospective adopter had to go dig through the repository's root `LICENSE`/`library/NOTICE` files directly.

**Approach:** Add a top-level `docs-site/license.md` page reproducing both notices verbatim (cross-checked byte-for-byte against their source files), corrected for one accuracy claim (the SPM XCFramework doesn't embed the files the way the JAR/AAR/npm package do) and one stale path caught in the source `NOTICE` file itself (logged separately, out of scope to fix here), then wired it into nav/sidebar and cross-linked from the homepage.

</frozen-after-approval>

## Suggested Review Order

**The page's accuracy**

- Entry point: the MIT block (verified byte-identical to root `LICENSE`) and the corrected "bundled in every artifact except the XCFramework" claim.
  [`license.md:1`](../../docs-site/license.md#L1)

- The Apache-2.0 attribution block, including the corrected vendored-file path (`library/vendor/uap-core/LICENSE`, not `NOTICE`'s stale `vendor/uap-core/LICENSE`) and the note distinguishing `NOTICE`'s two separate dependency audits.
  [`license.md:51`](../../docs-site/license.md#L51)

- Cross-check links to the Maven Central and npm registry listings' own license metadata, so a reader can independently verify the claims.
  [`license.md:89`](../../docs-site/license.md#L89)

**Nav wiring**

- New "License" nav entry and "Legal" sidebar group.
  [`config.ts:19`](../../docs-site/.vitepress/config.ts#L19)

**Peripherals**

- Homepage now cross-links to the License page.
  [`index.md:14`](../../docs-site/index.md#L14)
