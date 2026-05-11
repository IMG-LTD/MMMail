<template>
  <div
    class="base-layout"
    :class="{
      'base-layout--context-open': shellStore.contextPanelOpen,
      'base-layout--nav-collapsed': isShellSideNavCollapsed
    }"
  >
    <shell-top-bar />
    <div class="base-layout__body">
      <shell-side-nav />
      <main class="base-layout__content" :class="{ 'base-layout__content--flush': route.meta.contentMode === 'flush' }">
        <slot />
      </main>
      <context-panel />
    </div>
    <mobile-tab-bar />
    <theme-drawer />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import ContextPanel from '@/layouts/modules/ContextPanel.vue'
import ShellSideNav from '@/layouts/modules/ShellSideNav.vue'
import ShellTopBar from '@/layouts/modules/ShellTopBar.vue'
import MobileTabBar from '@/layouts/modules/MobileTabBar.vue'
import ThemeDrawer from '@/layouts/modules/ThemeDrawer.vue'
import { isMailRoute } from '@/layouts/modules/shell-nav'
import { useShellStore } from '@/store/modules/shell'

const route = useRoute()
const shellStore = useShellStore()
const isShellSideNavCollapsed = computed(() => !isMailRoute(route.path) && shellStore.sideNavCollapsed)
</script>

<style scoped>
.base-layout {
  --base-layout-side-nav-width: 240px;
  --base-layout-context-panel-width: 320px;

  min-height: 100vh;
  background: var(--mm-bg);
}

.base-layout--nav-collapsed {
  --base-layout-side-nav-width: 76px;
}

.base-layout__body {
  display: flex;
  min-height: calc(100vh - 56px);
}

.base-layout :deep(.side-nav) {
  width: var(--base-layout-side-nav-width);
}

.base-layout--nav-collapsed :deep(.side-nav) {
  width: 76px;
}

@media (min-width: 821px) {
  .base-layout--context-open .base-layout__content {
    max-width: calc(100vw - var(--base-layout-side-nav-width) - var(--base-layout-context-panel-width));
  }
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
