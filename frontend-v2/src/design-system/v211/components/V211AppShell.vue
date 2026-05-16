<script setup lang="ts">
import { computed, defineAsyncComponent } from "vue";
import { NLayout, NLayoutContent, NLayoutHeader, NLayoutSider } from "naive-ui";
import { useLocaleText, lt } from "@/locales";
import { v211Branding } from "../branding";
import { V211_MODULE_IDS, type ModuleId } from "../types";

const props = withDefaults(
  defineProps<{
    currentModule: ModuleId;
    navCollapsed?: boolean;
    rightPanelCollapsed?: boolean;
    rightPanelWidth?: number;
    sideNavCollapsedWidth?: number;
    sideNavWidth?: number;
  }>(),
  {
    navCollapsed: false,
    rightPanelCollapsed: false,
    rightPanelWidth: 340,
    sideNavCollapsedWidth: 56,
    sideNavWidth: 64,
  },
);

const emit = defineEmits<{
  navigate: [module: ModuleId];
}>();

const { tr } = useLocaleText();
const NAvatar = defineAsyncComponent(() =>
  import("naive-ui/es/avatar").then((module) => module.NAvatar),
);
const NBadge = defineAsyncComponent(() =>
  import("naive-ui/es/badge").then((module) => module.NBadge),
);
const NDropdown = defineAsyncComponent(() =>
  import("naive-ui/es/dropdown").then((module) => module.NDropdown),
);
const NMenu = defineAsyncComponent(() => import("naive-ui/es/menu").then((module) => module.NMenu));

const navOptions = computed(() => {
  return V211_MODULE_IDS.map((module) => ({
    key: module,
    label: module,
  }));
});

const accountOptions = computed(() => [
  { key: "settings", label: tr(lt("设置", "設定", "Settings")) },
  { key: "logout", label: tr(lt("退出", "登出", "Sign out")) },
]);
</script>

<template>
  <NLayout class="v211-app-shell" has-sider>
    <NLayoutSider
      class="v211-app-shell__sider"
      collapse-mode="width"
      :collapsed="navCollapsed"
      :collapsed-width="sideNavCollapsedWidth"
      :width="sideNavWidth"
    >
      <slot name="sideNav">
        <div class="v211-app-shell__brand" :aria-label="v211Branding.productName">
          {{ v211Branding.logoText }}
        </div>
        <NMenu
          :collapsed="navCollapsed"
          :options="navOptions"
          :value="currentModule"
          @update:value="(value) => emit('navigate', value as ModuleId)"
        />
      </slot>
    </NLayoutSider>

    <NLayout class="v211-app-shell__main">
      <NLayoutHeader class="v211-app-shell__header">
        <slot name="topBar">
          <slot name="topBarLeft" />
          <NDropdown :options="accountOptions">
            <NBadge dot>
              <NAvatar round>MM</NAvatar>
            </NBadge>
          </NDropdown>
          <slot name="topBarRight" />
        </slot>
      </NLayoutHeader>
      <NLayoutContent class="v211-app-shell__content">
        <slot />
      </NLayoutContent>
    </NLayout>

    <NLayoutSider
      v-if="!rightPanelCollapsed"
      class="v211-app-shell__right"
      :native-scrollbar="false"
      :width="rightPanelWidth"
    >
      <slot name="rightPanel" />
    </NLayoutSider>
  </NLayout>
</template>

<style scoped>
.v211-app-shell {
  min-height: 100vh;
  background: var(--mm-bg);
}

.v211-app-shell__sider,
.v211-app-shell__right {
  background: var(--mm-side-surface);
}

.v211-app-shell__brand {
  display: grid;
  place-items: center;
  height: 56px;
  color: var(--mm-brand-primary);
  font-weight: 800;
}

.v211-app-shell__header {
  min-height: 56px;
  background: var(--mm-topbar);
  border-bottom: 1px solid var(--mm-border);
}

.v211-app-shell__main {
  flex: 1 1 auto;
  min-width: 0;
}

.v211-app-shell__content {
  min-width: 0;
  width: 100%;
  background: var(--mm-bg);
}

.v211-app-shell__right {
  border-left: 1px solid var(--mm-border);
}
</style>
