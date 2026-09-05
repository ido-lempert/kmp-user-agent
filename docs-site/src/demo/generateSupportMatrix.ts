// Hand-maintained ground truth for GenerateDemo.vue's filter dropdowns and
// "randomize unset fields" behavior (Story 5.4, CAP-3; architecture spine
// AD-3).
//
// Every row below is a NAMED, COMPLETE `{browser, engine, os, device}` tuple
// already confirmed -- by reading the real generator, not by guessing -- to
// produce a real, non-degraded `UserAgentGenerator` output. This file never
// exports independent per-field option arrays: GenerateDemo.vue must never
// cross-product browser/engine/os/device itself, because most combinations
// the generator would accept per-field are either unsupported (e.g. a
// "Windows" os with any version other than "10") or actively unsafe (see
// `unsafeCombinations` below).
//
// Ground truth to re-check whenever `@lempert/user-agent` is bumped (spine
// AD-2's version-bump-and-recheck rule):
// library/src/commonMain/kotlin/site/lempert/useragent/UserAgentGenerator.kt
//   - `generateOsToken` (~line 75): Windows only accepts version "10"; Mac OS
//     X/iOS/Android require a non-blank version; Linux ignores version
//     entirely; the `unsafeCombination` check is exactly Firefox+Android,
//     Firefox+iOS, and Safari+Android; the iOS device token is "iPad" only
//     when `device?.model == "iPad"`, else it defaults to iPhone.
//   - `generateBrowserSegment` (~line 117): each browser-family `when`
//     branch -- Chrome/Firefox/Edge require a non-blank `browser.version`;
//     Safari's `AppleWebKit/` token is taken from `engine.version` directly
//     (not just as a fallback), so a Safari preset needs a real, distinct
//     WebKit-style engine version, not Safari's own version number.
//
// Not present as a preset row in this table (would hit `unsafeCombination`
// when merged from a preset, or just aren't a realistic/credible pairing
// for a demo): Firefox+Android, Firefox+iOS, Safari+Android, Chrome+iOS,
// Edge+iOS. This is a statement about what the curated PRESETS contain,
// not an enforced block on visitor-driven combinations: only the three
// pairs in `unsafeCombinations` below are actually checked (and only
// against a visitor's own explicit Browser+OS selections, per the "Never
// silently override" rule). A visitor who independently picks
// Browser=Chrome and OS=iOS from the dropdowns can still reconstruct that
// combination themselves -- it's real and non-crashing, just not one this
// table hands out as a matched pair.
//
// Field values below are plain primitives (strings, or null) only -- this
// file never imports from `@lempert/user-agent`. GenerateDemo.vue is what
// constructs real `Component`/`Device`/`UserAgentInfo` instances from these
// primitives, immediately before calling `UserAgentGenerator`.

export interface ComponentValue {
  name: string;
  version: string;
}

export interface DeviceValue {
  brand: string | null;
  model: string | null;
  name: string | null;
}

export interface UserAgentPreset {
  id: string;
  browser: ComponentValue;
  engine: ComponentValue;
  os: ComponentValue;
  device: DeviceValue | null;
}

/**
 * A (browser name, os name) pair that `generateOsToken`'s `unsafeCombination`
 * check silently strips the OS parenthetical from -- not an error, just a
 * terser (but still valid) generated string. Used by GenerateDemo.vue to
 * decide when to retry a different preset for fields the visitor left
 * unset; it is never used to override a visitor's own explicit selections.
 */
export interface UnsafeCombination {
  browser: string;
  os: string;
}

export const unsafeCombinations: readonly UnsafeCombination[] = [
  { browser: 'Firefox', os: 'Android' },
  { browser: 'Firefox', os: 'iOS' },
  { browser: 'Safari', os: 'Android' },
];

export const presets: readonly UserAgentPreset[] = [
  {
    id: 'chrome-windows',
    browser: { name: 'Chrome', version: '128.0' },
    engine: { name: 'Blink', version: '128.0' },
    os: { name: 'Windows', version: '10' },
    device: null,
  },
  {
    id: 'chrome-mac',
    browser: { name: 'Chrome', version: '128.0' },
    engine: { name: 'Blink', version: '128.0' },
    os: { name: 'Mac OS X', version: '14.5' },
    device: null,
  },
  {
    id: 'chrome-android',
    browser: { name: 'Chrome', version: '128.0' },
    engine: { name: 'Blink', version: '128.0' },
    os: { name: 'Android', version: '14' },
    device: { brand: 'Google', model: 'Pixel 8', name: null },
  },
  {
    id: 'chrome-linux',
    browser: { name: 'Chrome', version: '128.0' },
    engine: { name: 'Blink', version: '128.0' },
    // Linux's version is ignored entirely by generateOsToken -- "" is the
    // documented placeholder, not a real value the generator reads.
    os: { name: 'Linux', version: '' },
    device: null,
  },
  {
    id: 'firefox-windows',
    browser: { name: 'Firefox', version: '130.0' },
    engine: { name: 'Gecko', version: '130.0' },
    os: { name: 'Windows', version: '10' },
    device: null,
  },
  {
    id: 'firefox-mac',
    browser: { name: 'Firefox', version: '130.0' },
    engine: { name: 'Gecko', version: '130.0' },
    os: { name: 'Mac OS X', version: '14.5' },
    device: null,
  },
  {
    id: 'firefox-linux',
    browser: { name: 'Firefox', version: '130.0' },
    engine: { name: 'Gecko', version: '130.0' },
    os: { name: 'Linux', version: '' },
    device: null,
  },
  {
    id: 'safari-mac',
    browser: { name: 'Safari', version: '17.5' },
    engine: { name: 'WebKit', version: '605.1.15' },
    os: { name: 'Mac OS X', version: '14.5' },
    device: null,
  },
  {
    id: 'safari-iphone',
    browser: { name: 'Safari', version: '17.5' },
    engine: { name: 'WebKit', version: '605.1.15' },
    os: { name: 'iOS', version: '17.5' },
    // No device model -- generateOsToken defaults to the iPhone token.
    device: null,
  },
  {
    id: 'safari-ipad',
    browser: { name: 'Safari', version: '17.5' },
    engine: { name: 'WebKit', version: '605.1.15' },
    os: { name: 'iOS', version: '17.5' },
    device: { brand: null, model: 'iPad', name: null },
  },
  {
    id: 'edge-windows',
    browser: { name: 'Edge', version: '128.0' },
    engine: { name: 'Blink', version: '128.0' },
    os: { name: 'Windows', version: '10' },
    device: null,
  },
  {
    id: 'edge-mac',
    browser: { name: 'Edge', version: '128.0' },
    engine: { name: 'Blink', version: '128.0' },
    os: { name: 'Mac OS X', version: '14.5' },
    device: null,
  },
];

/** True when a (browser name, os name) pair matches `unsafeCombinations`. */
export function isUnsafeCombination(
  browserName: string | null | undefined,
  osName: string | null | undefined,
): boolean {
  if (!browserName || !osName) return false;
  return unsafeCombinations.some(
    (combo) => combo.browser === browserName && combo.os === osName,
  );
}

/**
 * The visitor's own per-field selections, or `null` for a field they left
 * unset ("Any (random)"). Passed to {@link resolveGenerateFields} alongside
 * a randomly-ordered list of presets to fill in whatever's unset.
 */
export interface ExplicitFields {
  browser: ComponentValue | null;
  engine: ComponentValue | null;
  os: ComponentValue | null;
  device: DeviceValue | null;
}

/** The fully-resolved fields {@link resolveGenerateFields} hands back, ready
 * to construct real `Component`/`Device`/`UserAgentInfo` instances from. */
export interface ResolvedFields {
  browser: ComponentValue;
  engine: ComponentValue;
  os: ComponentValue;
  device: DeviceValue | null;
}

/**
 * Merge-and-retry-on-unsafe-combination logic (Boundaries, spec-5-4): for
 * each of the 4 fields, use the visitor's `explicit` selection if set,
 * otherwise the field from a preset -- the SAME preset supplies every
 * still-unset field on a given attempt, so an unset engine/device stays
 * coherent with whichever preset filled the other unset fields.
 *
 * `candidatesInRandomOrder` is tried in the order given (the caller is
 * responsible for shuffling -- this function is otherwise pure and
 * deterministic, which is what makes it exercisable from a plain Node
 * script outside Vue/onMounted). The first candidate whose merged
 * `(browser.name, os.name)` pair is NOT one of `unsafeCombinations` wins.
 *
 * If every candidate is unsafe -- which only happens when the visitor's
 * OWN two explicit selections already form an unsafe pair (e.g.
 * Browser=Firefox + OS=Android), since every other case has multiple safe
 * presets available -- this does NOT retry forever and does NOT override
 * the visitor's choice: it falls back to the first candidate and returns
 * the merge anyway. `generateOsToken` never throws for this; it just omits
 * the OS parenthetical.
 *
 * Extracted out of GenerateDemo.vue (spec-5-4 loopback, review_loop_iteration
 * 1) specifically so a plain Node script can exercise it without a Vue
 * runtime -- this logic previously only ran client-side inside `onMounted`,
 * which no CI check could see. A one-line regression here (e.g. swapping
 * `explicit.browser ?? preset.browser` to `preset.browser ?? explicit.browser`)
 * would silently disable the demo's entire advertised feature -- the
 * visitor's filter choice -- while still producing a valid, non-crashing
 * string. `docs-site/scripts/verify-generate-demo.mjs` exists to catch
 * exactly that.
 */
export function resolveGenerateFields(
  explicit: ExplicitFields,
  candidatesInRandomOrder: readonly UserAgentPreset[],
): ResolvedFields {
  for (const preset of candidatesInRandomOrder) {
    const attempt: ResolvedFields = {
      browser: explicit.browser ?? preset.browser,
      engine: explicit.engine ?? preset.engine,
      os: explicit.os ?? preset.os,
      device: explicit.device ?? preset.device,
    };
    if (!isUnsafeCombination(attempt.browser.name, attempt.os.name)) {
      return attempt;
    }
  }

  const fallbackPreset = candidatesInRandomOrder[0];
  return {
    browser: explicit.browser ?? fallbackPreset.browser,
    engine: explicit.engine ?? fallbackPreset.engine,
    os: explicit.os ?? fallbackPreset.os,
    device: explicit.device ?? fallbackPreset.device,
  };
}
