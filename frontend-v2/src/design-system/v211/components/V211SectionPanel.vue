<script setup lang="ts">
import { computed } from "vue";
import {
  NButton,
  NCard,
  NCollapse,
  NCollapseItem,
  NEmpty,
  NResult,
  NSkeleton,
  NSpace,
} from "naive-ui";
import { useLocaleText } from "@/locales";
import type { SectionAction, TextLike } from "../types";

const props = withDefaults(
  defineProps<{
    title: TextLike;
    description?: TextLike;
    actions?: readonly SectionAction[];
    loading?: boolean;
    empty?: boolean;
    error?: string | null;
    collapsible?: boolean;
    defaultCollapsed?: boolean;
  }>(),
  {
    actions: () => [],
    collapsible: false,
    defaultCollapsed: false,
    description: undefined,
    empty: false,
    error: null,
    loading: false,
  },
);

const emit = defineEmits<{
  action: [actionId: string];
}>();

const { tr } = useLocaleText();
const defaultNames = computed(() => (props.defaultCollapsed ? [] : ["content"]));
</script>

<template>
  <NCard class="v211-section-panel">
    <template #header>
      <div class="v211-section-panel__header">
        <div>
          <strong>{{ tr(title) }}</strong>
          <p v-if="description">{{ tr(description) }}</p>
        </div>
        <NSpace>
          <slot name="actions" />
          <NButton
            v-for="action in actions"
            :key="action.id"
            size="small"
            :disabled="action.disabled"
            @click="emit('action', action.id)"
          >
            {{ tr(action.label) }}
          </NButton>
        </NSpace>
      </div>
    </template>

    <NSkeleton v-if="loading" text :repeat="3" />
    <NResult v-else-if="error" status="error" :title="error">
      <template #footer>
        <slot name="error" />
      </template>
    </NResult>
    <NEmpty v-else-if="empty">
      <template #default>
        <slot name="empty" />
      </template>
    </NEmpty>
    <NCollapse v-else-if="collapsible" :default-expanded-names="defaultNames">
      <NCollapseItem name="content" :title="tr(title)">
        <slot />
      </NCollapseItem>
    </NCollapse>
    <slot v-else />
  </NCard>
</template>
