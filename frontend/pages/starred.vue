<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { MailId, MailSummary } from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import { useMailApi } from '~/composables/useMailApi'
import { useMailStore } from '~/stores/mail'

const loading = ref(false)
const keyword = ref('')
const mails = ref<MailSummary[]>([])
const mailStore = useMailStore()
const { t } = useI18n()
const { fetchStarred, fetchStats, applyAction, applyBatchAction, undoSend, snoozeUntil } = useMailApi()

async function loadStarred(): Promise<void> {
  loading.value = true
  try {
    const [page, stats] = await Promise.all([fetchStarred(1, 30, keyword.value), fetchStats()])
    mails.value = page.items
    mailStore.updateStats(stats)
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
  await loadStarred()
}

async function onBatchAction(mailIds: MailId[], action: string): Promise<void> {
  const result = await applyBatchAction(mailIds, action)
  mailStore.updateStats(result.stats)
  ElMessage.success(t('mailbox.messages.updated', { count: result.affected }))
  await loadStarred()
}

async function onUndo(mailId: MailId): Promise<void> {
  try {
    await undoSend(mailId)
    ElMessage.success(t('mailbox.messages.undoSuccess'))
  } catch (error) {
    const message = error instanceof Error ? error.message : t('mailbox.messages.undoFailed')
    ElMessage.error(message)
  } finally {
    await loadStarred()
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
    await loadStarred()
  } catch (error) {
    const message = error instanceof Error ? error.message : t('mailbox.messages.customSnoozeFailed')
    ElMessage.error(message)
  }
}

onMounted(() => {
  loadStarred()
})
</script>

<template>
  <div class="mm-page">
    <section class="toolbar mm-card">
      <h1 class="mm-section-title">{{ t('page.starred.title') }}</h1>
      <div class="actions">
        <el-input v-model="keyword" :placeholder="t('page.starred.searchPlaceholder')" clearable @keyup.enter="loadStarred" />
        <el-button type="primary" @click="loadStarred">{{ t('mailbox.actions.search') }}</el-button>
      </div>
    </section>
    <MailList
      :title="t('page.starred.title')"
      :mails="mails"
      :loading="loading"
      @open="openMail"
      @action="onAction"
      @batch-action="onBatchAction"
      @undo="onUndo"
      @custom-snooze="onCustomSnooze"
    />
  </div>
</template>

<style scoped>
.toolbar {
  padding: 16px;
  margin-bottom: 12px;
}

.actions {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
}
</style>
