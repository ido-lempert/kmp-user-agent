import { defineConfig } from 'vitepress';
import type { HeadConfig } from 'vitepress';

// Served from the custom domain https://user-agent.lempert.site/ (repo
// Settings -> Pages -> Custom domain), which is a domain root, not a project
// subpath -- so `base` stays VitePress's default `/` in both dev and build.

// Single source of truth for the GA4 property -- interpolated into both the
// gtag.js loader `src` and the `gtag('config', ...)` call below so rotating
// the ID is a one-line change.
const GA_MEASUREMENT_ID = 'G-1TGW366KHQ';

// Google Analytics 4 (Consent Mode v2) `head` scripts. Only injected for a
// production build (`vitepress build`, which is what `docs:build` and the
// deploy workflow run with `NODE_ENV=production` implicitly set by
// VitePress/Vite) -- never for `npm run docs:dev` or any other non-production
// build, so dev/test traffic never reaches the live GA4 property. Order is
// load-bearing: the consent-default script MUST run before the gtag.js
// loader and before the `gtag('config', ...)` call, or Consent Mode doesn't
// actually gate collection (spec: spec-google-analytics-consent-mode.md).
// All four signals default to 'denied'; ConsentBanner.vue (theme/index.ts,
// layout-bottom slot) is what calls `gtag('consent', 'update', ...)` once
// the visitor makes a choice.
const head: HeadConfig[] =
  process.env.NODE_ENV === 'production'
    ? [
        [
          'script',
          {},
          `window.dataLayer = window.dataLayer || [];
function gtag(){dataLayer.push(arguments);}
gtag('consent', 'default', {
  ad_storage: 'denied',
  ad_user_data: 'denied',
  ad_personalization: 'denied',
  analytics_storage: 'denied'
});`,
        ],
        ['script', { async: '', src: `https://www.googletagmanager.com/gtag/js?id=${GA_MEASUREMENT_ID}` }],
        [
          'script',
          {},
          `gtag('js', new Date());
gtag('config', '${GA_MEASUREMENT_ID}');`,
        ],
      ]
    : [];

export default defineConfig({
  title: 'kmp-user-agent',
  description:
    'One Kotlin Multiplatform library that parses and generates User-Agent strings with the same logic and results on Android, iOS, JVM, and JS.',
  cleanUrls: true,

  head,

  themeConfig: {
    nav: [
      { text: 'Core Concepts', link: '/guide/core-concepts' },
      { text: 'Reference', link: '/reference' },
      { text: 'Browser & Node.js (JS)', link: '/guide/js' },
      { text: 'React Native', link: '/guide/react-native' },
      { text: 'Android', link: '/guide/android' },
      { text: 'iOS', link: '/guide/ios' },
      { text: 'JVM', link: '/guide/jvm' },
      {
        text: 'Legal',
        items: [
          { text: 'License', link: '/license' },
          { text: 'Legal disclaimers', link: '/legal' },
        ],
      },
    ],

    sidebar: [
      {
        text: 'Guide',
        items: [
          { text: 'Core Concepts', link: '/guide/core-concepts' },
          { text: 'Browser & Node.js (JS)', link: '/guide/js' },
          { text: 'React Native', link: '/guide/react-native' },
          { text: 'Android', link: '/guide/android' },
          { text: 'iOS', link: '/guide/ios' },
          { text: 'JVM', link: '/guide/jvm' },
        ],
      },
      {
        text: 'Reference',
        items: [{ text: 'Type Reference', link: '/reference' }],
      },
      {
        text: 'Legal',
        items: [
          { text: 'License', link: '/license' },
          { text: 'Legal disclaimers', link: '/legal' },
        ],
      },
    ],

    socialLinks: [{ icon: 'github', link: 'https://github.com/ido-lempert/kmp-user-agent' }],

    // Built-in local search index -- no external service/API key needed.
    search: {
      provider: 'local',
    },
  },
});
