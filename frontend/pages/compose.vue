<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type {
  DraftRequest,
  LabelItem,
  MailAttachment,
  MailComposeSubmitRequest,
  MailId,
  MailSenderIdentity,
  UploadDraftAttachmentOptions
} from '~/types/api'
import type { FailedMailAttachmentUpload } from '~/components/business/MailAttachmentPanel.vue'
import { useMailApi } from '~/composables/useMailApi'
import { useLabelApi } from '~/composables/useLabelApi'
import { useSettingsApi } from '~/composables/useSettingsApi'
import { useContactApi } from '~/composables/useContactApi'
import { usePassApi } from '~/composables/usePassApi'
import { useMailComposeE2ee } from '~/composables/useMailComposeE2ee'
import { useMailComposeMessageE2ee } from '~/composables/useMailComposeMessageE2ee'
import { useMailDraftE2ee } from '~/composables/useMailDraftE2ee'
import { useMailDetailE2ee } from '~/composables/useMailDetailE2ee'
import { useMailAttachmentE2ee } from '~/composables/useMailAttachmentE2ee'
import { useI18n } from '~/composables/useI18n'
import { useAuthStore } from '~/stores/auth'
import { useMailStore } from '~/stores/mail'
import {
  buildMailAttachmentFailureId,
  upsertMailAttachment,
  validateMailAttachmentFile
} from '~/utils/mail-attachments'
import { resolveDefaultSenderEmail, sortMailSenderIdentities } from '~/utils/mail-identities'
import type { MailBodyE2ee } from '~/types/api'

interface ComposerDefaults {
  to: string
  subject: string
  body: string
  sender: string
  draftId: string
}

interface UploadAttachmentPayload {
  files: File[]
  draft: DraftRequest
}

type FailedUploadState = FailedMailAttachmentUpload & { file: File }

const EMPTY_COMPOSER_DEFAULTS: ComposerDefaults = {
  to: '',
  subject: '',
  body: '',
  sender: '',
  draftId: ''
}

const authStore = useAuthStore()
const mailStore = useMailStore()
const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const labels = ref<LabelItem[]>([])
const senderOptions = ref<MailSenderIdentity[]>([])
const autoSaveSeconds = ref(15)
const undoSendSeconds = ref(0)
const composerDefaults = ref<ComposerDefaults>({ ...EMPTY_COMPOSER_DEFAULTS })
const composerError = ref('')
const draftLoading = ref(false)
const attachments = ref<MailAttachment[]>([])
const attachmentUploading = ref(false)
const activeAttachmentIds = ref<string[]>([])
const failedUploads = ref<FailedUploadState[]>([])
const encryptedDraft = ref<{ ciphertext: string, metadata: MailBodyE2ee } | null>(null)
const {
  recipientE2eeLoading,
  recipientE2eeStatus,
  recipientE2eeError,
  scheduleRecipientE2eeRefresh,
  ensureRecipientE2eeStatus
} = useMailComposeE2ee()
const { buildSendPayload } = useMailComposeMessageE2ee()
const { buildDraftPayload } = useMailDraftE2ee()
const {
  decrypting: draftDecrypting,
  decryptError: draftDecryptError,
  passphrase: draftPassphrase,
  decryptEncryptedBody: decryptDraftBody,
  resetDecryptedBody: resetDraftDecryptState
} = useMailDetailE2ee()
const {
  isDraftAttachmentEncryptionEnabled,
  encryptDraftAttachment,
  decryptDownloadedAttachment
} = useMailAttachmentE2ee()

const {
  sendMail,
  saveDraft,
  uploadDraftAttachment,
  deleteDraftAttachment,
  downloadMailAttachment,
  listSenderIdentities,
  fetchMailDetail,
  fetchStats
} = useMailApi()
const { listLabels } = useLabelApi()
const { fetchProfile } = useSettingsApi()
const { fetchSuggestions } = useContactApi()
const { suggestAliasContacts } = usePassApi()

const defaultSenderEmail = computed(() => resolveDefaultSenderEmail(senderOptions.value, authStore.user?.email || ''))
const aliasSenderEmails = computed(() => new Set(
  senderOptions.value
    .filter(item => item.source === 'PASS_ALIAS')
    .map(item => item.emailAddress.toLowerCase())
))

useHead(() => ({
  title: t('mailCompose.pageTitle')
}))

function readRouteQueryValue(value: unknown): string {
  if (Array.isArray(value)) {
    const firstValue = value.find(item => typeof item === 'string')
    return typeof firstValue === 'string' ? firstValue : ''
  }
  return typeof value === 'string' ? value : ''
}

function buildPrimarySenderIdentity(): MailSenderIdentity[] {
  if (!authStore.user?.email) {
    return []
  }
  return [{
    identityId: null,
    orgId: null,
    orgName: null,
    memberId: null,
    emailAddress: authStore.user.email,
    displayName: authStore.user.displayName,
    source: 'PRIMARY',
    status: 'ENABLED',
    defaultIdentity: true
  }]
}

function buildQueryDefaults(): ComposerDefaults {
  return {
    to: readRouteQueryValue(route.query.to),
    subject: readRouteQueryValue(route.query.subject),
    body: readRouteQueryValue(route.query.body),
    sender: readRouteQueryValue(route.query.from),
    draftId: ''
  }
}

function applyComposerDefaults(next: ComposerDefaults): void {
  composerDefaults.value = { ...next }
}

function resetAttachmentState(): void {
  attachments.value = []
  failedUploads.value = []
  activeAttachmentIds.value = []
}

async function syncMailboxStats(): Promise<void> {
  mailStore.updateStats(await fetchStats())
}

async function replaceDraftRoute(draftId: MailId): Promise<void> {
  await router.replace({
    path: '/compose',
    query: { draftId }
  })
}

async function loadDraft(draftId: string): Promise<void> {
  draftLoading.value = true
  composerError.value = ''
  try {
    const detail = await fetchMailDetail(draftId)
    if (!detail.isDraft) {
      throw new Error(t('mailCompose.messages.loadDraftFailed'))
    }
    encryptedDraft.value = detail.e2ee?.enabled
      ? { ciphertext: detail.body, metadata: detail.e2ee }
      : null
    resetDraftDecryptState()
    applyComposerDefaults({
      to: detail.peerEmail,
      subject: detail.subject,
      body: detail.e2ee?.enabled ? '' : detail.body,
      sender: detail.senderEmail || '',
      draftId: detail.id
    })
    attachments.value = [...detail.attachments]
    failedUploads.value = []
  } catch (error) {
    const message = error instanceof Error ? error.message : t('mailCompose.messages.loadDraftFailed')
    composerError.value = message
    applyComposerDefaults(buildQueryDefaults())
    encryptedDraft.value = null
    resetDraftDecryptState()
    resetAttachmentState()
    ElMessage.error(message)
  } finally {
    draftLoading.value = false
  }
}

async function syncComposerFromRoute(): Promise<void> {
  const draftId = readRouteQueryValue(route.query.draftId)
  if (draftId) {
    if (draftId !== composerDefaults.value.draftId) {
      await loadDraft(draftId)
    }
    return
  }
  composerError.value = ''
  encryptedDraft.value = null
  resetDraftDecryptState()
  applyComposerDefaults(buildQueryDefaults())
  resetAttachmentState()
}

function applySavedDraft(payload: DraftRequest, draftId: MailId): void {
  applyComposerDefaults({
    to: payload.toEmail,
    subject: payload.subject,
    body: payload.body || '',
    sender: payload.fromEmail || '',
    draftId
  })
}

async function ensureDraftId(payload: DraftRequest): Promise<MailId> {
  if (composerDefaults.value.draftId) {
    return composerDefaults.value.draftId
  }
  const outboundPayload = await buildDraftPayload(payload)
  const draftId = await saveDraft(outboundPayload)
  applySavedDraft(payload, draftId)
  await syncMailboxStats()
  await replaceDraftRoute(draftId)
  return draftId
}

function setFailure(file: File, message: string): void {
  const failureId = buildMailAttachmentFailureId(file)
  failedUploads.value = [
    ...failedUploads.value.filter(item => item.id !== failureId),
    { id: failureId, fileName: file.name, message, file }
  ]
}

function clearFailure(file: File): void {
  const failureId = buildMailAttachmentFailureId(file)
  failedUploads.value = failedUploads.value.filter(item => item.id !== failureId)
}

function buildPlainUploadOptions(file: File): UploadDraftAttachmentOptions {
  return {
    file,
    fileName: file.name,
    contentType: file.type || 'application/octet-stream',
    fileSize: file.size
  }
}

async function buildUploadOptions(file: File, attachmentE2eeEnabled: boolean): Promise<UploadDraftAttachmentOptions> {
  if (!attachmentE2eeEnabled) {
    return buildPlainUploadOptions(file)
  }
  const encrypted = await encryptDraftAttachment(file)
  return {
    file: encrypted.file,
    fileName: encrypted.fileName,
    contentType: encrypted.contentType,
    fileSize: encrypted.fileSize,
    e2ee: encrypted.e2ee
  }
}

async function uploadFiles(files: File[], draft: DraftRequest): Promise<void> {
  attachmentUploading.value = true
  composerError.value = ''
  try {
    const draftId = await ensureDraftId(draft)
    const attachmentE2eeEnabled = await isDraftAttachmentEncryptionEnabled()
    for (const file of files) {
      try {
        validateMailAttachmentFile(file)
        const uploadOptions = await buildUploadOptions(file, attachmentE2eeEnabled)
        const attachment = await uploadDraftAttachment(draftId, uploadOptions)
        attachments.value = upsertMailAttachment(attachments.value, attachment)
        clearFailure(file)
      } catch (error) {
        const message = error instanceof Error ? error.message : t('mailCompose.messages.attachmentUploadFailed')
        composerError.value = message
        setFailure(file, message)
        ElMessage.error(message)
      }
    }
  } catch (error) {
    const message = error instanceof Error ? error.message : t('mailCompose.messages.attachmentUploadDraftFailed')
    composerError.value = message
    for (const file of files) {
      setFailure(file, message)
    }
    ElMessage.error(message)
  } finally {
    attachmentUploading.value = false
  }
}

async function onSend(payload: MailComposeSubmitRequest): Promise<void> {
  try {
    const recipientStatus = await ensureRecipientE2eeStatus(payload.toEmail, payload.fromEmail || '')
    const outboundPayload = await buildSendPayload(payload, recipientStatus)
    await sendMail(outboundPayload)
    await syncMailboxStats()
    if (outboundPayload.scheduledAt) {
      ElMessage.success(t('mailCompose.messages.mailScheduled'))
      await navigateTo('/scheduled')
      return
    }
    if (undoSendSeconds.value > 0) {
      ElMessage.success(t('mailCompose.messages.mailQueued'))
      await navigateTo('/outbox')
      return
    }
    ElMessage.success(t('mailCompose.messages.mailSent'))
    await navigateTo('/sent')
  } catch (error) {
    const message = error instanceof Error ? error.message : t('mailCompose.messages.sendFailed')
    composerError.value = message
    ElMessage.error(message)
  }
}

async function onSave(payload: DraftRequest): Promise<void> {
  try {
    const outboundPayload = await buildDraftPayload(payload)
    const draftId = await saveDraft(outboundPayload)
    applySavedDraft(payload, draftId)
    await syncMailboxStats()
    await replaceDraftRoute(draftId)
    ElMessage.success(t(
      payload.draftId ? 'mailCompose.messages.draftUpdated' : 'mailCompose.messages.draftSaved',
      { id: draftId }
    ))
  } catch (error) {
    const message = error instanceof Error ? error.message : t('mailCompose.messages.draftSaveFailed')
    composerError.value = message
    ElMessage.error(message)
  }
}

async function onAutoSave(payload: DraftRequest): Promise<void> {
  try {
    const outboundPayload = await buildDraftPayload(payload)
    const draftId = await saveDraft(outboundPayload)
    applySavedDraft(payload, draftId)
    await syncMailboxStats()
    if (!payload.draftId) {
      await replaceDraftRoute(draftId)
    }
  } catch (error) {
    const message = error instanceof Error ? error.message : t('mailCompose.messages.draftAutosaveFailed')
    composerError.value = message
    ElMessage.error(message)
  }
}

async function onUploadAttachments(payload: UploadAttachmentPayload): Promise<void> {
  await uploadFiles(payload.files, payload.draft)
}

async function onRetryAttachment(payload: { failureId: string, draft: DraftRequest }): Promise<void> {
  const failed = failedUploads.value.find(item => item.id === payload.failureId)
  if (!failed) {
    return
  }
  await uploadFiles([failed.file], payload.draft)
}

async function onRemoveAttachment(payload: { attachmentId: string }): Promise<void> {
  const draftId = composerDefaults.value.draftId
  if (!draftId) {
    return
  }
  activeAttachmentIds.value = [...activeAttachmentIds.value, payload.attachmentId]
  try {
    await deleteDraftAttachment(draftId, payload.attachmentId)
    attachments.value = attachments.value.filter(item => item.id !== payload.attachmentId)
  } catch (error) {
    const message = error instanceof Error ? error.message : t('mailCompose.messages.attachmentDeleteFailed')
    composerError.value = message
    ElMessage.error(message)
  } finally {
    activeAttachmentIds.value = activeAttachmentIds.value.filter(item => item !== payload.attachmentId)
  }
}

async function onDownloadAttachment(payload: { attachmentId: string }): Promise<void> {
  const attachment = attachments.value.find(item => item.id === payload.attachmentId)
  if (!attachment) {
    return
  }
  try {
    const downloaded = await downloadMailAttachment(attachment.mailId, attachment.id)
    const localFile = attachment.e2ee?.enabled
      ? await decryptDownloadedAttachment(downloaded, attachment, resolveAttachmentPassphrase())
      : downloaded
    triggerBrowserDownload(localFile.blob, localFile.fileName)
  } catch (error) {
    const message = error instanceof Error ? error.message : t('mailCompose.messages.attachmentDownloadFailed')
    composerError.value = message
    ElMessage.error(message)
  }
}

function resolveAttachmentPassphrase(): string {
  if (draftPassphrase.value.trim()) {
    return draftPassphrase.value.trim()
  }
  if (typeof window === 'undefined') {
    throw new Error(t('mailCompose.attachments.e2ee.messages.passphraseRequired'))
  }
  const prompted = window.prompt(t('mailCompose.attachments.e2ee.passphrasePrompt'))?.trim() || ''
  if (!prompted) {
    throw new Error(t('mailCompose.attachments.e2ee.messages.passphraseRequired'))
  }
  return prompted
}

function triggerBrowserDownload(blob: Blob, fileName: string): void {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.click()
  URL.revokeObjectURL(url)
}

async function fetchRecipientSuggestions(keyword: string, senderEmail: string): Promise<string[]> {
  const normalizedSender = senderEmail.trim().toLowerCase()
  if (normalizedSender && aliasSenderEmails.value.has(normalizedSender)) {
    const suggestions = await suggestAliasContacts(normalizedSender, keyword, 8)
    return suggestions.map(item => item.reverseAliasEmail)
  }
  const suggestions = await fetchSuggestions(keyword, 8)
  return suggestions.map(item => item.email)
}

function onRouteContextChange(payload: { toEmail: string, fromEmail: string }): void {
  if (draftLoading.value) {
    return
  }
  scheduleRecipientE2eeRefresh(payload.toEmail, payload.fromEmail)
}

function onDraftUnlocked(body: string): void {
  applyComposerDefaults({
    ...composerDefaults.value,
    body
  })
  encryptedDraft.value = null
  resetDraftDecryptState()
}

async function onDraftDecrypt(): Promise<void> {
  if (!encryptedDraft.value) {
    return
  }
  try {
    const plaintext = await decryptDraftBody(encryptedDraft.value.ciphertext)
    onDraftUnlocked(plaintext)
    ElMessage.success(t('mailCompose.draftE2ee.messages.decryptSuccess'))
  } catch {
    ElMessage.error(draftDecryptError.value || t('mailCompose.draftE2ee.messages.decryptFailed'))
  }
}

watch(
  () => [route.query.to, route.query.subject, route.query.body, route.query.from, route.query.draftId],
  () => {
    void syncComposerFromRoute()
  },
  { immediate: true }
)

onMounted(async () => {
  try {
    const [labelResult, profile] = await Promise.all([listLabels(), fetchProfile()])
    labels.value = labelResult
    autoSaveSeconds.value = profile.autoSaveSeconds
    undoSendSeconds.value = profile.undoSendSeconds
  } catch (error) {
    labels.value = []
    ElMessage.error(error instanceof Error ? error.message : t('mailCompose.messages.loadSettingsFailed'))
  }

  try {
    senderOptions.value = sortMailSenderIdentities(await listSenderIdentities())
  } catch (error) {
    senderOptions.value = buildPrimarySenderIdentity()
    ElMessage.error(error instanceof Error ? error.message : t('mailCompose.messages.loadSenderIdentitiesFailed'))
  }
})
</script>

<template>
  <div class="mm-page">
    <el-alert
      v-if="composerError"
      data-testid="mail-compose-error"
      class="compose-alert"
      type="error"
      :closable="false"
      :title="composerError"
    />
    <el-skeleton v-if="draftLoading" :rows="8" animated />
    <section
      v-else-if="encryptedDraft"
      class="mm-card compose-draft-e2ee"
      data-testid="mail-compose-draft-e2ee"
    >
      <span class="compose-draft-e2ee__badge">{{ t('mailCompose.draftE2ee.badge') }}</span>
      <h2 class="mm-section-title">{{ t('mailCompose.draftE2ee.title') }}</h2>
      <p class="compose-draft-e2ee__copy">{{ t('mailCompose.draftE2ee.description') }}</p>
      <p class="compose-draft-e2ee__copy">
        {{ t('mailWorkspace.detail.e2ee.algorithm', { value: encryptedDraft.metadata.algorithm || 'unknown' }) }}
      </p>
      <p class="compose-draft-e2ee__copy">
        {{ t('mailWorkspace.detail.e2ee.fingerprintCount', { count: encryptedDraft.metadata.recipientFingerprints.length }) }}
      </p>
      <el-alert
        v-if="draftDecryptError"
        type="error"
        :closable="false"
        :title="draftDecryptError"
      />
      <p class="compose-draft-e2ee__copy">{{ t('mailCompose.draftE2ee.hint') }}</p>
      <el-input
        v-model="draftPassphrase"
        data-testid="mail-compose-draft-e2ee-passphrase"
        type="password"
        show-password
        :placeholder="t('mailCompose.draftE2ee.passphrasePlaceholder')"
      />
      <el-button
        type="primary"
        :loading="draftDecrypting"
        data-testid="mail-compose-draft-e2ee-decrypt"
        @click="onDraftDecrypt"
      >
        {{ t('mailCompose.draftE2ee.actions.decrypt') }}
      </el-button>
    </section>
    <MailComposer
      v-else
      :draft-id="composerDefaults.draftId || undefined"
      :default-to="composerDefaults.to"
      :default-subject="composerDefaults.subject"
      :default-body="composerDefaults.body"
      :default-sender-email="composerDefaults.sender || defaultSenderEmail"
      :available-labels="labels"
      :attachments="attachments"
      :failed-uploads="failedUploads"
      :upload-loading="attachmentUploading"
      :active-attachment-ids="activeAttachmentIds"
      :auto-save-seconds="autoSaveSeconds"
      :sender-options="senderOptions"
      :recipient-e2ee-loading="recipientE2eeLoading"
      :recipient-e2ee-status="recipientE2eeStatus"
      :recipient-e2ee-error="recipientE2eeError"
      :fetch-recipient-suggestions="fetchRecipientSuggestions"
      @send="onSend"
      @save="onSave"
      @autosave="onAutoSave"
      @route-context-change="onRouteContextChange"
      @upload-attachments="onUploadAttachments"
      @retry-attachment="onRetryAttachment"
      @remove-attachment="onRemoveAttachment"
      @download-attachment="onDownloadAttachment"
    />
  </div>
</template>

<style scoped>
.compose-alert {
  margin-bottom: 16px;
}

.compose-draft-e2ee {
  display: grid;
  gap: 12px;
  padding: 20px;
}

.compose-draft-e2ee__badge {
  display: inline-flex;
  width: fit-content;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(19, 126, 67, 0.12);
  color: #137e43;
  font-size: 12px;
  font-weight: 600;
}

.compose-draft-e2ee__copy {
  margin: 0;
  color: var(--mm-muted);
  line-height: 1.6;
}
</style>
