<template>
  <div class="base-layout">
    <shell-top-bar />
    <div class="base-layout__body">
      <shell-side-nav />
      <main class="base-layout__content" :class="{ 'base-layout__content--flush': route.meta.contentMode === 'flush' }">
        <slot />
      </main>
    </div>
    <mobile-tab-bar />
    <theme-drawer />
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import ShellSideNav from '@/layouts/modules/ShellSideNav.vue'
import ShellTopBar from '@/layouts/modules/ShellTopBar.vue'
import MobileTabBar from '@/layouts/modules/MobileTabBar.vue'
import ThemeDrawer from '@/layouts/modules/ThemeDrawer.vue'

const route = useRoute()
</script>

<style scoped>
.base-layout {
  min-height: 100vh;
  background: var(--mm-bg);
}

.base-layout__body {
  display: flex;
  min-height: calc(100vh - 56px);
}

.base-layout__content {
  flex: 1;
  min-width: 0;
  padding: 24px;
  overflow: auto;
}

.base-layout__content--flush {
  padding: 0;
}

@media (max-width: 1024px) {
  .base-layout__content {
    padding: 16px;
  }

  .base-layout__content--flush {
    padding: 0;
  }
}

@media (max-width: 820px) {
  .base-layout__body {
    display: block;
  }

  .base-layout__content {
    padding-bottom: 88px;
  }

  .base-layout__content--flush {
    padding: 0 0 88px;
  }
}
</style>
