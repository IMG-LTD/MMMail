<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { ConversationSummary, SystemMailFolder } from '~/types/api'
import { useConversationApi } from '~/composables/useConversationApi'

const loading = ref(false)
const keyword = ref('')
const folder = ref<SystemMailFolder | ''>('')
const items = ref<ConversationSummary[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

const { fetchConversations } = useConversationApi()

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
      <h1 class="mm-section-title">Conversations</h1>
      <div class="filters">
        <el-input v-model="keyword" placeholder="Search conversations" clearable @keyup.enter="onSearch" />
        <el-select v-model="folder" clearable placeholder="Folder">
          <el-option label="Inbox" value="INBOX" />
          <el-option label="Sent" value="SENT" />
          <el-option label="Outbox" value="OUTBOX" />
          <el-option label="Archive" value="ARCHIVE" />
          <el-option label="Spam" value="SPAM" />
          <el-option label="Snoozed" value="SNOOZED" />
          <el-option label="Scheduled" value="SCHEDULED" />
        </el-select>
        <el-button type="primary" @click="onSearch">Search</el-button>
      </div>
    </section>

    <section class="mm-card list">
      <el-table :data="items" v-loading="loading" empty-text="No conversations">
        <el-table-column prop="subject" label="Subject" min-width="260" />
        <el-table-column label="Participants" min-width="220">
          <template #default="{ row }">
            <div class="participants">{{ row.participants.join(', ') || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="messageCount" label="Messages" width="100" />
        <el-table-column prop="unreadCount" label="Unread" width="100" />
        <el-table-column prop="latestAt" label="Latest" min-width="180" />
        <el-table-column label="Actions" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openConversation(row.conversationId)">Open</el-button>
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
