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
    actionLabel: "Request access",
    description: "Your current role does not include permission for this surface.",
    title: "Permission required",
  },
);

const emit = defineEmits<{
  requestAccess: [];
}>();
</script>

<template>
  <slot v-if="allowed" />
  <EmptyState
    v-else
    class="permission-gate"
    :action-label="actionLabel"
    :description="description"
    :title="title"
    variant="permission"
    @action="emit('requestAccess')"
  />
</template>
