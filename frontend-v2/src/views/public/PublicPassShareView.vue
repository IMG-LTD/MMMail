<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { NButton, NInput } from 'naive-ui'
import { lt, useLocaleText } from '@/locales'
import { usePublicShareFlow } from '@/shared/composables/usePublicShareFlow'
import { readPublicPassShare, type PublicPassShare } from '@/service/api/public-share'

const route = useRoute()
const { tr } = useLocaleText()
const shareFlow = usePublicShareFlow()
const auditedActions = shareFlow.auditedActions
const passwordHeader = shareFlow.passwordHeader
const sharePassword = shareFlow.password

const share = ref<PublicPassShare | null>(null)
const loading = ref(false)
const loadError = ref('')
const copyFeedback = ref('')
const token = computed(() => String(route.params.token || ''))

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

async function loadShare() {
  loading.value = true
  loadError.value = ''
  copyFeedback.value = ''
  share.value = null

  const capabilityPromise = shareFlow.loadCapabilities()

  try {
    if (!token.value) {
      loadError.value = tr(lt('缺少共享令牌。', '缺少共享權杖。', 'Missing share token.'))
      return
    }

    share.value = (await readPublicPassShare(token.value)).data
    await capabilityPromise
  } catch (error) {
    await capabilityPromise
    loadError.value = error instanceof Error && error.message
      ? error.message
      : tr(lt('无法加载共享条目。', '無法載入共享項目。', 'Unable to load the shared item.'))
  } finally {
    loading.value = false
  }
}

function displayValue(value: string | null | undefined) {
  return value || tr(lt('无', '無', 'None'))
}

async function copySecret(value: string | null | undefined, label: string) {
  if (!value) {
    return
  }

  try {
    await navigator.clipboard.writeText(value)
    copyFeedback.value = `${label} ${tr(lt('已复制', '已複製', 'copied'))}`
    shareFlow.unlock()
  } catch (error) {
    copyFeedback.value = error instanceof Error && error.message
      ? error.message
      : tr(lt('复制失败。', '複製失敗。', 'Copy failed.'))
  }
}

onMounted(() => {
  void loadShare()
})

watch(token, () => {
  void loadShare()
})
</script>

<template>
  <section class="public-shell page-shell share-pass">
    <article class="surface-card share-pass__auth">
      <span class="section-label">{{ tr(lt('受保护访问', '受保護存取', 'Protected access')) }}</span>
      <h1 class="page-title">{{ share?.title || tr(lt('打开安全链接', '開啟安全連結', 'Open a secure link')) }}</h1>
      <p class="page-subtitle">
        {{ loadError || tr(lt('该链接会按路由令牌加载共享秘密项。', '此連結會依路由權杖載入共享秘密項目。', 'This link loads the shared secret item from the route token.')) }}
      </p>

      <div v-if="passwordHeader || auditedActions.length" class="share-pass__meta">
        <div v-if="passwordHeader" class="metric-chip">{{ passwordHeader }}</div>
        <div v-for="action in auditedActions" :key="action" class="metric-chip">{{ action }}</div>
      </div>

      <label>{{ tr(lt('访问密码', '存取密碼', 'Access password')) }}</label>
      <n-input
        v-model:value="sharePassword"
        type="password"
        :placeholder="tr(lt('输入共享密码', '輸入共享密碼', 'Enter share password'))"
      />

      <div class="share-pass__actions">
        <n-button type="primary" :loading="loading" @click="loadShare">
          {{ tr(lt('刷新条目', '重新整理項目', 'Refresh item')) }}
        </n-button>
      </div>
      <p v-if="copyFeedback" class="share-pass__feedback">{{ copyFeedback }}</p>
    </article>

    <article class="surface-card share-pass__preview">
      <span class="section-label">{{ tr(lt('链接摘要', '連結摘要', 'Link summary')) }}</span>
      <template v-if="share">
        <h2>{{ share.sharedVaultName }}</h2>
        <p class="page-subtitle">
          {{ tr(lt('项目类型', '項目類型', 'Item type')) }}: {{ share.itemType }} ·
          {{ tr(lt('查看次数', '檢視次數', 'Views')) }}: {{ share.currentViews }}/{{ share.maxViews }} ·
          {{ tr(lt('到期', '到期', 'Expires')) }}: {{ formatDateTime(share.expiresAt) }}
        </p>

        <div class="share-pass__meta">
          <div class="metric-chip">{{ tr(lt('用户名', '使用者名稱', 'Username')) }}: {{ displayValue(share.username) }}</div>
          <div class="metric-chip">{{ tr(lt('网站', '網站', 'Website')) }}: {{ displayValue(share.website) }}</div>
        </div>

        <div class="share-pass__cards">
          <div class="share-pass__card">
            <strong>{{ tr(lt('秘密', '秘密', 'Secret')) }}</strong>
            <code>{{ displayValue(share.secretCiphertext) }}</code>
            <n-button secondary :disabled="!share.secretCiphertext" @click="copySecret(share.secretCiphertext, tr(lt('秘密', '秘密', 'Secret')))">
              {{ tr(lt('复制秘密', '複製秘密', 'Copy secret')) }}
            </n-button>
          </div>
          <div class="share-pass__card">
            <strong>{{ tr(lt('备注', '備註', 'Note')) }}</strong>
            <p>{{ displayValue(share.note) }}</p>
            <n-button secondary :disabled="!share.username" @click="copySecret(share.username, tr(lt('用户名', '使用者名稱', 'Username')))">
              {{ tr(lt('复制用户名', '複製使用者名稱', 'Copy username')) }}
            </n-button>
          </div>
        </div>
      </template>
      <p v-else class="page-subtitle">
        {{ loading ? tr(lt('正在加载共享条目。', '正在載入共享項目。', 'Loading shared item.')) : tr(lt('共享条目不可用。', '共享項目不可用。', 'Shared item unavailable.')) }}
      </p>
    </article>
  </section>
</template>

<style scoped>
.public-shell {
  padding: 64px 0;
}

.share-pass {
  display: grid;
  grid-template-columns: 0.85fr 1.15fr;
  gap: 20px;
}

.share-pass__auth,
.share-pass__preview {
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

.share-pass__actions,
.share-pass__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 18px;
}

.share-pass__feedback {
  margin: 16px 0 0;
  color: var(--mm-text-secondary);
}

.share-pass__preview h2 {
  margin: 10px 0 12px;
  font-size: 32px;
  letter-spacing: -0.04em;
  color: var(--mm-pass);
}

.share-pass__cards {
  display: grid;
  gap: 14px;
  margin-top: 20px;
}

.share-pass__card {
  display: grid;
  gap: 10px;
  padding: 18px;
  border: 1px solid var(--mm-border);
  border-radius: 14px;
  background: var(--mm-card-muted);
}

.share-pass__card code,
.share-pass__card p {
  margin: 0;
  word-break: break-word;
}

@media (max-width: 900px) {
  .share-pass {
    grid-template-columns: 1fr;
  }
}
</style>
