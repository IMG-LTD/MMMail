<script setup lang="ts">
import { NButton } from "naive-ui";
import type { MailDetail } from "./mail-types";
import {
  fallbackMailAttachments,
  formatFileSize,
  formatMailTimestamp,
  resolveMailSender,
} from "./mail-view-helpers";

const props = defineProps<{
  activeMail: MailDetail | null;
  loading: boolean;
  showBack: boolean;
  trustCopy: string;
}>();

defineEmits<{
  back: [];
}>();
</script>

<template>
  <article class="mail-thread-reader surface-card">
    <div class="mail-thread-reader__head">
      <div>
        <span class="section-label">Conversation</span>
        <h1>{{ activeMail?.subject || "Select a message" }}</h1>
      </div>
      <NButton v-if="showBack" native-type="button" @click="$emit('back')">Back to inbox</NButton>
    </div>
    <div class="mail-thread-reader__trust">{{ trustCopy }}</div>
    <div v-if="activeMail" class="mail-thread-reader__meta">
      <span class="section-label">Sender</span>
      <strong>{{ resolveMailSender(activeMail) }}</strong>
      <span>{{ formatMailTimestamp(activeMail.sentAt) }}</span>
    </div>
    <div v-if="loading" class="mail-thread-reader__empty">Loading mail detail.</div>
    <div v-else-if="activeMail" class="mail-thread-reader__body">
      <p
        v-for="(paragraph, index) in activeMail.body.split(/\n+/).filter(Boolean)"
        :key="`${activeMail.id}-${index}`"
      >
        {{ paragraph }}
      </p>
      <p v-if="!activeMail.body.trim()">This message has no body content.</p>
    </div>
    <div v-else class="mail-thread-reader__empty">
      Choose a message from the list to inspect its detail.
    </div>
    <div class="mail-attachment-strip">
      <article
        v-for="attachment in activeMail?.attachments.length
          ? activeMail.attachments
          : fallbackMailAttachments()"
        :key="attachment.id"
        class="mail-attachment-strip__item"
      >
        <strong>{{ attachment.fileName }}</strong>
        <span>{{ formatFileSize(attachment.fileSize) }}</span>
      </article>
    </div>
  </article>
</template>
