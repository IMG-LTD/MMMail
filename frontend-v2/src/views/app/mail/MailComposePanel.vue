<script setup lang="ts">
import { NButton, NInput, NSelect } from "naive-ui";
import { computed } from "vue";
import type { MailDraft, MailSenderIdentity, RecipientTrustState } from "./mail-types";
import MailTrustPanel from "./MailTrustPanel.vue";
import { fallbackMailAttachments, formatFileSize, resolveIdentityLabel } from "./mail-view-helpers";

const props = defineProps<{
  discardConfirmationOpen: boolean;
  draft: MailDraft;
  identities: MailSenderIdentity[];
  sendError: string;
  sending: boolean;
  trustCopy: string;
  trustLoading: boolean;
  trustState: RecipientTrustState | null;
}>();

const emit = defineEmits<{
  discard: [];
  retry: [];
  submit: [];
  "update:draft": [draft: MailDraft];
}>();

const localAttachments = computed(() => fallbackMailAttachments());
const identityOptions = computed(() => {
  if (!props.identities.length) {
    return [{ label: "No sender identities available", value: "" }];
  }

  return props.identities.map((identity) => ({
    label: `${resolveIdentityLabel(identity)} · ${identity.emailAddress}`,
    value: identity.emailAddress,
  }));
});

function updateDraft(patch: Partial<MailDraft>) {
  emit("update:draft", { ...props.draft, ...patch });
}
</script>

<template>
  <section class="mail-compose-panel">
    <header class="mail-compose-panel__head">
      <div>
        <span class="section-label">Compose</span>
        <h1>Send mail with authenticated identities</h1>
      </div>
      <div class="mail-compose-panel__actions">
        <NButton class="mail-discard-trigger" native-type="button" @click="$emit('discard')"
          >Discard</NButton
        >
        <NButton
          class="mail-compose-trigger mail-send-trigger"
          native-type="button"
          @click="$emit('submit')"
        >
          {{ sending ? "Sending" : "Send" }}
        </NButton>
      </div>
    </header>
    <MailTrustPanel :copy="trustCopy" :loading="trustLoading" :state="trustState" />
    <div class="mail-compose-panel__fields">
      <label>
        <span class="section-label">From</span>
        <NSelect
          :options="identityOptions"
          :value="draft.fromEmail"
          @update:value="(value) => updateDraft({ fromEmail: String(value) })"
        />
      </label>
      <label>
        <span class="section-label">To</span>
        <NInput
          :value="draft.toEmail"
          placeholder="name@example.com"
          @update:value="(value) => updateDraft({ toEmail: value })"
        />
      </label>
      <label>
        <span class="section-label">Subject</span>
        <NInput
          :value="draft.subject"
          placeholder="Enter a subject"
          @update:value="(value) => updateDraft({ subject: value })"
        />
      </label>
      <label class="mail-compose-panel__editor">
        <span class="section-label">Message</span>
        <NInput
          :value="draft.body"
          placeholder="Write your message"
          type="textarea"
          @update:value="(value) => updateDraft({ body: value })"
        />
      </label>
    </div>
    <div class="mail-attachment-strip">
      <article
        v-for="attachment in localAttachments"
        :key="attachment.id"
        class="mail-attachment-strip__item"
      >
        <strong>{{ attachment.fileName }}</strong>
        <span>{{ formatFileSize(attachment.fileSize) }}</span>
      </article>
    </div>
    <p v-if="sendError" class="mail-send-error">{{ sendError }}</p>
    <NButton v-if="sendError" class="mail-send-retry" native-type="button" @click="$emit('retry')"
      >Retry</NButton
    >
    <div
      v-if="discardConfirmationOpen"
      class="mail-discard-confirmation"
      role="dialog"
      aria-modal="true"
    >
      <strong>Discard unsent draft?</strong>
      <p>This draft is still local and has not been sent.</p>
    </div>
  </section>
</template>
