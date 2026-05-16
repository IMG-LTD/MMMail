<script setup lang="ts">
import { ref } from "vue";
import { NBadge, NButton, NDropdown, NInput, NSpace } from "naive-ui";
import { useLocaleText } from "@/locales";
import type { BulkAction, FilterConfig, TextLike } from "../types";

const props = withDefaults(
  defineProps<{
    searchPlaceholder?: TextLike;
    filters?: readonly FilterConfig[];
    createLabel?: TextLike;
    createAction?: () => void;
    bulkActions?: readonly BulkAction[];
    selectedCount?: number;
  }>(),
  {
    bulkActions: () => [],
    createAction: undefined,
    createLabel: undefined,
    filters: () => [],
    searchPlaceholder: undefined,
    selectedCount: 0,
  },
);

const emit = defineEmits<{
  search: [keyword: string];
  filter: [filters: Record<string, unknown>];
  create: [];
  bulkAction: [actionId: string];
}>();

const { tr } = useLocaleText();
const keyword = ref("");

function updateKeyword(value: string) {
  keyword.value = value;
  emit("search", value);
}

function triggerCreate() {
  props.createAction?.();
  emit("create");
}
</script>

<template>
  <NSpace class="v211-data-toolbar" align="center" justify="space-between">
    <NInput
      :value="keyword"
      clearable
      :placeholder="searchPlaceholder ? tr(searchPlaceholder) : 'Search'"
      @update:value="updateKeyword"
    />
    <NSpace align="center">
      <NDropdown
        v-for="filter in filters"
        :key="filter.id"
        :options="
          filter.options.map((item) => ({ label: tr(item.label), key: String(item.value) }))
        "
        @select="(value) => emit('filter', { [filter.id]: value })"
      >
        <NButton secondary>{{ tr(filter.label) }}</NButton>
      </NDropdown>
      <NBadge v-if="selectedCount" :value="selectedCount">
        <NDropdown
          :options="
            bulkActions.map((item) => ({
              label: tr(item.label),
              key: item.id,
              disabled: item.disabled,
            }))
          "
          @select="(value) => emit('bulkAction', String(value))"
        >
          <NButton secondary>Bulk</NButton>
        </NDropdown>
      </NBadge>
      <NButton v-if="createLabel" type="primary" @click="triggerCreate">
        {{ tr(createLabel) }}
      </NButton>
    </NSpace>
  </NSpace>
</template>
