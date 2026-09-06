import { defineConfig } from 'vitepress';

// Served from the custom domain https://user-agent.lempert.site/ (repo
// Settings -> Pages -> Custom domain), which is a domain root, not a project
// subpath -- so `base` stays VitePress's default `/` in both dev and build.
export default defineConfig({
  title: 'kmp-user-agent',
  description:
    'Parse and generate User-Agent strings across Android, iOS, JVM, and JS from one Kotlin Multiplatform library.',
  cleanUrls: true,

  themeConfig: {
    nav: [
      { text: 'Core Concepts', link: '/guide/core-concepts' },
      { text: 'Guide', link: '/guide/js' },
      { text: 'Android', link: '/guide/android' },
      { text: 'iOS', link: '/guide/ios' },
    ],

    sidebar: [
      {
        text: 'Guide',
        items: [
          { text: 'Core Concepts', link: '/guide/core-concepts' },
          { text: 'Browser & Node.js (JS)', link: '/guide/js' },
          { text: 'Android', link: '/guide/android' },
          { text: 'iOS', link: '/guide/ios' },
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
