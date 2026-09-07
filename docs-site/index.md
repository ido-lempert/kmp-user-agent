<script setup>
import ParseDemo from './src/demo/ParseDemo.vue';
import GenerateDemo from './src/demo/GenerateDemo.vue';
</script>

# kmp-user-agent

**One Kotlin Multiplatform library that parses and generates User-Agent
strings with the same logic and results on Android, iOS, JVM, and JS** -- so
you stop hand-rolling UA regex per platform, and stop finding out your
detection logic silently diverges on the one platform nobody tested it on.

The proof is below: not a mockup, your own browser, parsed right now by the
real, published `@lempert/user-agent` package. Already sold? Jump straight
to [Get started](#get-started).

## Why kmp-user-agent

- **One shared implementation, four platforms.** `UserAgentParser`/
  `UserAgentGenerator` run the exact same detection/generation logic and
  return the exact same results everywhere -- see
  [Core Concepts](/guide/core-concepts) for the shared model (the language
  bindings differ slightly per platform; each platform guide shows the
  real syntax). React Native isn't a fifth target here -- it's the same JS
  build as [Browser & Node.js](/guide/js), verified separately under Metro
  and Hermes; see its own [guide](/guide/react-native).
- **Composable, tree-shakeable type packs.** Pull in only the detection
  categories you need -- browser, engine, OS, device, bots, AI/LLM crawlers.
  There's no implicit fallback pulling in packs you didn't ask for, so a
  bundler can tree-shake the rest out of your JS build (verified for
  webpack/esbuild; [React Native's Metro is one known exception](/guide/react-native#bundle-size-and-tree-shaking-under-metro)).
- **Bot and AI-agent detection, on the way.** `UserAgentInfo` already
  carries `bot`/`aiAgent` fields, and the library's source already includes
  `UserAgentBotTypes`/`UserAgentAIAgentTypes` packs recognizing dozens of
  well-known crawlers and LLM agents -- see the [Type Reference](/reference)
  for the full current roster and its publish status (not yet in a
  published release, so the live demo below won't show a match yet even
  for a real bot's UA string).
- **Extensible without forking.** Add detection for something the built-in
  packs miss, or override one of their results, via a plain
  `UserAgentTypePack` -- either populate a named field directly, or use the
  open-ended `custom` map for anything that doesn't fit one.
- **MIT-licensed and actually published.** A real npm package backs both
  demos on this page; the same logic ships to Maven Central for
  Android/iOS/JVM. See [License](/license) for the full license and
  third-party attribution.

## Supported platforms

The same shared parsing/generation logic, with a native binding per
platform -- pick your guide:

<div role="list" aria-label="Supported platforms" style="display:flex;flex-wrap:wrap;gap:1rem;margin:1.5rem 0 2rem;">
<span role="listitem"><a href="/guide/js" style="display:flex;align-items:center;gap:0.9rem;width:240px;max-width:100%;padding:1rem;border-radius:12px;border:1px solid var(--vp-c-divider);background:var(--vp-c-bg-soft);text-decoration:none;">
<span style="display:inline-flex;align-items:center;justify-content:center;width:3rem;height:3rem;border-radius:8px;background:#f6f6f7;border:1px solid var(--vp-c-divider);flex-shrink:0;" title="JavaScript"><img src="https://cdn.jsdelivr.net/npm/simple-icons@13.21.0/icons/javascript.svg" alt="JavaScript" width="28" height="28" loading="lazy" onerror="this.closest('span').style.display='none'" /></span>
<span><strong style="display:block;color:var(--vp-c-text-1);font-size:0.95rem;">Browser & Node.js (JS)</strong><span style="display:block;font-size:0.8rem;color:var(--vp-c-text-2);margin-top:0.15rem;">Parse and generate UA strings in the browser or Node.js.</span></span>
</a></span>
<span role="listitem"><a href="/guide/react-native" style="display:flex;align-items:center;gap:0.9rem;width:240px;max-width:100%;padding:1rem;border-radius:12px;border:1px solid var(--vp-c-divider);background:var(--vp-c-bg-soft);text-decoration:none;">
<span style="display:inline-flex;align-items:center;justify-content:center;width:3rem;height:3rem;border-radius:8px;background:#f6f6f7;border:1px solid var(--vp-c-divider);flex-shrink:0;" title="React (React Native)"><img src="https://cdn.jsdelivr.net/npm/simple-icons@13.21.0/icons/react.svg" alt="React (React Native)" width="28" height="28" loading="lazy" onerror="this.closest('span').style.display='none'" /></span>
<span><strong style="display:block;color:var(--vp-c-text-1);font-size:0.95rem;">React Native</strong><span style="display:block;font-size:0.8rem;color:var(--vp-c-text-2);margin-top:0.15rem;">The same JS build, verified under Metro and Hermes.</span></span>
</a></span>
<span role="listitem"><a href="/guide/android" style="display:flex;align-items:center;gap:0.9rem;width:240px;max-width:100%;padding:1rem;border-radius:12px;border:1px solid var(--vp-c-divider);background:var(--vp-c-bg-soft);text-decoration:none;">
<span style="display:inline-flex;align-items:center;justify-content:center;width:3rem;height:3rem;border-radius:8px;background:#f6f6f7;border:1px solid var(--vp-c-divider);flex-shrink:0;" title="Android"><img src="https://cdn.jsdelivr.net/npm/simple-icons@13.21.0/icons/android.svg" alt="Android" width="28" height="28" loading="lazy" onerror="this.closest('span').style.display='none'" /></span>
<span><strong style="display:block;color:var(--vp-c-text-1);font-size:0.95rem;">Android</strong><span style="display:block;font-size:0.8rem;color:var(--vp-c-text-2);margin-top:0.15rem;">Kotlin bindings for the same shared detection logic.</span></span>
</a></span>
<span role="listitem"><a href="/guide/ios" style="display:flex;align-items:center;gap:0.9rem;width:240px;max-width:100%;padding:1rem;border-radius:12px;border:1px solid var(--vp-c-divider);background:var(--vp-c-bg-soft);text-decoration:none;">
<span style="display:inline-flex;align-items:center;justify-content:center;width:3rem;height:3rem;border-radius:8px;background:#f6f6f7;border:1px solid var(--vp-c-divider);flex-shrink:0;" title="Apple (iOS)"><img src="https://cdn.jsdelivr.net/npm/simple-icons@13.21.0/icons/apple.svg" alt="Apple (iOS)" width="28" height="28" loading="lazy" onerror="this.closest('span').style.display='none'" /></span>
<span><strong style="display:block;color:var(--vp-c-text-1);font-size:0.95rem;">iOS</strong><span style="display:block;font-size:0.8rem;color:var(--vp-c-text-2);margin-top:0.15rem;">Swift bindings via the published XCFramework.</span></span>
</a></span>
<span role="listitem"><a href="/guide/jvm" style="display:flex;align-items:center;gap:0.9rem;width:240px;max-width:100%;padding:1rem;border-radius:12px;border:1px solid var(--vp-c-divider);background:var(--vp-c-bg-soft);text-decoration:none;">
<span style="display:inline-flex;align-items:center;justify-content:center;width:3rem;height:3rem;border-radius:8px;background:#f6f6f7;border:1px solid var(--vp-c-divider);flex-shrink:0;" title="Kotlin (JVM)"><img src="https://cdn.jsdelivr.net/npm/simple-icons@13.21.0/icons/kotlin.svg" alt="Kotlin (JVM)" width="28" height="28" loading="lazy" onerror="this.closest('span').style.display='none'" /></span>
<span><strong style="display:block;color:var(--vp-c-text-1);font-size:0.95rem;">JVM</strong><span style="display:block;font-size:0.8rem;color:var(--vp-c-text-2);margin-top:0.15rem;">Kotlin bindings for server or desktop JVM apps.</span></span>
</a></span>
</div>

Logos via [Simple Icons](https://simpleicons.org) (CC0-1.0, see
[License](/license) for the full attribution) -- identification only, not
an endorsement by, or partnership with, any company shown.

## A sample of what it detects

A few of the real browsers, operating systems, bots, and AI agents the
library recognizes by name -- see the [Type Reference](/reference) for the
complete list. Icons are identification only (see the attribution note at
the end), not an endorsement by, or partnership with, any company shown.

**Browsers and operating systems** -- live in the published `0.2.0` package
today:

<div role="list" aria-label="Browsers and operating systems currently detected" style="display:flex;flex-wrap:wrap;gap:0.6rem;margin:1rem 0 1.5rem;">
<span role="listitem" style="display:inline-flex;align-items:center;justify-content:center;width:2.75rem;height:2.75rem;border-radius:8px;background:#f6f6f7;border:1px solid var(--vp-c-divider);" title="Google Chrome"><img src="https://cdn.jsdelivr.net/npm/simple-icons@13.21.0/icons/googlechrome.svg" alt="Google Chrome" width="24" height="24" loading="lazy" onerror="this.closest('span').style.display='none'" /></span>
<span role="listitem" style="display:inline-flex;align-items:center;justify-content:center;width:2.75rem;height:2.75rem;border-radius:8px;background:#f6f6f7;border:1px solid var(--vp-c-divider);" title="Firefox"><img src="https://cdn.jsdelivr.net/npm/simple-icons@13.21.0/icons/firefox.svg" alt="Firefox" width="24" height="24" loading="lazy" onerror="this.closest('span').style.display='none'" /></span>
<span role="listitem" style="display:inline-flex;align-items:center;justify-content:center;width:2.75rem;height:2.75rem;border-radius:8px;background:#f6f6f7;border:1px solid var(--vp-c-divider);" title="Safari"><img src="https://cdn.jsdelivr.net/npm/simple-icons@13.21.0/icons/safari.svg" alt="Safari" width="24" height="24" loading="lazy" onerror="this.closest('span').style.display='none'" /></span>
<span role="listitem" style="display:inline-flex;align-items:center;justify-content:center;width:2.75rem;height:2.75rem;border-radius:8px;background:#f6f6f7;border:1px solid var(--vp-c-divider);" title="Opera"><img src="https://cdn.jsdelivr.net/npm/simple-icons@13.21.0/icons/opera.svg" alt="Opera" width="24" height="24" loading="lazy" onerror="this.closest('span').style.display='none'" /></span>
<span role="listitem" style="display:inline-flex;align-items:center;justify-content:center;width:2.75rem;height:2.75rem;border-radius:8px;background:#f6f6f7;border:1px solid var(--vp-c-divider);" title="Brave"><img src="https://cdn.jsdelivr.net/npm/simple-icons@13.21.0/icons/brave.svg" alt="Brave" width="24" height="24" loading="lazy" onerror="this.closest('span').style.display='none'" /></span>
<span role="listitem" style="display:inline-flex;align-items:center;justify-content:center;width:2.75rem;height:2.75rem;border-radius:8px;background:#f6f6f7;border:1px solid var(--vp-c-divider);" title="DuckDuckGo"><img src="https://cdn.jsdelivr.net/npm/simple-icons@13.21.0/icons/duckduckgo.svg" alt="DuckDuckGo" width="24" height="24" loading="lazy" onerror="this.closest('span').style.display='none'" /></span>
<span role="listitem" style="display:inline-flex;align-items:center;justify-content:center;width:2.75rem;height:2.75rem;border-radius:8px;background:#f6f6f7;border:1px solid var(--vp-c-divider);" title="Android"><img src="https://cdn.jsdelivr.net/npm/simple-icons@13.21.0/icons/android.svg" alt="Android" width="24" height="24" loading="lazy" onerror="this.closest('span').style.display='none'" /></span>
<span role="listitem" style="display:inline-flex;align-items:center;justify-content:center;width:2.75rem;height:2.75rem;border-radius:8px;background:#f6f6f7;border:1px solid var(--vp-c-divider);" title="Apple (macOS/iOS)"><img src="https://cdn.jsdelivr.net/npm/simple-icons@13.21.0/icons/apple.svg" alt="Apple (macOS/iOS)" width="24" height="24" loading="lazy" onerror="this.closest('span').style.display='none'" /></span>
<span role="listitem" style="display:inline-flex;align-items:center;justify-content:center;width:2.75rem;height:2.75rem;border-radius:8px;background:#f6f6f7;border:1px solid var(--vp-c-divider);" title="Linux"><img src="https://cdn.jsdelivr.net/npm/simple-icons@13.21.0/icons/linux.svg" alt="Linux" width="24" height="24" loading="lazy" onerror="this.closest('span').style.display='none'" /></span>
<span role="listitem" style="display:inline-flex;align-items:center;justify-content:center;width:2.75rem;height:2.75rem;border-radius:8px;background:#f6f6f7;border:1px solid var(--vp-c-divider);" title="KaiOS"><img src="https://cdn.jsdelivr.net/npm/simple-icons@13.21.0/icons/kaios.svg" alt="KaiOS" width="24" height="24" loading="lazy" onerror="this.closest('span').style.display='none'" /></span>
<span role="listitem" style="display:inline-flex;align-items:center;justify-content:center;width:2.75rem;height:2.75rem;border-radius:8px;background:#f6f6f7;border:1px solid var(--vp-c-divider);" title="FreeBSD"><img src="https://cdn.jsdelivr.net/npm/simple-icons@13.21.0/icons/freebsd.svg" alt="FreeBSD" width="24" height="24" loading="lazy" onerror="this.closest('span').style.display='none'" /></span>
</div>

Windows and Microsoft Edge have no logos here -- notably,
[Simple Icons](https://simpleicons.org) (the CC0-licensed icon set these
come from) doesn't carry either mark, despite both being extremely common.

**Bots** -- exist in the library's source since an unreleased `0.3.0`, not
in the published `0.2.0` package this site installs:

<div role="list" aria-label="A sample of bot operators the library detects by name" style="display:flex;flex-wrap:wrap;gap:0.6rem;margin:1rem 0 1.5rem;">
<span role="listitem" style="display:inline-flex;align-items:center;justify-content:center;width:2.75rem;height:2.75rem;border-radius:8px;background:#f6f6f7;border:1px solid var(--vp-c-divider);" title="Googlebot (Google)"><img src="https://cdn.jsdelivr.net/npm/simple-icons@13.21.0/icons/google.svg" alt="Googlebot (Google)" width="24" height="24" loading="lazy" onerror="this.closest('span').style.display='none'" /></span>
<span role="listitem" style="display:inline-flex;align-items:center;justify-content:center;width:2.75rem;height:2.75rem;border-radius:8px;background:#f6f6f7;border:1px solid var(--vp-c-divider);" title="Baiduspider (Baidu)"><img src="https://cdn.jsdelivr.net/npm/simple-icons@13.21.0/icons/baidu.svg" alt="Baiduspider (Baidu)" width="24" height="24" loading="lazy" onerror="this.closest('span').style.display='none'" /></span>
<span role="listitem" style="display:inline-flex;align-items:center;justify-content:center;width:2.75rem;height:2.75rem;border-radius:8px;background:#f6f6f7;border:1px solid var(--vp-c-divider);" title="Bytespider (ByteDance)"><img src="https://cdn.jsdelivr.net/npm/simple-icons@13.21.0/icons/bytedance.svg" alt="Bytespider (ByteDance)" width="24" height="24" loading="lazy" onerror="this.closest('span').style.display='none'" /></span>
<span role="listitem" style="display:inline-flex;align-items:center;justify-content:center;width:2.75rem;height:2.75rem;border-radius:8px;background:#f6f6f7;border:1px solid var(--vp-c-divider);" title="facebookexternalhit (Meta)"><img src="https://cdn.jsdelivr.net/npm/simple-icons@13.21.0/icons/meta.svg" alt="facebookexternalhit (Meta)" width="24" height="24" loading="lazy" onerror="this.closest('span').style.display='none'" /></span>
<span role="listitem" style="display:inline-flex;align-items:center;justify-content:center;width:2.75rem;height:2.75rem;border-radius:8px;background:#f6f6f7;border:1px solid var(--vp-c-divider);" title="Discordbot (Discord)"><img src="https://cdn.jsdelivr.net/npm/simple-icons@13.21.0/icons/discord.svg" alt="Discordbot (Discord)" width="24" height="24" loading="lazy" onerror="this.closest('span').style.display='none'" /></span>
<span role="listitem" style="display:inline-flex;align-items:center;justify-content:center;width:2.75rem;height:2.75rem;border-radius:8px;background:#f6f6f7;border:1px solid var(--vp-c-divider);" title="LinkedInBot (LinkedIn)"><img src="https://cdn.jsdelivr.net/npm/simple-icons@13.21.0/icons/linkedin.svg" alt="LinkedInBot (LinkedIn)" width="24" height="24" loading="lazy" onerror="this.closest('span').style.display='none'" /></span>
</div>

**AI agents** -- the fuller roster shown below exists only in an also
unreleased `0.4.0`; same not-yet-published status as bots above:

<div role="list" aria-label="A sample of AI agent operators the library detects by name" style="display:flex;flex-wrap:wrap;gap:0.6rem;margin:1rem 0 1.5rem;">
<span role="listitem" style="display:inline-flex;align-items:center;justify-content:center;width:2.75rem;height:2.75rem;border-radius:8px;background:#f6f6f7;border:1px solid var(--vp-c-divider);" title="GPTBot (OpenAI)"><img src="https://cdn.jsdelivr.net/npm/simple-icons@13.21.0/icons/openai.svg" alt="GPTBot (OpenAI)" width="24" height="24" loading="lazy" onerror="this.closest('span').style.display='none'" /></span>
<span role="listitem" style="display:inline-flex;align-items:center;justify-content:center;width:2.75rem;height:2.75rem;border-radius:8px;background:#f6f6f7;border:1px solid var(--vp-c-divider);" title="ClaudeBot (Anthropic)"><img src="https://cdn.jsdelivr.net/npm/simple-icons@13.21.0/icons/anthropic.svg" alt="ClaudeBot (Anthropic)" width="24" height="24" loading="lazy" onerror="this.closest('span').style.display='none'" /></span>
<span role="listitem" style="display:inline-flex;align-items:center;justify-content:center;width:2.75rem;height:2.75rem;border-radius:8px;background:#f6f6f7;border:1px solid var(--vp-c-divider);" title="PerplexityBot (Perplexity)"><img src="https://cdn.jsdelivr.net/npm/simple-icons@13.21.0/icons/perplexity.svg" alt="PerplexityBot (Perplexity)" width="24" height="24" loading="lazy" onerror="this.closest('span').style.display='none'" /></span>
<span role="listitem" style="display:inline-flex;align-items:center;justify-content:center;width:2.75rem;height:2.75rem;border-radius:8px;background:#f6f6f7;border:1px solid var(--vp-c-divider);" title="Amazonbot (Amazon)"><img src="https://cdn.jsdelivr.net/npm/simple-icons@13.21.0/icons/amazon.svg" alt="Amazonbot (Amazon)" width="24" height="24" loading="lazy" onerror="this.closest('span').style.display='none'" /></span>
<span role="listitem" style="display:inline-flex;align-items:center;justify-content:center;width:2.75rem;height:2.75rem;border-radius:8px;background:#f6f6f7;border:1px solid var(--vp-c-divider);" title="Meta-ExternalAgent (Meta)"><img src="https://cdn.jsdelivr.net/npm/simple-icons@13.21.0/icons/meta.svg" alt="Meta-ExternalAgent (Meta)" width="24" height="24" loading="lazy" onerror="this.closest('span').style.display='none'" /></span>
</div>

Meta appears in both rows above on purpose -- it operates both a documented
bot (`facebookexternalhit`) and a separate documented AI-agent crawler
(`Meta-ExternalAgent`), and the library detects them as distinct entries.

Logos via [Simple Icons](https://simpleicons.org) (CC0-1.0, see
[License](/license) for the full attribution) -- each mark remains its
respective owner's trademark. Showing a logo here identifies which
company's crawler/browser a UA string matches; it does not imply that
company endorses or partners with this library.

## Parse a User-Agent

<ParseDemo />

<noscript>This demo requires JavaScript to parse and display your browser's User-Agent -- it won't run with JavaScript disabled.</noscript>

## Generate a User-Agent

Pick browser/engine/OS/device filters and generate a plausible User-Agent
string. Leave any filter on "Any (random)" and it's filled in for you.

<GenerateDemo />

<noscript>This demo requires JavaScript to generate a User-Agent string -- it won't run with JavaScript disabled.</noscript>

## Get started

- [Core Concepts](/guide/core-concepts) -- the shared model and
  pack-composition pattern, explained once.
- [Type Reference](/reference) -- every name each built-in pack recognizes.
- [Browser & Node.js](/guide/js) or [React Native](/guide/react-native) --
  for JavaScript or TypeScript.
- [Android](/guide/android) or [JVM](/guide/jvm) -- for Kotlin.
- [iOS](/guide/ios) -- for Swift.
