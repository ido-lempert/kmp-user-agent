<script setup lang="ts">
// Google Analytics Consent Mode v2 accept/reject banner
// (spec-google-analytics-consent-mode.md). This is the ONLY place that ever
// calls `gtag('consent', 'update', ...)` -- the default (all four signals
// 'denied') is set inline in docs-site/.vitepress/config.ts `head`, before
// gtag.js loads, so collection stays gated until a visitor makes a choice
// here.
//
// The banner's own visibility must never be gated on consent/tracking state
// (Never clause) -- it always mounts and decides for itself whether to show,
// purely from the stored choice.
import { ref, onMounted, nextTick, watch } from 'vue';
import { decideConsentOnMount, type StoredChoice } from './consentDecision';

declare global {
  interface Window {
    // Defined unconditionally by the inline consent-default script in
    // config.ts `head`, independent of whether the async gtag.js loader
    // itself succeeds (e.g. blocked by an ad blocker) -- gtag() just pushes
    // onto dataLayer either way.
    gtag?: (...args: unknown[]) => void;
  }
}

const STORAGE_KEY = 'ga-consent-choice';

const visible = ref(false);
const bannerRef = ref<HTMLElement | null>(null);

function readStoredChoice(): StoredChoice {
  try {
    const value = window.localStorage.getItem(STORAGE_KEY);
    return value === 'accepted' || value === 'rejected' ? value : null;
  } catch {
    // Private browsing / blocked storage: treat as "no choice yet" so the
    // banner still renders every visit (denied-by-default stays safe).
    return null;
  }
}

function writeStoredChoice(choice: Exclude<StoredChoice, null>): void {
  try {
    window.localStorage.setItem(STORAGE_KEY, choice);
  } catch {
    // Storage unavailable -- the choice just won't persist across visits.
    // Not fatal: denied-by-default is still safe, and the banner will
    // simply show again next time.
  }
}

function updateConsentGranted(): void {
  // A thrown error here (e.g. window.gtag somehow not a function despite
  // the `?.` guard, or an ad-blocker-patched gtag stub that throws) must
  // never prevent the choice from being persisted or the banner from
  // hiding -- that would leave the visitor stuck staring at a banner their
  // click already "worked" from their perspective.
  try {
    window.gtag?.('consent', 'update', {
      ad_storage: 'granted',
      ad_user_data: 'granted',
      ad_personalization: 'granted',
      analytics_storage: 'granted',
    });
  } catch {
    // Ignored -- see comment above. Persistence/hiding happens regardless.
  }
}

onMounted(() => {
  const stored = readStoredChoice();
  const decision = decideConsentOnMount(stored);

  if (decision.consentUpdateArgs) {
    // Returning visitor who previously accepted: re-affirm granted consent
    // on every load (idempotent) so mode stays granted across sessions.
    updateConsentGranted();
  }

  // No stored choice: first visit (or storage was unavailable) -- show it.
  // 'rejected': signals are already denied by the default script; nothing
  // else to do, banner stays hidden.
  visible.value = decision.showBanner;
});

// Move focus to the banner when it appears so keyboard/screen-reader
// visitors notice it rather than it silently rendering off-screen from
// their attention.
watch(visible, (isVisible) => {
  if (isVisible) {
    nextTick(() => {
      bannerRef.value?.focus();
    });
  }
});

function accept(): void {
  // Persistence and hiding must happen unconditionally, even if the gtag
  // call above throws -- see updateConsentGranted's try/catch.
  updateConsentGranted();
  writeStoredChoice('accepted');
  visible.value = false;
}

function reject(): void {
  // Signals remain denied (the default) -- no consent update call needed,
  // just persist the choice so the banner doesn't re-show.
  writeStoredChoice('rejected');
  visible.value = false;
}
</script>

<template>
  <div
    v-if="visible"
    ref="bannerRef"
    class="consent-banner"
    role="dialog"
    aria-live="polite"
    aria-label="Analytics consent"
    tabindex="-1"
  >
    <p class="consent-banner__text">
      This site uses Google Analytics to understand how visitors use the
      docs. Analytics is off by default until you accept.
    </p>
    <div class="consent-banner__actions">
      <button type="button" class="consent-banner__button consent-banner__button--reject" @click="reject">
        Reject
      </button>
      <button type="button" class="consent-banner__button consent-banner__button--accept" @click="accept">
        Accept
      </button>
    </div>
  </div>
</template>

<style scoped>
.consent-banner {
  position: fixed;
  z-index: 100;
  left: 1rem;
  right: 1rem;
  bottom: 1rem;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem 1.5rem;
  padding: 1rem 1.25rem;
  border: 1px solid var(--vp-c-divider);
  border-radius: 8px;
  background-color: var(--vp-c-bg-elv);
  box-shadow: var(--vp-shadow-3);
}

@media (min-width: 640px) {
  .consent-banner {
    left: auto;
    right: 1.5rem;
    bottom: 1.5rem;
    max-width: 28rem;
  }
}

.consent-banner__text {
  flex: 1 1 16rem;
  margin: 0;
  font-size: 0.85em;
  color: var(--vp-c-text-1);
}

.consent-banner__actions {
  display: flex;
  gap: 0.5rem;
  flex: 0 0 auto;
}

/* Both buttons share identical size, weight, border, and background so
   neither reads as the "default"/emphasized action -- rejecting must be no
   harder to choose than accepting (CNIL/EDPB consent-UX guidance). Only
   text color differs, for a light visual distinction between the two
   choices. */
.consent-banner__button {
  padding: 0.4rem 0.9rem;
  border-radius: 6px;
  font-size: 0.85em;
  font-weight: 600;
  cursor: pointer;
  background-color: transparent;
  border: 1px solid var(--vp-c-divider);
}

.consent-banner__button--reject {
  color: var(--vp-c-text-1);
}

.consent-banner__button--accept {
  color: var(--vp-c-brand-1);
}

.consent-banner__button--reject:hover,
.consent-banner__button--accept:hover {
  background-color: var(--vp-c-bg-soft);
}
</style>
