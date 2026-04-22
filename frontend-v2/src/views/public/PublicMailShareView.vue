<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { NButton, NInput } from 'naive-ui'
import { lt, useLocaleText } from '@/locales'
import { usePublicShareFlow } from '@/shared/composables/usePublicShareFlow'
import {
  decryptMailPublicAttachmentBlob,
  decryptMailPublicBody,
  triggerMailPublicDownload
} from '@/shared/utils/mail-public'
import {
  downloadPublicMailAttachment,
  readPublicMailShare,
  type PublicMailShare,
  type PublicMailShareAttachment
} from '@/service/api/public-share'

const route = useRoute()
const { tr } = useLocaleText()
const shareFlow = usePublicShareFlow()
const auditedActions = shareFlow.auditedActions
const passwordHeader = shareFlow.passwordHeader
const sharePassword = shareFlow.password

const share = ref<PublicMailShare | null>(null)
const plaintext = ref('')
const loading = ref(false)
const decrypting = ref(false)
const downloadingAttachmentId = ref('')
const loadError = ref('')
const decryptError = ref('')
const token = computed(() => String(route.params.token || ''))

const pageTitle = computed(() => {
  if (share.value?.subject) {
    return share.value.subject
  }

  return tr(lt('打开受保护消息', '開啟受保護訊息', 'Open a protected message'))
})

const pageSubtitle = computed(() => {
  if (share.value) {
    return `${share.value.senderEmail} → ${share.value.recipientEmail}`
  }

  if (loadError.value) {
    return loadError.value
  }

  return tr(lt('该链接会根据路由令牌加载消息正文与附件元数据。', '此連結會依路由權杖載入訊息正文與附件中繼資料。', 'This link loads message content and attachment metadata from the route token.'))
})

function formatDateTime(value: string | null) {
  if (!value) {
    return tr(lt('未设置', '未設定', 'Not set'))
  }

  const parsed = new Date(value)

  if (Number.isNaN(parsed.getTime())) {
    return value
  }

  return parsed.toLocaleString()
}

function formatBytes(value: number) {
  if (!Number.isFinite(value) || value <= 0) {
    return '0 B'
  }

  const units = ['B', 'KB', 'MB', 'GB']
  let size = value
  let index = 0

  while (size >= 1024 && index < units.length - 1) {
    size /= 1024
    index += 1
  }

  return `${size >= 10 || index === 0 ? size.toFixed(0) : size.toFixed(1)} ${units[index]}`
}

async function loadShare() {
  loading.value = true
  loadError.value = ''
  decryptError.value = ''
  plaintext.value = ''
  share.value = null

  const capabilityPromise = shareFlow.loadCapabilities()

  try {
    if (!token.value) {
      loadError.value = tr(lt('缺少共享令牌。', '缺少共享權杖。', 'Missing share token.'))
      return
    }

    share.value = (await readPublicMailShare(token.value)).data
    await capabilityPromise
  } catch (error) {
    await capabilityPromise
    loadError.value = error instanceof Error && error.message
      ? error.message
      : tr(lt('无法加载共享消息。', '無法載入共享訊息。', 'Unable to load shared message.'))
  } finally {
    loading.value = false
  }
}

async function unlockShare() {
  if (!share.value) {
    return
  }

  decrypting.value = true
  decryptError.value = ''

  try {
    plaintext.value = await decryptMailPublicBody(share.value.bodyCiphertext, shareFlow.password.value)
    shareFlow.unlock()
  } catch (error) {
    plaintext.value = ''
    decryptError.value = error instanceof Error && error.message
      ? error.message
      : tr(lt('无法解密消息正文。', '無法解密訊息正文。', 'Unable to decrypt the message body.'))
  } finally {
    decrypting.value = false
  }
}

async function downloadAttachment(attachment: PublicMailShareAttachment) {
  if (!share.value) {
    return
  }

  downloadingAttachmentId.value = attachment.id
  decryptError.value = ''

  try {
    const payload = await downloadPublicMailAttachment(token.value, attachment.id)
    const blob = await decryptMailPublicAttachmentBlob(
      payload.blob,
      shareFlow.password.value,
      payload.contentType || attachment.contentType
    )
    triggerMailPublicDownload(blob, attachment.fileName)
    shareFlow.unlock()
  } catch (error) {
    decryptError.value = error instanceof Error && error.message
      ? error.message
      : tr(lt('无法下载附件。', '無法下載附件。', 'Unable to download the attachment.'))
  } finally {
    downloadingAttachmentId.value = ''
  }
}

onMounted(() => {
  void loadShare()
})
</script>

<template>
  <section class="public-shell page-shell share-page">
    <article class="surface-card share-page__auth">
      <span class="section-label">{{ tr(lt('安全邮件投递', '安全郵件投遞', 'Secure mail delivery')) }}</span>
      <h1 class="page-title">{{ pageTitle }}</h1>
      <p class="page-subtitle">{{ pageSubtitle }}</p>

      <div v-if="passwordHeader || auditedActions.length" class="share-page__capabilities">
        <div v-if="passwordHeader" class="metric-chip">{{ passwordHeader }}</div>
        <div v-for="action in auditedActions" :key="action" class="metric-chip">{{ action }}</div>
      </div>

      <div v-if="share" class="share-page__facts">
        <div class="metric-chip">{{ tr(lt('发件人', '寄件者', 'Sender')) }}: {{ share.senderEmail }}</div>
        <div class="metric-chip">{{ tr(lt('收件人', '收件者', 'Recipient')) }}: {{ share.recipientEmail }}</div>
        <div class="metric-chip">{{ tr(lt('附件', '附件', 'Attachments')) }}: {{ share.attachments.length }}</div>
        <div class="metric-chip">{{ tr(lt('到期', '到期', 'Expires')) }}: {{ formatDateTime(share.expiresAt) }}</div>
      </div>

      <label>{{ tr(lt('密码', '密碼', 'Password')) }}</label>
      <n-input
        v-model:value="sharePassword"
        type="password"
        :placeholder="tr(lt('输入访问密码', '輸入存取密碼', 'Enter access password'))"
      />

      <p v-if="share?.passwordHint" class="share-page__hint">
        {{ tr(lt('密码提示', '密碼提示', 'Password hint')) }}: {{ share.passwordHint }}
      </p>
      <p v-if="decryptError" class="share-page__error">{{ decryptError }}</p>

      <div class="share-page__auth-actions">
        <n-button type="primary" :loading="decrypting" :disabled="!share" @click="unlockShare">
          {{ tr(lt('解密消息', '解密訊息', 'Decrypt message')) }}
        </n-button>
        <n-button secondary :loading="loading" @click="loadShare">
          {{ tr(lt('刷新消息', '重新整理訊息', 'Refresh message')) }}
        </n-button>
      </div>
    </article>

    <article class="surface-card share-page__preview">
      <span class="section-label">{{ tr(lt('投递预览', '投遞預覽', 'Delivery preview')) }}</span>
      <template v-if="share">
        <h2>{{ share.subject }}</h2>
        <p class="page-subtitle">{{ tr(lt('消息正文会在本地解密后显示。', '訊息正文會在本機解密後顯示。', 'The message body appears after local decryption.')) }}</p>

        <div class="mail-body-card">
          <strong>{{ tr(lt('正文', '正文', 'Body')) }}</strong>
          <pre>{{ plaintext || tr(lt('输入密码后解密邮件正文。', '輸入密碼後解密郵件正文。', 'Enter the password to decrypt the message body.')) }}</pre>
        </div>

        <div class="attachment-list">
          <div v-for="attachment in share.attachments" :key="attachment.id" class="attachment-card">
            <div>
              <strong>{{ attachment.fileName }}</strong>
              <span>{{ attachment.contentType }} · {{ formatBytes(attachment.fileSize) }}</span>
            </div>
            <n-button
              secondary
              :loading="downloadingAttachmentId === attachment.id"
              @click="downloadAttachment(attachment)"
            >
              {{ tr(lt('下载附件', '下載附件', 'Download attachment')) }}
            </n-button>
          </div>
          <p v-if="!share.attachments.length" class="page-subtitle">
            {{ tr(lt('此消息没有附件。', '此訊息沒有附件。', 'This message has no attachments.')) }}
          </p>
        </div>
      </template>
      <p v-else class="page-subtitle">
        {{ loading ? tr(lt('正在加载共享消息。', '正在載入共享訊息。', 'Loading shared message.')) : tr(lt('未找到共享消息。', '找不到共享訊息。', 'Shared message unavailable.')) }}
      </p>
    </article>
  </section>
</template>

<style scoped>
.public-shell {
  padding: 64px 0;
}

.share-page {
  display: grid;
  grid-template-columns: 0.85fr 1.15fr;
  gap: 20px;
}

.share-page__auth,
.share-page__preview {
  padding: 28px;
}

label {
  display: block;
  margin: 20px 0 10px;
  color: var(--mm-text-secondary);
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.share-page__capabilities,
.share-page__auth-actions,
.share-page__facts {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 18px;
}

.share-page__hint,
.share-page__error,
.mail-body-card pre,
.attachment-card span {
  margin: 0;
}

.share-page__hint,
.attachment-card span {
  color: var(--mm-text-secondary);
  font-size: 12px;
}

.share-page__error {
  margin-top: 12px;
  color: var(--mm-error, #d03050);
  font-size: 13px;
}

.share-page__preview h2 {
  margin: 10px 0 12px;
  font-size: 32px;
  letter-spacing: -0.04em;
}

.mail-body-card {
  display: grid;
  gap: 12px;
  margin-top: 20px;
  padding: 18px;
  border: 1px solid var(--mm-border);
  border-radius: 16px;
  background: var(--mm-card-muted);
}

.mail-body-card pre {
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--mm-text-primary);
}

.attachment-list {
  display: grid;
  gap: 14px;
  margin-top: 24px;
}

.attachment-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 18px;
  border: 1px solid var(--mm-border);
  border-radius: 12px;
  background: var(--mm-card-muted);
}

.attachment-card > div {
  display: grid;
  gap: 6px;
}

@media (max-width: 900px) {
  .share-page {
    grid-template-columns: 1fr;
  }

  .attachment-card {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
