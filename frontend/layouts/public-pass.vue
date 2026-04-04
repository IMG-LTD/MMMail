<template>
  <div class="public-pass-layout">
    <a class="skip-link" href="#public-pass-main">{{ t('shell.a11y.skipToContent') }}</a>
    <header class="public-pass-header" :aria-label="t('shell.publicPass.a11y.headerLabel')">
      <div>
        <NuxtLink to="/" class="brand">{{ t('shell.publicShare.brand') }}</NuxtLink>
        <p class="subtitle">{{ t('shell.publicPassShare.subtitle') }}</p>
      </div>
      <LocaleSwitcher size="small" />
    </header>
    <main id="public-pass-main" class="public-pass-main" tabindex="-1" :aria-label="t('shell.publicPass.a11y.mainLabel')">
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'

const { t } = useI18n()
</script>

<style scoped>
.skip-link {
  position: absolute;
  left: 16px;
  top: 12px;
  z-index: 50;
  padding: 8px 12px;
  border-radius: 10px;
  background: #ffffff;
  color: #0f172a;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.2);
  transform: translateY(-160%);
}

.skip-link:focus-visible {
  transform: translateY(0);
}

.public-pass-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background:
    radial-gradient(circle at top left, rgba(95, 247, 215, 0.16), transparent 24%),
    radial-gradient(circle at top right, rgba(122, 92, 255, 0.18), transparent 30%),
    linear-gradient(160deg, #07121d 0%, #10172c 48%, #10253a 100%);
}

.public-pass-header {
  padding: 20px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.brand {
  color: #f4ffff;
  font-weight: 700;
  letter-spacing: 0.4px;
}

.subtitle {
  margin: 6px 0 0;
  color: rgba(226, 241, 255, 0.72);
  font-size: 13px;
}

.public-pass-main {
  flex: 1;
  padding: 24px;
}

@media (max-width: 768px) {
  .public-pass-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
    padding: 18px;
  }

  .public-pass-main {
    padding: 16px;
  }
}
</style>
