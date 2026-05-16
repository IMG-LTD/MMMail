<script setup lang="ts">
import { computed } from "vue";
import {
  NBreadcrumb,
  NBreadcrumbItem,
  NButton,
  NPageHeader,
  NTabPane,
  NTabs,
  NSpace,
} from "naive-ui";
import { useLocaleText } from "@/locales";
import type { BreadcrumbItem, ModuleToolbarAction, ModuleToolbarTab, TextLike } from "../types";

const props = withDefaults(
  defineProps<{
    title: TextLike;
    breadcrumbs?: readonly BreadcrumbItem[];
    tabs?: readonly ModuleToolbarTab[];
    actions?: readonly ModuleToolbarAction[];
    density?: "compact" | "comfortable";
    activeTab?: string;
  }>(),
  {
    actions: () => [],
    activeTab: undefined,
    breadcrumbs: () => [],
    density: "comfortable",
    tabs: () => [],
  },
);

const emit = defineEmits<{
  action: [actionId: string];
  tab: [tabId: string];
}>();

const { tr } = useLocaleText();
const currentTab = computed(() => props.activeTab ?? props.tabs[0]?.id);
</script>

<template>
  <NPageHeader class="v211-module-toolbar" :class="`v211-module-toolbar--${density}`">
    <template #title>{{ tr(title) }}</template>
    <template v-if="breadcrumbs.length" #subtitle>
      <NBreadcrumb>
        <NBreadcrumbItem v-for="item in breadcrumbs" :key="tr(item.label)">
          {{ tr(item.label) }}
        </NBreadcrumbItem>
      </NBreadcrumb>
    </template>
    <template #extra>
      <NSpace>
        <slot name="actions" />
        <NButton
          v-for="action in actions"
          :key="action.id"
          :disabled="action.disabled"
          :type="action.tone === 'brand' ? 'primary' : 'default'"
          @click="emit('action', action.id)"
        >
          {{ tr(action.label) }}
        </NButton>
      </NSpace>
    </template>
    <NTabs
      v-if="tabs.length"
      :value="currentTab"
      @update:value="(value) => emit('tab', String(value))"
    >
      <NTabPane
        v-for="tab in tabs"
        :key="tab.id"
        :name="tab.id"
        :tab="tr(tab.label)"
        :disabled="tab.disabled"
      />
    </NTabs>
    <slot />
  </NPageHeader>
</template>
