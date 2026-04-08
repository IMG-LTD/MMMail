<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { MailPublicSecureAttachment, MailPublicSecureLink } from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import { useMailApi } from '~/composables/useMailApi'
import {
  decryptMailPublicAttachmentBlob,
  decryptMailPublicBody,
  formatMailPublicTime,
  requireMailPublicPassword,
  resolveMailPublicSecureLinkErrorKey,
  triggerMailPublicDownload
} from '~/utils/mail-public'

definePageMeta({
  layout: 'public-mail'
})

const route = useRoute()
const { t } = useI18n()
const { getPublicSecureLink, downloadPublicSecureAttachment } = useMailApi()

const token = computed(() => String(route.params.token || ''))
const loading = ref(false)
const decrypting = ref(false)
const downloadingAttachmentId = ref('')
const share = ref<MailPublicSecureLink | null>(null)
const plaintext = ref('')
const password = ref('')
const loadErrorKey = ref('')
const decryptErrorKey = ref('')

const pageTitle = computed(() => {
  if (share.value) {
    return share.value.subject
  }
  return loading.value ? t('mailPublicShare.loadingTitle') : t('mailPublicShare.unavailableTitle')
})

const pageSubtitle = computed(() => {
  if (share.value) {
    return t('mailPublicShare.sharedFrom', { value: share.value.senderEmail })
  }
  if (loading.value) {
    return t('mailPublicShare.loadingSubtitle')
  }
  return t(loadErrorKey.value || 'mailPublicShare.unavailableSubtitle')
})

const shareAttachments = computed(() => share.value?.attachments || [])
const pageTitleId = 'public-mail-page-title'

async function loadShare(): Promise<void> {
  if (!token.value) {
    loadErrorKey.value = 'mailPublicShare.errors.notFound'
    return
  }
  loading.value = true
  share.value = null
  plaintext.value = ''
  loadErrorKey.value = ''
  try {
    share.value = await getPublicSecureLink(token.value)
  } catch (error) {
    const message = error instanceof Error ? error.message : ''
    loadErrorKey.value = resolveMailPublicSecureLinkErrorKey(message) || 'mailPublicShare.errors.loadFailed'
  } finally {
    loading.value = false
  }
}

async function unlockBody(): Promise<void> {
  if (!share.value) {
    return
  }
  decrypting.value = true
  decryptErrorKey.value = ''
  try {
    const normalizedPassword = requireMailPublicPassword(password.value)
    plaintext.value = await decryptMailPublicBody(share.value.bodyCiphertext, normalizedPassword)
    ElMessage.success(t('mailPublicShare.messages.decryptSuccess'))
  } catch (error) {
    plaintext.value = ''
    decryptErrorKey.value = resolveDecryptErrorKey(error, 'mailPublicShare.messages.decryptFailed')
    ElMessage.error(t(decryptErrorKey.value))
  } finally {
    decrypting.value = false
  }
}

async function downloadAttachment(attachment: MailPublicSecureAttachment): Promise<void> {
  if (!share.value) {
    return
  }
  downloadingAttachmentId.value = attachment.id
  decryptErrorKey.value = ''
  try {
    const normalizedPassword = requireMailPublicPassword(password.value)
    const encryptedPayload = await downloadPublicSecureAttachment(token.value, attachment.id)
    const decryptedBlob = await decryptMailPublicAttachmentBlob(
      encryptedPayload.blob,
      attachment.contentType,
      normalizedPassword
    )
    triggerMailPublicDownload(decryptedBlob, attachment.fileName || encryptedPayload.fileName)
    ElMessage.success(t('mailPublicShare.messages.attachmentDecryptSuccess'))
  } catch (error) {
    decryptErrorKey.value = resolveDecryptErrorKey(error, 'mailPublicShare.messages.attachmentDecryptFailed')
    ElMessage.error(t(decryptErrorKey.value))
  } finally {
    downloadingAttachmentId.value = ''
  }
}

onMounted(() => {
  void loadShare()
})

function resolveDecryptErrorKey(error: unknown, fallbackKey: string): string {
  const message = error instanceof Error ? error.message : ''
  if (message.startsWith('mailPublicShare.')) {
    return message
  }
  return fallbackKey
}
</script>

<template>
  <div class="public-mail-page">
    <section
      class="public-mail-shell mm-card"
      :aria-busy="loading ? 'true' : 'false'"
      :aria-labelledby="pageTitleId"
    >
      <header class="public-mail-head">
        <div class="public-mail-copy">
          <p class="public-mail-badge">{{ t('mailPublicShare.badge') }}</p>
          <h1 :id="pageTitleId">{{ pageTitle }}</h1>
          <p class="public-mail-subtitle" :class="{ error: !share && !loading }" aria-live="polite">{{ pageSubtitle }}</p>
        </div>
        <el-button plain :loading="loading" @click="loadShare">{{ t('mailPublicShare.actions.refresh') }}</el-button>
      </header>

      <div v-if="share" class="public-mail-grid">
        <section class="public-mail-summary">
          <article class="field-card">
            <span>{{ t('mailPublicShare.fields.subject') }}</span>
            <strong>{{ share.subject }}</strong>
          </article>
          <article class="field-card">
            <span>{{ t('mailPublicShare.fields.sender') }}</span>
            <strong>{{ share.senderEmail }}</strong>
          </article>
          <article class="field-card">
            <span>{{ t('mailPublicShare.fields.recipient') }}</span>
            <strong>{{ share.recipientEmail }}</strong>
          </article>
          <article class="field-card">
            <span>{{ t('mailPublicShare.passwordHint') }}</span>
            <strong>{{ share.passwordHint || t('mailPublicShare.fields.noHint') }}</strong>
          </article>
          <article class="field-card">
            <span>{{ t('mailPublicShare.expiresAt') }}</span>
            <strong>{{ formatMailPublicTime(share.expiresAt) }}</strong>
          </article>
        </section>

        <section class="public-mail-body">
          <article class="trust-card" data-testid="mail-public-trust">
            <span>{{ t('mailPublicShare.fields.trustTitle') }}</span>
            <ul>
              <li>{{ t('mailPublicShare.trust.localDecrypt') }}</li>
              <li>{{ t('mailPublicShare.trust.attachmentDecrypt') }}</li>
              <li>{{ t('mailPublicShare.trust.expiryReminder') }}</li>
            </ul>
          </article>

          <el-form label-position="top">
            <el-form-item :label="t('mailPublicShare.passwordLabel')">
              <el-input
                v-model="password"
                data-testid="mail-public-password"
                show-password
                type="password"
                :placeholder="t('mailPublicShare.passwordPlaceholder')"
              />
            </el-form-item>
            <el-button
              type="primary"
              data-testid="mail-public-unlock"
              :loading="decrypting"
              @click="unlockBody"
            >
              {{ t('mailPublicShare.actions.decrypt') }}
            </el-button>
          </el-form>

          <article class="body-card">
            <span>{{ t('mailPublicShare.fields.body') }}</span>
            <pre>{{ plaintext }}</pre>
          </article>

          <article class="body-card">
            <div class="attachment-head">
              <span>{{ t('mailPublicShare.fields.attachments') }}</span>
              <strong>{{ shareAttachments.length }}</strong>
            </div>
            <div v-if="shareAttachments.length" class="attachment-list">
              <div
                v-for="attachment in shareAttachments"
                :key="attachment.id"
                class="attachment-item"
                :data-testid="`mail-public-attachment-${attachment.id}`"
              >
                <div class="attachment-copy">
                  <strong>{{ attachment.fileName }}</strong>
                  <span>{{ attachment.contentType }} · {{ attachment.fileSize }} B</span>
                </div>
                <el-button
                  plain
                  :data-testid="`mail-public-download-${attachment.id}`"
                  :loading="downloadingAttachmentId === attachment.id"
                  @click="downloadAttachment(attachment)"
                >
                  {{ t('mailPublicShare.actions.downloadAttachment') }}
                </el-button>
              </div>
            </div>
            <p v-else class="attachment-empty">{{ t('mailPublicShare.fields.noAttachments') }}</p>
          </article>
        </section>
      </div>
    </section>
  </div>
</template>

<style scoped>
.public-mail-page {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.public-mail-shell {
  width: min(1080px, 100%);
  padding: 28px;
  border-radius: 32px;
  background: rgba(12, 18, 31, 0.82);
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: #f8fbff;
  box-shadow: 0 32px 96px rgba(4, 12, 26, 0.42);
}

.public-mail-head,
.field-card {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.public-mail-copy,
.public-mail-summary,
.public-mail-body,
.trust-card,
.field-card,
.body-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.public-mail-badge {
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 0.16em;
  color: rgba(125, 247, 232, 0.82);
  font-size: 12px;
}

.public-mail-copy h1,
.public-mail-subtitle,
.body-card pre {
  margin: 0;
}

.public-mail-copy h1 {
  font-size: clamp(32px, 4vw, 48px);
}

.public-mail-subtitle {
  color: rgba(235, 244, 255, 0.74);
}

.public-mail-subtitle.error {
  color: rgba(255, 173, 173, 0.92);
}

.public-mail-grid {
  display: grid;
  gap: 18px;
  grid-template-columns: minmax(0, 0.9fr) minmax(360px, 1.1fr);
  margin-top: 24px;
}

.field-card,
.body-card {
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.trust-card ul,
.attachment-empty {
  margin: 0;
}

.trust-card ul {
  padding-left: 18px;
  color: rgba(235, 244, 255, 0.82);
}

.field-card span,
.body-card span {
  color: rgba(235, 244, 255, 0.72);
  font-size: 13px;
}

.attachment-head,
.attachment-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.attachment-list,
.attachment-copy {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.attachment-copy strong,
.attachment-copy span {
  margin: 0;
}

.attachment-copy span {
  color: rgba(235, 244, 255, 0.72);
  font-size: 13px;
}

.body-card pre {
  white-space: pre-wrap;
  word-break: break-word;
  min-height: 220px;
  color: #ffffff;
}

@media (max-width: 900px) {
  .public-mail-grid {
    grid-template-columns: 1fr;
  }

  .public-mail-head {
    flex-direction: column;
  }
}
</style>
