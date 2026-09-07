// Pure, framework-free Consent Mode v2 decision logic
// (spec-google-analytics-consent-mode.md), factored out of
// ConsentBanner.vue's <script setup> so it can be exercised by plain-Node
// verification (docs-site/scripts/verify-consent-decision.mjs) that has no
// access to the component's client-only onMounted/click handlers -- the
// same "client-only logic invisible to SSR-based CI checks" gap that
// verify-generate-demo.mjs already exists to close for GenerateDemo.vue.
//
// This module owns ONLY the decision (given a stored choice, should the
// banner show, and what `gtag('consent', 'update', ...)` call, if any,
// should fire) -- it never touches `window`, `localStorage`, or `gtag`
// itself. ConsentBanner.vue is still the only place that performs those
// side effects.

export type StoredChoice = 'accepted' | 'rejected' | null;

export interface ConsentUpdateCall {
  ad_storage: 'granted';
  ad_user_data: 'granted';
  ad_personalization: 'granted';
  analytics_storage: 'granted';
}

export interface ConsentDecision {
  /** Whether the banner should be visible. */
  showBanner: boolean;
  /**
   * The `gtag('consent', 'update', ...)` argument to send, or `null` if no
   * consent-update call should fire for this stored choice.
   */
  consentUpdateArgs: ConsentUpdateCall | null;
}

const GRANT_ALL: ConsentUpdateCall = {
  ad_storage: 'granted',
  ad_user_data: 'granted',
  ad_personalization: 'granted',
  analytics_storage: 'granted',
};

/**
 * Decide banner visibility and whether/what consent-update call to fire,
 * purely from the stored choice read on mount. Mirrors the three branches
 * in ConsentBanner.vue's onMounted:
 * - no stored choice (null): banner shows, nothing granted yet
 * - 'accepted': banner stays hidden, grant call re-affirmed (idempotent)
 * - 'rejected': banner stays hidden, no grant call (denied-by-default holds)
 */
export function decideConsentOnMount(stored: StoredChoice): ConsentDecision {
  if (stored === 'accepted') {
    return { showBanner: false, consentUpdateArgs: GRANT_ALL };
  }

  if (stored === 'rejected') {
    return { showBanner: false, consentUpdateArgs: null };
  }

  return { showBanner: true, consentUpdateArgs: null };
}
