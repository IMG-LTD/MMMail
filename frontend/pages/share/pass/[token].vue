<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { PassPublicSecureLink } from '~/types/pass-business'
import { useI18n } from '~/composables/useI18n'
import { usePassApi } from '~/composables/usePassApi'
import {
  formatPassItemType,
  formatPassTime,
  resolvePassSecureLinkErrorKey
} from '~/utils/pass'

definePageMeta({
  layout: 'public-pass'
})

const route = useRoute()
const { t } = useI18n()
const { getPublicSecureLink } = usePassApi()

const token = computed(() => String(route.params.token || ''))
const loading = ref(false)
const share = ref<PassPublicSecureLink | null>(null)
const loadErrorKey = ref('')

const pageTitle = computed(() => {
  if (share.value) {
    return share.value.title
  }
  return loading.value ? t('pass.publicShare.loadingTitle') : t('pass.publicShare.unavailableTitle')
})

const pageSubtitle = computed(() => {
  if (share.value) {
    return t('pass.publicShare.sharedFrom', { value: share.value.sharedVaultName })
  }
  if (loading.value) {
    return t('pass.publicShare.loadingSubtitle')
  }
  return t(loadErrorKey.value || 'pass.publicShare.unavailableSubtitle')
})

const remainingViews = computed(() => share.value?.remainingViews ?? t('common.none'))

async function loadShare(): Promise<void> {
  if (!token.value) {
    loadErrorKey.value = 'pass.publicShare.errors.tokenMissing'
    return
  }
  loading.value = true
  loadErrorKey.value = ''
  share.value = null
  try {
    share.value = await getPublicSecureLink(token.value)
  } catch (error) {
    const message = (error as Error).message || ''
    loadErrorKey.value = resolvePassSecureLinkErrorKey(message) || 'pass.publicShare.errors.loadFailed'
  } finally {
    loading.value = false
  }
}

function displayValue(value: string | null | undefined): string {
  return value || t('common.none')
}

async function onCopy(value: string | null | undefined, successKey: string): Promise<void> {
  if (!value) {
    return
  }
  try {
    await navigator.clipboard.writeText(value)
    ElMessage.success(t(successKey))
  } catch {
    ElMessage.error(t('pass.publicShare.messages.copyFailed'))
  }
}

function openWebsite(): void {
  if (!share.value?.website) {
    return
  }
  window.open(share.value.website, '_blank', 'noopener,noreferrer')
}

onMounted(() => {
  void loadShare()
})
</script>

<template>
  <div class="public-pass-page">
    <section class="public-pass-shell mm-card">
      <header class="public-pass-head">
        <div class="public-pass-copy">
          <p class="public-pass-badge">{{ t('pass.publicShare.badge') }}</p>
          <h1>{{ pageTitle }}</h1>
          <p class="public-pass-subtitle" :class="{ error: !share && !loading }">{{ pageSubtitle }}</p>
        </div>
        <el-button plain :loading="loading" @click="loadShare">{{ t('common.actions.refresh') }}</el-button>
      </header>

      <div v-if="share" class="public-pass-grid">
        <section class="public-pass-summary">
          <div class="public-pass-metrics">
            <article class="metric-card">
              <span>{{ t('pass.publicShare.metrics.itemType') }}</span>
              <strong>{{ formatPassItemType(share.itemType) }}</strong>
            </article>
            <article class="metric-card">
              <span>{{ t('pass.publicShare.metrics.views') }}</span>
              <strong>{{ share.currentViews }}/{{ share.maxViews }}</strong>
            </article>
            <article class="metric-card">
              <span>{{ t('pass.publicShare.metrics.expiresAt') }}</span>
              <strong>{{ formatPassTime(share.expiresAt) }}</strong>
            </article>
            <article class="metric-card">
              <span>{{ t('pass.publicShare.metrics.remaining') }}</span>
              <strong>{{ remainingViews }}</strong>
            </article>
          </div>

          <article class="field-card">
            <div class="field-card__head">
              <span>{{ t('pass.publicShare.fields.website') }}</span>
              <el-button plain size="small" :disabled="!share.website" @click="openWebsite">
                {{ t('pass.publicShare.actions.openWebsite') }}
              </el-button>
            </div>
            <strong>{{ displayValue(share.website) }}</strong>
          </article>

          <article class="field-card">
            <div class="field-card__head">
              <span>{{ t('pass.publicShare.fields.username') }}</span>
              <el-button plain size="small" :disabled="!share.username" @click="onCopy(share.username, 'pass.publicShare.messages.usernameCopied')">
                {{ t('pass.publicShare.actions.copyUsername') }}
              </el-button>
            </div>
            <strong>{{ displayValue(share.username) }}</strong>
          </article>
        </section>

        <section class="public-pass-secret">
          <article class="secret-card">
            <div class="field-card__head">
              <span>{{ t('pass.publicShare.fields.secret') }}</span>
              <el-button
                type="primary"
                size="small"
                :disabled="!share.secretCiphertext"
                @click="onCopy(share.secretCiphertext, 'pass.publicShare.messages.secretCopied')"
              >
                {{ t('pass.publicShare.actions.copySecret') }}
              </el-button>
            </div>
            <code>{{ displayValue(share.secretCiphertext) }}</code>
          </article>

          <article class="note-card">
            <span>{{ t('pass.publicShare.fields.note') }}</span>
            <p>{{ share.note || t('pass.publicShare.fields.noNote') }}</p>
          </article>
        </section>
      </div>
    </section>
  </div>
</template>

<style scoped>
.public-pass-page {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.public-pass-shell {
  width: min(1080px, 100%);
  padding: 28px;
  border-radius: 32px;
  background: rgba(12, 18, 31, 0.82);
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: #f8fbff;
  box-shadow: 0 32px 96px rgba(4, 12, 26, 0.42);
}

.public-pass-head,
.field-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.public-pass-copy,
.public-pass-summary,
.public-pass-secret,
.secret-card,
.note-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.public-pass-badge {
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 0.16em;
  color: rgba(125, 247, 232, 0.82);
  font-size: 12px;
}

.public-pass-copy h1,
.public-pass-subtitle,
.note-card p {
  margin: 0;
}

.public-pass-copy h1 {
  font-size: clamp(32px, 4vw, 48px);
}

.public-pass-subtitle {
  color: rgba(235, 244, 255, 0.74);
}

.public-pass-subtitle.error {
  color: rgba(255, 173, 173, 0.92);
}

.public-pass-grid,
.public-pass-metrics {
  display: grid;
  gap: 14px;
}

.public-pass-grid {
  grid-template-columns: minmax(0, 1.05fr) minmax(320px, 0.95fr);
  margin: 24px 0;
}

.public-pass-metrics {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.metric-card,
.field-card,
.secret-card,
.note-card {
  padding: 18px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.metric-card span,
.field-card span,
.secret-card span,
.note-card span {
  font-size: 12px;
  color: rgba(167, 186, 219, 0.78);
}

.secret-card code {
  display: block;
  padding: 14px;
  border-radius: 16px;
  background: rgba(15, 23, 42, 0.24);
  color: #f8fbff;
  word-break: break-all;
}

.note-card p {
  color: rgba(244, 248, 255, 0.78);
  line-height: 1.7;
}

@media (max-width: 900px) {
  .public-pass-head,
  .field-card__head {
    flex-direction: column;
  }

  .public-pass-grid,
  .public-pass-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
