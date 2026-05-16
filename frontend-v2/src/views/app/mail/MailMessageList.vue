<script setup lang="ts">
import { NButton } from "naive-ui";
import type { MailSummary } from "./mail-types";
import { formatMailTimestamp, resolveMailSender } from "./mail-view-helpers";

defineProps<{
  emptyCopy: string;
  loading: boolean;
  messages: MailSummary[];
}>();

defineEmits<{
  open: [mailId: string];
}>();
</script>

<template>
  <section class="mail-message-list">
    <header class="mail-message-list__head">
      <strong>All mail</strong>
      <span>{{ messages.length }} conversations</span>
    </header>
    <NButton
      v-for="message in messages"
      :key="message.id"
      class="mail-message-list__row"
      native-type="button"
      @click="$emit('open', message.id)"
    >
      <div>
        <div class="mail-message-list__row-head">
          <strong>{{ resolveMailSender(message) }}</strong>
          <span>{{ formatMailTimestamp(message.sentAt) }}</span>
        </div>
        <p>{{ message.subject || "No subject" }}</p>
        <small>{{ message.preview || "No preview available" }}</small>
      </div>
      <span class="mail-message-list__badge">{{ message.unread ? "Unread" : "Read" }}</span>
    </NButton>
    <div v-if="loading" class="mail-message-list__empty">Loading mail list.</div>
    <div v-else-if="!messages.length" class="mail-message-list__empty">{{ emptyCopy }}</div>
  </section>
</template>
