<script setup lang="ts">
import { NBadge, NButton, NPopconfirm, NSpace } from "naive-ui";
import { useLocaleText } from "@/locales";
import type { BulkAction } from "../types";

const props = withDefaults(
  defineProps<{
    selectedCount: number;
    primaryAction: BulkAction;
    secondaryActions?: readonly BulkAction[];
    dangerActions?: readonly BulkAction[];
  }>(),
  {
    dangerActions: () => [],
    secondaryActions: () => [],
  },
);

const emit = defineEmits<{
  action: [actionId: string];
}>();

const { tr } = useLocaleText();
</script>

<template>
  <NSpace class="v211-action-bar" align="center">
    <NBadge :value="selectedCount" />
    <NButton
      type="primary"
      :disabled="primaryAction.disabled"
      @click="emit('action', primaryAction.id)"
    >
      {{ tr(primaryAction.label) }}
    </NButton>
    <NButton
      v-for="action in secondaryActions"
      :key="action.id"
      :disabled="action.disabled"
      @click="emit('action', action.id)"
    >
      {{ tr(action.label) }}
    </NButton>
    <NPopconfirm
      v-for="action in dangerActions"
      :key="action.id"
      @positive-click="emit('action', action.id)"
    >
      <template #trigger>
        <NButton type="error" :disabled="action.disabled">{{ tr(action.label) }}</NButton>
      </template>
      {{ tr(action.label) }}
    </NPopconfirm>
  </NSpace>
</template>
