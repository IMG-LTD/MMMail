<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { lt, useLocaleText } from '@/locales'
import { patchLabsModuleSettings, readLabsModule, type LabsModule } from '@/service/api/labs'
import { findSurface, labsModules } from '@/shared/content/route-surfaces'
import { useAuthStore } from '@/store/modules/auth'

const route = useRoute()
const authStore = useAuthStore()
const { tr } = useLocaleText()
const current = computed(() => findSurface(labsModules, String(route.params.moduleKey ?? 'authenticator'), 'authenticator'))
const moduleDetail = ref<LabsModule | null>(null)
const moduleLoading = ref(false)
const settingsSaving = ref(false)
const loadError = ref('')
let latestLabsModuleRequest = 0

const moduleKey = computed(() => String(route.params.moduleKey ?? 'authenticator'))
const title = computed(() => moduleDetail.value?.label || tr(current.value.label))
const description = computed(() => moduleDetail.value?.description || tr(current.value.description))
const moduleStatusCopy = computed(() => {
  if (!authStore.accessToken) {
    return tr(lt('登录后即可读取模块配置。', '登入後即可讀取模組設定。', 'Sign in to load module configuration.'))
  }

  if (loadError.value) {
    return loadError.value
  }

  return moduleLoading.value
    ? tr(lt('正在读取模块配置。', '正在讀取模組設定。', 'Loading module configuration.'))
    : tr(lt('模块配置来自 v2.1 运行时。', '模組設定來自 v2.1 執行期。', 'Module configuration is loaded from the v2.1 runtime.'))
})

function clearModuleState() {
  moduleDetail.value = null
  loadError.value = ''
  moduleLoading.value = false
}

function resolveErrorMessage(error: unknown) {
  return error instanceof Error
    ? error.message
    : tr(lt('读取模块配置失败。', '讀取模組設定失敗。', 'Failed to load module configuration.'))
}

async function loadLabsModule() {
  const requestId = ++latestLabsModuleRequest
  const requestToken = authStore.accessToken
  const requestModuleKey = moduleKey.value
  if (!requestToken) {
    clearModuleState()
    return
  }

  moduleLoading.value = true
  loadError.value = ''

  try {
    const nextModule = await readLabsModule(requestModuleKey, requestToken)
    if (requestId !== latestLabsModuleRequest || requestToken !== authStore.accessToken || requestModuleKey !== moduleKey.value) {
      return
    }
    moduleDetail.value = nextModule
  } catch (error) {
    if (requestId !== latestLabsModuleRequest || requestToken !== authStore.accessToken || requestModuleKey !== moduleKey.value) {
      return
    }
    moduleDetail.value = null
    loadError.value = resolveErrorMessage(error)
  } finally {
    if (requestId === latestLabsModuleRequest && requestToken === authStore.accessToken && requestModuleKey === moduleKey.value) {
      moduleLoading.value = false
    }
  }
}

async function saveModuleSettings() {
  const requestToken = authStore.accessToken
  if (!requestToken || !moduleDetail.value || settingsSaving.value) {
    return
  }

  settingsSaving.value = true
  await patchLabsModuleSettings(moduleKey.value, { enabled: !moduleDetail.value.enabled }, requestToken)
  settingsSaving.value = false
  await loadLabsModule()
}

watch(() => [moduleKey.value, authStore.accessToken], () => {
  void loadLabsModule()
}, { immediate: true })
</script>

<template>
  <section class="page-shell surface-grid labs-module">
    <article class="surface-card labs-module__hero">
      <span class="section-label">{{ tr(lt('预览模块', '預覽模組', 'Preview module')) }}</span>
      <h1>{{ title }}</h1>
      <p class="page-subtitle">{{ description }}</p>
      <div class="labs-module__actions">
        <span class="metric-chip">{{ moduleDetail?.maturity || tr(current.badge ?? lt('预览', '預覽', 'Preview')) }}</span>
        <span class="metric-chip">{{ moduleDetail?.enabled ? tr(lt('已启用', '已啟用', 'Enabled')) : tr(lt('未启用', '未啟用', 'Disabled')) }}</span>
      </div>
    </article>
    <article class="surface-card labs-module__panel">
      <span class="section-label">{{ tr(lt('当前范围', '目前範圍', 'Current scope')) }}</span>
      <strong>{{ moduleStatusCopy }}</strong>
      <p class="page-subtitle">{{ moduleDetail ? `${moduleDetail.premium ? 'Premium' : 'Community'} · ${moduleDetail.hosted ? 'Hosted optional' : 'Self-hosted'}` : moduleStatusCopy }}</p>
      <button type="button" :disabled="!moduleDetail || settingsSaving" @click="saveModuleSettings()">
        {{ settingsSaving ? tr(lt('保存中', '儲存中', 'Saving')) : tr(lt('切换启用状态', '切換啟用狀態', 'Toggle enabled')) }}
      </button>
    </article>
  </section>
</template>

<style scoped>
.labs-module__hero,
.labs-module__panel {
  display: grid;
  gap: 12px;
  padding: 20px;
}

.labs-module__hero h1 {
  margin: 0;
  font-size: 28px;
  letter-spacing: -0.04em;
}

.labs-module__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.labs-module__panel button {
  justify-self: start;
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid var(--mm-border);
  border-radius: 10px;
  background: var(--mm-card);
}
</style>
