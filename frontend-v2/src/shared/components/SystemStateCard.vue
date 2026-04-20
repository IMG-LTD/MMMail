<script setup lang="ts">
import { useLocaleText, type TextLike } from '@/locales'

const { tr } = useLocaleText()

defineProps<{
  eyebrow: TextLike
  title: TextLike
  description: TextLike
  accent: string
  primaryLabel: TextLike
  secondaryLabel: TextLike
}>()
</script>

<template>
  <section class="system-state">
    <article class="system-state__card">
      <span class="section-label">{{ tr(eyebrow) }}</span>
      <span class="system-state__mark" :style="{ background: accent }" />
      <h1>{{ tr(title) }}</h1>
      <p>{{ tr(description) }}</p>
      <div class="system-state__actions">
        <a class="system-state__primary" href="/suite">{{ tr(primaryLabel) }}</a>
        <a class="system-state__secondary" href="/boundary">{{ tr(secondaryLabel) }}</a>
      </div>
    </article>
  </section>
</template>

<style scoped>
.system-state {
  display: grid;
  place-items: center;
  min-height: 100vh;
  padding: 24px;
}

.system-state__card {
  width: min(520px, 100%);
  padding: 28px;
  border: 1px solid var(--mm-border);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: var(--mm-shadow);
}

.system-state__mark {
  display: block;
  width: 56px;
  height: 6px;
  margin: 18px 0 18px;
  border-radius: 999px;
}

h1 {
  margin: 0;
  font-size: 36px;
  line-height: 0.98;
  letter-spacing: -0.05em;
}

p {
  margin: 18px 0 0;
  color: var(--mm-text-secondary);
  line-height: 1.8;
}

.system-state__actions {
  display: flex;
  gap: 12px;
  margin-top: 26px;
}

.system-state__primary,
.system-state__secondary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  padding: 0 16px;
  border-radius: 12px;
}

.system-state__primary {
  background: #1f2937;
  color: #fff;
}

.system-state__secondary {
  border: 1px solid var(--mm-border);
  color: var(--mm-text-secondary);
}

@media (max-width: 640px) {
  .system-state__actions {
    flex-direction: column;
  }
}
</style>
