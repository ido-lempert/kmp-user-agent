<script setup>
import ParseDemo from './src/demo/ParseDemo.vue';
import GenerateDemo from './src/demo/GenerateDemo.vue';
</script>

# kmp-user-agent

Parse and generate User-Agent strings across Android, iOS, JVM, and JS from
one Kotlin Multiplatform library, behind a single composable API.

See [Core Concepts](/guide/core-concepts) for the shared model and
pack-composition pattern, then the [Browser & Node.js guide](/guide/js) to
get started from JavaScript or TypeScript, the [Android guide](/guide/android)
to get started from Kotlin, or the [iOS guide](/guide/ios) to get started
from Swift.

## Try it live

This isn't a mockup -- it's your own browser, parsed right now by the real,
published `@lempert/user-agent` package.

<ParseDemo />

<noscript>This demo requires JavaScript to parse and display your browser's User-Agent -- it won't run with JavaScript disabled.</noscript>

## Generate a User-Agent

Pick browser/engine/OS/device filters and generate a plausible User-Agent
string -- also via the real, published `@lempert/user-agent` package. Leave
any filter on "Any (random)" and it's filled in for you.

<GenerateDemo />

<noscript>This demo requires JavaScript to generate a User-Agent string -- it won't run with JavaScript disabled.</noscript>
