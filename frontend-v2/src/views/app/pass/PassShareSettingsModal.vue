<script setup lang="ts">
import { NButton, NInput, NSelect } from "naive-ui";
defineProps<{
  actionError: string;
  open: boolean;
}>();

defineEmits<{
  close: [];
  retry: [];
  save: [];
}>();

const permissionOptions = [
  { label: "Can view", value: "view" },
  { label: "Can edit", value: "edit" },
];
</script>

<template>
  <section v-if="open" class="pass-share-settings-modal" role="dialog" aria-modal="true">
    <div class="pass-share-settings-modal__head">
      <strong>Share settings</strong>
      <NButton native-type="button" @click="$emit('close')">Close</NButton>
    </div>
    <label>
      <span>Recipients</span>
      <NInput aria-label="Share recipients" value="lihua@acme.com, wanglei@acme.com" />
    </label>
    <label>
      <span>Permission</span>
      <NSelect aria-label="Share permission" value="view" :options="permissionOptions" />
    </label>
    <label>
      <span>Expiration</span>
      <NInput aria-label="Expiration date" value="2026-07-31" />
    </label>
    <p v-if="actionError" class="pass-action-error">{{ actionError }}</p>
    <NButton class="pass-share-settings-modal__save" native-type="button" @click="$emit('save')"
      >Save share</NButton
    >
    <NButton
      v-if="actionError"
      class="pass-action-retry"
      native-type="button"
      @click="$emit('retry')"
      >Retry</NButton
    >
  </section>
</template>
