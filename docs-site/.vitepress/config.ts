import { defineConfig } from 'vitepress';

// GitHub Pages serves the deployed build as a project site at
// https://ido-lempert.github.io/kmp-user-agent/ (not a user/org site at the
// domain root), so a production build's `base` must match the repo name --
// the VitePress default `base: '/'` would 404 every asset once deployed.
// Local `npm run docs:dev` has no such subpath, so it stays at `/`; only
// `npm run docs:build`'s `command === 'build'` gets the GitHub Pages base.
export default defineConfig(({ command }) => ({
  title: 'kmp-user-agent',
  description:
    'Parse and generate User-Agent strings across Android, iOS, JVM, and JS from one Kotlin Multiplatform library.',
  base: command === 'build' ? '/kmp-user-agent/' : '/',
  cleanUrls: true,

  themeConfig: {
    nav: [
      { text: 'Core Concepts', link: '/guide/core-concepts' },
      { text: 'Guide', link: '/guide/js' },
      { text: 'Android', link: '/guide/android' },
    ],

    sidebar: [
      {
        text: 'Guide',
        items: [
          { text: 'Core Concepts', link: '/guide/core-concepts' },
          { text: 'Browser & Node.js (JS)', link: '/guide/js' },
          { text: 'Android', link: '/guide/android' },
        ],
      },
    ],

    socialLinks: [{ icon: 'github', link: 'https://github.com/ido-lempert/kmp-user-agent' }],

    // Built-in local search index -- no external service/API key needed.
    search: {
      provider: 'local',
    },
  },
}));
