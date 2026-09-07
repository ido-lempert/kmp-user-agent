// Extends VitePress's default theme purely to inject the Google Analytics
// Consent Mode v2 banner (spec-google-analytics-consent-mode.md) into every
// page via the default theme's documented `layout-bottom` slot -- no
// generated HTML is hand-edited, and nothing else about the default theme
// changes.
import DefaultTheme from 'vitepress/theme';
import { h } from 'vue';
import type { Theme } from 'vitepress';
import ConsentBanner from '../../src/theme/ConsentBanner.vue';

export default {
  extends: DefaultTheme,
  Layout: () => {
    return h(DefaultTheme.Layout, null, {
      'layout-bottom': () => h(ConsentBanner),
    });
  },
} satisfies Theme;
