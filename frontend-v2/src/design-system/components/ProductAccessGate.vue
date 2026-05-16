<script setup lang="ts">
import EmptyState from "./EmptyState.vue";

withDefaults(
  defineProps<{
    actionLabel?: string;
    description?: string;
    enabled: boolean;
    productKey: string;
    title?: string;
  }>(),
  {
    actionLabel: "Request access",
    description: undefined,
    title: undefined,
  },
);

const emit = defineEmits<{
  requestAccess: [];
}>();
</script>

<template>
  <slot v-if="enabled" />
  <EmptyState
    v-else
    class="product-access-gate"
    :action-label="actionLabel"
    :description="description ?? `${productKey} is not enabled for this workspace.`"
    :title="title ?? `${productKey} unavailable`"
    variant="permission"
    @action="emit('requestAccess')"
  />
</template>
