<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { LabelItem, MailDetail } from '~/types/api'
import MailE2eeEncryptedBody from '~/components/mail/MailE2eeEncryptedBody.vue'
import { useMailApi } from '~/composables/useMailApi'
import { useLabelApi } from '~/composables/useLabelApi'
import { useContactApi } from '~/composables/useContactApi'
import { useI18n } from '~/composables/useI18n'
import { useMailStore } from '~/stores/mail'
import { formatMailAttachmentSize } from '~/utils/mail-attachments'
import { buildMailComposeQuery } from '~/utils/mail-compose'

const route = useRoute()
const loading = ref(false)
const labelSaving = ref(false)
const mail = ref<MailDetail | null>(null)
const labels = ref<LabelItem[]>([])
const selectedLabels = ref<string[]>([])

const mailStore = useMailStore()
const { t } = useI18n()
const { fetchMailDetail, applyAction, updateLabels, fetchStats, downloadMailAttachment } = useMailApi()
const { listLabels } = useLabelApi()
const { quickAddContact } = useContactApi()
const canEditDraft = computed(() => Boolean(mail.value?.isDraft))
const canReply = computed(() => Boolean(mail.value && !mail.value.isDraft))
const canForward = computed(() => Boolean(mail.value && !mail.value.isDraft))

useHead(() => ({
  title: mail.value?.subject || t('mailWorkspace.detail.pageTitle')
}))

async function loadDetail(): Promise<void> {
  const id = String(route.params.id || '')
  if (!id) {
    return
  }
  loading.value = true
  try {
    const [detail, allLabels] = await Promise.all([fetchMailDetail(id), listLabels()])
    mail.value = detail
    labels.value = allLabels
    selectedLabels.value = [...detail.labels]
  } finally {
    loading.value = false
  }
}

async function runAction(action: string): Promise<void> {
  if (!mail.value) {
    return
  }
  const result = await applyAction(mail.value.id, action)
  mailStore.updateStats(result.stats)
  await loadDetail()
}

async function saveLabels(): Promise<void> {
  if (!mail.value) {
    return
  }
  labelSaving.value = true
  try {
    await updateLabels(mail.value.id, selectedLabels.value)
    const stats = await fetchStats()
    mailStore.updateStats(stats)
    ElMessage.success(t('mailWorkspace.detail.messages.labelsUpdated'))
    await loadDetail()
  } finally {
    labelSaving.value = false
  }
}

async function addContactFromMail(): Promise<void> {
  if (!mail.value?.peerEmail) {
    ElMessage.warning(t('mailWorkspace.detail.messages.noSenderEmail'))
    return
  }
  await quickAddContact(mail.value.peerEmail)
  ElMessage.success(t('mailWorkspace.detail.messages.contactSaved'))
}

async function openCompose(mode: 'reply' | 'forward' | 'draft'): Promise<void> {
  if (!mail.value) {
    return
  }
  await navigateTo({
    path: '/compose',
    query: buildMailComposeQuery(mode, mail.value)
  })
}

async function downloadAttachment(attachmentId: string): Promise<void> {
  if (!mail.value) {
    return
  }
  try {
    const downloaded = await downloadMailAttachment(mail.value.id, attachmentId)
    const url = URL.createObjectURL(downloaded.blob)
    const link = document.createElement('a')
    link.href = url
    link.download = downloaded.fileName
    link.click()
    URL.revokeObjectURL(url)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('mailWorkspace.detail.messages.attachmentDownloadFailed'))
  }
}

onMounted(() => {
  void loadDetail()
})
</script>

<template>
  <div class="mm-page">
    <section class="mm-card panel">
      <el-skeleton v-if="loading" :rows="8" animated />
      <template v-else-if="mail">
        <div class="head">
          <h1 class="mm-section-title">{{ mail.subject || t('mailWorkspace.detail.fallbackSubject') }}</h1>
          <div class="actions">
            <el-button v-if="canReply" data-testid="mail-reply-button" type="primary" size="small" @click="openCompose('reply')">{{ t('mailWorkspace.detail.actions.reply') }}</el-button>
            <el-button v-if="canForward" data-testid="mail-forward-button" size="small" @click="openCompose('forward')">{{ t('mailWorkspace.detail.actions.forward') }}</el-button>
            <el-button v-if="canEditDraft" data-testid="mail-edit-draft-button" type="primary" plain size="small" @click="openCompose('draft')">{{ t('mailWorkspace.detail.actions.editDraft') }}</el-button>
            <el-button size="small" @click="runAction(mail.isRead ? 'MARK_UNREAD' : 'MARK_READ')">
              {{ t(mail.isRead ? 'mailList.actions.markUnread' : 'mailList.actions.markRead') }}
            </el-button>
            <el-button size="small" @click="runAction(mail.isStarred ? 'UNSTAR' : 'STAR')">
              {{ t(mail.isStarred ? 'mailList.actions.unstar' : 'mailList.actions.star') }}
            </el-button>
            <el-button size="small" @click="runAction('SNOOZE_24H')">{{ t('mailList.actions.snooze24h') }}</el-button>
            <el-button size="small" @click="runAction('SNOOZE_7D')">{{ t('mailList.actions.snooze7d') }}</el-button>
            <el-button size="small" @click="runAction('UNSNOOZE')">{{ t('mailList.actions.unsnooze') }}</el-button>
            <el-button size="small" @click="runAction('MOVE_ARCHIVE')">{{ t('mailList.actions.archive') }}</el-button>
            <el-button size="small" @click="runAction('MOVE_SPAM')">{{ t('mailList.actions.spam') }}</el-button>
            <el-button size="small" @click="runAction('MOVE_TRASH')">{{ t('mailList.actions.trash') }}</el-button>
            <el-button size="small" @click="runAction('MOVE_INBOX')">{{ t('mailWorkspace.detail.actions.moveInbox') }}</el-button>
            <el-button size="small" @click="addContactFromMail">{{ t('mailWorkspace.detail.actions.addContact') }}</el-button>
          </div>
        </div>
        <p class="meta">{{ mail.peerEmail }} · {{ new Date(mail.sentAt).toLocaleString() }}</p>

        <el-form label-position="top" class="label-editor">
          <el-form-item :label="t('mailCompose.form.labels')">
            <el-select v-model="selectedLabels" multiple collapse-tags filterable style="width: 100%">
              <el-option v-for="label in labels" :key="label.id" :label="label.name" :value="label.name" />
            </el-select>
          </el-form-item>
          <el-button size="small" :loading="labelSaving" @click="saveLabels">{{ t('mailWorkspace.detail.actions.saveLabels') }}</el-button>
        </el-form>

        <section v-if="mail.attachments.length > 0" class="attachment-list" data-testid="mail-detail-attachments">
          <div v-for="attachment in mail.attachments" :key="attachment.id" class="attachment-item" data-testid="mail-attachment-item">
            <div>
              <strong>{{ attachment.fileName }}</strong>
              <p>{{ formatMailAttachmentSize(attachment.fileSize) }}</p>
            </div>
            <el-button data-testid="mail-attachment-download-button" size="small" text @click="downloadAttachment(attachment.id)">{{ t('mailCompose.attachments.download') }}</el-button>
          </div>
        </section>

        <MailE2eeEncryptedBody
          v-if="mail.e2ee?.enabled"
          :ciphertext="mail.body"
          :metadata="mail.e2ee"
        />
        <article v-else class="body">{{ mail.body }}</article>
      </template>
      <el-empty v-else :description="t('mailWorkspace.detail.empty')" />
    </section>
  </div>
</template>

<style scoped>
.panel {
  padding: 20px;
}

.head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: flex-start;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.meta {
  color: var(--mm-muted);
  margin-top: 8px;
  margin-bottom: 16px;
}

.label-editor {
  margin-bottom: 20px;
  max-width: 520px;
}

.attachment-list {
  display: grid;
  gap: 10px;
  margin-bottom: 20px;
}

.attachment-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: 12px 14px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
}

.attachment-item p {
  margin: 4px 0 0;
  color: var(--mm-muted);
  font-size: 12px;
}

.body {
  white-space: pre-wrap;
  line-height: 1.7;
}

@media (max-width: 900px) {
  .head,
  .attachment-item {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
