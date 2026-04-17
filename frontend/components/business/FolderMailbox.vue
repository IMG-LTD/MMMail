<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { MailId, MailPage, MailTriageFilters, SystemMailFolder } from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import { useMailApi } from '~/composables/useMailApi'
import { useMailFolderApi } from '~/composables/useMailFolderApi'
import { useMailStore } from '~/stores/mail'

const DEFAULT_PAGE = 1
const DEFAULT_PAGE_SIZE = 20
const PAGE_SIZES = [20, 50, 100]

const props = withDefaults(defineProps<{
  titleKey: string
  folder: Lowercase<SystemMailFolder>
  showTrashBulkActions?: boolean
  showSpamBulkActions?: boolean
}>(), {
  showTrashBulkActions: false,
  showSpamBulkActions: false
})

const loading = ref(false)
const keyword = ref('')
const currentPage = ref(DEFAULT_PAGE)
const pageSize = ref(DEFAULT_PAGE_SIZE)
const mailPage = ref<MailPage>({
  items: [],
  total: 0,
  page: DEFAULT_PAGE,
  size: DEFAULT_PAGE_SIZE,
  unread: 0
})
const loadError = ref('')
const triageFilters = reactive<MailTriageFilters>({
  unread: false,
  needsReply: false,
  starred: false,
  hasAttachments: false,
  importantContact: false
})

const mailStore = useMailStore()
const { t } = useI18n()
const { listMailFolders } = useMailFolderApi()
const {
  fetchFolder,
  fetchStats,
  applyAction,
  applyBatchAction,
  undoSend,
  restoreAllTrash,
  emptyTrash,
  restoreAllSpam,
  emptySpam,
  snoozeUntil
} = useMailApi()

const actionSuccessKeyMap: Record<string, string> = {
  REPORT_PHISHING: 'mailbox.messages.reportPhishingSuccess',
  REPORT_NOT_PHISHING: 'mailbox.messages.reportNotPhishingSuccess',
  BLOCK_SENDER: 'mailbox.messages.blockSenderSuccess',
  TRUST_SENDER: 'mailbox.messages.trustSenderSuccess',
  BLOCK_DOMAIN: 'mailbox.messages.blockDomainSuccess',
  TRUST_DOMAIN: 'mailbox.messages.trustDomainSuccess'
}

const folderMap: Record<Lowercase<SystemMailFolder>, SystemMailFolder> = {
  inbox: 'INBOX',
  sent: 'SENT',
  drafts: 'DRAFTS',
  outbox: 'OUTBOX',
  scheduled: 'SCHEDULED',
  snoozed: 'SNOOZED',
  archive: 'ARCHIVE',
  spam: 'SPAM',
  trash: 'TRASH'
}

function currentInboxTriageFilters(): MailTriageFilters {
  if (props.folder !== 'inbox') {
    return {}
  }
  return {
    ...(triageFilters.unread ? { unread: true } : {}),
    ...(triageFilters.needsReply ? { needsReply: true } : {}),
    ...(triageFilters.starred ? { starred: true } : {}),
    ...(triageFilters.hasAttachments ? { hasAttachments: true } : {}),
    ...(triageFilters.importantContact ? { importantContact: true } : {})
  }
}

async function loadFolder(targetPage = currentPage.value, targetSize = pageSize.value): Promise<void> {
  loading.value = true
  loadError.value = ''
  try {
    const [page, stats, customFolders] = await Promise.all([
      fetchFolder(props.folder, targetPage, targetSize, keyword.value, currentInboxTriageFilters()),
      fetchStats(),
      listMailFolders()
    ])
    currentPage.value = page.page
    pageSize.value = page.size
    mailPage.value = page
    mailStore.setFolder(folderMap[props.folder], page)
    mailStore.updateStats(stats)
    mailStore.setCustomFolders(customFolders)
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to load mailbox'
    loadError.value = message
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

function openMail(mailId: MailId): void {
  void navigateTo(`/mail/${mailId}`)
}

async function onAction(mailId: MailId, action: string): Promise<void> {
  const result = await applyAction(mailId, action)
  mailStore.updateStats(result.stats)
  const successKey = actionSuccessKeyMap[action]
  if (successKey) {
    ElMessage.success(t(successKey))
  }
  await loadFolder()
}

async function onBatchAction(mailIds: MailId[], action: string): Promise<void> {
  const result = await applyBatchAction(mailIds, action)
  mailStore.updateStats(result.stats)
  ElMessage.success(t('mailbox.messages.updated', { count: result.affected }))
  await loadFolder()
}

async function onUndo(mailId: MailId): Promise<void> {
  try {
    await undoSend(mailId)
    ElMessage.success(t('mailbox.messages.undoSuccess'))
  } catch (error) {
    const message = error instanceof Error ? error.message : t('mailbox.messages.undoFailed')
    ElMessage.error(message)
  } finally {
    await loadFolder()
  }
}

async function onCustomSnooze(mailId: MailId): Promise<void> {
  let untilAt = ''
  try {
    const prompt = await ElMessageBox.prompt(
      t('mailbox.customSnooze.message'),
      t('mailbox.customSnooze.title'),
      {
        confirmButtonText: t('mailbox.customSnooze.confirm'),
        cancelButtonText: t('mailbox.customSnooze.cancel'),
        inputPlaceholder: t('mailbox.customSnooze.placeholder')
      }
    )
    untilAt = prompt.value.trim()
    if (!untilAt) {
      return
    }
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    ElMessage.error(t('mailbox.messages.customSnoozeCancelled'))
    return
  }

  try {
    const result = await snoozeUntil(mailId, untilAt)
    mailStore.updateStats(result.stats)
    ElMessage.success(t('mailbox.messages.customSnoozeSuccess', { untilAt }))
    await loadFolder()
  } catch (error) {
    const message = error instanceof Error ? error.message : t('mailbox.messages.customSnoozeFailed')
    ElMessage.error(message)
  }
}

async function onRestoreAllTrash(): Promise<void> {
  const result = await restoreAllTrash()
  mailStore.updateStats(result.stats)
  ElMessage.success(t('mailbox.messages.restoreAllSuccess', { count: result.affected }))
  await loadFolder()
}

async function onEmptyTrash(): Promise<void> {
  try {
    await ElMessageBox.confirm(t('mailbox.confirm.emptyMessage'), t('mailbox.confirm.emptyTitle'), {
      confirmButtonText: t('mailbox.confirm.emptyTrash'),
      cancelButtonText: t('common.actions.cancel'),
      type: 'warning'
    })
  } catch {
    return
  }

  const result = await emptyTrash()
  mailStore.updateStats(result.stats)
  ElMessage.success(t('mailbox.messages.emptyAllSuccess', { count: result.affected }))
  await loadFolder()
}

async function onRestoreAllSpam(): Promise<void> {
  const result = await restoreAllSpam()
  mailStore.updateStats(result.stats)
  ElMessage.success(t('mailbox.messages.restoreAllSuccess', { count: result.affected }))
  await loadFolder()
}

async function onEmptySpam(): Promise<void> {
  try {
    await ElMessageBox.confirm(t('mailbox.confirm.emptyMessage'), t('mailbox.confirm.emptyTitle'), {
      confirmButtonText: t('mailbox.confirm.emptySpam'),
      cancelButtonText: t('common.actions.cancel'),
      type: 'warning'
    })
  } catch {
    return
  }

  const result = await emptySpam()
  mailStore.updateStats(result.stats)
  ElMessage.success(t('mailbox.messages.emptyAllSuccess', { count: result.affected }))
  await loadFolder()
}

onMounted(() => {
  loadFolder()
})

function onSearch(): void {
  void loadFolder(DEFAULT_PAGE, pageSize.value)
}

function toggleTriageFilter(key: keyof MailTriageFilters): void {
  triageFilters[key] = !triageFilters[key]
  void loadFolder(DEFAULT_PAGE, pageSize.value)
}

function onPageChange(page: number): void {
  void loadFolder(page, pageSize.value)
}

function onPageSizeChange(size: number): void {
  void loadFolder(DEFAULT_PAGE, size)
}
</script>

<template>
  <div class="mm-page">
    <section class="toolbar mm-card">
      <h1 class="mm-section-title">{{ t(props.titleKey) }}</h1>
      <div class="actions">
        <el-input
          v-model="keyword"
          class="keyword-input"
          data-testid="mail-search-input"
          :placeholder="t('mailbox.searchPlaceholder')"
          clearable
          @keyup.enter="onSearch"
        />
        <el-button data-testid="mail-search-button" type="primary" @click="onSearch">{{ t('mailbox.actions.search') }}</el-button>
        <el-button v-if="props.showTrashBulkActions" @click="onRestoreAllTrash">{{ t('mailbox.actions.restoreAll') }}</el-button>
        <el-button v-if="props.showTrashBulkActions" type="danger" plain @click="onEmptyTrash">{{ t('mailbox.actions.emptyTrash') }}</el-button>
        <el-button v-if="props.showSpamBulkActions" @click="onRestoreAllSpam">{{ t('mailbox.actions.restoreAll') }}</el-button>
        <el-button v-if="props.showSpamBulkActions" type="danger" plain @click="onEmptySpam">{{ t('mailbox.actions.emptySpam') }}</el-button>
      </div>
      <div v-if="props.folder === 'inbox'" class="triage-filters">
        <el-button data-testid="mail-filter-unread" :type="triageFilters.unread ? 'primary' : 'default'" @click="toggleTriageFilter('unread')">
          {{ t('mailList.filters.unread') }}
        </el-button>
        <el-button data-testid="mail-filter-needsReply" :type="triageFilters.needsReply ? 'primary' : 'default'" @click="toggleTriageFilter('needsReply')">
          {{ t('mailList.filters.needsReply') }}
        </el-button>
        <el-button data-testid="mail-filter-starred" :type="triageFilters.starred ? 'primary' : 'default'" @click="toggleTriageFilter('starred')">
          {{ t('mailList.filters.starred') }}
        </el-button>
        <el-button data-testid="mail-filter-attachments" :type="triageFilters.hasAttachments ? 'primary' : 'default'" @click="toggleTriageFilter('hasAttachments')">
          {{ t('mailList.filters.attachments') }}
        </el-button>
        <el-button data-testid="mail-filter-importantContact" :type="triageFilters.importantContact ? 'primary' : 'default'" @click="toggleTriageFilter('importantContact')">
          {{ t('mailList.filters.importantContact') }}
        </el-button>
      </div>
    </section>
    <el-alert
      v-if="loadError"
      data-testid="mail-load-error"
      class="load-error"
      type="error"
      :closable="false"
      :title="loadError"
    />
    <MailList
      :title="t(props.titleKey)"
      :mails="mailStore.getFolder(folderMap[props.folder])"
      :loading="loading"
      :allow-batch-actions="props.folder !== 'outbox'"
      @open="openMail"
      @action="onAction"
      @batch-action="onBatchAction"
      @undo="onUndo"
      @custom-snooze="onCustomSnooze"
    />
    <section class="pagination-wrap mm-card">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next"
        :current-page="currentPage"
        :page-size="pageSize"
        :page-sizes="PAGE_SIZES"
        :total="mailPage.total"
        @current-change="onPageChange"
        @size-change="onPageSizeChange"
      />
    </section>
  </div>
</template>

<style scoped>
.toolbar {
  padding: 16px;
  margin-bottom: 12px;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.triage-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.keyword-input {
  flex: 1;
  min-width: 240px;
}

.load-error {
  margin-bottom: 12px;
}

.pagination-wrap {
  margin-top: 12px;
  padding: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
