# React Native Usage

`@lempert/user-agent` is the same JS/TypeScript build documented in the
[Browser & Node.js guide](./js) -- there's no separate React Native package,
and no native module to link on either platform (Podfile/`pod install` on
iOS, Gradle autolinking on Android): it's plain JS, so Metro bundles it like
any other npm dependency. That also means it works unmodified under Expo's
managed workflow (Expo Go or a plain EAS build) -- there's no native code
requiring a custom dev client.

## Install

```sh
npm install @lempert/user-agent
```

If Metro doesn't pick up the new dependency (rare, but a known general
Metro gotcha, not specific to this package), clear its cache:
`npx react-native start --reset-cache`.

## Verified compatibility

Compatibility with Metro (React Native's bundler) and Hermes (its default
JS engine) was an open question before this guide existed -- neither is
guaranteed just because a package works in a browser or Node.js. This was
checked empirically this way, not assumed: the published package was
bundled through the real `metro build` CLI (`react-native@0.81.4` +
`@react-native/metro-config@0.81.4`) and the resulting bundle was executed
on the actual Hermes binary Meta ships inside the `react-native` package
itself (`node_modules/react-native/sdks/hermesc/*-bin/hermes`) -- the real
JS engine, not V8/Node or a simulation. That's a standalone bundle-and-run
check outside a full RN app shell (no bridge, no on-device/simulator run);
treat it as "the JS executes correctly under Hermes," not "confirmed inside
a running app."

Metro resolved the package's plain `"main": "library.mjs"` field with no
extra configuration needed. Both parse and generate produced correct
results, including the case most likely to expose a JS-engine-specific
regex gap -- multi-field device detection from a real UA string:

```ts
import {
  UserAgentAllTypes,
  UserAgentParser,
  UserAgentGenerator,
  UserAgentInfo,
  Component,
} from '@lempert/user-agent';

const parse = UserAgentParser([UserAgentAllTypes.get()]);
const info = parse(
  'Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 ' +
    '(KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1',
);

console.log(info.browser?.name); // "Mobile Safari"
console.log(info.os?.name, info.os?.version); // "iOS" "17.5"
console.log(info.device?.name, info.device?.brand); // "iPhone" "Apple"

const generate = UserAgentGenerator([UserAgentAllTypes.get()]);
const generated = generate(
  new UserAgentInfo(
    new Component('Chrome', '128.0'),
    new Component('Blink', '128.0'),
    new Component('Windows', '10'),
    null,
  ),
);
console.log(generated);
// Mozilla/5.0 (Windows 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0 Safari/537.36
```

Use the same API shape documented for JS/TS generally -- see
[Browser & Node.js's "API shape in JS/TS"](./js#api-shape-in-js-ts) for the
`.get()`/array/`new`-constructor details (they apply identically here; this
isn't a React-Native-specific API).

## Where to get a real User-Agent string

`navigator.userAgent` (the web/Node approach in the other guide) isn't
available in React Native. Two real sources instead:

- `react-native-device-info`'s `getUserAgent()` -- an async `Promise<string>`
  supported on both iOS and Android -- returns the platform's WebView UA
  string (e.g. `Mozilla/5.0 (iPhone; CPU iPhone OS 9_1 like Mac OS X) ...`
  on iOS, `Mozilla/5.0 (Linux; Android 12; ...) ... Chrome/91.0... Mobile
  Safari/537.36` on Android -- both per that package's own docs). Unlike
  `@lempert/user-agent`, this package does ship native modules and needs
  the usual autolinking/`pod install` step.
- If your app makes its own HTTP requests, whatever HTTP client you use
  (fetch, axios, ...) lets you set/read the `User-Agent` header your app
  itself sends -- that's a string you control, not one to detect.

Either way, once you have a real UA string, parsing it works exactly like
the example above.

## Bundle size and tree-shaking under Metro

The [Browser & Node.js guide](./js) and [Core Concepts](./core-concepts)
both note that passing a narrower pack (e.g. `UserAgentBrowserTypes` alone)
instead of `UserAgentAllTypes` lets a bundler tree-shake out the unused
packs' rule tables. That's verified true for webpack/esbuild-style bundlers.
It is **not** true for Metro's default configuration -- checked by bundling
a browser-only entry point (214,286 bytes minified) against an all-types
entry point (215,363 bytes minified): about 1 KB smaller, not the
substantial reduction a web bundler produces. This isn't specific to this
library -- Metro's default setup doesn't eliminate any unused top-level
export this way, for any package. If you only need one detection category,
you'll still get every pack's rule table bundled in either way under Metro.

## Next steps

For the platform-agnostic `UserAgentInfo` model and the built-in type packs,
see [Core Concepts](./core-concepts) -- and the
[Type Reference](../reference) for every name each pack recognizes.
