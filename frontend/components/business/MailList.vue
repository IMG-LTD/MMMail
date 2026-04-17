<script setup lang="ts">
import { computed, ref, useId, watch } from 'vue'
import type { MailId, MailSummary } from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import MailListItem from '~/components/business/mail-list/MailListItem.vue'

const props = defineProps<{
  title: string
  mails: MailSummary[]
  loading: boolean
  allowBatchActions?: boolean
}>()

const emit = defineEmits<{
  open: [mailId: MailId]
  action: [mailId: MailId, action: string]
  batchAction: [mailIds: MailId[], action: string]
  undo: [mailId: MailId]
  customSnooze: [mailId: MailId]
}>()

const { t } = useI18n()
const titleId = useId()
const selectedIds = ref<MailId[]>([])
const selectedFolderType = computed(() => {
  if (!selectedIds.value.length) {
    return ''
  }
  return props.mails.find((mail) => mail.id === selectedIds.value[0])?.folderType || ''
})
const canShowPolicyBatch = computed(() => selectedFolderType.value === 'INBOX' || selectedFolderType.value === 'SPAM')
const allSelected = computed(() => props.mails.length > 0 && props.mails.every((mail) => selectedIds.value.includes(mail.id)))
const canBatch = computed(() => props.allowBatchActions !== false)

watch(() => props.mails, (mails) => {
  const visibleIds = new Set(mails.map((mail) => mail.id))
  selectedIds.value = selectedIds.value.filter((id) => visibleIds.has(id))
}, { deep: true })

function toggleSelection(mailId: MailId, checked: boolean): void {
  if (checked) {
    if (!selectedIds.value.includes(mailId)) {
      selectedIds.value = [...selectedIds.value, mailId]
    }
    return
  }

  selectedIds.value = selectedIds.value.filter((id) => id !== mailId)
}

function toggleAllSelection(checked: boolean): void {
  selectedIds.value = checked ? props.mails.map((mail) => mail.id) : []
}

function runBatch(action: string): void {
  if (!selectedIds.value.length) {
    return
  }
  emit('batchAction', [...selectedIds.value], action)
}
</script>

<template>
  <section class="mm-card mail-list" :aria-labelledby="titleId">
    <header class="head">
      <h2 :id="titleId">{{ props.title }}</h2>
      <span>{{ t('mailList.header.items', { count: props.mails.length }) }}</span>
    </header>

    <div v-if="selectedIds.length && canBatch" class="batch-actions">
      <label class="select-all">
        <input
          :checked="allSelected"
          type="checkbox"
          @change="toggleAllSelection(($event.target as HTMLInputElement).checked)"
        >
        <span>{{ t('mailList.batch.selected', { count: selectedIds.length }) }}</span>
      </label>
      <el-button size="small" @click="runBatch('MARK_READ')">{{ t('mailList.actions.markRead') }}</el-button>
      <el-button size="small" @click="runBatch('MARK_UNREAD')">{{ t('mailList.actions.markUnread') }}</el-button>
      <el-button size="small" @click="runBatch('SNOOZE_24H')">{{ t('mailList.actions.snooze24h') }}</el-button>
      <el-button size="small" @click="runBatch('SNOOZE_7D')">{{ t('mailList.actions.snooze7d') }}</el-button>
      <el-button size="small" @click="runBatch('UNSNOOZE')">{{ t('mailList.actions.unsnooze') }}</el-button>
      <el-button size="small" @click="runBatch('MOVE_ARCHIVE')">{{ t('mailList.actions.archive') }}</el-button>
      <el-button size="small" @click="runBatch('MOVE_SPAM')">{{ t('mailList.actions.spam') }}</el-button>
      <el-button size="small" @click="runBatch('MOVE_TRASH')">{{ t('mailList.actions.trash') }}</el-button>
      <el-button size="small" @click="runBatch('STAR')">{{ t('mailList.actions.star') }}</el-button>
      <el-button v-if="canShowPolicyBatch" size="small" type="danger" plain @click="runBatch('BLOCK_SENDER')">
        {{ t('mailList.actions.blockSender') }}
      </el-button>
      <el-button v-if="canShowPolicyBatch" size="small" type="success" plain @click="runBatch('TRUST_SENDER')">
        {{ t('mailList.actions.trustSender') }}
      </el-button>
      <el-button v-if="canShowPolicyBatch" size="small" type="danger" plain @click="runBatch('BLOCK_DOMAIN')">
        {{ t('mailList.actions.blockDomain') }}
      </el-button>
      <el-button v-if="canShowPolicyBatch" size="small" type="success" plain @click="runBatch('TRUST_DOMAIN')">
        {{ t('mailList.actions.trustDomain') }}
      </el-button>
    </div>

    <div v-if="props.loading" class="list-status">{{ t('mailList.status.loading') }}</div>
    <el-empty v-else-if="!props.mails.length" :description="t('mailList.empty')" />
    <ul v-else class="items">
      <li v-for="mail in props.mails" :key="mail.id" class="item">
        <MailListItem
          :mail="mail"
          :selected="selectedIds.includes(mail.id)"
          @toggle-selection="toggleSelection(mail.id, $event)"
          @open="emit('open', mail.id)"
          @action="emit('action', mail.id, $event)"
          @undo="emit('undo', mail.id)"
          @custom-snooze="emit('customSnooze', mail.id)"
        />
      </li>
    </ul>
  </section>
</template>

<style scoped>
.mail-list {
  padding: 16px;
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

h2 {
  margin: 0;
  font-size: 18px;
}

.batch-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-bottom: 12px;
}

.select-all {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding-right: 4px;
  color: var(--el-text-color-regular, #606266);
}

.list-status {
  padding: 28px 12px;
  text-align: center;
  color: var(--el-text-color-secondary, #909399);
}

.items {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 0;
  margin: 0;
  list-style: none;
}

.item {
  margin: 0;
}
</style>
