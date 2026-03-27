<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type {
  SuiteCollaborationCenter,
  SuiteCollaborationEvent,
  SuiteCollaborationProductCode,
  SuiteCollaborationSync
} from '~/types/api'
import { useSuiteApi } from '~/composables/useSuiteApi'
import { useCollaborationSyncStream } from '~/composables/useCollaborationSyncStream'
import { useI18n } from '~/composables/useI18n'
import { useAuthStore } from '~/stores/auth'
import { useOrgAccessStore } from '~/stores/org-access'
import { resolveSessionIdFromAccessToken } from '~/utils/auth-session'
import {
  COLLABORATION_PRODUCT_LABELS,
  buildCollaborationCounts,
  filterCollaborationEvents,
  formatCollaborationProductList,
  isExternalCollaborationEvent,
  listVisibleCollaborationProductCodes
} from '~/utils/collaboration'
import {
  filterSuiteCollaborationCenterByAccess,
  filterSuiteCollaborationSyncByAccess
} from '~/utils/org-product-surface-filter'

const authStore = useAuthStore()
const orgAccessStore = useOrgAccessStore()
const loading = ref(false)
const productFilter = ref<'ALL' | SuiteCollaborationProductCode>('ALL')
const collaborationCenter = ref<SuiteCollaborationCenter | null>(null)
const latestSyncEvent = ref<SuiteCollaborationEvent | null>(null)
const currentSessionId = ref('')
const syncCursor = ref(0)
const syncVersion = ref('COLLAB-0')
const lastSyncedAt = ref('')
const syncUpdateCount = ref(0)
const externalSyncNotice = ref('')
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

const visibleCollaborationCenter = computed(() => filterSuiteCollaborationCenterByAccess(
  collaborationCenter.value,
  orgAccessStore.isProductEnabled
))

const filteredItems = computed(() => {
  return filterCollaborationEvents(visibleCollaborationCenter.value?.items ?? [], productFilter.value)
})

const productCounts = computed(() => {
  return visibleCollaborationCenter.value?.productCounts ?? buildCollaborationCounts(visibleCollaborationCenter.value?.items ?? [])
})

const visibleProductCodes = computed(() => listVisibleCollaborationProductCodes(orgAccessStore.isProductEnabled))

const visibleProductLabelText = computed(() => formatCollaborationProductList(visibleProductCodes.value, 'slash'))

const visibleProductActionText = computed(() => formatCollaborationProductList(visibleProductCodes.value, 'slash'))

const filterOptions = computed(() => [
  {
    value: 'ALL' as const,
    label: t('collaboration.filter.all'),
    count: productCounts.value.ALL || 0
  },
  ...visibleProductCodes.value.map((productCode) => ({
    value: productCode,
    label: COLLABORATION_PRODUCT_LABELS[productCode],
    count: productCounts.value[productCode] || 0
  }))
])

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
  const source = isExternalSession(latestSyncEvent.value)
    ? t('collaboration.sync.otherSession')
    : t('collaboration.sync.thisSession')
  return `${latestSyncEvent.value.productCode} · ${latestSyncEvent.value.title} · ${source}`
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
  if (productFilter.value === 'DOCS') {
    return t('collaboration.context.docs')
  }
  if (productFilter.value === 'DRIVE') {
    return t('collaboration.context.drive')
  }
  if (productFilter.value === 'SHEETS') {
    return t('collaboration.context.sheets')
  }
  return t('collaboration.context.meet')
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

async function loadData(): Promise<void> {
  loading.value = true
  try {
    const center = await getCollaborationCenter(60)
    collaborationCenter.value = center
    applySyncMetadata(center.syncCursor, center.syncVersion, center.generatedAt)
  } catch (error) {
    ElMessage.error((error as Error).message || t('collaboration.messages.loadFailed'))
  } finally {
    loading.value = false
  }
}

function applySyncMetadata(cursor: number, version: string, generatedAt: string): void {
  syncCursor.value = cursor
  syncVersion.value = version
  lastSyncedAt.value = generatedAt
  lastCursor.value = cursor
}

function formatDateTime(value: string | null): string {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 19)
}

function productTagType(productCode: SuiteCollaborationProductCode): 'success' | 'warning' | 'primary' {
  if (productCode === 'DOCS') {
    return 'success'
  }
  if (productCode === 'DRIVE') {
    return 'warning'
  }
  return 'primary'
}

function isExternalSession(item: SuiteCollaborationEvent): boolean {
  return isExternalCollaborationEvent(item, currentSessionId.value)
}

async function handleSyncPayload(payload: SuiteCollaborationSync): Promise<void> {
  applySyncMetadata(payload.syncCursor, payload.syncVersion, payload.generatedAt)
  const scopedPayload = filterSuiteCollaborationSyncByAccess(payload, orgAccessStore.isProductEnabled)
  if (scopedPayload.items.length === 0) {
    return
  }
  latestSyncEvent.value = scopedPayload.items[scopedPayload.items.length - 1]
  syncUpdateCount.value += scopedPayload.items.length
  if (scopedPayload.items.some((item) => isExternalSession(item))) {
    externalSyncNotice.value = t('collaboration.sync.externalNotice')
  }
  await loadData()
}

async function openEvent(item: SuiteCollaborationEvent): Promise<void> {
  await navigateTo(item.routePath || '/suite')
}

async function refreshStream(): Promise<void> {
  externalSyncNotice.value = ''
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
    externalSyncNotice.value = ''
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
      v-if="externalSyncNotice"
      :title="externalSyncNotice"
      type="warning"
      show-icon
      :closable="false"
      class="notice"
    />

    <el-alert
      v-else-if="syncErrorMessage"
      :title="syncErrorMessage"
      type="error"
      show-icon
      :closable="false"
      class="notice"
    />

    <div class="layout-grid">
      <aside class="signal-rail mm-card">
        <div class="rail-header">
          <h2>{{ t('collaboration.signals.title') }}</h2>
          <p>{{ contextDescription }}</p>
        </div>

        <div class="filter-list">
          <button
            v-for="option in filterOptions"
            :key="option.value"
            class="filter-chip"
            :class="{ active: productFilter === option.value }"
            @click="productFilter = option.value"
          >
            <span>{{ option.label }}</span>
            <strong>{{ option.count }}</strong>
          </button>
        </div>

        <div class="rail-stats">
          <div class="stat-card">
            <span>{{ t('collaboration.signals.syncVersion') }}</span>
            <strong>{{ syncVersion }}</strong>
          </div>
          <div class="stat-card">
            <span>{{ t('collaboration.signals.updatedEvents') }}</span>
            <strong>{{ syncUpdateCount }}</strong>
          </div>
          <div class="stat-card">
            <span>{{ t('collaboration.signals.lastSynced') }}</span>
            <strong>{{ formatDateTime(lastSyncedAt) }}</strong>
          </div>
        </div>
      </aside>

      <main class="stream-panel mm-card">
        <div class="panel-header">
          <div>
            <h2>{{ t('collaboration.stream.title') }}</h2>
            <p>{{ t('collaboration.stream.visibleCount', { count: filteredItems.length }) }}</p>
          </div>
          <el-tag type="info" effect="plain">{{ t('collaboration.stream.cursor', { cursor: syncCursor }) }}</el-tag>
        </div>

        <div v-if="loading" class="stream-loading">
          <el-skeleton :rows="6" animated />
        </div>

        <div v-else-if="filteredItems.length === 0" class="empty-state">
          <h3>{{ t('collaboration.stream.emptyTitle') }}</h3>
          <p>{{ emptyStateDescription }}</p>
        </div>

        <div v-else class="stream-list">
          <button
            v-for="item in filteredItems"
            :key="`${item.eventId}-${item.productCode}`"
            class="stream-card"
            @click="openEvent(item)"
          >
            <div class="card-topline">
              <el-tag :type="productTagType(item.productCode)" effect="dark">{{ item.productCode }}</el-tag>
              <span class="event-type">{{ item.eventType }}</span>
              <span class="event-time">{{ formatDateTime(item.createdAt) }}</span>
            </div>
            <div class="card-content">
              <h3>{{ item.title }}</h3>
              <p>{{ item.summary }}</p>
            </div>
            <div class="card-footer">
              <span>{{ item.actorEmail || t('collaboration.stream.unknownActor') }}</span>
              <span v-if="isExternalSession(item)" class="external-pill">{{ t('collaboration.stream.otherSession') }}</span>
              <span v-else class="route-pill">{{ t('collaboration.stream.openProduct') }}</span>
            </div>
          </button>
        </div>
      </main>

      <aside class="context-panel mm-card">
        <div class="panel-header compact">
          <div>
            <h2>{{ t('collaboration.realtime.title') }}</h2>
            <p>{{ latestSyncSummary }}</p>
          </div>
        </div>

        <div class="context-block">
          <span class="context-label">{{ t('collaboration.realtime.currentFilter') }}</span>
          <strong>{{ currentFilterLabel }}</strong>
        </div>
        <div class="context-block">
          <span class="context-label">{{ t('collaboration.realtime.latestEvent') }}</span>
          <strong>{{ latestSyncEvent?.title || t('collaboration.latest.waiting') }}</strong>
          <p>{{ latestEventDescription }}</p>
        </div>
        <div class="context-block">
          <span class="context-label">{{ t('collaboration.realtime.mode') }}</span>
          <strong>{{ syncStatusLabel }}</strong>
          <p>{{ t('collaboration.realtime.description') }}</p>
        </div>
      </aside>
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

.signal-rail,
.stream-panel,
.context-panel {
  background: rgba(6, 18, 31, 0.78);
  border: 1px solid rgba(120, 227, 214, 0.12);
  box-shadow: 0 16px 36px rgba(2, 8, 15, 0.35);
}

.signal-rail,
.context-panel,
.stream-panel {
  padding: 18px;
}

.rail-header h2,
.panel-header h2 {
  margin: 0;
  font-size: 20px;
}

.rail-header p,
.panel-header p,
.context-block p {
  margin: 8px 0 0;
  color: rgba(236, 246, 255, 0.7);
  line-height: 1.5;
}

.filter-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 18px;
}

.filter-chip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid rgba(120, 227, 214, 0.14);
  background: rgba(15, 37, 53, 0.82);
  color: #e8f7ff;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}

.filter-chip.active,
.filter-chip:hover {
  transform: translateY(-1px);
  border-color: rgba(120, 227, 214, 0.42);
  background: rgba(19, 57, 77, 0.96);
}

.rail-stats {
  display: grid;
  gap: 12px;
  margin-top: 18px;
}

.stat-card,
.context-block {
  padding: 14px;
  border-radius: 16px;
  background: rgba(13, 33, 47, 0.82);
  border: 1px solid rgba(120, 227, 214, 0.1);
}

.stat-card span,
.context-label {
  display: block;
  color: rgba(236, 246, 255, 0.62);
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.12em;
}

.stat-card strong,
.context-block strong {
  display: block;
  margin-top: 8px;
  font-size: 18px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.panel-header.compact {
  margin-bottom: 14px;
}

.stream-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.stream-card {
  width: 100%;
  text-align: left;
  padding: 16px;
  border-radius: 18px;
  border: 1px solid rgba(120, 227, 214, 0.12);
  background: linear-gradient(180deg, rgba(17, 41, 58, 0.96), rgba(11, 28, 42, 0.96));
  color: inherit;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.stream-card:hover {
  transform: translateY(-2px);
  border-color: rgba(120, 227, 214, 0.34);
  box-shadow: 0 18px 32px rgba(2, 8, 15, 0.28);
}

.card-topline,
.card-footer {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.event-type,
.event-time,
.route-pill,
.external-pill {
  color: rgba(236, 246, 255, 0.66);
  font-size: 12px;
}

.card-content h3 {
  margin: 14px 0 8px;
  font-size: 18px;
}

.card-content p {
  margin: 0 0 14px;
  color: rgba(236, 246, 255, 0.76);
  line-height: 1.6;
}

.route-pill,
.external-pill {
  margin-left: auto;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(120, 227, 214, 0.12);
}

.external-pill {
  background: rgba(250, 204, 21, 0.15);
  color: #fde68a;
}

.empty-state {
  display: grid;
  place-items: center;
  min-height: 320px;
  text-align: center;
  border-radius: 18px;
  border: 1px dashed rgba(120, 227, 214, 0.2);
  background: rgba(12, 28, 42, 0.72);
}

.empty-state h3 {
  margin: 0 0 10px;
}

.empty-state p {
  max-width: 420px;
  margin: 0;
  color: rgba(236, 246, 255, 0.72);
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
