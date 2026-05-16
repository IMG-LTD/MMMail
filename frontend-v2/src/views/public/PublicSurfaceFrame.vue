<script setup lang="ts">
import { RouterLink } from "vue-router";
import { lt, useLocaleText, type TextLike } from "@/locales";

withDefaults(
  defineProps<{
    eyebrow?: TextLike;
    title: TextLike;
    description?: TextLike;
  }>(),
  {
    description: undefined,
    eyebrow: () => lt("公开访问", "公開存取", "Public access"),
  },
);

const { tr } = useLocaleText();
</script>

<template>
  <section class="public-surface-frame">
    <header class="public-surface-frame__header">
      <RouterLink class="public-surface-frame__brand" to="/workspace">MMMail</RouterLink>
      <nav class="public-surface-frame__nav" aria-label="Public navigation">
        <RouterLink to="/boundary">{{ tr(lt("边界", "邊界", "Boundary")) }}</RouterLink>
        <RouterLink to="/login">{{ tr(lt("登录", "登入", "Login")) }}</RouterLink>
      </nav>
    </header>

    <main class="public-surface-frame__body">
      <section class="public-surface-frame__intro">
        <span class="section-label">{{ tr(eyebrow) }}</span>
        <h1>{{ tr(title) }}</h1>
        <p v-if="description">{{ tr(description) }}</p>
      </section>
      <slot />
    </main>
  </section>
</template>

<style scoped>
.public-surface-frame {
  display: grid;
  min-height: 100vh;
  grid-template-rows: auto 1fr;
  background: var(--mm-surface);
}

.public-surface-frame__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 64px;
  padding: 0 28px;
  border-bottom: 1px solid var(--mm-border);
  background: color-mix(in srgb, var(--mm-surface) 92%, white);
}

.public-surface-frame__brand {
  color: var(--mm-text-primary);
  font-weight: 800;
  text-decoration: none;
}

.public-surface-frame__nav {
  display: inline-flex;
  gap: 16px;
}

.public-surface-frame__nav a {
  color: var(--mm-text-secondary);
  font-size: 13px;
  font-weight: 700;
  text-decoration: none;
}

.public-surface-frame__body {
  width: min(1120px, calc(100% - 32px));
  margin: 0 auto;
  padding: 44px 0 56px;
}

.public-surface-frame__intro {
  display: grid;
  gap: 12px;
  margin-bottom: 22px;
}

.public-surface-frame__intro h1 {
  max-width: 780px;
  margin: 0;
  color: var(--mm-text-primary);
  font-size: 46px;
  line-height: 1;
  letter-spacing: 0;
}

.public-surface-frame__intro p {
  max-width: 680px;
  margin: 0;
  color: var(--mm-text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

@media (max-width: 640px) {
  .public-surface-frame__header {
    padding: 0 16px;
  }

  .public-surface-frame__intro h1 {
    font-size: 32px;
  }
}
</style>
