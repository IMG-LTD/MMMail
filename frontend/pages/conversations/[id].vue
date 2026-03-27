<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ConversationAction, ConversationDetail } from '~/types/api'
import { useConversationApi } from '~/composables/useConversationApi'
import { useMailApi } from '~/composables/useMailApi'
import { useMailStore } from '~/stores/mail'

const route = useRoute()
const loading = ref(false)
const detail = ref<ConversationDetail | null>(null)

const { fetchConversationDetail } = useConversationApi()
const { applyConversationAction } = useMailApi()
const mailStore = useMailStore()

async function loadDetail(): Promise<void> {
  const conversationId = String(route.params.id || '')
  if (!conversationId) {
    await navigateTo('/conversations')
    return
  }

  loading.value = true
  try {
    detail.value = await fetchConversationDetail(conversationId)
  } finally {
    loading.value = false
  }
}

function openMail(mailId: string): void {
  void navigateTo(`/mail/${mailId}`)
}

async function runConversationAction(action: ConversationAction): Promise<void> {
  const conversationId = String(route.params.id || '')
  if (!conversationId) {
    return
  }
  if (action === 'MOVE_TRASH') {
    try {
      await ElMessageBox.confirm(
        'Move all mails in this conversation to Trash?',
        'Confirm Conversation Action',
        {
          confirmButtonText: 'Move to Trash',
          cancelButtonText: 'Cancel',
          type: 'warning'
        }
      )
    } catch {
      return
    }
  }
  try {
    const result = await applyConversationAction(conversationId, action)
    mailStore.updateStats(result.stats)
    ElMessage.success(`${result.affected} mails updated in conversation`)
    await loadDetail()
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Conversation action failed'
    ElMessage.error(message)
  }
}

onMounted(() => {
  loadDetail()
})
</script>

<template>
  <div class="mm-page" v-loading="loading">
    <section class="mm-card header">
      <el-button link type="primary" @click="navigateTo('/conversations')">Back</el-button>
      <h1 class="mm-section-title">{{ detail?.subject || 'Conversation' }}</h1>
      <p class="meta">{{ detail?.messages.length || 0 }} messages</p>
      <div class="conversation-actions" v-if="detail">
        <el-button size="small" @click="runConversationAction('MARK_READ')">Mark all read</el-button>
        <el-button size="small" @click="runConversationAction('MARK_UNREAD')">Mark all unread</el-button>
        <el-button size="small" @click="runConversationAction('MOVE_ARCHIVE')">Archive all</el-button>
        <el-button size="small" type="danger" plain @click="runConversationAction('MOVE_TRASH')">Trash all</el-button>
      </div>
    </section>

    <section class="mm-card timeline" v-if="detail">
      <article v-for="mail in detail.messages" :key="mail.id" class="message-item">
        <div class="top-row">
          <strong>{{ mail.subject || '(No subject)' }}</strong>
          <span>{{ mail.sentAt }}</span>
        </div>
        <p class="peer">{{ mail.peerEmail }}</p>
        <p class="preview">{{ mail.preview }}</p>
        <el-button link type="primary" @click="openMail(mail.id)">Open Mail</el-button>
      </article>
    </section>
  </div>
</template>

<style scoped>
.header {
  padding: 16px;
  margin-bottom: 12px;
}

.meta {
  color: var(--mm-muted);
  margin-top: 4px;
}

.conversation-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.timeline {
  padding: 16px;
}

.message-item {
  border: 1px solid var(--mm-border);
  border-radius: 10px;
  padding: 12px;
  margin-bottom: 10px;
}

.top-row {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.peer {
  margin: 6px 0;
  color: var(--mm-muted);
}

.preview {
  margin-bottom: 6px;
}
</style>
