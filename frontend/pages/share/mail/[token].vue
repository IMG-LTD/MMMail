<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { decrypt, readMessage } from 'openpgp'
import type { MailPublicSecureLink } from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import { useMailApi } from '~/composables/useMailApi'
import { formatMailPublicTime, resolveMailPublicSecureLinkErrorKey } from '~/utils/mail-public'

definePageMeta({
  layout: 'public-mail'
})

const route = useRoute()
const { t } = useI18n()
const { getPublicSecureLink } = useMailApi()

const token = computed(() => String(route.params.token || ''))
const loading = ref(false)
const decrypting = ref(false)
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
  if (!password.value.trim()) {
    decryptErrorKey.value = 'mailPublicShare.messages.passwordRequired'
    ElMessage.error(t(decryptErrorKey.value))
    return
  }
  decrypting.value = true
  decryptErrorKey.value = ''
  try {
    const message = await readMessage({ armoredMessage: share.value.bodyCiphertext })
    const result = await decrypt({
      message,
      passwords: [password.value.trim()],
      format: 'utf8'
    })
    plaintext.value = String(result.data || '')
    ElMessage.success(t('mailPublicShare.messages.decryptSuccess'))
  } catch {
    plaintext.value = ''
    decryptErrorKey.value = 'mailPublicShare.messages.decryptFailed'
    ElMessage.error(t(decryptErrorKey.value))
  } finally {
    decrypting.value = false
  }
}

onMounted(() => {
  void loadShare()
})
</script>

<template>
  <div class="public-mail-page">
    <section class="public-mail-shell mm-card" :aria-busy="loading ? 'true' : 'false'">
      <header class="public-mail-head">
        <div class="public-mail-copy">
          <p class="public-mail-badge">{{ t('mailPublicShare.badge') }}</p>
          <h1>{{ pageTitle }}</h1>
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

.field-card span,
.body-card span {
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
