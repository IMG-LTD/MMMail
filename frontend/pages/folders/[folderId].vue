<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { MailId, MailSummary } from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import { useMailApi } from '~/composables/useMailApi'
import { useMailFolderApi } from '~/composables/useMailFolderApi'
import { useMailStore } from '~/stores/mail'
import { findMailFolderNode, findMailFolderPath } from '~/utils/mail-folders'

const route = useRoute()
const { t } = useI18n()
const mailStore = useMailStore()
const loading = ref(false)
const keyword = ref('')
const mails = ref<MailSummary[]>([])
const total = ref(0)

const folderId = computed(() => String(route.params.folderId || ''))
const currentFolder = computed(() => findMailFolderNode(mailStore.customFolders, folderId.value))
const folderPathLabel = computed(() => {
  const path = findMailFolderPath(mailStore.customFolders, folderId.value)
  if (path.length <= 1) {
    return t('mailFolders.folder.root')
  }
  return path.slice(0, -1).map((item) => item.name).join(' / ')
})

const { fetchStats, applyAction, applyBatchAction, snoozeUntil } = useMailApi()
const { fetchMailFolderMessages, listMailFolders } = useMailFolderApi()

async function loadFolder(): Promise<void> {
  if (!folderId.value) {
    return
  }

  loading.value = true
  try {
    const [page, stats, folders] = await Promise.all([
      fetchMailFolderMessages(folderId.value, 1, 20, keyword.value),
      fetchStats(),
      listMailFolders()
    ])
    mails.value = page.items
    total.value = page.total
    mailStore.updateStats(stats)
    mailStore.setCustomFolders(folders)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('mailFolders.messages.loadFailed'))
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
  await loadFolder()
}

async function onBatchAction(mailIds: MailId[], action: string): Promise<void> {
  const result = await applyBatchAction(mailIds, action)
  mailStore.updateStats(result.stats)
  ElMessage.success(`${result.affected}`)
  await loadFolder()
}

async function onCustomSnooze(mailId: MailId): Promise<void> {
  let untilAt = ''
  try {
    const prompt = await ElMessageBox.prompt(
      'YYYY-MM-DDTHH:mm:ss',
      t('mailFolders.mailbox.customSnoozeTitle'),
      {
        confirmButtonText: t('common.actions.confirm'),
        cancelButtonText: t('common.actions.cancel'),
        inputPlaceholder: t('mailFolders.mailbox.customSnoozePlaceholder')
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
    await loadFolder()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('mailFolders.messages.loadFailed'))
  }
}

watch(folderId, () => {
  void loadFolder()
})

onMounted(() => {
  void loadFolder()
})
</script>

<template>
  <div class="mm-page custom-folder-page">
    <section class="mm-card hero">
      <template v-if="currentFolder">
        <div>
          <h1 class="mm-section-title">{{ currentFolder.name }}</h1>
          <p class="hero-copy">
            {{ t('mailFolders.mailbox.path') }} ·
            {{ folderPathLabel }}
          </p>
        </div>
        <div class="hero-meta">
          <el-tag effect="plain">{{ t('mailFolders.mailbox.total', { count: total }) }}</el-tag>
          <el-tag :type="currentFolder.notificationsEnabled ? 'success' : 'info'" effect="plain">
            {{ t(currentFolder.notificationsEnabled ? 'mailFolders.mailbox.notificationsOn' : 'mailFolders.mailbox.notificationsOff') }}
          </el-tag>
        </div>
      </template>
      <el-empty v-else :description="t('mailFolders.mailbox.notFound')" />
    </section>

    <section v-if="currentFolder" class="mm-card toolbar">
      <el-input
        v-model="keyword"
        class="keyword-input"
        :placeholder="t('mailFolders.mailbox.searchPlaceholder')"
        clearable
        @keyup.enter="loadFolder"
      />
      <el-button type="primary" @click="loadFolder">{{ t('mailFolders.mailbox.refresh') }}</el-button>
    </section>

    <MailList
      v-if="currentFolder"
      :title="currentFolder.name"
      :mails="mails"
      :loading="loading"
      @open="openMail"
      @action="onAction"
      @batch-action="onBatchAction"
      @custom-snooze="onCustomSnooze"
    />
  </div>
</template>

<style scoped>
.custom-folder-page {
  display: grid;
  gap: 14px;
}

.hero,
.toolbar,
.hero-meta {
  display: flex;
}

.hero {
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 20px;
}

.hero-copy {
  color: var(--mm-muted);
  margin-top: 8px;
}

.hero-meta,
.toolbar {
  gap: 10px;
}

.toolbar {
  padding: 16px;
}

.keyword-input {
  flex: 1;
}

@media (max-width: 900px) {
  .hero,
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
