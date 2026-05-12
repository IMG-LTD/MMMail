<script setup lang="ts">
defineProps<{
  actionError: string
  open: boolean
}>()

defineEmits<{
  close: []
  retry: []
  save: []
}>()
</script>

<template>
  <section v-if="open" class="pass-share-settings-modal" role="dialog" aria-modal="true">
    <div class="pass-share-settings-modal__head">
      <strong>Share settings</strong>
      <button type="button" @click="$emit('close')">Close</button>
    </div>
    <label>
      <span>Recipients</span>
      <input aria-label="Share recipients" value="lihua@acme.com, wanglei@acme.com">
    </label>
    <label>
      <span>Permission</span>
      <select aria-label="Share permission"><option>Can view</option><option>Can edit</option></select>
    </label>
    <label>
      <span>Expiration</span>
      <input aria-label="Expiration date" value="2026-07-31">
    </label>
    <p v-if="actionError" class="pass-action-error">{{ actionError }}</p>
    <button class="pass-share-settings-modal__save" type="button" @click="$emit('save')">Save share</button>
    <button v-if="actionError" class="pass-action-retry" type="button" @click="$emit('retry')">Retry</button>
  </section>
</template>
