#!/usr/bin/env node
// Plain Node verification for GenerateDemo.vue's merge-and-retry logic
// (Story 5.4 loopback, spec-5-4-live-generate-demo.md review_loop_iteration
// 1). No new test framework/dependency -- imports the REAL
// `resolveGenerateFields`/`presets` from generateSupportMatrix.ts and the
// REAL installed `@lempert/user-agent` package, and calls
// `UserAgentGenerator` exactly as GenerateDemo.vue does.
//
// Why this exists: `resolveGenerateFields` previously only ran client-side
// inside GenerateDemo.vue's onMounted/click handler, so neither of the two
// existing CI checks (which only inspect the SSR build output) could ever
// see it. A one-line regression -- swapping
// `explicit.browser ?? preset.browser` to
// `preset.browser ?? explicit.browser` -- would silently disable the
// demo's entire advertised feature (the visitor's filter choice winning
// over the randomizer) while still producing a valid, non-crashing
// string. This script's assertion (b) below is the one that specifically
// catches that regression shape; the others cover the rest of the
// Boundaries/I-O matrix in spec-5-4.
//
// Run directly: `node docs-site/scripts/verify-generate-demo.mjs`
// Exits non-zero if any assertion fails.

import {
  UserAgentBrowserTypes,
  UserAgentEngineTypes,
  UserAgentOsTypes,
  UserAgentDeviceTypes,
  UserAgentGenerator,
  UserAgentInfo,
  Component,
  Device,
} from '@lempert/user-agent';

import { presets, resolveGenerateFields } from '../src/demo/generateSupportMatrix.ts';

function shuffled(items) {
  const copy = [...items];
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [copy[i], copy[j]] = [copy[j], copy[i]];
  }
  return copy;
}

const generateFn = UserAgentGenerator([
  UserAgentBrowserTypes.get(),
  UserAgentEngineTypes.get(),
  UserAgentOsTypes.get(),
  UserAgentDeviceTypes.get(),
]);

function generateFromFields(merged) {
  const info = new UserAgentInfo(
    new Component(merged.browser.name, merged.browser.version),
    new Component(merged.engine.name, merged.engine.version),
    new Component(merged.os.name, merged.os.version),
    merged.device ? new Device(merged.device.brand, merged.device.model, merged.device.name) : null,
  );
  return generateFn(info);
}

const NO_EXPLICIT = { browser: null, engine: null, os: null, device: null };

let failures = 0;

function check(label, condition) {
  if (!condition) {
    failures++;
    console.error(`FAIL: ${label}`);
  } else {
    console.log(`PASS: ${label}`);
  }
}

// (a) Every preset produces non-degraded output, whether resolved with all
// fields explicit or with everything left for the randomizer to fill in
// from that same preset alone (a single-preset candidate list forces
// resolveGenerateFields to use it).
for (const preset of presets) {
  const explicitAll = resolveGenerateFields(
    { browser: preset.browser, engine: preset.engine, os: preset.os, device: preset.device },
    shuffled(presets),
  );
  const uaExplicit = generateFromFields(explicitAll);
  check(`(a) preset ${preset.id}, all fields explicit: non-empty UA`, typeof uaExplicit === 'string' && uaExplicit.length > 0);

  const randomized = resolveGenerateFields(NO_EXPLICIT, [preset]);
  const uaRandomized = generateFromFields(randomized);
  check(`(a) preset ${preset.id}, resolved from single-candidate list: non-empty UA`, typeof uaRandomized === 'string' && uaRandomized.length > 0);
}

// (b) Forcing an explicit browser actually shows up in the generated
// string's browser token, across many resolutions with everything else
// left to the randomizer. This is the specific check that catches an
// operand-order regression in resolveGenerateFields (e.g.
// `preset.browser ?? explicit.browser` instead of
// `explicit.browser ?? preset.browser`) -- such a regression would let a
// random preset's browser silently win instead, and this loop would see a
// browser token that does NOT match the explicit selection.
const edgePreset = presets.find((p) => p.id === 'edge-windows');
const explicitBrowser = edgePreset.browser; // { name: 'Edge', version: '128.0' }
for (let i = 0; i < 40; i++) {
  const merged = resolveGenerateFields(
    { browser: explicitBrowser, engine: null, os: null, device: null },
    shuffled(presets),
  );
  check(`(b) trial #${i}: resolved browser matches explicit selection`, merged.browser.name === explicitBrowser.name && merged.browser.version === explicitBrowser.version);
  const ua = generateFromFields(merged);
  check(`(b) trial #${i}: generated string carries the Edge browser token`, ua.includes(`Edg/${explicitBrowser.version}`));
}

// (c) Forcing browser=Firefox never resolves to an Android/iOS os across
// many trials (the randomizer must retry presets to avoid the unsafe
// combination when only browser is explicit).
const firefoxPreset = presets.find((p) => p.id === 'firefox-windows');
const explicitFirefox = firefoxPreset.browser;
for (let i = 0; i < 40; i++) {
  const merged = resolveGenerateFields(
    { browser: explicitFirefox, engine: null, os: null, device: null },
    shuffled(presets),
  );
  check(`(c) trial #${i}: OS never resolves to Android/iOS when browser=Firefox`, merged.os.name !== 'Android' && merged.os.name !== 'iOS');
}

// (d) The forced Firefox+Android case still produces a non-empty,
// OS-token-less string -- not blocked, not overridden, not throwing.
const explicitAndroidOs = presets.find((p) => p.id === 'chrome-android').os;
{
  const merged = resolveGenerateFields(
    { browser: explicitFirefox, engine: null, os: explicitAndroidOs, device: null },
    shuffled(presets),
  );
  check('(d) forced Firefox+Android: browser not silently overridden', merged.browser.name === 'Firefox');
  check('(d) forced Firefox+Android: OS not silently overridden', merged.os.name === 'Android');
  const ua = generateFromFields(merged);
  check('(d) forced Firefox+Android: non-empty UA', typeof ua === 'string' && ua.length > 0);
  // Firefox's OS-token-present shape is "(<os token>; rv:<version>)"; the
  // OS-token-less fallback shape is "(rv:<version>)" with nothing before
  // "rv:" inside the parenthetical.
  check('(d) forced Firefox+Android: no OS parenthetical (terser "(rv:...)" form)', /\(rv:[\d.]+\)/.test(ua));
  console.log(`  forced Firefox+Android UA: ${ua}`);
}

console.log('');
if (failures > 0) {
  console.error(`${failures} CHECK(S) FAILED`);
  process.exit(1);
} else {
  console.log('ALL CHECKS PASSED');
  process.exit(0);
}
