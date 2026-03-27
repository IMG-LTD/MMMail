<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'

const { t } = useI18n()
</script>

<template>
  <div class="blank-layout">
    <header class="blank-header">
      <div>
        <NuxtLink to="/login" class="brand">MMMail</NuxtLink>
        <p class="brand-subtitle">{{ t('shell.brand.subtitle') }}</p>
      </div>
      <LocaleSwitcher size="small" />
    </header>
    <main class="blank-main">
      <section class="auth-rail mm-card">
        <p class="eyebrow">{{ t('shell.auth.badge') }}</p>
        <h2>{{ t('shell.auth.title') }}</h2>
        <p class="summary">{{ t('shell.auth.subtitle') }}</p>
        <p class="products">{{ t('shell.auth.products') }}</p>
      </section>
      <slot />
    </main>
  </div>
</template>

<style scoped>
.blank-layout {
  min-height: 100vh;
  display: grid;
  padding: 20px;
  background: radial-gradient(circle at 20% 20%, #dff3f3 0%, #f9f4eb 55%, #ffffff 100%);
}

.blank-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.brand {
  display: inline-flex;
  align-items: center;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: #0b5353;
}

.brand-subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  color: #5d7277;
}

.blank-main {
  display: grid;
  grid-template-columns: minmax(260px, 360px) minmax(320px, 460px);
  justify-content: center;
  align-items: center;
  gap: 28px;
  min-height: calc(100vh - 92px);
}

.auth-rail {
  padding: 28px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(11, 83, 83, 0.08);
}

.eyebrow {
  margin: 0 0 10px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #0f6e6e;
}

h2 {
  margin: 0 0 12px;
  font-size: 28px;
  line-height: 1.18;
  color: #12363a;
}

.summary,
.products {
  margin: 0;
  color: #587474;
  line-height: 1.6;
}

.products {
  margin-top: 16px;
  font-size: 13px;
}

@media (max-width: 960px) {
  .blank-main {
    grid-template-columns: 1fr;
    min-height: auto;
    padding-top: 28px;
  }
}
</style>
