<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { MailId, MailSummary } from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import { useMailApi } from '~/composables/useMailApi'
import { useMailStore } from '~/stores/mail'

const loading = ref(false)
const keyword = ref('')
const items = ref<MailSummary[]>([])

const mailStore = useMailStore()
const { t } = useI18n()
const { fetchUnread, fetchStats, applyAction, applyBatchAction, snoozeUntil } = useMailApi()

async function loadUnread(): Promise<void> {
  loading.value = true
  try {
    const [page, stats] = await Promise.all([
      fetchUnread(1, 30, keyword.value),
      fetchStats()
    ])
    items.value = page.items
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
  await loadUnread()
}

async function onBatchAction(mailIds: MailId[], action: string): Promise<void> {
  const result = await applyBatchAction(mailIds, action)
  mailStore.updateStats(result.stats)
  ElMessage.success(t('mailbox.messages.updated', { count: result.affected }))
  await loadUnread()
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
  } catch {
    return
  }

  try {
    const result = await snoozeUntil(mailId, untilAt)
    mailStore.updateStats(result.stats)
    ElMessage.success(t('mailbox.messages.customSnoozeSuccess', { untilAt }))
    await loadUnread()
  } catch (error) {
    const message = error instanceof Error ? error.message : t('mailbox.messages.customSnoozeFailed')
    ElMessage.error(message)
  }
}

onMounted(() => {
  loadUnread()
})
</script>

<template>
  <div class="mm-page">
    <section class="mm-card toolbar">
      <h1 class="mm-section-title">{{ t('page.unread.title') }}</h1>
      <div class="actions">
        <el-input
          v-model="keyword"
          class="keyword-input"
          :placeholder="t('page.unread.searchPlaceholder')"
          clearable
          @keyup.enter="loadUnread"
        />
        <el-button type="primary" @click="loadUnread">{{ t('mailbox.actions.search') }}</el-button>
      </div>
    </section>

    <MailList
      :title="t('page.unread.title')"
      :mails="items"
      :loading="loading"
      @open="openMail"
      @action="onAction"
      @batch-action="onBatchAction"
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
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.keyword-input {
  flex: 1;
  min-width: 240px;
}
</style>
