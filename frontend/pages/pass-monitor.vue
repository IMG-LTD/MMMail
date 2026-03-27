<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { AuthenticatorCodePayload, OrgWorkspace } from '~/types/api'
import type {
  PassMonitorItem,
  PassMonitorOverview,
  PassWorkspaceMode,
  PassWorkspaceItemDetail,
  UpsertPassItemTwoFactorRequest
} from '~/types/pass-business'
import { useI18n } from '~/composables/useI18n'
import { useOrganizationApi } from '~/composables/useOrganizationApi'
import { usePassApi } from '~/composables/usePassApi'
import {
  buildPassMonitorMetricCards,
  buildPassMonitorSections,
  resolveDefaultMonitorOrgId
} from '~/utils/pass-monitor'

const { t } = useI18n()
const { listOrganizations } = useOrganizationApi()
const {
  getPersonalMonitor,
  getSharedMonitor,
  excludePersonalMonitorItem,
  includePersonalMonitorItem,
  excludeSharedMonitorItem,
  includeSharedMonitorItem,
  upsertPersonalItemTwoFactor,
  deletePersonalItemTwoFactor,
  generatePersonalItemTwoFactorCode,
  upsertSharedItemTwoFactor,
  deleteSharedItemTwoFactor,
  generateSharedItemTwoFactorCode
} = usePassApi()

const workspaceMode = ref<PassWorkspaceMode>('PERSONAL')
const organizations = ref<OrgWorkspace[]>([])
const selectedOrgId = ref('')
const overview = ref<PassMonitorOverview | null>(null)
const loading = ref(false)
const mutationItemId = ref('')
const twoFactorDialogVisible = ref(false)
const activeTwoFactorItem = ref<PassMonitorItem | null>(null)
const twoFactorSaving = ref(false)
const twoFactorRemoving = ref(false)
const twoFactorCodeLoading = ref(false)
const twoFactorCode = ref<AuthenticatorCodePayload | null>(null)

const metricCards = computed(() => buildPassMonitorMetricCards(overview.value, t))
const sections = computed(() => buildPassMonitorSections(overview.value, t))
const noSharedOrganizations = computed(() => workspaceMode.value === 'SHARED' && organizations.value.length === 0)
const missingSharedOrgSelection = computed(() => workspaceMode.value === 'SHARED' && !selectedOrgId.value)

useHead(() => ({
  title: t('pass.monitor.nav')
}))

onMounted(async () => {
  await loadOrganizations()
  await loadMonitor()
})

watch(twoFactorDialogVisible, (visible) => {
  if (visible) {
    return
  }
  activeTwoFactorItem.value = null
  twoFactorCode.value = null
})

async function loadOrganizations(): Promise<void> {
  organizations.value = await listOrganizations()
  selectedOrgId.value = resolveDefaultMonitorOrgId(
    organizations.value.map(item => item.id),
    selectedOrgId.value
  )
}

async function loadMonitor(): Promise<void> {
  if (workspaceMode.value === 'SHARED' && !selectedOrgId.value) {
    overview.value = null
    return
  }
  loading.value = true
  try {
    overview.value = workspaceMode.value === 'PERSONAL'
      ? await getPersonalMonitor()
      : await getSharedMonitor(selectedOrgId.value)
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, 'pass.monitor.messages.loadFailed'))
  } finally {
    loading.value = false
  }
}

async function handleWorkspaceModeChange(mode: PassWorkspaceMode): Promise<void> {
  workspaceMode.value = mode
  if (mode === 'SHARED') {
    selectedOrgId.value = resolveDefaultMonitorOrgId(
      organizations.value.map(item => item.id),
      selectedOrgId.value
    )
  }
  await loadMonitor()
}

async function handleOrganizationChange(orgId: string): Promise<void> {
  selectedOrgId.value = orgId
  await loadMonitor()
}

async function toggleMonitorItem(item: PassMonitorItem): Promise<void> {
  mutationItemId.value = item.id
  try {
    if (workspaceMode.value === 'PERSONAL') {
      await togglePersonalItem(item)
    } else {
      await toggleSharedItem(item)
    }
    ElMessage.success(item.excluded ? t('pass.monitor.messages.includeSuccess') : t('pass.monitor.messages.excludeSuccess'))
    await loadMonitor()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, 'pass.monitor.messages.updateFailed'))
  } finally {
    mutationItemId.value = ''
  }
}

async function togglePersonalItem(item: PassMonitorItem): Promise<void> {
  if (item.excluded) {
    await includePersonalMonitorItem(item.id)
    return
  }
  await excludePersonalMonitorItem(item.id)
}

async function toggleSharedItem(item: PassMonitorItem): Promise<void> {
  if (!selectedOrgId.value) {
    throw new Error(t('pass.monitor.messages.orgRequired'))
  }
  if (item.excluded) {
    await includeSharedMonitorItem(selectedOrgId.value, item.id)
    return
  }
  await excludeSharedMonitorItem(selectedOrgId.value, item.id)
}

function openTwoFactorDialog(item: PassMonitorItem): void {
  activeTwoFactorItem.value = {
    ...item,
    twoFactor: { ...item.twoFactor }
  }
  twoFactorCode.value = null
  twoFactorDialogVisible.value = true
}

async function saveTwoFactor(payload: UpsertPassItemTwoFactorRequest): Promise<void> {
  const item = requireActiveTwoFactorItem()
  twoFactorSaving.value = true
  try {
    const detail = item.scopeType === 'PERSONAL'
      ? await upsertPersonalItemTwoFactor(item.id, payload)
      : await upsertSharedItemTwoFactor(resolveSharedOrgId(item), item.id, payload)
    syncActiveTwoFactorItem(detail)
    ElMessage.success(t('pass.monitor.messages.twoFactorSaved'))
    await loadMonitor()
    await generateTwoFactorCode()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, 'pass.monitor.messages.twoFactorSaveFailed'))
  } finally {
    twoFactorSaving.value = false
  }
}

async function removeTwoFactor(): Promise<void> {
  const item = requireActiveTwoFactorItem()
  twoFactorRemoving.value = true
  try {
    const detail = item.scopeType === 'PERSONAL'
      ? await deletePersonalItemTwoFactor(item.id)
      : await deleteSharedItemTwoFactor(resolveSharedOrgId(item), item.id)
    syncActiveTwoFactorItem(detail)
    twoFactorCode.value = null
    ElMessage.success(t('pass.monitor.messages.twoFactorRemoved'))
    await loadMonitor()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, 'pass.monitor.messages.twoFactorRemoveFailed'))
  } finally {
    twoFactorRemoving.value = false
  }
}

async function generateTwoFactorCode(): Promise<void> {
  const item = requireActiveTwoFactorItem()
  if (!item.twoFactor.enabled) {
    return
  }
  twoFactorCodeLoading.value = true
  try {
    twoFactorCode.value = item.scopeType === 'PERSONAL'
      ? await generatePersonalItemTwoFactorCode(item.id)
      : await generateSharedItemTwoFactorCode(resolveSharedOrgId(item), item.id)
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, 'pass.monitor.messages.twoFactorCodeFailed'))
  } finally {
    twoFactorCodeLoading.value = false
  }
}

function requireActiveTwoFactorItem(): PassMonitorItem {
  if (!activeTwoFactorItem.value) {
    throw new Error(t('pass.monitor.messages.loadFailed'))
  }
  return activeTwoFactorItem.value
}

function resolveSharedOrgId(item: PassMonitorItem): string {
  if (item.orgId) {
    return item.orgId
  }
  if (selectedOrgId.value) {
    return selectedOrgId.value
  }
  throw new Error(t('pass.monitor.messages.orgRequired'))
}

function syncActiveTwoFactorItem(detail: PassWorkspaceItemDetail): void {
  if (!activeTwoFactorItem.value) {
    return
  }
  activeTwoFactorItem.value = {
    ...activeTwoFactorItem.value,
    title: detail.title,
    website: detail.website,
    username: detail.username,
    itemType: detail.itemType,
    updatedAt: detail.updatedAt,
    inactiveTwoFactor: !detail.twoFactor.enabled,
    twoFactor: detail.twoFactor
  }
}

function resolveErrorMessage(error: unknown, fallbackKey: string): string {
  return error instanceof Error && error.message ? error.message : t(fallbackKey)
}
</script>

<template>
  <div class="pass-monitor-page">
    <PassMonitorHero
      :workspace-mode="workspaceMode"
      :organizations="organizations"
      :selected-org-id="selectedOrgId"
      :overview="overview"
      :metric-cards="metricCards"
      :loading="loading"
      @update:workspace-mode="handleWorkspaceModeChange"
      @update:selected-org-id="handleOrganizationChange"
      @refresh="loadMonitor"
    />

    <el-alert
      v-if="noSharedOrganizations"
      type="info"
      :closable="false"
      :title="t('pass.monitor.messages.noOrganizations')"
    />

    <el-alert
      v-else-if="missingSharedOrgSelection"
      type="warning"
      :closable="false"
      :title="t('pass.monitor.messages.orgRequired')"
    />

    <el-alert
      v-else-if="workspaceMode === 'SHARED'"
      type="info"
      :closable="false"
      :title="t('pass.monitor.notes.sharedScope')"
    />

    <div class="monitor-grid">
      <PassMonitorSectionPanel
        v-for="section in sections"
        :key="section.key"
        :section="section"
        :loading-item-id="mutationItemId"
        @toggle="toggleMonitorItem"
        @manage-two-factor="openTwoFactorDialog"
      />
    </div>

    <PassMonitorTwoFactorDialog
      v-model="twoFactorDialogVisible"
      :item="activeTwoFactorItem"
      :saving="twoFactorSaving"
      :removing="twoFactorRemoving"
      :code-loading="twoFactorCodeLoading"
      :code="twoFactorCode"
      @save="saveTwoFactor"
      @remove="removeTwoFactor"
      @generate="generateTwoFactorCode"
    />
  </div>
</template>

<style scoped>
.pass-monitor-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 24px;
  background:
    radial-gradient(circle at top, rgba(120, 101, 255, 0.08), transparent 28%),
    linear-gradient(180deg, #f4f7ff 0%, #eef3fb 100%);
  min-height: 100vh;
}

.monitor-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

@media (max-width: 1200px) {
  .monitor-grid {
    grid-template-columns: 1fr;
  }
}
</style>
