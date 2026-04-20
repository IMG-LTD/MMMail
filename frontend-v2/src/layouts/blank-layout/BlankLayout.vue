<script setup lang="ts">
import LocaleSwitcher from '@/shared/components/LocaleSwitcher.vue'
</script>

<template>
  <div class="blank-layout">
    <div class="blank-layout__tools">
      <locale-switcher />
    </div>
    <div class="blank-layout__content">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.blank-layout {
  position: relative;
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, var(--mm-accent-soft), transparent 28%),
    radial-gradient(circle at top right, color-mix(in srgb, var(--mm-side-surface) 82%, transparent), transparent 34%),
    linear-gradient(180deg, var(--mm-bg) 0%, var(--mm-card-muted) 100%);
}

.blank-layout__tools {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  justify-content: flex-end;
  padding: 12px 16px 0;
}

.blank-layout__content {
  min-height: 100vh;
  margin-top: -48px;
}

@media (max-width: 720px) {
  .blank-layout__tools {
    padding-inline: 12px;
  }
}
</style>
