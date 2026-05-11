<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import CompactPageHeader from '@/shared/components/CompactPageHeader.vue'
import { lt, useLocaleText } from '@/locales'
import { listLabsModules, type LabsModule } from '@/service/api/labs'
import { useAuthStore } from '@/store/modules/auth'

const { tr } = useLocaleText()
const authStore = useAuthStore()

const modules = ref<LabsModule[]>([])
const labsLoading = ref(false)
const loadError = ref('')
let latestLabsRequest = 0

const enabledModules = computed(() => modules.value.filter(item => item.enabled))
const disabledModules = computed(() => modules.value.filter(item => !item.enabled))
const statusCopy = computed(() => {
  if (!authStore.accessToken) {
    return tr(lt('登录后即可读取实验模块。', '登入後即可讀取實驗模組。', 'Sign in to load Labs modules.'))
  }

  if (loadError.value) {
    return loadError.value
  }

  return labsLoading.value
    ? tr(lt('正在读取实验模块。', '正在讀取實驗模組。', 'Loading Labs modules.'))
    : `${modules.value.length} ${tr(lt('个模块已载入', '個模組已載入', 'modules loaded'))}`
})

function clearLabsState() {
  modules.value = []
  loadError.value = ''
  labsLoading.value = false
}

function resolveErrorMessage(error: unknown) {
  return error instanceof Error
    ? error.message
    : tr(lt('读取实验模块失败。', '讀取實驗模組失敗。', 'Failed to load Labs modules.'))
}

async function loadLabsModules() {
  const requestId = ++latestLabsRequest
  const requestToken = authStore.accessToken
  if (!requestToken) {
    clearLabsState()
    return
  }

  labsLoading.value = true
  loadError.value = ''

  try {
    const nextModules = await listLabsModules(requestToken)
    if (requestId !== latestLabsRequest || requestToken !== authStore.accessToken) {
      return
    }
    modules.value = Array.isArray(nextModules) ? nextModules : []
  } catch (error) {
    if (requestId !== latestLabsRequest || requestToken !== authStore.accessToken) {
      return
    }
    modules.value = []
    loadError.value = resolveErrorMessage(error)
  } finally {
    if (requestId === latestLabsRequest && requestToken === authStore.accessToken) {
      labsLoading.value = false
    }
  }
}

watch(() => authStore.accessToken, () => {
  void loadLabsModules()
}, { immediate: true })
</script>

<template>
  <section class="page-shell surface-grid">
    <compact-page-header
      :eyebrow="lt('Labs', 'Labs', 'Labs')"
      :title="lt('实验模块', '實驗模組', 'Experimental modules')"
      :description="lt('实验能力可以被看见，但必须在视觉上从属于主功能，并明确标记为不稳定。', '實驗能力可以被看見，但必須在視覺上從屬於主要功能，並明確標記為不穩定。', 'Experimental capabilities are visible, but they stay visually subordinate and clearly marked as unstable.')"
      :badge="lt('预览', '預覽', 'Preview')"
      badge-tone="preview"
    />

    <article class="surface-card labs-banner">
      <strong>{{ tr(lt('实验能力不保证稳定性或兼容性。', '實驗能力不保證穩定性或相容性。', 'Experimental capabilities do not guarantee stability or compatibility.')) }}</strong>
      <p class="page-subtitle">{{ statusCopy }}</p>
    </article>

    <div class="labs-grid">
      <article class="surface-card labs-card">
        <span class="section-label">{{ tr(lt('已启用', '已啟用', 'Enabled')) }}</span>
        <div class="labs-card__stack">
          <span v-for="item in enabledModules" :key="item.key" class="metric-chip">{{ item.label }}</span>
          <p v-if="!enabledModules.length" class="page-subtitle">{{ statusCopy }}</p>
        </div>
      </article>
      <article class="surface-card labs-card">
        <span class="section-label">{{ tr(lt('可配置', '可設定', 'Configurable')) }}</span>
        <div class="labs-card__stack labs-card__stack--muted">
          <span v-for="item in disabledModules" :key="item.key" class="metric-chip">{{ item.label }}</span>
          <p v-if="!disabledModules.length" class="page-subtitle">{{ statusCopy }}</p>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.labs-banner,
.labs-card {
  padding: 20px;
}

.labs-banner strong {
  display: block;
  margin-bottom: 8px;
}

.labs-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.labs-card__stack {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}

.labs-card__stack--muted :deep(.metric-chip) {
  opacity: 0.76;
}

@media (max-width: 820px) {
  .labs-grid {
    grid-template-columns: 1fr;
  }
}
</style>
