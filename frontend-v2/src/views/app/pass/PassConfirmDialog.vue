<script setup lang="ts">
import type { PassConfirmAction } from './pass-types'

defineProps<{
  action: PassConfirmAction | null
  actionError: string
}>()

defineEmits<{
  cancel: []
  confirm: []
  retry: []
}>()
</script>

<template>
  <section
    v-if="action"
    class="pass-confirm-dialog"
    :class="{ 'pass-rotate-confirmation': action === 'rotate', 'pass-revoke-confirmation': action === 'revoke' }"
    role="dialog"
    aria-modal="true"
  >
    <strong>{{ action === 'rotate' ? 'Rotate this secret?' : action === 'revoke' ? 'Revoke sharing?' : 'Delete this item?' }}</strong>
    <p>This high-risk action needs explicit confirmation before local UI state changes.</p>
    <p v-if="actionError" class="pass-action-error">{{ actionError }}</p>
    <button type="button" @click="$emit('confirm')">Confirm</button>
    <button type="button" @click="$emit('cancel')">Cancel</button>
    <button v-if="actionError" class="pass-action-retry" type="button" @click="$emit('retry')">Retry</button>
  </section>
</template>
