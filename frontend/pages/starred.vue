<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { MailId, MailSummary } from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import { useMailApi } from '~/composables/useMailApi'
import { useMailStore } from '~/stores/mail'

const loading = ref(false)
const keyword = ref('')
const mails = ref<MailSummary[]>([])
const mailStore = useMailStore()
const { t } = useI18n()
const { fetchStarred, fetchStats, applyAction, applyBatchAction } = useMailApi()

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
