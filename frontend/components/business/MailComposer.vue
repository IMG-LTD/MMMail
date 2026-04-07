<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, watch } from 'vue'
import type {
  DraftRequest,
  LabelItem,
  MailAttachment,
  MailComposeSubmitRequest,
  MailE2eeRecipientStatus,
  MailSenderIdentity,
} from '~/types/api'
import MailAttachmentPanel, { type FailedMailAttachmentUpload } from '~/components/business/MailAttachmentPanel.vue'
import { useI18n } from '~/composables/useI18n'
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
    recipientE2eeStatus?: MailE2eeRecipientStatus | null
    recipientE2eeLoading?: boolean
    recipientE2eeError?: string
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
    recipientE2eeStatus: null,
    recipientE2eeLoading: false,
    recipientE2eeError: '',
    fetchRecipientSuggestions: async () => []
  }
)

const emit = defineEmits<{
  send: [payload: MailComposeSubmitRequest]
  save: [payload: DraftRequest]
  autosave: [payload: DraftRequest]
  uploadAttachments: [{ files: File[], draft: DraftRequest }]
  retryAttachment: [{ failureId: string, draft: DraftRequest }]
  removeAttachment: [{ attachmentId: string }]
  downloadAttachment: [{ attachmentId: string }]
  routeContextChange: [payload: { toEmail: string, fromEmail: string }]
}>()

const form = reactive({
  toEmail: props.defaultTo,
  fromEmail: props.defaultSenderEmail,
  subject: props.defaultSubject,
  body: props.defaultBody,
  labels: [] as string[],
  scheduledAt: ''
})
const externalSecureDelivery = reactive({
  enabled: false,
  password: '',
  passwordHint: '',
  expiresAt: ''
})

const senderDisabled = computed(() => props.senderOptions.length <= 1)
const { t } = useI18n()
let draftTimer: ReturnType<typeof setTimeout> | null = null
const supportsExternalSecureDelivery = computed(() => {
  if (!props.recipientE2eeStatus?.deliverable || props.recipientE2eeStatus.routes.length === 0) {
    return false
  }
  return props.recipientE2eeStatus.routes.every(route => route.smtpOutbound === true)
})

const recipientE2eeAlertType = computed(() => {
  if (props.recipientE2eeError) {
    return 'error'
  }
  if (supportsExternalSecureDelivery.value) {
    return 'info'
  }
  switch (props.recipientE2eeStatus?.readiness) {
    case 'READY':
      return 'success'
    case 'NOT_READY':
      return 'warning'
    case 'UNDELIVERABLE':
      return 'error'
    default:
      return 'info'
  }
})

const recipientE2eeAlertTitle = computed(() => {
  if (props.recipientE2eeError) {
    return props.recipientE2eeError
  }
  if (supportsExternalSecureDelivery.value) {
    return t('mailCompose.externalSecure.title')
  }
  switch (props.recipientE2eeStatus?.readiness) {
    case 'READY':
      return t('mailCompose.e2ee.readyTitle')
    case 'NOT_READY':
      return t('mailCompose.e2ee.notReadyTitle')
    case 'UNDELIVERABLE':
      return t('mailCompose.e2ee.undeliverableTitle')
    default:
      return ''
  }
})

const recipientE2eeAlertDescription = computed(() => {
  if (props.recipientE2eeError) {
    return t('mailCompose.e2ee.boundaryNote')
  }
  if (supportsExternalSecureDelivery.value) {
    return t('mailCompose.externalSecure.description')
  }
  switch (props.recipientE2eeStatus?.readiness) {
    case 'READY':
      return t('mailCompose.e2ee.readyDescription', { count: props.recipientE2eeStatus.routeCount })
    case 'NOT_READY':
      return t('mailCompose.e2ee.notReadyDescription', { count: props.recipientE2eeStatus.routeCount })
    case 'UNDELIVERABLE':
      return t('mailCompose.e2ee.undeliverableDescription')
    default:
      return ''
  }
})

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
    scheduledAt: form.scheduledAt || undefined,
    externalSecureDelivery: externalSecureDelivery.enabled
      ? {
          enabled: true,
          password: externalSecureDelivery.password,
          passwordHint: externalSecureDelivery.passwordHint || undefined,
          expiresAt: externalSecureDelivery.expiresAt || undefined
        }
      : undefined
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
  () => [form.toEmail, form.fromEmail],
  () => {
    emit('routeContextChange', {
      toEmail: form.toEmail,
      fromEmail: form.fromEmail
    })
  },
  { immediate: true }
)

watch(
  () => [props.defaultSenderEmail, props.senderOptions],
  () => syncSenderSelection(),
  { immediate: true, deep: true }
)

watch(
  supportsExternalSecureDelivery,
  (supported) => {
    if (supported) {
      if (!externalSecureDelivery.expiresAt) {
        externalSecureDelivery.expiresAt = buildDefaultSecureExpiryValue()
      }
      return
    }
    resetExternalSecureDelivery()
  },
  { immediate: true }
)

watch(
  () => externalSecureDelivery.enabled,
  (enabled) => {
    if (enabled && !externalSecureDelivery.expiresAt) {
      externalSecureDelivery.expiresAt = buildDefaultSecureExpiryValue()
    }
  }
)

onBeforeUnmount(() => {
  if (draftTimer) {
    clearTimeout(draftTimer)
  }
})

function resetExternalSecureDelivery(): void {
  externalSecureDelivery.enabled = false
  externalSecureDelivery.password = ''
  externalSecureDelivery.passwordHint = ''
  externalSecureDelivery.expiresAt = ''
}

function buildDefaultSecureExpiryValue(reference = new Date()): string {
  const target = new Date(reference.getTime() + 7 * 24 * 60 * 60 * 1000)
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${target.getFullYear()}-${pad(target.getMonth() + 1)}-${pad(target.getDate())}T${pad(target.getHours())}:${pad(target.getMinutes())}:${pad(target.getSeconds())}`
}
</script>

<template>
  <section class="mm-card composer">
    <h2 class="mm-section-title">{{ t('mailCompose.form.title') }}</h2>
    <el-form label-position="top">
      <el-form-item :label="t('mailCompose.form.from')">
        <el-select
          v-model="form.fromEmail"
          style="width: 100%"
          :disabled="senderDisabled"
          :placeholder="t('mailCompose.form.fromPlaceholder')"
        >
          <el-option
            v-for="identity in props.senderOptions"
            :key="identity.identityId || identity.emailAddress"
            :label="formatMailSenderLabel(identity)"
            :value="identity.emailAddress"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('mailCompose.form.to')">
        <el-autocomplete
          v-model="form.toEmail"
          :placeholder="t('mailCompose.form.toPlaceholder')"
          clearable
          style="width: 100%"
          :fetch-suggestions="queryRecipientSuggestions"
        />
      </el-form-item>
      <p
        v-if="props.recipientE2eeLoading"
        class="recipient-e2ee recipient-e2ee--loading"
        data-testid="mail-compose-e2ee-loading"
      >
        {{ t('mailCompose.e2ee.loading') }}
      </p>
      <el-alert
        v-else-if="recipientE2eeAlertTitle"
        data-testid="mail-compose-e2ee-alert"
        :type="recipientE2eeAlertType"
        :closable="false"
        :title="recipientE2eeAlertTitle"
        :description="recipientE2eeAlertDescription"
      />
      <section
        v-if="supportsExternalSecureDelivery"
        class="external-secure-card"
        data-testid="mail-compose-external-secure"
      >
        <div class="external-secure-card__head">
          <span>{{ t('mailCompose.externalSecure.form.title') }}</span>
          <el-switch v-model="externalSecureDelivery.enabled" />
        </div>
        <p class="external-secure-card__hint">{{ t('mailCompose.externalSecure.form.hint') }}</p>
        <div v-if="externalSecureDelivery.enabled" class="external-secure-card__body">
          <el-form-item :label="t('mailCompose.externalSecure.form.password')">
            <el-input
              v-model="externalSecureDelivery.password"
              show-password
              type="password"
              :placeholder="t('mailCompose.externalSecure.form.passwordPlaceholder')"
            />
          </el-form-item>
          <el-form-item :label="t('mailCompose.externalSecure.form.passwordHint')">
            <el-input
              v-model="externalSecureDelivery.passwordHint"
              :placeholder="t('mailCompose.externalSecure.form.passwordHintPlaceholder')"
            />
          </el-form-item>
          <el-form-item :label="t('mailCompose.externalSecure.form.expiresAt')">
            <el-date-picker
              v-model="externalSecureDelivery.expiresAt"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              :placeholder="t('mailCompose.externalSecure.form.expiresAtPlaceholder')"
              style="width: 100%"
            />
          </el-form-item>
          <p class="external-secure-card__boundary">{{ t('mailCompose.externalSecure.form.boundary') }}</p>
        </div>
      </section>
      <el-form-item :label="t('mailCompose.form.subject')">
        <el-input v-model="form.subject" :placeholder="t('mailCompose.form.subjectPlaceholder')" />
      </el-form-item>
      <el-form-item :label="t('mailCompose.form.labels')">
        <el-select
          v-model="form.labels"
          multiple
          filterable
          collapse-tags
          :placeholder="t('mailCompose.form.labelsPlaceholder')"
          style="width: 100%"
        >
          <el-option
            v-for="label in props.availableLabels"
            :key="label.id"
            :label="label.name"
            :value="label.name"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('mailCompose.form.schedule')">
        <el-date-picker
          v-model="form.scheduledAt"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          :placeholder="t('mailCompose.form.schedulePlaceholder')"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item :label="t('mailCompose.form.body')">
        <el-input v-model="form.body" type="textarea" :rows="12" :placeholder="t('mailCompose.form.bodyPlaceholder')" />
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
        <el-button type="primary" @click="onSend">{{ t('mailCompose.actions.send') }}</el-button>
        <el-button @click="onSaveDraft">{{ t('mailCompose.actions.saveDraft') }}</el-button>
      </div>
      <p class="hint">{{ t('mailCompose.hint.autoSave', { seconds: props.autoSaveSeconds }) }}</p>
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

.recipient-e2ee {
  margin: -6px 0 16px;
  font-size: 13px;
  color: var(--mm-muted);
}

.recipient-e2ee--loading {
  color: var(--mm-accent, #0c5a5a);
}

.external-secure-card {
  margin: 12px 0 18px;
  padding: 16px;
  border-radius: 16px;
  border: 1px solid rgba(12, 90, 90, 0.12);
  background: rgba(12, 90, 90, 0.04);
}

.external-secure-card__head,
.external-secure-card__body {
  display: grid;
  gap: 12px;
}

.external-secure-card__head {
  grid-template-columns: 1fr auto;
  align-items: center;
}

.external-secure-card__hint,
.external-secure-card__boundary {
  margin: 0;
  color: var(--mm-muted);
  font-size: 13px;
}
</style>
