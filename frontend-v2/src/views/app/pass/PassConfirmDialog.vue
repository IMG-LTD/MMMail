<script setup lang="ts">
import { NButton } from "naive-ui";
import type { PassConfirmAction } from "./pass-types";

defineProps<{
  action: PassConfirmAction | null;
  actionError: string;
}>();

defineEmits<{
  cancel: [];
  confirm: [];
  retry: [];
}>();
</script>

<template>
  <section
    v-if="action"
    class="pass-confirm-dialog"
    :class="{
      'pass-rotate-confirmation': action === 'rotate',
      'pass-revoke-confirmation': action === 'revoke',
    }"
    role="dialog"
    aria-modal="true"
  >
    <strong>{{
      action === "rotate"
        ? "Rotate this secret?"
        : action === "revoke"
          ? "Revoke sharing?"
          : "Delete this item?"
    }}</strong>
    <p>This high-risk action needs explicit confirmation before local UI state changes.</p>
    <p v-if="actionError" class="pass-action-error">{{ actionError }}</p>
    <NButton native-type="button" @click="$emit('confirm')">Confirm</NButton>
    <NButton native-type="button" @click="$emit('cancel')">Cancel</NButton>
    <NButton
      v-if="actionError"
      class="pass-action-retry"
      native-type="button"
      @click="$emit('retry')"
      >Retry</NButton
    >
  </section>
</template>
