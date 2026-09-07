# Legal disclaimers

_Last updated: 2026-09-07_

This page is general, good-faith boilerplate, not a legal opinion -- it isn't
written or reviewed by a lawyer, and it isn't a substitute for one. If
anything here is compliance-critical for your own use of this site or this
library, consult your own counsel. Nothing on this page should be read as a
guarantee of any particular legal, accessibility, or privacy-compliance
status.

For the library's and this site's copyright licensing (MIT, the vendored
Apache-2.0 `uap-core` data, and the CC0-1.0 logo icons), see
[License](/license) -- this page doesn't repeat that content.

## Trademarks & third-party names

`kmp-user-agent` parses and generates User-Agent strings, which means its
detection logic references third-party browser, operating system, device,
bot, and AI-agent crawler names and identifiers (for example "Chrome",
"Windows", "iPhone", "Googlebot", "GPTBot"). Every such reference is
**nominative use**: identifying which real-world product or service a given
User-Agent string matches. It does not imply affiliation with, sponsorship
by, or endorsement from any of the companies or projects named. All product
and company names are trademarks of their respective owners.

The site's homepage also displays third-party brand logo icons (via Simple
Icons) next to some of these detection results, for the same identifying
purpose. See [License &sect; Third-party attribution: docs-site logos](/license#third-party-attribution-docs-site-logos-simple-icons-cc0-1-0)
for the specifics of that attribution and the same nominative-use framing.

## Accessibility

This documentation site is built on [VitePress](https://vitepress.dev)'s
default theme, used largely as-is. No formal accessibility audit and no
WCAG conformance testing (at any level -- A, AA, or AAA) has been performed
against this site, and we make no claim of WCAG or other accessibility
conformance. That said, specific interactive elements were built with
accessibility in mind on a best-effort basis: for example, the analytics
consent banner uses an ARIA `dialog` role with `aria-live` and moves
keyboard focus to itself when it appears, so it doesn't render silently
outside a keyboard or screen-reader visitor's attention. This is targeted,
uncertified engineering care for that one component -- not a substitute for
an audit, and not a claim that the rest of the site has received the same
scrutiny.

If you encounter an accessibility barrier using this site, please
[open an issue](https://github.com/ido-lempert/kmp-user-agent/issues) so it
can be looked at. The same issue tracker is also the right place for privacy
questions -- see the [Privacy & analytics](#privacy) section below.

## Privacy & analytics {#privacy}

This site uses Google Analytics (GA4), gated behind a cookie-consent banner
shown on your first visit. Here's what that means in practice:

- **Consent-gated by default.** Google's Consent Mode v2 is wired in with
  all four consent signals (`ad_storage`, `ad_user_data`,
  `ad_personalization`, `analytics_storage`) defaulted to **denied** before
  any analytics call is made. Nothing is granted until you click "Accept"
  on the banner; clicking "Reject" (or simply not choosing) leaves
  everything denied. (See the Advanced Consent Mode caveat a couple of
  bullets down -- "denied" here doesn't mean literally nothing is ever
  sent to Google before you choose.)
- **Why ad-related signals, on a site with no ads.** Consent Mode v2
  defines a fixed set of four signals as a package -- `ad_storage`,
  `ad_user_data`, `ad_personalization`, and `analytics_storage` -- and
  GA4's standard integration (the one wired in here) grants all four
  together when you click "Accept," even though this site only uses GA4
  for basic analytics. That's the standard shape of Google's own
  consent-signal API, not a signal that this site runs ads or
  remarketing: there are still no advertising pixels or ad campaigns
  active on this site (see "No other tracking" below).
- **Your choice is remembered.** Once you accept or reject, that choice is
  stored in your browser (`localStorage`) so the banner doesn't reappear on
  later visits. If your browser blocks or clears that storage, the banner
  will simply show again.
- **A real caveat about "denied":** Consent Mode v2 here runs in Google's
  "Advanced" mode, not "Basic" mode. In Advanced mode, Google's `gtag.js`
  script loads for every visitor -- even before you make a choice, and even
  if you reject -- and Google may still receive limited, cookieless signals
  used for conversion modeling. This is different from analytics tracking
  being fully absent pre-consent; it's Google's own documented behavior for
  sites running Consent Mode v2 in Advanced mode rather than blocking the
  script entirely until consent is granted. We're documenting this plainly
  rather than implying "denied" means "nothing is ever sent."
- **No other tracking.** Google Analytics, as described above, is the only
  analytics or tracking mechanism on this site. There are no advertising
  pixels, session-replay tools, or third-party trackers beyond it.

We don't claim GDPR, CCPA, or any other privacy-law "compliance" as a
certified or guaranteed status -- the above describes the site's actual,
current behavior, not a legal conclusion about any specific jurisdiction's
requirements.

For how Google itself describes its own data handling, see
[Google's Privacy Policy](https://policies.google.com/privacy) (external
site, not something this project controls or vouches for). If you have
privacy-related questions about this site specifically, please
[open an issue](https://github.com/ido-lempert/kmp-user-agent/issues).

## General liability

This site, its documentation content, and the `kmp-user-agent` library
itself are provided on an "as is" basis, without warranty of any kind,
express or implied, to the fullest extent permitted by law. See
[License](/license) for the library's own MIT license text, which spells
this out formally for the code itself.

A specific consequence worth calling out for a User-Agent parsing library:
a User-Agent string is client-supplied data, and the sender can set it to
anything -- including a spoofed or forged value that doesn't match the
actual browser, OS, or bot making the request. `kmp-user-agent`'s parsed
(or generated) output reflects only what the input string says, not any
independently verified fact about the requester. It should not be relied
on as the sole basis for security-critical decisions (for example,
bot-blocking or fraud detection) without additional verification.

## Docs content licensing

The library's own code is MIT-licensed (see [License](/license)). Whether a
separate license applies to this documentation site's own written content
(the guide prose, page copy, and demo component source under
`docs-site/`) is, as of this writing, an **open question that hasn't been
decided** -- it isn't released under MIT, CC-BY, or any other specific
license at this time. Don't assume any particular license applies to this
content beyond what's stated here.
