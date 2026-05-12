<script setup lang="ts">
import { computed } from 'vue'
import type { MailDraft, MailSenderIdentity, RecipientTrustState } from './mail-types'
import MailTrustPanel from './MailTrustPanel.vue'
import { fallbackMailAttachments, formatFileSize, resolveIdentityLabel } from './mail-view-helpers'

const props = defineProps<{
  discardConfirmationOpen: boolean
  draft: MailDraft
  identities: MailSenderIdentity[]
  sendError: string
  sending: boolean
  trustCopy: string
  trustLoading: boolean
  trustState: RecipientTrustState | null
}>()

const emit = defineEmits<{
  discard: []
  retry: []
  submit: []
  'update:draft': [draft: MailDraft]
}>()

const localAttachments = computed(() => fallbackMailAttachments())

function updateDraft(patch: Partial<MailDraft>) {
  emit('update:draft', { ...props.draft, ...patch })
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
        <button class="mail-discard-trigger" type="button" @click="$emit('discard')">Discard</button>
        <button class="mail-compose-trigger mail-send-trigger" type="button" @click="$emit('submit')">
          {{ sending ? 'Sending' : 'Send' }}
        </button>
      </div>
    </header>
    <MailTrustPanel :copy="trustCopy" :loading="trustLoading" :state="trustState" />
    <div class="mail-compose-panel__fields">
      <label>
        <span class="section-label">From</span>
        <select :value="draft.fromEmail" @change="updateDraft({ fromEmail: ($event.target as HTMLSelectElement).value })">
          <option v-if="!identities.length" value="">No sender identities available</option>
          <option v-for="identity in identities" :key="identity.emailAddress" :value="identity.emailAddress">
            {{ resolveIdentityLabel(identity) }} · {{ identity.emailAddress }}
          </option>
        </select>
      </label>
      <label>
        <span class="section-label">To</span>
        <input :value="draft.toEmail" type="email" placeholder="name@example.com" @input="updateDraft({ toEmail: ($event.target as HTMLInputElement).value })">
      </label>
      <label>
        <span class="section-label">Subject</span>
        <input :value="draft.subject" type="text" placeholder="Enter a subject" @input="updateDraft({ subject: ($event.target as HTMLInputElement).value })">
      </label>
      <label class="mail-compose-panel__editor">
        <span class="section-label">Message</span>
        <textarea :value="draft.body" placeholder="Write your message" @input="updateDraft({ body: ($event.target as HTMLTextAreaElement).value })" />
      </label>
    </div>
    <div class="mail-attachment-strip">
      <article v-for="attachment in localAttachments" :key="attachment.id" class="mail-attachment-strip__item">
        <strong>{{ attachment.fileName }}</strong>
        <span>{{ formatFileSize(attachment.fileSize) }}</span>
      </article>
    </div>
    <p v-if="sendError" class="mail-send-error">{{ sendError }}</p>
    <button v-if="sendError" class="mail-send-retry" type="button" @click="$emit('retry')">Retry</button>
    <div v-if="discardConfirmationOpen" class="mail-discard-confirmation" role="dialog" aria-modal="true">
      <strong>Discard unsent draft?</strong>
      <p>This draft is still local and has not been sent.</p>
    </div>
  </section>
</template>
