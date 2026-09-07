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
  real syntax).
- **Composable, tree-shakeable type packs.** Pull in only the detection
  categories you need -- browser, engine, OS, device, bots, AI/LLM crawlers.
  There's no implicit fallback pulling in packs you didn't ask for, so a
  bundler can tree-shake the rest out of your JS build.
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
- [Browser & Node.js](/guide/js) -- for JavaScript or TypeScript.
- [Android](/guide/android) or [JVM](/guide/jvm) -- for Kotlin.
- [iOS](/guide/ios) -- for Swift.
