<script setup lang="ts">
import EmptyState from "./EmptyState.vue";

withDefaults(
  defineProps<{
    actionLabel?: string;
    allowed: boolean;
    description?: string;
    title?: string;
  }>(),
  {
    actionLabel: "Upgrade",
    description: "This surface is available on premium plans.",
    title: "Premium access required",
  },
);

const emit = defineEmits<{
  upgrade: [];
}>();
</script>

<template>
  <slot v-if="allowed" />
  <EmptyState
    v-else
    class="premium-gate"
    :action-label="actionLabel"
    :description="description"
    :title="title"
    variant="premium"
    @action="emit('upgrade')"
  />
</template>
