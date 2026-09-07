# Browser & Node.js Usage

`@lempert/user-agent` is the JS/TypeScript build of `kmp-user-agent`, published
to npm straight from the library's own Kotlin/JS target -- the same
detection/generation logic used by every other platform.

## Install

```sh
npm install @lempert/user-agent
```

## API shape in JS/TS

Kotlin/JS's `@JsExport` lowering exports top-level pack constants (like
`UserAgentBrowserTypes`) as getter objects rather than plain values -- call
`.get()` to retrieve the actual `UserAgentTypePack`. `UserAgentParser` and
`UserAgentGenerator` take packs as a plain array (there's no vararg support
across the JS boundary), and `UserAgentInfo`/`Component`/`Device` are real JS
classes, constructed with `new`. TypeScript type declarations ship with the
package too (its `package.json` points `types` at a generated `library.d.mts`),
so every example below is fully typed with no extra `@types` package needed.

## Browser

### With a bundler (Vite, webpack, esbuild, ...)

```ts
import { UserAgentAllTypes, UserAgentParser } from '@lempert/user-agent';

const parse = UserAgentParser([UserAgentAllTypes.get()]);
const info = parse(navigator.userAgent);

console.log(info.browser?.name, info.browser?.version); // e.g. "Chrome" "128.0"
console.log(info.os?.name, info.os?.version);
console.log(info.device);
```

### With a `<script type="module">` tag (no bundler)

The published package ships a plain ES module (`library.mjs`), so a CDN that
serves npm packages directly -- here, jsDelivr -- works from a module script
with no build step at all:

```html
<script type="module">
  import { UserAgentAllTypes, UserAgentParser } from
    'https://cdn.jsdelivr.net/npm/@lempert/user-agent@0.2.0/library.mjs';

  const parse = UserAgentParser([UserAgentAllTypes.get()]);
  console.log(parse(navigator.userAgent));
</script>
```

> The `@0.2.0` above is a hand-pinned CDN URL, not something a bundler
> resolves for you -- bump it in the same commit that bumps
> `docs-site/package.json`'s `@lempert/user-agent` dependency, the same
> version-bump-and-recheck discipline the architecture spine requires for
> `generateSupportMatrix.ts`.

## Node.js

Install it the same way as above (see [Install](#install)). The package is
ESM-only -- its `package.json` declares `"main": "library.mjs"` with no
CommonJS build -- so consume it with `import`:

```ts
// index.mjs, or any .js file once the nearest package.json has "type": "module"
import { UserAgentAllTypes, UserAgentGenerator, UserAgentInfo, Component } from '@lempert/user-agent';

const generate = UserAgentGenerator([UserAgentAllTypes.get()]);

const userAgentString = generate(
  new UserAgentInfo(
    new Component('Chrome', '128.0'),
    new Component('Blink', '128.0'),
    new Component('Windows', '10'),
    null,
  ),
);

console.log(userAgentString);
// Mozilla/5.0 (Windows 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0 Safari/537.36
```

### From CommonJS (`require`)

The package has no separate CommonJS build -- `require('@lempert/user-agent')`
works directly as long as Node's `require(esm)` support is unflagged, which it
is since Node 20.19+ and 22.12+ (so both of today's supported LTS lines, 22
and 24, handle it out of the box):

```js
// CommonJS file (.cjs, or a package.json without "type": "module")
const { UserAgentAllTypes, UserAgentParser } = require('@lempert/user-agent');

const parse = UserAgentParser([UserAgentAllTypes.get()]);
console.log(parse(process.argv[2] ?? ''));
```

On an older Node (pre-20.19), that same `require()` throws `ERR_REQUIRE_ESM`
instead -- from a CommonJS file on those versions, load the package with a
dynamic `import()` there instead:

```js
async function main() {
  const { UserAgentAllTypes, UserAgentParser } = await import('@lempert/user-agent');
  const parse = UserAgentParser([UserAgentAllTypes.get()]);
  console.log(parse(process.argv[2] ?? ''));
}

main();
```

## Next steps

For the platform-agnostic `UserAgentInfo` model and the built-in type packs
(including `UserAgentBotTypes`/`UserAgentAIAgentTypes`) behind the
`UserAgentAllTypes`/`UserAgentBrowserTypes`-style constants used above, see
[Core Concepts](./core-concepts) -- and the [Type Reference](../reference)
for every name each pack recognizes.
