#!/usr/bin/env node
// Plain Node verification for ConsentBanner.vue's Consent Mode v2 decision
// logic (spec-google-analytics-consent-mode.md). No new test
// framework/dependency -- imports the REAL `decideConsentOnMount` from
// consentDecision.ts and asserts its output for each stored-choice input.
//
// Why this exists: `decideConsentOnMount`'s three branches previously lived
// only inline inside ConsentBanner.vue's onMounted handler, which runs
// client-side only -- none of docs-deploy.yml's existing SSR-build-output
// checks can see it. A regression here (e.g. showing the banner for a
// visitor who already accepted, or silently dropping the granted-signals
// call on accept) would break Consent Mode v2's actual gating behavior
// while producing a clean, non-crashing SSR build. This mirrors the same
// "client-only onMounted/click logic invisible to SSR-based CI checks" gap
// that verify-generate-demo.mjs already exists to close for
// GenerateDemo.vue.
//
// Run directly: `node docs-site/scripts/verify-consent-decision.mjs`
// Exits non-zero if any assertion fails.

import { decideConsentOnMount } from '../src/theme/consentDecision.ts';

let failures = 0;

function check(label, condition) {
  if (!condition) {
    failures++;
    console.error(`FAIL: ${label}`);
  } else {
    console.log(`PASS: ${label}`);
  }
}

const GRANT_ALL = {
  ad_storage: 'granted',
  ad_user_data: 'granted',
  ad_personalization: 'granted',
  analytics_storage: 'granted',
};

// (a) No stored choice (first visit, or storage unavailable): banner shows,
// and no grant call fires -- denied-by-default stays in effect until the
// visitor actively chooses.
{
  const decision = decideConsentOnMount(null);
  check('(a) no stored choice: banner shows', decision.showBanner === true);
  check('(a) no stored choice: no consent-update call fires', decision.consentUpdateArgs === null);
}

// (b) 'accepted': banner stays hidden, and the grant call fires with all
// four signals granted (re-affirmed on every load, per the spec's
// "Returning visitor" row).
{
  const decision = decideConsentOnMount('accepted');
  check("(b) stored 'accepted': banner hidden", decision.showBanner === false);
  check(
    "(b) stored 'accepted': consent-update call grants all four signals",
    JSON.stringify(decision.consentUpdateArgs) === JSON.stringify(GRANT_ALL),
  );
}

// (c) 'rejected': banner stays hidden, and no grant call fires -- signals
// remain denied by the default script, nothing else to do.
{
  const decision = decideConsentOnMount('rejected');
  check("(c) stored 'rejected': banner hidden", decision.showBanner === false);
  check("(c) stored 'rejected': no consent-update call fires", decision.consentUpdateArgs === null);
}

console.log('');
if (failures > 0) {
  console.error(`${failures} CHECK(S) FAILED`);
  process.exit(1);
} else {
  console.log('ALL CHECKS PASSED');
  process.exit(0);
}
