import { defineConfig } from 'vitepress';

// Served from the custom domain https://user-agent.lempert.site/ (repo
// Settings -> Pages -> Custom domain), which is a domain root, not a project
// subpath -- so `base` stays VitePress's default `/` in both dev and build.
export default defineConfig({
  title: 'kmp-user-agent',
  description:
    'One Kotlin Multiplatform library that parses and generates User-Agent strings with the same logic and results on Android, iOS, JVM, and JS.',
  cleanUrls: true,

  themeConfig: {
    nav: [
      { text: 'Core Concepts', link: '/guide/core-concepts' },
      { text: 'Reference', link: '/reference' },
      { text: 'Browser & Node.js (JS)', link: '/guide/js' },
      { text: 'Android', link: '/guide/android' },
      { text: 'iOS', link: '/guide/ios' },
      { text: 'JVM', link: '/guide/jvm' },
      { text: 'License', link: '/license' },
    ],

    sidebar: [
      {
        text: 'Guide',
        items: [
          { text: 'Core Concepts', link: '/guide/core-concepts' },
          { text: 'Browser & Node.js (JS)', link: '/guide/js' },
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
        items: [{ text: 'License', link: '/license' }],
      },
    ],

    socialLinks: [{ icon: 'github', link: 'https://github.com/ido-lempert/kmp-user-agent' }],

    // Built-in local search index -- no external service/API key needed.
    search: {
      provider: 'local',
    },
  },
});
