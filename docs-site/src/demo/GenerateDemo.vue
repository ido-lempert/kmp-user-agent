<script setup lang="ts">
// Live proof-of-work for the intro page (Story 5.4, CAP-3): lets a visitor
// pick browser/engine/os/device filters and generate a User-Agent string
// through the real, published @lempert/user-agent package -- never a
// reimplementation. Any filter left unset is randomized from a
// known-safe preset in generateSupportMatrix.ts (spine AD-3) rather than
// left blank or broken.
//
// The initial result is generated inside onMounted, never during
// setup/top-level render: this component has no navigator/window
// dependency (so it doesn't share ParseDemo's SSR-crash concern), but
// Math.random() would still bake one random value into the prerendered
// static HTML while client-side hydration computes a different one --
// a Vue hydration mismatch. Deferring to onMounted avoids this entirely.
import { ref, computed, onMounted } from 'vue';
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
import {
  presets,
  resolveGenerateFields,
  type ComponentValue,
  type DeviceValue,
} from './generateSupportMatrix';

const UNSET = '';

function componentKey(value: ComponentValue): string {
  return `${value.name} ${value.version}`;
}

function deviceKey(value: DeviceValue): string {
  return `${value.brand ?? ''} ${value.model ?? ''} ${value.name ?? ''}`;
}

function componentLabel(value: ComponentValue): string {
  return value.version ? `${value.name} ${value.version}` : value.name;
}

function deviceLabel(value: DeviceValue): string {
  if (value.name) return value.name;
  return [value.brand, value.model].filter(Boolean).join(' ') || 'Device';
}

// Dropdown option lists are derived from `presets` (Boundaries: never
// hand-duplicated separately), deduped by a plain string key so a native
// <select>'s v-model can bind to them directly.
function uniqueBy<T>(items: T[], key: (item: T) => string): T[] {
  const seen = new Map<string, T>();
  for (const item of items) {
    const k = key(item);
    if (!seen.has(k)) seen.set(k, item);
  }
  return [...seen.values()];
}

const browserValues = computed(() => uniqueBy(presets.map((p) => p.browser), componentKey));
const engineValues = computed(() => uniqueBy(presets.map((p) => p.engine), componentKey));
const osValues = computed(() => uniqueBy(presets.map((p) => p.os), componentKey));
const deviceValues = computed(() =>
  uniqueBy(
    presets.map((p) => p.device).filter((d): d is DeviceValue => d !== null),
    deviceKey,
  ),
);

const selectedBrowserKey = ref(UNSET);
const selectedEngineKey = ref(UNSET);
const selectedOsKey = ref(UNSET);
const selectedDeviceKey = ref(UNSET);

function lookup<T>(values: T[], key: string, keyOf: (v: T) => string): T | null {
  if (key === UNSET) return null;
  return values.find((v) => keyOf(v) === key) ?? null;
}

const explicitBrowser = computed(() => lookup(browserValues.value, selectedBrowserKey.value, componentKey));
const explicitEngine = computed(() => lookup(engineValues.value, selectedEngineKey.value, componentKey));
const explicitOs = computed(() => lookup(osValues.value, selectedOsKey.value, componentKey));
const explicitDevice = computed(() => lookup(deviceValues.value, selectedDeviceKey.value, deviceKey));

const result = ref('');
const generateError = ref<string | null>(null);

/** Fisher-Yates shuffle -- used to try presets in random order when a field
 * is unset and the visitor's own selections risk an unsafe combination. */
function shuffled<T>(items: readonly T[]): T[] {
  const copy = [...items];
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [copy[i], copy[j]] = [copy[j], copy[i]];
  }
  return copy;
}

function generate(): void {
  // Merge-and-retry-on-unsafe-combination logic lives in
  // resolveGenerateFields (generateSupportMatrix.ts) -- extracted out of
  // this component (spec-5-4 loopback) so a plain Node script can exercise
  // it without a Vue runtime; this function stays framework-free and pure,
  // this component only shuffles (a UI-adjacent randomization concern) and
  // hands it the visitor's explicit selections plus the shuffled order.
  const candidates = shuffled(presets);
  const merged = resolveGenerateFields(
    {
      browser: explicitBrowser.value,
      engine: explicitEngine.value,
      os: explicitOs.value,
      device: explicitDevice.value,
    },
    candidates,
  );

  try {
    // Plain primitives from the matrix -> real library instances,
    // constructed immediately before calling UserAgentGenerator
    // (Boundaries).
    const info = new UserAgentInfo(
      new Component(merged.browser.name, merged.browser.version),
      new Component(merged.engine.name, merged.engine.version),
      new Component(merged.os.name, merged.os.version),
      merged.device ? new Device(merged.device.brand, merged.device.model, merged.device.name) : null,
    );

    const generateFn = UserAgentGenerator([
      UserAgentBrowserTypes.get(),
      UserAgentEngineTypes.get(),
      UserAgentOsTypes.get(),
      // Included for parity with the parse-direction pack list (and spine
      // AD-2's exact four-pack requirement) even though it contributes no
      // independent generate-direction segment of its own --
      // UserAgentGenerator.kt has no generateDeviceSegment; device data
      // instead flows into the OS token via generateOsToken's `device`
      // parameter (e.g. the Android "; Pixel 8" suffix, or the
      // iPad-vs-iPhone iOS token choice). Passing this pack is harmless
      // (its applyToGenerate contributes nothing) and keeps this call
      // matching the matrix's documented four fields.
      UserAgentDeviceTypes.get(),
    ]);

    // Surface an explicit error state rather than leaving the demo stuck
    // on a stale/loading result if the real generator throws -- matches
    // ParseDemo.vue's pattern. Also log it so there's a diagnostic trail
    // beyond the rendered error text.
    result.value = generateFn(info);
    generateError.value = null;
  } catch (error) {
    console.error(error);
    generateError.value = error instanceof Error ? error.message : String(error);
  }
}

onMounted(() => {
  generate();
});
</script>

<template>
  <div class="generate-demo">
    <div class="generate-demo__filters">
      <label class="generate-demo__field">
        <span>Browser</span>
        <select v-model="selectedBrowserKey">
          <option :value="UNSET">Any (random)</option>
          <option v-for="value in browserValues" :key="componentKey(value)" :value="componentKey(value)">
            {{ componentLabel(value) }}
          </option>
        </select>
      </label>
      <label class="generate-demo__field">
        <span>Engine</span>
        <select v-model="selectedEngineKey">
          <option :value="UNSET">Any (random)</option>
          <option v-for="value in engineValues" :key="componentKey(value)" :value="componentKey(value)">
            {{ componentLabel(value) }}
          </option>
        </select>
      </label>
      <label class="generate-demo__field">
        <span>OS</span>
        <select v-model="selectedOsKey">
          <option :value="UNSET">Any (random)</option>
          <option v-for="value in osValues" :key="componentKey(value)" :value="componentKey(value)">
            {{ componentLabel(value) }}
          </option>
        </select>
      </label>
      <label class="generate-demo__field">
        <span>Device</span>
        <select v-model="selectedDeviceKey">
          <option :value="UNSET">Any (random)</option>
          <option v-for="value in deviceValues" :key="deviceKey(value)" :value="deviceKey(value)">
            {{ deviceLabel(value) }}
          </option>
        </select>
      </label>
    </div>
    <button class="generate-demo__button" type="button" @click="generate">Generate</button>
    <div
      class="generate-demo__result-region"
      aria-live="polite"
      :aria-busy="!result && !generateError"
    >
      <p class="generate-demo__result">
        <code v-if="result">{{ result }}</code>
        <em v-else-if="generateError" class="generate-demo__error">Could not generate: {{ generateError }}</em>
        <em v-else class="generate-demo__loading">Generating&hellip;</em>
      </p>
      <p class="generate-demo__note">
        Fields left on "Any (random)" are filled from a known-safe preset;
        picking both a browser and OS that don't normally pair together
        (e.g. Firefox + Android) still generates a valid string -- just
        without the OS detail.
      </p>
    </div>
  </div>
</template>

<style scoped>
.generate-demo {
  margin: 1.5rem 0;
  padding: 1rem 1.25rem;
  border: 1px solid var(--vp-c-divider);
  border-radius: 8px;
  background-color: var(--vp-c-bg-soft);
}

.generate-demo__filters {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem 1rem;
  margin-bottom: 1rem;
}

.generate-demo__field {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  font-size: 0.85em;
  color: var(--vp-c-text-2);
}

.generate-demo__field select {
  padding: 0.35rem 0.5rem;
  border: 1px solid var(--vp-c-divider);
  border-radius: 6px;
  background-color: var(--vp-c-bg);
  color: var(--vp-c-text-1);
  font-size: 0.95em;
}

.generate-demo__button {
  padding: 0.45rem 1rem;
  border: 1px solid var(--vp-c-brand-1);
  border-radius: 6px;
  background-color: var(--vp-c-brand-1);
  color: var(--vp-c-white, #fff);
  font-weight: 500;
  cursor: pointer;
}

.generate-demo__button:hover {
  background-color: var(--vp-c-brand-2);
  border-color: var(--vp-c-brand-2);
}

.generate-demo__result {
  margin: 1rem 0 0;
  word-break: break-all;
}

.generate-demo__loading {
  color: var(--vp-c-text-3);
  font-style: italic;
}

.generate-demo__error {
  color: var(--vp-c-danger-1);
  font-style: italic;
}

.generate-demo__note {
  margin: 0.75rem 0 0;
  font-size: 0.85em;
  color: var(--vp-c-text-2);
}
</style>
