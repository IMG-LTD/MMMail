<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { lt, useLocaleText } from '@/locales'
import {
  listDriveItems,
  listDriveShares,
  readDriveUsage,
  type DriveItem,
  type DriveShareLink,
  type DriveUsage
} from '@/service/api/drive'
import { driveSections, findSurface } from '@/shared/content/route-surfaces'
import { useAuthStore } from '@/store/modules/auth'
import DriveSharePanel from './drive/DriveSharePanel.vue'
import {
  filterDriveItems,
  formatDateLabel,
  formatDateTime,
  formatFileSize,
  isFolderItem,
  resolveRuntimeError
} from './drive-section-helpers'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { tr } = useLocaleText()

const surfaceKey = computed(() => String(route.meta.surfaceKey ?? 'drive'))
const current = computed(() => findSurface(driveSections, surfaceKey.value, 'drive'))

const driveItems = ref<DriveItem[]>([])
const driveShares = ref<DriveShareLink[]>([])
const driveUsage = ref<DriveUsage | null>(null)
const driveLoading = ref(false)
const shareLoading = ref(false)
const loadError = ref('')
const selectedItemId = ref('')
const sharePanelOpen = ref(false)

let latestDriveRequest = 0
let latestShareRequest = 0

const visibleItems = computed(() => filterDriveItems(driveItems.value, surfaceKey.value))

const selectedItem = computed<DriveItem | null>(() => {
  return visibleItems.value.find(item => item.id === selectedItemId.value) || visibleItems.value[0] || null
})

const usageRatio = computed(() => {
  const limit = driveUsage.value?.storageLimitBytes || 0
  if (!limit) {
    return 0
  }

  return Math.min(1, Math.max(0, (driveUsage.value?.storageBytes || 0) / limit))
})

const usageMeterStyle = computed(() => {
  return { width: `${usageRatio.value * 100}%` }
})

const usageHeading = computed(() => {
  if (!authStore.accessToken) {
    return tr(lt('登录后读取用量', '登入後讀取用量', 'Sign in to load usage'))
  }

  if (!driveUsage.value) {
    return driveLoading.value
      ? tr(lt('正在读取用量', '正在讀取用量', 'Loading usage'))
      : tr(lt('暂无用量数据', '暫無用量資料', 'No usage data'))
  }

  return `${formatFileSize(driveUsage.value.storageBytes)} / ${formatFileSize(driveUsage.value.storageLimitBytes)}`
})

const usageCopy = computed(() => {
  if (!authStore.accessToken) {
    return tr(lt('登录后即可读取文件数、文件夹数与云盘配额。', '登入後即可讀取檔案數、資料夾數與雲端硬碟配額。', 'Sign in to load file, folder, and storage usage.'))
  }

  if (!driveUsage.value) {
    return tr(lt('当前没有可显示的配额摘要。', '目前沒有可顯示的配額摘要。', 'No usage summary is available yet.'))
  }

  return `${driveUsage.value.fileCount} ${tr(lt('个文件', '個檔案', 'files'))} · ${driveUsage.value.folderCount} ${tr(lt('个文件夹', '個資料夾', 'folders'))}`
})

const tableEmptyCopy = computed(() => {
  if (!authStore.accessToken) {
    return tr(lt('登录后即可读取云盘工作区。', '登入後即可讀取雲端硬碟工作區。', 'Sign in to load your drive workspace.'))
  }

  if (driveLoading.value) {
    return tr(lt('正在加载云盘项目。', '正在載入雲端硬碟項目。', 'Loading drive items.'))
  }

  return tr(lt('当前视图没有可显示的项目。', '目前檢視沒有可顯示的項目。', 'No items are available for this view.'))
})

const detailTitle = computed(() => {
  return selectedItem.value?.name || tr(lt('未选择项目', '未選取項目', 'No item selected'))
})

const detailCopy = computed(() => {
  if (!authStore.accessToken) {
    return tr(lt('登录后即可加载文件详情与共享信息。', '登入後即可載入檔案詳情與共享資訊。', 'Sign in to load file details and share data.'))
  }

  if (driveLoading.value && !selectedItem.value) {
    return tr(lt('正在读取文件详情。', '正在讀取檔案詳情。', 'Loading file details.'))
  }

  if (!selectedItem.value) {
    return tr(lt('当前视图没有可检查的文件。', '目前檢視沒有可檢查的檔案。', 'There is no file to inspect in this view.'))
  }

  return `${resolveItemType(selectedItem.value)} · ${formatFileSize(selectedItem.value.sizeBytes)} · ${formatDateTime(selectedItem.value.updatedAt, tr(lt('未设置', '未設定', 'Not set')))}`
})

const detailFacts = computed(() => {
  if (!selectedItem.value) {
    return []
  }

  return [
    {
      label: tr(lt('类型', '類型', 'Type')),
      value: resolveItemType(selectedItem.value)
    },
    {
      label: tr(lt('大小', '大小', 'Size')),
      value: formatFileSize(selectedItem.value.sizeBytes)
    },
    {
      label: tr(lt('共享', '共享', 'Shares')),
      value: `${selectedItem.value.shareCount}`
    },
    {
      label: tr(lt('更新于', '更新於', 'Updated')),
      value: formatDateTime(selectedItem.value.updatedAt, tr(lt('未设置', '未設定', 'Not set')))
    }
  ]
})

const shareHeading = computed(() => {
  if (!selectedItem.value) {
    return tr(lt('未选择文件', '未選取檔案', 'No file selected'))
  }

  return `${driveShares.value.length} ${tr(lt('条共享链接', '條共享連結', 'share links'))}`
})

const shareEmptyCopy = computed(() => {
  if (!authStore.accessToken) {
    return tr(lt('登录后即可读取共享详情。', '登入後即可讀取共享詳情。', 'Sign in to load share details.'))
  }

  if (shareLoading.value) {
    return tr(lt('正在读取共享链接。', '正在讀取共享連結。', 'Loading share links.'))
  }

  if (!selectedItem.value) {
    return tr(lt('选择一个项目以查看共享详情。', '選擇一個項目以查看共享詳情。', 'Select an item to view share details.'))
  }

  return tr(lt('当前项目没有共享链接。', '目前項目沒有共享連結。', 'This item has no share links.'))
})

async function loadDrive() {
  const requestId = ++latestDriveRequest
  const requestToken = authStore.accessToken
  const requestPath = route.fullPath

  if (!requestToken) {
    if (requestId !== latestDriveRequest || requestToken !== authStore.accessToken || requestPath !== route.fullPath) {
      return
    }

    driveItems.value = []
    driveShares.value = []
    driveUsage.value = null
    selectedItemId.value = ''
    loadError.value = ''
    driveLoading.value = false
    shareLoading.value = false
    return
  }

  driveLoading.value = true
  loadError.value = ''

  try {
    const [itemsResponse, usageResponse] = await Promise.all([
      listDriveItems(requestToken),
      readDriveUsage(requestToken)
    ])

    if (requestId !== latestDriveRequest || requestToken !== authStore.accessToken || requestPath !== route.fullPath) {
      return
    }

    const nextItems = Array.isArray(itemsResponse.data) ? itemsResponse.data : []
    const nextVisibleItems = filterDriveItems(nextItems, surfaceKey.value)

    driveItems.value = nextItems
    driveUsage.value = usageResponse.data || null
    selectedItemId.value = nextVisibleItems.some(item => item.id === selectedItemId.value)
      ? selectedItemId.value
      : nextVisibleItems[0]?.id || ''

    await loadDriveShares(selectedItemId.value, requestToken, requestPath)
  } catch (error) {
    if (requestId !== latestDriveRequest || requestToken !== authStore.accessToken || requestPath !== route.fullPath) {
      return
    }

    driveItems.value = []
    driveShares.value = []
    driveUsage.value = null
    selectedItemId.value = ''
    loadError.value = resolveRuntimeError(
      error,
      tr(lt('读取云盘数据失败，请稍后重试。', '讀取雲端硬碟資料失敗，請稍後重試。', 'Failed to load drive data. Please try again later.'))
    )
  } finally {
    if (requestId === latestDriveRequest && requestToken === authStore.accessToken && requestPath === route.fullPath) {
      driveLoading.value = false
    }
  }
}

async function loadDriveShares(itemId: string, token = authStore.accessToken, path = route.fullPath) {
  const requestId = ++latestShareRequest

  if (!token || !itemId) {
    if (requestId === latestShareRequest && token === authStore.accessToken && path === route.fullPath) {
      driveShares.value = []
      shareLoading.value = false
    }
    return
  }

  driveShares.value = []
  shareLoading.value = true

  try {
    const response = await listDriveShares(itemId, token)

    if (requestId !== latestShareRequest || token !== authStore.accessToken || path !== route.fullPath || itemId !== selectedItemId.value) {
      return
    }

    driveShares.value = Array.isArray(response.data) ? response.data : []
  } catch (error) {
    if (requestId !== latestShareRequest || token !== authStore.accessToken || path !== route.fullPath || itemId !== selectedItemId.value) {
      return
    }

    driveShares.value = []
    loadError.value = loadError.value || resolveRuntimeError(
      error,
      tr(lt('读取共享链接失败，请稍后重试。', '讀取共享連結失敗，請稍後重試。', 'Failed to load share links. Please try again later.'))
    )
  } finally {
    if (requestId === latestShareRequest && token === authStore.accessToken && path === route.fullPath && itemId === selectedItemId.value) {
      shareLoading.value = false
    }
  }
}

function openSection(key: string) {
  const pathMap: Record<string, string> = {
    drive: '/drive',
    'drive-recent': '/drive/recent',
    'drive-shared': '/drive/shared',
    'drive-starred': '/drive/starred',
    'drive-trash': '/drive/trash'
  }
  void router.push(pathMap[key] ?? '/drive')
}

function selectItem(itemId: string) {
  if (itemId === selectedItemId.value) {
    return
  }

  selectedItemId.value = itemId
  void loadDriveShares(itemId)
}

function openItemMenu(itemId: string) {
  selectItem(itemId)
  openSharePanel()
}

function openSharePanel() {
  sharePanelOpen.value = true
}

function resolveItemType(item: DriveItem) {
  if (isFolderItem(item)) {
    return tr(lt('文件夹', '資料夾', 'Folder'))
  }

  return item.mimeType || item.itemType || tr(lt('文件', '檔案', 'File'))
}

function describeShare(share: DriveShareLink) {
  const expiresCopy = share.expiresAt
    ? `${tr(lt('到期', '到期', 'Expires'))} ${formatDateTime(share.expiresAt, tr(lt('未设置', '未設定', 'Not set')))}`
    : tr(lt('无到期时间', '無到期時間', 'No expiration'))
  const passwordCopy = share.passwordProtected
    ? tr(lt('受密码保护', '受密碼保護', 'Password protected'))
    : tr(lt('无密码', '無密碼', 'No password'))

  return `${share.status} · ${expiresCopy} · ${passwordCopy}`
}

watch(() => [route.fullPath, authStore.accessToken], () => {
  void loadDrive()
}, { immediate: true })
</script>

<template>
  <section class="drive-surface">
    <aside class="drive-surface__nav">
      <button class="drive-surface__primary" type="button">{{ tr(lt('+ 新建', '+ 新增', '+ New')) }}</button>
      <button
        v-for="item in driveSections"
        :key="item.key"
        type="button"
        :class="{ 'drive-surface__nav--active': item.key === current.key }"
        @click="openSection(item.key)"
      >
        {{ tr(item.label) }}
      </button>
      <article class="surface-card drive-surface__storage">
        <strong>{{ usageHeading }}</strong>
        <span class="drive-surface__meter"><span :style="usageMeterStyle" /></span>
        <p class="page-subtitle">{{ usageCopy }}</p>
      </article>
    </aside>

    <div class="drive-surface__content">
      <header class="drive-surface__head">
        <div>
          <span class="section-label">{{ tr(lt('云盘', '雲端硬碟', 'Drive')) }}</span>
          <h1>{{ tr(current.label) }}</h1>
          <p class="page-subtitle">{{ tr(current.description) }}</p>
        </div>
        <div class="drive-surface__actions">
          <button type="button">{{ tr(lt('网格', '網格', 'Grid')) }}</button>
          <button type="button">{{ tr(lt('排序', '排序', 'Sort')) }}</button>
          <button type="button">{{ tr(lt('上传', '上傳', 'Upload')) }}</button>
        </div>
      </header>

      <p v-if="!authStore.accessToken" class="page-subtitle drive-surface__status">
        {{ tr(lt('登录后即可读取文件列表、共享链接与配额信息。', '登入後即可讀取檔案清單、共享連結與配額資訊。', 'Sign in to load drive items, share links, and usage data.')) }}
      </p>
      <p v-else-if="loadError" class="page-subtitle drive-surface__status">{{ loadError }}</p>

      <article class="surface-card drive-surface__table">
        <div class="drive-surface__table-head">
          <span>{{ tr(lt('名称', '名稱', 'Name')) }}</span>
          <span>{{ tr(lt('类型', '類型', 'Type')) }}</span>
          <span>{{ tr(lt('大小', '大小', 'Size')) }}</span>
          <span>{{ tr(lt('修改时间', '修改時間', 'Modified')) }}</span>
        </div>
        <button
          v-for="item in visibleItems"
          :key="item.id"
          class="drive-surface__row"
          :class="{ 'drive-surface__row--active': item.id === selectedItem?.id }"
          type="button"
          @click="selectItem(item.id)"
        >
          <strong>{{ item.name }}</strong>
          <span>{{ resolveItemType(item) }}</span>
          <span>{{ formatFileSize(item.sizeBytes) }}</span>
          <span>{{ formatDateLabel(item.updatedAt, tr(lt('未知时间', '未知時間', 'Unknown time'))) }}</span>
        </button>
        <p v-if="!visibleItems.length" class="drive-surface__empty">{{ tableEmptyCopy }}</p>
      </article>

      <div class="drive-surface__cards">
        <article
          v-for="item in visibleItems"
          :key="`${item.id}-card`"
          class="surface-card drive-surface__card"
          :class="{ 'drive-surface__card--active': item.id === selectedItem?.id }"
        >
          <button class="drive-surface__card-main" type="button" @click="selectItem(item.id)">
            <span class="section-label">{{ resolveItemType(item) }}</span>
            <strong>{{ item.name }}</strong>
            <span>{{ formatFileSize(item.sizeBytes) }}</span>
            <span class="page-subtitle">{{ tr(lt(`最后更新 ${formatDateLabel(item.updatedAt, tr(lt('未知时间', '未知時間', 'Unknown time')))}。共享详情会在抽屉中加载。`, `最後更新 ${formatDateLabel(item.updatedAt, tr(lt('未知时间', '未知時間', 'Unknown time')))}。共享詳情會在抽屜中載入。`, `Last updated ${formatDateLabel(item.updatedAt, tr(lt('未知时间', '未知時間', 'Unknown time')))}. Share details load in the drawer.`)) }}</span>
          </button>
          <button
            class="drive-surface__card-menu"
            type="button"
            :aria-label="`${tr(lt('打开共享设置', '開啟共享設定', 'Open share settings'))}: ${item.name}`"
            @click="openItemMenu(item.id)"
          >
            ...
          </button>
        </article>
        <p v-if="!visibleItems.length" class="drive-surface__empty">{{ tableEmptyCopy }}</p>
      </div>
    </div>

    <aside class="drive-surface__detail">
      <article class="surface-card drive-surface__panel">
        <span class="section-label">{{ tr(lt('详情', '詳情', 'Details')) }}</span>
        <strong>{{ detailTitle }}</strong>
        <p class="page-subtitle">{{ detailCopy }}</p>
        <div v-if="detailFacts.length" class="drive-surface__facts">
          <div v-for="fact in detailFacts" :key="fact.label" class="drive-surface__fact">
            <span class="section-label">{{ fact.label }}</span>
            <strong>{{ fact.value }}</strong>
          </div>
        </div>
      </article>
      <article class="surface-card drive-surface__panel">
        <span class="section-label">{{ tr(lt('共享', '共享', 'Shares')) }}</span>
        <strong>{{ shareHeading }}</strong>
        <p class="page-subtitle">{{ selectedItem ? selectedItem.name : shareEmptyCopy }}</p>
        <button class="drive-share-trigger" type="button" @click="openSharePanel">
          {{ tr(lt('分享设置', '分享設定', 'Share settings')) }}
        </button>
        <div v-if="driveShares.length" class="drive-surface__share-list">
          <div v-for="share in driveShares" :key="share.id" class="drive-surface__share-item">
            <strong>{{ share.permission }}</strong>
            <span>{{ describeShare(share) }}</span>
            <span>{{ tr(lt('创建于', '建立於', 'Created')) }} {{ formatDateTime(share.createdAt, tr(lt('未设置', '未設定', 'Not set'))) }}</span>
          </div>
        </div>
        <p v-else class="page-subtitle">{{ shareEmptyCopy }}</p>
      </article>
    </aside>
    <DriveSharePanel v-model:show="sharePanelOpen" :item="selectedItem" :shares="driveShares" />
  </section>
</template>

<style scoped src="./drive-section-view.css"></style>
<style scoped>
@media (max-width: 820px) {
  .drive-surface__nav {
    overflow-x: auto;
    white-space: nowrap;
  }
}
</style>
