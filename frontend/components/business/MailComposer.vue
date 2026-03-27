<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, watch } from 'vue'
import type { DraftRequest, LabelItem, MailAttachment, MailSenderIdentity, SendMailRequest } from '~/types/api'
import MailAttachmentPanel, { type FailedMailAttachmentUpload } from '~/components/business/MailAttachmentPanel.vue'
import { formatMailSenderLabel } from '~/utils/mail-identities'

interface SuggestionItem {
  value: string
}

const props = withDefaults(
  defineProps<{
    defaultTo?: string
    defaultSubject?: string
    defaultBody?: string
    draftId?: string
    defaultSenderEmail?: string
    senderOptions?: MailSenderIdentity[]
    availableLabels?: LabelItem[]
    attachments?: MailAttachment[]
    failedUploads?: FailedMailAttachmentUpload[]
    uploadLoading?: boolean
    activeAttachmentIds?: string[]
    autoSaveSeconds?: number
    fetchRecipientSuggestions?: (keyword: string, senderEmail: string) => Promise<string[]>
  }>(),
  {
    defaultTo: '',
    defaultSubject: '',
    defaultBody: '',
    draftId: '',
    defaultSenderEmail: '',
    senderOptions: () => [],
    availableLabels: () => [],
    attachments: () => [],
    failedUploads: () => [],
    uploadLoading: false,
    activeAttachmentIds: () => [],
    autoSaveSeconds: 15,
    fetchRecipientSuggestions: async () => []
  }
)

const emit = defineEmits<{
  send: [payload: SendMailRequest]
  save: [payload: DraftRequest]
  autosave: [payload: DraftRequest]
  uploadAttachments: [{ files: File[], draft: DraftRequest }]
  retryAttachment: [{ failureId: string, draft: DraftRequest }]
  removeAttachment: [{ attachmentId: string }]
  downloadAttachment: [{ attachmentId: string }]
}>()

const form = reactive({
  toEmail: props.defaultTo,
  fromEmail: props.defaultSenderEmail,
  subject: props.defaultSubject,
  body: props.defaultBody,
  labels: [] as string[],
  scheduledAt: ''
})

const senderDisabled = computed(() => props.senderOptions.length <= 1)
let draftTimer: ReturnType<typeof setTimeout> | null = null

function buildIdempotencyKey(): string {
  return `mail-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}

function syncSenderSelection(): void {
  const available = props.senderOptions.map((item) => item.emailAddress)
  if (available.length === 0) {
    form.fromEmail = props.defaultSenderEmail
    return
  }
  if (!form.fromEmail || !available.includes(form.fromEmail)) {
    form.fromEmail = props.defaultSenderEmail || available[0]
  }
}

function syncDefaultDraftFields(): void {
  form.toEmail = props.defaultTo
  form.subject = props.defaultSubject
  form.body = props.defaultBody
}

function buildDraftPayload(): DraftRequest {
  return {
    draftId: props.draftId || undefined,
    toEmail: form.toEmail,
    fromEmail: form.fromEmail || undefined,
    subject: form.subject,
    body: form.body
  }
}

function emitDraftSave(manual: boolean): void {
  if (manual) {
    emit('save', buildDraftPayload())
    return
  }
  emit('autosave', buildDraftPayload())
}

function scheduleAutoSave(): void {
  if (draftTimer) {
    clearTimeout(draftTimer)
  }
  if (!form.toEmail && !form.subject && !form.body) {
    return
  }
  draftTimer = setTimeout(() => {
    emitDraftSave(false)
  }, props.autoSaveSeconds * 1000)
}

function onSend(): void {
  emit('send', {
    draftId: props.draftId || undefined,
    toEmail: form.toEmail,
    fromEmail: form.fromEmail || undefined,
    subject: form.subject,
    body: form.body,
    labels: [...form.labels],
    idempotencyKey: buildIdempotencyKey(),
    scheduledAt: form.scheduledAt || undefined
  })
}

function onSaveDraft(): void {
  emitDraftSave(true)
}

function onUploadAttachments(payload: { files: File[] }): void {
  emit('uploadAttachments', {
    files: payload.files,
    draft: buildDraftPayload()
  })
}

function onRetryAttachment(payload: { failureId: string }): void {
  emit('retryAttachment', {
    failureId: payload.failureId,
    draft: buildDraftPayload()
  })
}

async function queryRecipientSuggestions(
  queryString: string,
  callback: (items: SuggestionItem[]) => void
): Promise<void> {
  const suggestions = await props.fetchRecipientSuggestions(queryString, form.fromEmail)
  callback(suggestions.map((email) => ({ value: email })))
}

watch(
  () => [props.defaultTo, props.defaultSubject, props.defaultBody],
  () => {
    syncDefaultDraftFields()
  },
  { immediate: true }
)

watch(
  () => [form.toEmail, form.subject, form.body, form.fromEmail],
  () => {
    scheduleAutoSave()
  }
)

watch(
  () => [props.defaultSenderEmail, props.senderOptions],
  () => syncSenderSelection(),
  { immediate: true, deep: true }
)

onBeforeUnmount(() => {
  if (draftTimer) {
    clearTimeout(draftTimer)
  }
})
</script>

<template>
  <section class="mm-card composer">
    <h2 class="mm-section-title">Compose</h2>
    <el-form label-position="top">
      <el-form-item label="From">
        <el-select v-model="form.fromEmail" style="width: 100%" :disabled="senderDisabled" placeholder="Select sender identity">
          <el-option
            v-for="identity in props.senderOptions"
            :key="identity.identityId || identity.emailAddress"
            :label="formatMailSenderLabel(identity)"
            :value="identity.emailAddress"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="To">
        <el-autocomplete
          v-model="form.toEmail"
          placeholder="recipient@example.com"
          clearable
          style="width: 100%"
          :fetch-suggestions="queryRecipientSuggestions"
        />
      </el-form-item>
      <el-form-item label="Subject">
        <el-input v-model="form.subject" placeholder="Subject" />
      </el-form-item>
      <el-form-item label="Labels">
        <el-select v-model="form.labels" multiple filterable collapse-tags placeholder="Optional labels" style="width: 100%">
          <el-option
            v-for="label in props.availableLabels"
            :key="label.id"
            :label="label.name"
            :value="label.name"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="Schedule Send (Optional)">
        <el-date-picker
          v-model="form.scheduledAt"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          placeholder="Send now if empty"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="Body">
        <el-input v-model="form.body" type="textarea" :rows="12" placeholder="Write your message" />
      </el-form-item>
      <MailAttachmentPanel
        :attachments="props.attachments"
        :failed-uploads="props.failedUploads"
        :upload-loading="props.uploadLoading"
        :active-attachment-ids="props.activeAttachmentIds"
        @upload="onUploadAttachments"
        @retry="onRetryAttachment"
        @remove="emit('removeAttachment', $event)"
        @download="emit('downloadAttachment', $event)"
      />
      <div class="actions">
        <el-button type="primary" @click="onSend">Send</el-button>
        <el-button @click="onSaveDraft">Save Draft</el-button>
      </div>
      <p class="hint">Auto-save is enabled every {{ props.autoSaveSeconds }} seconds.</p>
    </el-form>
  </section>
</template>

<style scoped>
.composer {
  padding: 20px;
}

.actions {
  display: flex;
  gap: 8px;
}

.hint {
  margin-top: 10px;
  color: var(--mm-muted);
  font-size: 12px;
}
</style>
