<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ConversationAction, ConversationDetail } from '~/types/api'
import { useConversationApi } from '~/composables/useConversationApi'
import { useI18n } from '~/composables/useI18n'
import { useMailApi } from '~/composables/useMailApi'
import { useMailStore } from '~/stores/mail'

const route = useRoute()
const loading = ref(false)
const detail = ref<ConversationDetail | null>(null)

const { fetchConversationDetail } = useConversationApi()
const { applyConversationAction } = useMailApi()
const mailStore = useMailStore()
const { t } = useI18n()

useHead(() => ({
  title: detail.value?.subject || t('mailWorkspace.conversationDetail.pageTitle')
}))

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
        t('mailWorkspace.conversationDetail.confirmTrashMessage'),
        t('mailWorkspace.conversationDetail.confirmTrashTitle'),
        {
          confirmButtonText: t('mailWorkspace.conversationDetail.confirmTrash'),
          cancelButtonText: t('mailbox.customSnooze.cancel'),
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
    ElMessage.success(t('mailWorkspace.conversationDetail.messages.updated', { count: result.affected }))
    await loadDetail()
  } catch (error) {
    const message = error instanceof Error ? error.message : t('mailWorkspace.conversationDetail.messages.actionFailed')
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
      <el-button link type="primary" @click="navigateTo('/conversations')">{{ t('mailWorkspace.conversationDetail.back') }}</el-button>
      <h1 class="mm-section-title">{{ detail?.subject || t('mailWorkspace.conversationDetail.pageTitle') }}</h1>
      <p class="meta">{{ t('mailWorkspace.conversationDetail.messagesCount', { count: detail?.messages.length || 0 }) }}</p>
      <div class="conversation-actions" v-if="detail">
        <el-button size="small" @click="runConversationAction('MARK_READ')">{{ t('mailWorkspace.conversationDetail.actions.markRead') }}</el-button>
        <el-button size="small" @click="runConversationAction('MARK_UNREAD')">{{ t('mailWorkspace.conversationDetail.actions.markUnread') }}</el-button>
        <el-button size="small" @click="runConversationAction('MOVE_ARCHIVE')">{{ t('mailWorkspace.conversationDetail.actions.archive') }}</el-button>
        <el-button size="small" type="danger" plain @click="runConversationAction('MOVE_TRASH')">{{ t('mailWorkspace.conversationDetail.actions.trash') }}</el-button>
      </div>
    </section>

    <section class="mm-card timeline" v-if="detail">
      <article v-for="mail in detail.messages" :key="mail.id" class="message-item">
        <div class="top-row">
          <strong>{{ mail.subject || t('mailWorkspace.detail.fallbackSubject') }}</strong>
          <span>{{ mail.sentAt }}</span>
        </div>
        <p class="peer">{{ mail.peerEmail }}</p>
        <p class="preview">{{ mail.preview }}</p>
        <el-button link type="primary" @click="openMail(mail.id)">{{ t('mailWorkspace.conversationDetail.actions.openMail') }}</el-button>
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
