<template>
  <V211AppShell
    v-if="isV211ShellActive"
    class="base-layout"
    :class="{
      'base-layout--context-open': shellStore.contextPanelOpen,
      'base-layout--nav-collapsed': isShellSideNavCollapsed,
    }"
    :current-module="currentModule"
    :nav-collapsed="isShellSideNavCollapsed"
    :right-panel-collapsed="isRightPanelCollapsed"
    :right-panel-width="CONTEXT_PANEL_WIDTH"
    :side-nav-collapsed-width="SIDE_NAV_COLLAPSED_WIDTH"
    :side-nav-width="SIDE_NAV_WIDTH"
    @navigate="navigateModule"
  >
    <template #topBar>
      <shell-top-bar />
    </template>

    <template #sideNav>
      <shell-side-nav />
    </template>

    <main
      class="base-layout__content"
      :class="{ 'base-layout__content--flush': route.meta.contentMode === 'flush' }"
    >
      <slot />
    </main>

    <template #rightPanel>
      <context-panel />
    </template>
  </V211AppShell>
  <div
    v-else
    class="base-layout"
    :class="{
      'base-layout--context-open': shellStore.contextPanelOpen,
      'base-layout--nav-collapsed': isShellSideNavCollapsed,
    }"
  >
    <shell-top-bar />
    <div class="base-layout__body">
      <shell-side-nav />
      <main
        class="base-layout__content"
        :class="{ 'base-layout__content--flush': route.meta.contentMode === 'flush' }"
      >
        <slot />
      </main>
      <context-panel />
    </div>
  </div>
  <div class="base-layout__overlays">
    <mobile-tab-bar />
    <shell-command-palette />
    <shell-quick-create-modal />
    <theme-drawer />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { V211AppShell, type ModuleId } from "@/design-system/v211";
import { isV211ShellEnabled } from "@/app/feature-flags";
import ContextPanel from "@/layouts/modules/ContextPanel.vue";
import ShellSideNav from "@/layouts/modules/ShellSideNav.vue";
import ShellTopBar from "@/layouts/modules/ShellTopBar.vue";
import ShellCommandPalette from "@/layouts/modules/ShellCommandPalette.vue";
import ShellQuickCreateModal from "@/layouts/modules/ShellQuickCreateModal.vue";
import MobileTabBar from "@/layouts/modules/MobileTabBar.vue";
import ThemeDrawer from "@/layouts/modules/ThemeDrawer.vue";
import { isMailRoute } from "@/layouts/modules/shell-nav";
import { useShellStore } from "@/store/modules/shell";

const CONTEXT_PANEL_WIDTH = 320;
const SIDE_NAV_COLLAPSED_WIDTH = 64;
const SIDE_NAV_WIDTH = 240;

const moduleRouteTargets: Readonly<Record<ModuleId, string>> = {
  admin: "/admin",
  calendar: "/calendar",
  collaboration: "/collaboration",
  "command-center": "/command-center",
  docs: "/docs",
  drive: "/drive",
  home: "/suite",
  mail: "/inbox",
  notifications: "/notifications",
  pass: "/pass",
  settings: "/settings",
  sheets: "/sheets",
};

const moduleRoutePrefixes: readonly [ModuleId, readonly string[]][] = [
  [
    "mail",
    [
      "/mail",
      "/inbox",
      "/archive",
      "/compose",
      "/contacts",
      "/drafts",
      "/sent",
      "/starred",
      "/trash",
    ],
  ],
  ["calendar", ["/calendar"]],
  ["drive", ["/drive"]],
  ["docs", ["/docs"]],
  ["sheets", ["/sheets"]],
  ["pass", ["/pass"]],
  ["collaboration", ["/collaboration"]],
  ["command-center", ["/command-center"]],
  ["notifications", ["/notifications"]],
  ["admin", ["/admin", "/organizations", "/business", "/security"]],
  ["settings", ["/settings"]],
];

const route = useRoute();
const router = useRouter();
const shellStore = useShellStore();
const initialSlimNavApplied = ref(false);
const isShellSideNavCollapsed = computed(
  () => !isMailRoute(route.path) && shellStore.sideNavCollapsed,
);
const isRightPanelCollapsed = computed(
  () => !shellStore.contextPanelOpen && !shellStore.mobileMorePanelOpen,
);
const isV211ShellActive = computed(() => isV211ShellEnabled());
const currentModule = computed(() => resolveCurrentModule(route.path));

watch(() => route.path, applyInitialSlimNav, { immediate: true });

onMounted(() => {
  window.addEventListener("keydown", handleCommandPaletteShortcut);
});

onBeforeUnmount(() => {
  window.removeEventListener("keydown", handleCommandPaletteShortcut);
});

function navigateModule(module: ModuleId) {
  const target = moduleRouteTargets[module];

  if (route.path !== target) {
    router.push(target);
  }
}

function applyInitialSlimNav(path: string) {
  if (initialSlimNavApplied.value || !isWorkspaceShellRoute(path)) {
    return;
  }

  shellStore.setSideNavCollapsed(true);
  initialSlimNavApplied.value = true;
}

function handleCommandPaletteShortcut(event: KeyboardEvent) {
  const isPaletteShortcut = (event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "k";

  if (!isPaletteShortcut) {
    return;
  }

  event.preventDefault();
  shellStore.openCommandPalette();
}

function isWorkspaceShellRoute(path: string) {
  return path === "/suite" || path === "/workspace" || path.startsWith("/workspace/");
}

function resolveCurrentModule(path: string): ModuleId {
  const entry = moduleRoutePrefixes.find(([, prefixes]) =>
    prefixes.some((prefix) => path.startsWith(prefix)),
  );
  return entry?.[0] ?? "home";
}
</script>

<style scoped>
.base-layout {
  --base-layout-side-nav-width: 240px;
  --base-layout-context-panel-width: 320px;

  min-height: 100vh;
  background: var(--mm-bg);
}

.base-layout--nav-collapsed {
  --base-layout-side-nav-width: 64px;
}

.base-layout :deep(.side-nav) {
  width: 100%;
  min-height: 100vh;
}

.base-layout--nav-collapsed :deep(.side-nav) {
  width: 64px;
}

.base-layout__body {
  display: flex;
  min-height: calc(100vh - 56px);
}

@media (min-width: 821px) {
  .base-layout--context-open .base-layout__content {
    max-width: calc(
      100vw - var(--base-layout-side-nav-width) - var(--base-layout-context-panel-width)
    );
  }
}

.base-layout :deep(.context-panel) {
  box-sizing: border-box;
  width: 100%;
  min-height: 100vh;
}

.base-layout__content {
  flex: 1;
  min-width: 0;
  box-sizing: border-box;
  width: calc(100vw - var(--base-layout-side-nav-width));
  min-height: calc(100vh - 56px);
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
