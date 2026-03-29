<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { ConversationSummary, SystemMailFolder } from '~/types/api'
import { useConversationApi } from '~/composables/useConversationApi'
import { useI18n } from '~/composables/useI18n'

const loading = ref(false)
const keyword = ref('')
const folder = ref<SystemMailFolder | ''>('')
const items = ref<ConversationSummary[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

const { fetchConversations } = useConversationApi()
const { t } = useI18n()

useHead(() => ({
  title: t('mailWorkspace.conversations.pageTitle')
}))

async function loadConversations(): Promise<void> {
  loading.value = true
  try {
    const result = await fetchConversations(page.value, size.value, keyword.value, folder.value)
    items.value = result.items
    total.value = result.total
  } finally {
    loading.value = false
  }
}

async function onSearch(): Promise<void> {
  page.value = 1
  await loadConversations()
}

async function onPageChange(next: number): Promise<void> {
  page.value = next
  await loadConversations()
}

function openConversation(conversationId: string): void {
  void navigateTo(`/conversations/${conversationId}`)
}

onMounted(() => {
  loadConversations()
})
</script>

<template>
  <div class="mm-page">
    <section class="mm-card panel">
      <h1 class="mm-section-title">{{ t('mailWorkspace.conversations.pageTitle') }}</h1>
      <div class="filters">
        <el-input v-model="keyword" :placeholder="t('mailWorkspace.conversations.searchPlaceholder')" clearable @keyup.enter="onSearch" />
        <el-select v-model="folder" clearable :placeholder="t('mailWorkspace.conversations.folderPlaceholder')">
          <el-option :label="t('nav.inbox')" value="INBOX" />
          <el-option :label="t('nav.sent')" value="SENT" />
          <el-option :label="t('nav.outbox')" value="OUTBOX" />
          <el-option :label="t('nav.archive')" value="ARCHIVE" />
          <el-option :label="t('nav.spam')" value="SPAM" />
          <el-option :label="t('nav.snoozed')" value="SNOOZED" />
          <el-option :label="t('nav.scheduled')" value="SCHEDULED" />
        </el-select>
        <el-button type="primary" @click="onSearch">{{ t('mailbox.actions.search') }}</el-button>
      </div>
    </section>

    <section class="mm-card list">
      <el-table :data="items" v-loading="loading" :empty-text="t('mailWorkspace.conversations.empty')">
        <el-table-column prop="subject" :label="t('mailWorkspace.conversations.columns.subject')" min-width="260" />
        <el-table-column :label="t('mailWorkspace.conversations.columns.participants')" min-width="220">
          <template #default="{ row }">
            <div class="participants">{{ row.participants.join(', ') || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="messageCount" :label="t('mailWorkspace.conversations.columns.messages')" width="100" />
        <el-table-column prop="unreadCount" :label="t('mailWorkspace.conversations.columns.unread')" width="100" />
        <el-table-column prop="latestAt" :label="t('mailWorkspace.conversations.columns.latest')" min-width="180" />
        <el-table-column :label="t('mailWorkspace.conversations.columns.actions')" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openConversation(row.conversationId)">{{ t('mailWorkspace.conversations.actions.open') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :current-page="page"
          :page-size="size"
          :total="total"
          @current-change="onPageChange"
        />
      </div>
    </section>
  </div>
</template>

<style scoped>
.panel {
  padding: 16px;
  margin-bottom: 12px;
}

.filters {
  display: grid;
  grid-template-columns: 2fr 1fr auto;
  gap: 8px;
}

.list {
  padding: 8px;
}

.participants {
  color: var(--mm-muted);
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding: 12px 8px 4px;
}

@media (max-width: 960px) {
  .filters {
    grid-template-columns: 1fr;
  }
}
</style>
