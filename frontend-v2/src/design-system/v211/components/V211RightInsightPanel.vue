<script setup lang="ts">
import { computed } from "vue";
import { NButton, NCard, NEmpty, NResult, NScrollbar, NSkeleton, NTabPane, NTabs } from "naive-ui";
import { useLocaleText } from "@/locales";
import type { InsightTab, ModuleId } from "../types";

const props = withDefaults(
  defineProps<{
    module: ModuleId;
    tabs: readonly InsightTab[];
    activeTab?: string;
    loading?: boolean;
    error?: string | null;
    collapsed?: boolean;
    width?: number;
  }>(),
  {
    activeTab: undefined,
    collapsed: false,
    error: null,
    loading: false,
    width: 340,
  },
);

const emit = defineEmits<{
  "update:activeTab": [id: string];
  toggle: [];
}>();

const { tr } = useLocaleText();
const active = computed(() => props.activeTab ?? props.tabs[0]?.id);
</script>

<template>
  <NCard
    class="v211-right-insight-panel"
    :style="{ width: `${width}px` }"
    :data-module="module"
    role="complementary"
  >
    <template #header>
      <NButton
        text
        :aria-expanded="!collapsed"
        :aria-controls="`v211-panel-${module}`"
        @click="emit('toggle')"
      >
        {{ collapsed ? "Open" : "Close" }}
      </NButton>
    </template>

    <NScrollbar v-if="!collapsed" :id="`v211-panel-${module}`">
      <NSkeleton v-if="loading" text :repeat="4" />
      <NResult v-else-if="error" status="error" :title="error">
        <template #footer>
          <slot name="error" />
        </template>
      </NResult>
      <NEmpty v-else-if="!tabs.length">
        <template #default>
          <slot name="empty" />
        </template>
      </NEmpty>
      <NTabs
        v-else
        :value="active"
        @update:value="(value) => emit('update:activeTab', String(value))"
      >
        <NTabPane
          v-for="tab in tabs"
          :key="tab.id"
          :name="tab.id"
          :tab="tr(tab.label)"
          :disabled="tab.disabled"
        >
          <slot :name="tab.id" />
        </NTabPane>
      </NTabs>
    </NScrollbar>
  </NCard>
</template>
