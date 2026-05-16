<script setup lang="ts">
import { NButton } from "naive-ui";
type ErrorStateVariant = "inline" | "card" | "full" | "overlay";

withDefaults(
  defineProps<{
    description: string;
    details?: string;
    errorCode?: string;
    retryLabel?: string;
    secondaryActionLabel?: string;
    supportActionLabel?: string;
    title: string;
    variant?: ErrorStateVariant;
  }>(),
  {
    details: undefined,
    errorCode: undefined,
    retryLabel: "Retry",
    secondaryActionLabel: undefined,
    supportActionLabel: undefined,
    variant: "card",
  },
);

const emit = defineEmits<{
  retry: [];
  secondary: [];
  support: [];
}>();
</script>

<template>
  <section class="error-state" :class="`error-state--${variant}`" role="alert">
    <div class="error-state__icon" aria-hidden="true">!</div>
    <div class="error-state__copy">
      <p v-if="errorCode" class="error-state__code">{{ errorCode }}</p>
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
      <pre v-if="details" class="error-state__details">{{ details }}</pre>
    </div>
    <div class="error-state__actions">
      <NButton class="error-state__primary" native-type="button" @click="emit('retry')">
        {{ retryLabel }}
      </NButton>
      <NButton v-if="supportActionLabel" native-type="button" @click="emit('support')">
        {{ supportActionLabel }}
      </NButton>
      <NButton v-if="secondaryActionLabel" native-type="button" @click="emit('secondary')">
        {{ secondaryActionLabel }}
      </NButton>
    </div>
  </section>
</template>

<style scoped>
.error-state {
  display: grid;
  gap: 14px;
  padding: 24px;
  border: 1px solid color-mix(in srgb, var(--mm-danger) 22%, var(--mm-border));
  border-radius: var(--mm-radius-md);
  background: color-mix(in srgb, var(--mm-danger) 5%, var(--mm-surface));
  color: var(--mm-text-primary);
}

.error-state__icon {
  display: inline-grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--mm-danger) 12%, white);
  color: var(--mm-danger);
  font-weight: 800;
}

.error-state__copy h2 {
  margin: 0;
  font-size: 18px;
  line-height: 1.25;
  letter-spacing: 0;
}

.error-state__copy p {
  margin: 8px 0 0;
  color: var(--mm-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.error-state__code {
  margin: 0 0 6px;
  color: var(--mm-danger);
  font-size: 11px;
  font-weight: 800;
  text-transform: uppercase;
}

.error-state__details {
  max-height: 140px;
  margin: 12px 0 0;
  overflow: auto;
  padding: 12px;
  border-radius: var(--mm-radius-sm);
  background: var(--mm-surface-soft);
  color: var(--mm-text-secondary);
  font-size: 12px;
  white-space: pre-wrap;
}

.error-state__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.error-state__actions button {
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid var(--mm-border);
  border-radius: var(--mm-radius-sm);
  background: var(--mm-surface);
  color: var(--mm-text-primary);
  font-weight: 700;
}

.error-state__primary {
  border-color: var(--mm-danger) !important;
  background: var(--mm-danger) !important;
  color: white !important;
}

.error-state--inline {
  grid-template-columns: auto 1fr auto;
  align-items: center;
  padding: 14px;
}

.error-state--full,
.error-state--overlay {
  min-height: 320px;
  align-content: center;
  justify-items: center;
  text-align: center;
}

.error-state--overlay {
  background: color-mix(in srgb, var(--mm-surface) 94%, transparent);
  box-shadow: var(--mm-shadow-lg);
}
</style>
