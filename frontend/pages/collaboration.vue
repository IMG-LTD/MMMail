<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { SuiteCollaborationCenter, SuiteCollaborationSync } from '~/types/api'
import CollaborationRealtimePanel from '~/components/collaboration/CollaborationRealtimePanel.vue'
import CollaborationSignalRail from '~/components/collaboration/CollaborationSignalRail.vue'
import CollaborationStreamPanel from '~/components/collaboration/CollaborationStreamPanel.vue'
import { useSuiteApi } from '~/composables/useSuiteApi'
import { useCollaborationSyncStream } from '~/composables/useCollaborationSyncStream'
import { useI18n } from '~/composables/useI18n'
import { useAuthStore } from '~/stores/auth'
import { useOrgAccessStore } from '~/stores/org-access'
import { resolveSessionIdFromAccessToken } from '~/utils/auth-session'
import {
  buildCollaborationCounts,
  COLLABORATION_PRODUCT_LABELS,
  filterCollaborationEvents,
  filterMainlineCollaborationItems,
  formatCollaborationProductList,
  listVisibleCollaborationProductCodes,
  type CollaborationFilter,
  type CollaborationFilterOption,
  type MainlineCollaborationEvent
} from '~/utils/collaboration'
import {
  filterSuiteCollaborationCenterByAccess,
  filterSuiteCollaborationSyncByAccess
} from '~/utils/org-product-surface-filter'

type MainlineCollaborationCenter = Omit<SuiteCollaborationCenter, 'items' | 'productCounts'> & {
  items: MainlineCollaborationEvent[]
  productCounts: Record<string, number>
}

type MainlineCollaborationSync = Omit<SuiteCollaborationSync, 'items'> & {
  items: MainlineCollaborationEvent[]
}

const authStore = useAuthStore()
const orgAccessStore = useOrgAccessStore()
const loading = ref(false)
const productFilter = ref<CollaborationFilter>('ALL')
const collaborationCenter = ref<MainlineCollaborationCenter | null>(null)
const latestSyncEvent = ref<MainlineCollaborationEvent | null>(null)
const currentSessionId = ref('')
const syncCursor = ref(0)
const syncVersion = ref('COLLAB-0')
const lastSyncedAt = ref('')
const syncUpdateCount = ref(0)
const { t } = useI18n()

const { getCollaborationCenter } = useSuiteApi()
const {
  status: syncStatus,
  errorMessage: syncErrorMessage,
  connect: connectSync,
  reconnect: reconnectSync,
  lastCursor
} = useCollaborationSyncStream({
  onPayload: handleSyncPayload
})

const visibleCollaborationCenter = computed(() => normalizeCollaborationCenter(
  filterSuiteCollaborationCenterByAccess(
    collaborationCenter.value as unknown as SuiteCollaborationCenter | null,
    orgAccessStore.isProductEnabled
  ) as MainlineCollaborationCenter | null
))

const filteredItems = computed(() => {
  return filterCollaborationEvents(visibleCollaborationCenter.value?.items ?? [], productFilter.value)
})

const productCounts = computed(() => {
  return visibleCollaborationCenter.value?.productCounts ?? buildCollaborationCounts(filteredItems.value)
})

const visibleProductCodes = computed(() => listVisibleCollaborationProductCodes(orgAccessStore.isProductEnabled))

const filterOptions = computed<CollaborationFilterOption[]>(() => {
  return [
    {
      value: 'ALL',
      label: t('collaboration.filter.all'),
      count: productCounts.value.ALL || 0
    },
    ...visibleProductCodes.value.map((productCode) => ({
      value: productCode,
      label: COLLABORATION_PRODUCT_LABELS[productCode],
      count: productCounts.value[productCode] || 0
    }))
  ]
})

const visibleProductLabelText = computed(() => formatCollaborationProductList(visibleProductCodes.value, 'slash'))
const visibleProductActionText = computed(() => formatCollaborationProductList(visibleProductCodes.value, 'slash'))

const syncStatusLabel = computed(() => {
  if (syncStatus.value === 'CONNECTED') return t('collaboration.sync.connected')
  if (syncStatus.value === 'RECONNECTING') return t('collaboration.sync.reconnecting')
  if (syncStatus.value === 'CONNECTING') return t('collaboration.sync.connecting')
  if (syncStatus.value === 'ERROR') return t('collaboration.sync.error')
  return t('collaboration.sync.idle')
})

const latestSyncSummary = computed(() => {
  if (!latestSyncEvent.value) {
    return t('collaboration.sync.none')
  }
  return `${latestSyncEvent.value.productCode} · ${latestSyncEvent.value.title}`
})

const heroCopy = computed(() => {
  if (visibleProductCodes.value.length === 0) {
    return t('collaboration.hero.noProducts')
  }
  return t('collaboration.hero.summary', { products: visibleProductLabelText.value })
})

const contextDescription = computed(() => {
  if (visibleProductCodes.value.length === 0) {
    return t('collaboration.context.none')
  }
  if (productFilter.value === 'ALL') {
    return t('collaboration.context.unified', { products: visibleProductLabelText.value })
  }
  return t(`collaboration.context.${productFilter.value.toLowerCase()}`)
})

const emptyStateDescription = computed(() => {
  if (visibleProductCodes.value.length === 0) {
    return t('collaboration.empty.noProducts')
  }
  return t('collaboration.empty.other', { products: visibleProductActionText.value })
})

const currentFilterLabel = computed(() => {
  if (productFilter.value === 'ALL') {
    return t('collaboration.filter.all')
  }
  return COLLABORATION_PRODUCT_LABELS[productFilter.value]
})

const latestEventDescription = computed(() => {
  if (latestSyncEvent.value?.summary) {
    return latestSyncEvent.value.summary
  }
  if (visibleProductCodes.value.length === 0) {
    return t('collaboration.empty.noProducts')
  }
  return t('collaboration.latest.prompt', { products: visibleProductActionText.value })
})

useHead(() => ({
  title: t('page.collaboration.title')
}))

function normalizeCollaborationCenter(center: MainlineCollaborationCenter | null): MainlineCollaborationCenter | null {
  if (!center) {
    return null
  }
  const items = filterMainlineCollaborationItems(center.items)
  return {
    ...center,
    total: items.length,
    productCounts: buildCollaborationCounts(items),
    items
  }
}

function normalizeCollaborationSync(payload: MainlineCollaborationSync): MainlineCollaborationSync {
  const items = filterMainlineCollaborationItems(payload.items)
  return {
    ...payload,
    hasUpdates: items.length > 0,
    total: items.length,
    items
  }
}

function applySyncMetadata(cursor: number, version: string, generatedAt: string): void {
  syncCursor.value = cursor
  syncVersion.value = version
  lastSyncedAt.value = generatedAt
  lastCursor.value = cursor
}

async function loadData(): Promise<void> {
  loading.value = true
  try {
    const center = normalizeCollaborationCenter(
      await getCollaborationCenter(60) as MainlineCollaborationCenter
    )
    collaborationCenter.value = center
    if (center) {
      applySyncMetadata(center.syncCursor, center.syncVersion, center.generatedAt)
    }
  } catch (error) {
    ElMessage.error((error as Error).message || t('collaboration.messages.loadFailed'))
  } finally {
    loading.value = false
  }
}

async function handleSyncPayload(payload: SuiteCollaborationSync): Promise<void> {
  const scopedPayload = normalizeCollaborationSync(
    filterSuiteCollaborationSyncByAccess(payload, orgAccessStore.isProductEnabled) as MainlineCollaborationSync
  )
  applySyncMetadata(scopedPayload.syncCursor, scopedPayload.syncVersion, scopedPayload.generatedAt)
  if (scopedPayload.items.length === 0) {
    return
  }
  latestSyncEvent.value = scopedPayload.items[scopedPayload.items.length - 1]
  syncUpdateCount.value += scopedPayload.items.length
  await loadData()
}

async function openEvent(item: MainlineCollaborationEvent): Promise<void> {
  await navigateTo(item.routePath || '/suite')
}

async function refreshStream(): Promise<void> {
  await loadData()
  reconnectSync()
}

onMounted(async () => {
  currentSessionId.value = resolveSessionIdFromAccessToken(authStore.accessToken || '')
  await loadData()
  void connectSync(syncCursor.value || undefined)
})

watch(
  () => orgAccessStore.activeOrgId,
  (nextOrgId, previousOrgId) => {
    if (nextOrgId === previousOrgId) {
      return
    }
    if (productFilter.value !== 'ALL' && !orgAccessStore.isProductEnabled(productFilter.value)) {
      productFilter.value = 'ALL'
    }
    latestSyncEvent.value = null
    syncUpdateCount.value = 0
    void refreshStream()
  }
)
</script>

<template>
  <section class="collaboration-page mm-card">
    <header class="hero">
      <div>
        <p class="eyebrow">{{ t('collaboration.hero.eyebrow') }}</p>
        <h1>{{ t('collaboration.hero.title') }}</h1>
        <p class="hero-copy">{{ heroCopy }}</p>
      </div>
      <div class="hero-actions">
        <el-tag :type="syncStatus === 'CONNECTED' ? 'success' : syncStatus === 'ERROR' ? 'danger' : 'warning'" effect="dark">
          {{ syncStatusLabel }}
        </el-tag>
        <el-button :loading="loading" @click="refreshStream">{{ t('common.actions.refresh') }}</el-button>
      </div>
    </header>

    <el-alert
      v-if="syncErrorMessage"
      :title="syncErrorMessage"
      type="error"
      show-icon
      :closable="false"
      class="notice"
    />

    <div class="layout-grid">
      <CollaborationSignalRail
        :filter-options="filterOptions"
        :product-filter="productFilter"
        :context-description="contextDescription"
        :sync-version="syncVersion"
        :sync-update-count="syncUpdateCount"
        :last-synced-at="lastSyncedAt"
        @select="productFilter = $event"
      />

      <CollaborationStreamPanel
        :loading="loading"
        :items="filteredItems"
        :sync-cursor="syncCursor"
        :empty-state-description="emptyStateDescription"
        :current-session-id="currentSessionId"
        @open="openEvent"
      />

      <CollaborationRealtimePanel
        :latest-sync-summary="latestSyncSummary"
        :current-filter-label="currentFilterLabel"
        :latest-event-title="latestSyncEvent?.title || t('collaboration.latest.waiting')"
        :latest-event-description="latestEventDescription"
        :sync-status-label="syncStatusLabel"
      />
    </div>
  </section>
</template>

<style scoped>
.collaboration-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 20px;
  background:
    radial-gradient(circle at top left, rgba(20, 184, 166, 0.18), transparent 28%),
    linear-gradient(180deg, #08131d 0%, #0f2130 45%, #10293d 100%);
  color: #ecf6ff;
}

.hero {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.eyebrow {
  margin: 0 0 8px;
  color: #78e3d6;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  font-size: 12px;
}

.hero h1 {
  margin: 0;
  font-size: 34px;
}

.hero-copy {
  max-width: 680px;
  margin: 10px 0 0;
  color: rgba(236, 246, 255, 0.74);
}

.hero-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.notice {
  border: none;
}

.layout-grid {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr) 300px;
  gap: 16px;
}

@media (max-width: 1200px) {
  .layout-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .hero {
    flex-direction: column;
  }

  .hero-actions {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
