<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type {
  CreateSearchPresetRequest,
  LabelItem,
  MailId,
  MailSummary,
  SearchHistoryItem,
  SearchPreset,
  SystemMailFolder,
  UpdateSearchPresetRequest
} from '~/types/api'
import SearchHistoryPanel from '~/components/search/SearchHistoryPanel.vue'
import SearchPresetEditDialog from '~/components/search/SearchPresetEditDialog.vue'
import SearchSavedPresetsPanel from '~/components/search/SearchSavedPresetsPanel.vue'
import SearchWorkspacePanel from '~/components/search/SearchWorkspacePanel.vue'
import { useI18n } from '~/composables/useI18n'
import { useLabelApi } from '~/composables/useLabelApi'
import { useMailApi } from '~/composables/useMailApi'
import { useSearchHistoryApi } from '~/composables/useSearchHistoryApi'
import { useSearchPresetApi } from '~/composables/useSearchPresetApi'
import { useMailStore } from '~/stores/mail'
import type {
  SearchPresetEditorState,
  SearchReadState,
  SearchStarState
} from '~/utils/search-workspace'

definePageMeta({
  layout: 'default'
})

const SEARCH_RESULT_LIMIT = 30

const route = useRoute()
const { t } = useI18n()
const mailStore = useMailStore()
const { fetchSearch, fetchStats, applyAction, applyBatchAction, snoozeUntil, undoSend } = useMailApi()
const { listLabels } = useLabelApi()
const {
  listSearchPresets,
  createSearchPreset,
  useSearchPreset,
  updateSearchPreset,
  pinSearchPreset,
  unpinSearchPreset,
  deleteSearchPreset
} = useSearchPresetApi()
const {
  listSearchHistory,
  deleteSearchHistoryItem,
  clearSearchHistory
} = useSearchHistoryApi()

const loading = ref(false)
const results = ref<MailSummary[]>([])
const keyword = ref('')
const folder = ref<SystemMailFolder | ''>('')
const unread = ref<SearchReadState>('ALL')
const starred = ref<SearchStarState>('ALL')
const dateRange = ref<string[]>([])
const label = ref('')
const labels = ref<LabelItem[]>([])
const saveName = ref('')
const presetLoading = ref(false)
const savedPresets = ref<SearchPreset[]>([])
const historyLoading = ref(false)
const searchHistory = ref<SearchHistoryItem[]>([])
const editDialogVisible = ref(false)
const editingPresetId = ref('')
const editForm = reactive<SearchPresetEditorState>({
  name: '',
  keyword: '',
  folder: '',
  unread: 'ALL',
  starred: 'ALL',
  dateRange: [],
  label: ''
})

useHead(() => ({
  title: t('page.search.title')
}))

onMounted(() => {
  keyword.value = typeof route.query.keyword === 'string' ? route.query.keyword : ''
  void bootstrapPage()
})

async function bootstrapPage(): Promise<void> {
  try {
    const [labelItems] = await Promise.all([
      listLabels(),
      runSearch(),
      refreshSavedPresets()
    ])
    labels.value = labelItems
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, t('search.messages.loadWorkspaceFailed')))
  }
}

function toNullableReadState(value: SearchReadState): boolean | null {
  if (value === 'ALL') {
    return null
  }
  return value === 'UNREAD'
}

function toNullableStarState(value: SearchStarState): boolean | null {
  if (value === 'ALL') {
    return null
  }
  return value === 'STARRED'
}

function buildPresetPayload(name: string): CreateSearchPresetRequest {
  return {
    name,
    keyword: keyword.value || undefined,
    folder: folder.value || undefined,
    unread: toNullableReadState(unread.value),
    starred: toNullableStarState(starred.value),
    from: dateRange.value[0] || undefined,
    to: dateRange.value[1] || undefined,
    label: label.value || undefined
  }
}

function applyPresetFields(preset: SearchPreset): void {
  keyword.value = preset.keyword || ''
  folder.value = preset.folder || ''
  unread.value = preset.unread === null ? 'ALL' : preset.unread ? 'UNREAD' : 'READ'
  starred.value = preset.starred === null ? 'ALL' : preset.starred ? 'STARRED' : 'UNSTARRED'
  label.value = preset.label || ''
  dateRange.value = [preset.from || '', preset.to || ''].filter(Boolean)
}

function syncEditForm(preset: SearchPreset): void {
  editForm.name = preset.name
  editForm.keyword = preset.keyword || ''
  editForm.folder = preset.folder || ''
  editForm.unread = preset.unread === null ? 'ALL' : preset.unread ? 'UNREAD' : 'READ'
  editForm.starred = preset.starred === null ? 'ALL' : preset.starred ? 'STARRED' : 'UNSTARRED'
  editForm.label = preset.label || ''
  editForm.dateRange = [preset.from || '', preset.to || ''].filter(Boolean)
}

async function runSearch(): Promise<void> {
  loading.value = true
  try {
    const [page, stats] = await Promise.all([
      fetchSearch({
        keyword: keyword.value,
        folder: folder.value || undefined,
        unread: toNullableReadState(unread.value),
        starred: toNullableStarState(starred.value),
        from: dateRange.value[0] || undefined,
        to: dateRange.value[1] || undefined,
        label: label.value || undefined,
        page: 1,
        size: SEARCH_RESULT_LIMIT
      }),
      fetchStats()
    ])
    results.value = page.items
    mailStore.updateStats(stats)
    await refreshSearchHistory()
  } finally {
    loading.value = false
  }
}

async function refreshSavedPresets(): Promise<void> {
  presetLoading.value = true
  try {
    savedPresets.value = await listSearchPresets()
  } finally {
    presetLoading.value = false
  }
}

async function refreshSearchHistory(): Promise<void> {
  historyLoading.value = true
  try {
    searchHistory.value = await listSearchHistory()
  } finally {
    historyLoading.value = false
  }
}

function openMail(mailId: MailId): void {
  void navigateTo(`/mail/${mailId}`)
}

async function onAction(mailId: MailId, action: string): Promise<void> {
  const result = await applyAction(mailId, action)
  mailStore.updateStats(result.stats)
  await runSearch()
}

async function onBatchAction(mailIds: MailId[], action: string): Promise<void> {
  const result = await applyBatchAction(mailIds, action)
  mailStore.updateStats(result.stats)
  ElMessage.success(t('search.messages.batchUpdated', { count: result.affected }))
  await runSearch()
}

async function onUndo(mailId: MailId): Promise<void> {
  try {
    await undoSend(mailId)
    ElMessage.success(t('mailbox.messages.undoSuccess'))
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, t('mailbox.messages.undoFailed')))
  } finally {
    await runSearch()
  }
}

async function onCustomSnooze(mailId: MailId): Promise<void> {
  const untilAt = await requestCustomSnoozeTime()
  if (!untilAt) {
    return
  }
  try {
    const result = await snoozeUntil(mailId, untilAt)
    mailStore.updateStats(result.stats)
    ElMessage.success(t('search.messages.snoozedUntil', { time: untilAt }))
    await runSearch()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, t('search.messages.customSnoozeFailed')))
  }
}

async function requestCustomSnoozeTime(): Promise<string> {
  try {
    const prompt = await ElMessageBox.prompt(
      t('search.prompts.customSnoozeMessage'),
      t('search.prompts.customSnoozeTitle'),
      {
        confirmButtonText: t('search.prompts.customSnoozeConfirm'),
        cancelButtonText: t('common.actions.cancel'),
        inputPlaceholder: t('search.prompts.customSnoozePlaceholder')
      }
    )
    return prompt.value.trim()
  } catch {
    return ''
  }
}

async function saveCurrentSearch(): Promise<void> {
  const name = saveName.value.trim()
  if (!name) {
    ElMessage.warning(t('search.messages.saveNameRequired'))
    return
  }
  await createSearchPreset(buildPresetPayload(name))
  saveName.value = ''
  ElMessage.success(t('search.messages.presetCreated'))
  await refreshSavedPresets()
}

async function applySavedPreset(presetId: string): Promise<void> {
  const preset = await useSearchPreset(presetId)
  applyPresetFields(preset)
  await runSearch()
  await refreshSavedPresets()
  ElMessage.success(t('search.messages.presetApplied'))
}

function openEditDialog(item: SearchPreset): void {
  editingPresetId.value = item.id
  syncEditForm(item)
  editDialogVisible.value = true
}

async function submitPresetEdit(): Promise<void> {
  if (!editingPresetId.value) {
    return
  }
  if (!editForm.name.trim()) {
    ElMessage.warning(t('search.messages.presetNameRequired'))
    return
  }
  const payload: UpdateSearchPresetRequest = {
    name: editForm.name.trim(),
    keyword: editForm.keyword || undefined,
    folder: editForm.folder || undefined,
    unread: toNullableReadState(editForm.unread),
    starred: toNullableStarState(editForm.starred),
    from: editForm.dateRange[0] || undefined,
    to: editForm.dateRange[1] || undefined,
    label: editForm.label || undefined
  }
  const updated = await updateSearchPreset(editingPresetId.value, payload)
  applyPresetFields(updated)
  editDialogVisible.value = false
  ElMessage.success(t('search.messages.presetUpdated'))
  await Promise.all([runSearch(), refreshSavedPresets()])
}

async function togglePinSavedPreset(item: SearchPreset): Promise<void> {
  if (item.isPinned) {
    await unpinSearchPreset(item.id)
    ElMessage.success(t('search.messages.presetUnpinned'))
  } else {
    await pinSearchPreset(item.id)
    ElMessage.success(t('search.messages.presetPinned'))
  }
  await refreshSavedPresets()
}

async function removeSavedPreset(presetId: string): Promise<void> {
  try {
    await ElMessageBox.confirm(
      t('search.prompts.deletePresetMessage'),
      t('search.prompts.deletePresetTitle'),
      {
        confirmButtonText: t('common.actions.delete'),
        cancelButtonText: t('common.actions.cancel'),
        type: 'warning'
      }
    )
  } catch {
    return
  }
  await deleteSearchPreset(presetId)
  ElMessage.success(t('search.messages.presetRemoved'))
  await refreshSavedPresets()
}

async function applyHistoryKeyword(historyKeyword: string): Promise<void> {
  keyword.value = historyKeyword
  await runSearch()
}

async function removeHistoryItem(historyId: string): Promise<void> {
  await deleteSearchHistoryItem(historyId)
  ElMessage.success(t('search.messages.historyRemoved'))
  await refreshSearchHistory()
}

async function clearHistory(): Promise<void> {
  if (searchHistory.value.length === 0) {
    return
  }
  try {
    await ElMessageBox.confirm(
      t('search.prompts.clearHistoryMessage'),
      t('search.prompts.clearHistoryTitle'),
      {
        confirmButtonText: t('common.actions.clear'),
        cancelButtonText: t('common.actions.cancel'),
        type: 'warning'
      }
    )
  } catch {
    return
  }
  await clearSearchHistory()
  ElMessage.success(t('search.messages.historyCleared'))
  await refreshSearchHistory()
}

function resolveErrorMessage(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback
}
</script>

<template>
  <div class="search-page">
    <SearchWorkspacePanel
      v-model:keyword="keyword"
      v-model:folder="folder"
      v-model:date-range="dateRange"
      v-model:label="label"
      v-model:unread="unread"
      v-model:starred="starred"
      v-model:save-name="saveName"
      :labels="labels"
      @search="runSearch"
      @save="saveCurrentSearch"
    />

    <SearchHistoryPanel
      :items="searchHistory"
      :loading="historyLoading"
      @apply="applyHistoryKeyword"
      @remove="removeHistoryItem"
      @clear="clearHistory"
    />

    <SearchSavedPresetsPanel
      :items="savedPresets"
      :loading="presetLoading"
      @apply="applySavedPreset"
      @edit="openEditDialog"
      @toggle-pin="togglePinSavedPreset"
      @remove="removeSavedPreset"
    />

    <MailList
      :title="t('search.results.title')"
      :mails="results"
      :loading="loading"
      @open="openMail"
      @action="onAction"
      @batch-action="onBatchAction"
      @undo="onUndo"
      @custom-snooze="onCustomSnooze"
    />

    <SearchPresetEditDialog
      v-model:visible="editDialogVisible"
      v-model:form="editForm"
      :labels="labels"
      @save="submitPresetEdit"
    />
  </div>
</template>

<style scoped>
.search-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
