<script setup lang="ts">
// Live proof-of-work for the intro page (Story 5.3, CAP-2): parses the
// visitor's own navigator.userAgent through the real, published
// @lempert/user-agent package -- never a reimplementation.
//
// navigator.userAgent is read only inside onMounted, never at the top level
// of this script -- VitePress's SSR build pass does NOT crash on a top-level
// `navigator.userAgent` read (Node 21+ silently resolves its own polyfill to
// "Node.js/<version>"), so a top-level read would silently bake a wrong
// static value into the prerendered HTML instead of failing loudly.
import { ref, onMounted, computed } from 'vue';
import { UserAgentAllTypes, UserAgentParser } from '@lempert/user-agent';
import type { UserAgentInfo, Component, Device } from '@lempert/user-agent';

const userAgentString = ref('');
const info = ref<UserAgentInfo | null>(null);
const parseError = ref<string | null>(null);

onMounted(() => {
  userAgentString.value = navigator.userAgent;

  try {
    // Exact documented call shape (docs-site/guide/js.md): array-wrapped
    // pack argument, `.get()` on the getter-object export. No parse logic
    // is reimplemented here.
    const parse = UserAgentParser([UserAgentAllTypes.get()]);
    info.value = parse(navigator.userAgent);
  } catch (error) {
    // Surface an explicit error state rather than leaving the demo stuck on
    // the loading placeholder forever if the real parser throws. Also log
    // it so there's a diagnostic trail beyond the rendered error text.
    console.error(error);
    parseError.value = error instanceof Error ? error.message : String(error);
  }
});

function formatComponent(component: Component | null | undefined): string | null {
  if (!component) return null;
  const parts = [component.name, component.version].filter(
    (part): part is string => Boolean(part),
  );
  return parts.length ? parts.join(' ') : null;
}

function formatDevice(device: Device | null | undefined): string | null {
  if (!device) return null;
  if (device.name) return device.name;
  const parts = [device.brand, device.model].filter((part): part is string => Boolean(part));
  return parts.length ? parts.join(' ') : null;
}

const fields = computed(() => [
  { label: 'Browser', value: info.value ? formatComponent(info.value.browser) : null },
  { label: 'Engine', value: info.value ? formatComponent(info.value.engine) : null },
  { label: 'OS', value: info.value ? formatComponent(info.value.os) : null },
  { label: 'Device', value: info.value ? formatDevice(info.value.device) : null },
  { label: 'Bot', value: info.value ? formatComponent(info.value.bot) : null },
  { label: 'AI Agent', value: info.value ? formatComponent(info.value.aiAgent) : null },
]);
</script>

<template>
  <div class="parse-demo" aria-live="polite" :aria-busy="!info && !parseError">
    <p class="parse-demo__source">
      <strong>Your User-Agent: </strong>
      <code v-if="info">{{ userAgentString }}</code>
      <em v-else-if="parseError" class="parse-demo__error">Could not parse: {{ parseError }}</em>
      <em v-else class="parse-demo__loading">Detecting your browser&hellip;</em>
    </p>
    <table class="parse-demo__table">
      <tbody>
        <tr v-for="field in fields" :key="field.label">
          <th scope="row">{{ field.label }}</th>
          <td v-if="parseError" class="parse-demo__error">Error</td>
          <td v-else-if="!info" class="parse-demo__loading">Loading&hellip;</td>
          <td v-else-if="field.value">{{ field.value }}</td>
          <td v-else class="parse-demo__not-detected">Not detected</td>
        </tr>
      </tbody>
    </table>
    <p class="parse-demo__note">
      "Not detected" can mean either that this browser/OS isn't recognized, or
      that the field isn't covered by the currently published package
      version.
    </p>
  </div>
</template>

<style scoped>
.parse-demo {
  margin: 1.5rem 0;
  padding: 1rem 1.25rem;
  border: 1px solid var(--vp-c-divider);
  border-radius: 8px;
  background-color: var(--vp-c-bg-soft);
}

.parse-demo__source {
  margin: 0 0 0.75rem;
  word-break: break-all;
}

.parse-demo__table {
  width: 100%;
  border-collapse: collapse;
}

.parse-demo__table th {
  padding: 0.35rem 0.75rem 0.35rem 0;
  color: var(--vp-c-text-2);
  font-weight: 500;
  text-align: left;
  white-space: nowrap;
}

.parse-demo__table td {
  padding: 0.35rem 0;
}

.parse-demo__loading,
.parse-demo__not-detected {
  color: var(--vp-c-text-3);
  font-style: italic;
}

.parse-demo__error {
  color: var(--vp-c-danger-1);
  font-style: italic;
}

.parse-demo__note {
  margin: 0.75rem 0 0;
  font-size: 0.85em;
  color: var(--vp-c-text-2);
}
</style>
