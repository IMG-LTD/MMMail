<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { DraftRequest, LabelItem, MailAttachment, MailId, MailSenderIdentity, SendMailRequest } from '~/types/api'
import type { FailedMailAttachmentUpload } from '~/components/business/MailAttachmentPanel.vue'
import { useMailApi } from '~/composables/useMailApi'
import { useLabelApi } from '~/composables/useLabelApi'
import { useSettingsApi } from '~/composables/useSettingsApi'
import { useContactApi } from '~/composables/useContactApi'
import { usePassApi } from '~/composables/usePassApi'
import { useI18n } from '~/composables/useI18n'
import { useAuthStore } from '~/stores/auth'
import { useMailStore } from '~/stores/mail'
import {
  buildMailAttachmentFailureId,
  upsertMailAttachment,
  validateMailAttachmentFile
} from '~/utils/mail-attachments'
import { resolveDefaultSenderEmail, sortMailSenderIdentities } from '~/utils/mail-identities'

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
    applyComposerDefaults({
      to: detail.peerEmail,
      subject: detail.subject,
      body: detail.body,
      sender: detail.senderEmail || '',
      draftId: detail.id
    })
    attachments.value = [...detail.attachments]
    failedUploads.value = []
  } catch (error) {
    const message = error instanceof Error ? error.message : t('mailCompose.messages.loadDraftFailed')
    composerError.value = message
    applyComposerDefaults(buildQueryDefaults())
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
  applyComposerDefaults(buildQueryDefaults())
  resetAttachmentState()
}

function applySavedDraft(payload: DraftRequest, draftId: MailId): void {
  applyComposerDefaults({
    to: payload.toEmail,
    subject: payload.subject,
    body: payload.body,
    sender: payload.fromEmail || '',
    draftId
  })
}

async function ensureDraftId(payload: DraftRequest): Promise<MailId> {
  if (composerDefaults.value.draftId) {
    return composerDefaults.value.draftId
  }
  const draftId = await saveDraft(payload)
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

async function uploadFiles(files: File[], draft: DraftRequest): Promise<void> {
  attachmentUploading.value = true
  composerError.value = ''
  try {
    const draftId = await ensureDraftId(draft)
    for (const file of files) {
      try {
        validateMailAttachmentFile(file)
        const attachment = await uploadDraftAttachment(draftId, file)
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

async function onSend(payload: SendMailRequest): Promise<void> {
  try {
    await sendMail(payload)
    await syncMailboxStats()
    if (payload.scheduledAt) {
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
    const draftId = await saveDraft(payload)
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
    const draftId = await saveDraft(payload)
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
    const url = URL.createObjectURL(downloaded.blob)
    const link = document.createElement('a')
    link.href = url
    link.download = downloaded.fileName
    link.click()
    URL.revokeObjectURL(url)
  } catch (error) {
    const message = error instanceof Error ? error.message : t('mailCompose.messages.attachmentDownloadFailed')
    composerError.value = message
    ElMessage.error(message)
  }
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
      :fetch-recipient-suggestions="fetchRecipientSuggestions"
      @send="onSend"
      @save="onSave"
      @autosave="onAutoSave"
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
</style>
