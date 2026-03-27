<template>
  <div class="public-drive-layout">
    <header class="public-header">
      <div>
        <NuxtLink to="/" class="brand">{{ t('shell.publicShare.brand') }}</NuxtLink>
        <p class="subtitle">{{ t('shell.publicShare.subtitle') }}</p>
      </div>
      <LocaleSwitcher size="small" />
    </header>
    <main class="public-main">
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'

const { t } = useI18n()
</script>

<style scoped>
.public-drive-layout {
  min-height: 100vh;
  background: radial-gradient(circle at 0% 0%, #e8f7f7 0%, #f8fbfd 45%, #ffffff 100%);
  display: flex;
  flex-direction: column;
}

.public-header {
  padding: 20px;
  border-bottom: 1px solid #dbe8e8;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.brand {
  color: #0b5353;
  font-weight: 700;
  letter-spacing: 0.4px;
}

.subtitle {
  margin: 6px 0 0;
  color: #587474;
  font-size: 13px;
}

.public-main {
  flex: 1;
  padding: 20px;
}

@media (max-width: 768px) {
  .public-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .public-main {
    padding: 14px;
  }
}
</style>
