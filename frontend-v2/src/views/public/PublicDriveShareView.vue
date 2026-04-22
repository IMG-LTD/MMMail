<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { NButton, NInput } from 'naive-ui'
import { lt, useLocaleText } from '@/locales'
import { usePublicShareFlow } from '@/shared/composables/usePublicShareFlow'
import {
  downloadPublicDriveShareItem,
  listPublicDriveShareItems,
  readPublicDriveShareMetadata,
  type PublicDriveShareMetadata
} from '@/service/api/public-share'

interface DriveShareItem {
  id: string
  name?: string
  itemType?: string
  size?: number
  contentType?: string
}

const route = useRoute()
const { tr } = useLocaleText()
const shareFlow = usePublicShareFlow()
const auditedActions = shareFlow.auditedActions
const passwordHeader = shareFlow.passwordHeader
const sharePassword = shareFlow.password

const metadata = ref<PublicDriveShareMetadata | null>(null)
const items = ref<DriveShareItem[]>([])
const loading = ref(false)
const downloadLoadingId = ref('')
const loadError = ref('')
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

function formatBytes(value?: number) {
  if (!value || value <= 0) {
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

function inferFileName(contentDisposition: string, fallback: string) {
  const match = contentDisposition.match(/filename\*=UTF-8''([^;]+)|filename="?([^";]+)"?/i)
  const encoded = match?.[1] || match?.[2]

  if (!encoded) {
    return fallback
  }

  try {
    return decodeURIComponent(encoded)
  } catch {
    return encoded
  }
}

function triggerDownload(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = fileName
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  URL.revokeObjectURL(url)
}

async function loadDriveShare() {
  loading.value = true
  loadError.value = ''
  metadata.value = null
  items.value = []

  const capabilityPromise = shareFlow.loadCapabilities()

  try {
    if (!token.value) {
      loadError.value = tr(lt('缺少共享令牌。', '缺少共享權杖。', 'Missing share token.'))
      return
    }

    metadata.value = (await readPublicDriveShareMetadata(token.value)).data
    items.value = ((await listPublicDriveShareItems(token.value, shareFlow.password.value)).data || []) as DriveShareItem[]
    await capabilityPromise
  } catch (error) {
    await capabilityPromise
    loadError.value = error instanceof Error && error.message
      ? error.message
      : tr(lt('无法加载共享文件。', '無法載入共享檔案。', 'Unable to load shared files.'))
  } finally {
    loading.value = false
  }
}

async function downloadItem(item: DriveShareItem) {
  downloadLoadingId.value = item.id

  try {
    const payload = await downloadPublicDriveShareItem(token.value, item.id, shareFlow.password.value)
    const fileName = inferFileName(payload.contentDisposition, item.name || 'download')
    triggerDownload(payload.blob, fileName)
    shareFlow.unlock()
  } finally {
    downloadLoadingId.value = ''
  }
}

onMounted(() => {
  void loadDriveShare()
})
</script>

<template>
  <section class="public-shell page-shell share-drive">
    <article class="surface-card share-drive__hero">
      <span class="section-label">{{ tr(lt('安全共享访问', '安全共享存取', 'Secure share access')) }}</span>
      <h1 class="page-title">
        {{ metadata?.rootItemName || tr(lt('共享文件夹访问', '共享資料夾存取', 'Shared folder access')) }}
      </h1>
      <p class="page-subtitle">
        {{ loadError || tr(lt('该链接会按路由令牌加载共享元数据和文件列表。', '此連結會依路由權杖載入共享中繼資料與檔案清單。', 'This link loads share metadata and item listings from the route token.')) }}
      </p>

      <div v-if="metadata" class="share-drive__meta">
        <div class="metric-chip">{{ tr(lt('权限', '權限', 'Permission')) }}: {{ metadata.permission }}</div>
        <div class="metric-chip">{{ tr(lt('项目数', '項目數', 'Items')) }}: {{ metadata.itemCount }}</div>
        <div class="metric-chip">{{ tr(lt('到期', '到期', 'Expires')) }}: {{ formatDateTime(metadata.expiresAt) }}</div>
      </div>

      <div v-if="passwordHeader || auditedActions.length" class="share-drive__meta">
        <div v-if="passwordHeader" class="metric-chip">{{ passwordHeader }}</div>
        <div v-for="action in auditedActions" :key="action" class="metric-chip">{{ action }}</div>
      </div>

      <label>{{ tr(lt('共享密码', '共享密碼', 'Share password')) }}</label>
      <n-input
        v-model:value="sharePassword"
        type="password"
        :placeholder="tr(lt('输入共享密码以刷新列表', '輸入共享密碼以重新整理清單', 'Enter the share password to refresh the list'))"
      />

      <div class="share-drive__actions">
        <n-button type="primary" :loading="loading" @click="loadDriveShare">
          {{ tr(lt('打开共享文件', '開啟共享檔案', 'Open shared files')) }}
        </n-button>
      </div>
    </article>

    <article class="surface-card share-drive__items">
      <span class="section-label">{{ tr(lt('共享内容', '共享內容', 'Shared items')) }}</span>
      <div v-if="items.length" class="share-drive__list">
        <div v-for="item in items" :key="item.id" class="share-drive__item">
          <div>
            <strong>{{ item.name || item.id }}</strong>
            <span>
              {{ item.itemType || tr(lt('文件', '檔案', 'File')) }}
              <template v-if="typeof item.size === 'number'"> · {{ formatBytes(item.size) }}</template>
            </span>
          </div>
          <n-button secondary :loading="downloadLoadingId === item.id" @click="downloadItem(item)">
            {{ tr(lt('下载', '下載', 'Download')) }}
          </n-button>
        </div>
      </div>
      <p v-else class="page-subtitle">
        {{ loading ? tr(lt('正在加载项目列表。', '正在載入項目清單。', 'Loading shared items.')) : tr(lt('没有可显示的共享项目。', '沒有可顯示的共享項目。', 'No shared items to display.')) }}
      </p>
    </article>
  </section>
</template>

<style scoped>
.public-shell {
  padding: 72px 0;
}

.share-drive {
  display: grid;
  grid-template-columns: 0.9fr 1.1fr;
  gap: 20px;
}

.share-drive__hero,
.share-drive__items {
  padding: 30px;
}

label {
  display: block;
  margin: 20px 0 10px;
  color: var(--mm-text-secondary);
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.share-drive__meta,
.share-drive__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 20px;
}

.share-drive__list {
  display: grid;
  gap: 12px;
  margin-top: 18px;
}

.share-drive__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px;
  border: 1px solid var(--mm-border);
  border-radius: 14px;
  background: var(--mm-card-muted);
}

.share-drive__item > div {
  display: grid;
  gap: 6px;
}

.share-drive__item span {
  color: var(--mm-text-secondary);
  font-size: 12px;
}

@media (max-width: 900px) {
  .share-drive {
    grid-template-columns: 1fr;
  }

  .share-drive__item {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
