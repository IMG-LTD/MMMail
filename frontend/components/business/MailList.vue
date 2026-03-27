<script setup lang="ts">
import { computed, ref } from 'vue'
import type { MailId, MailSummary } from '~/types/api'
import { useI18n } from '~/composables/useI18n'

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

const { locale, t } = useI18n()
const selectedIds = ref<MailId[]>([])
const selectedFolderType = computed(() => {
  if (!selectedIds.value.length) {
    return ''
  }
  const selectedMail = props.mails.find((mail) => mail.id === selectedIds.value[0])
  return selectedMail?.folderType || ''
})
const canShowPolicyBatch = computed(() => selectedFolderType.value === 'INBOX' || selectedFolderType.value === 'SPAM')

function onSelectionChange(rows: MailSummary[]): void {
  selectedIds.value = rows.map((row) => row.id)
}

function runBatch(action: string): void {
  if (!selectedIds.value.length) {
    return
  }
  emit('batchAction', [...selectedIds.value], action)
}

function formatSentAt(value: string): string {
  return new Date(value).toLocaleString(locale.value)
}
</script>

<template>
  <section class="mm-card mail-list">
    <header class="head">
      <h2>{{ props.title }}</h2>
      <span>{{ t('mailList.header.items', { count: props.mails.length }) }}</span>
    </header>

    <div v-if="selectedIds.length && props.allowBatchActions !== false" class="batch-actions">
      <el-tag type="info">{{ t('mailList.batch.selected', { count: selectedIds.length }) }}</el-tag>
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

    <el-table
      v-loading="props.loading"
      :data="props.mails"
      row-key="id"
      style="width: 100%"
      @selection-change="onSelectionChange"
    >
      <el-table-column type="selection" width="48" />
      <el-table-column :label="t('mailList.columns.subject')" min-width="260">
        <template #default="scope">
          <button class="subject-btn" @click="emit('open', scope.row.id)">
            <span :class="['subject', { unread: !scope.row.isRead && !scope.row.isDraft }]">
              {{ scope.row.subject || t('mailList.subjectFallback') }}
            </span>
            <el-tag v-for="label in scope.row.labels" :key="label" size="small" effect="plain" class="label-tag">
              {{ label }}
            </el-tag>
          </button>
        </template>
      </el-table-column>
      <el-table-column prop="peerEmail" :label="t('mailList.columns.peer')" min-width="180" />
      <el-table-column :label="t('mailList.columns.flags')" width="140">
        <template #default="scope">
          <el-tag v-if="scope.row.isStarred" size="small" type="warning">{{ t('mailList.flags.starred') }}</el-tag>
          <el-tag v-else-if="!scope.row.isRead" size="small" type="success">{{ t('mailList.flags.unread') }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('mailList.columns.time')" min-width="180">
        <template #default="scope">
          {{ formatSentAt(scope.row.sentAt) }}
        </template>
      </el-table-column>
      <el-table-column :label="t('mailList.columns.actions')" width="520">
        <template #default="scope">
          <div class="row-actions">
            <template v-if="scope.row.folderType === 'OUTBOX'">
              <el-button size="small" text @click="emit('undo', scope.row.id)">{{ t('mailList.actions.undoSend') }}</el-button>
            </template>
            <template v-else>
              <el-button size="small" text @click="emit('action', scope.row.id, scope.row.isRead ? 'MARK_UNREAD' : 'MARK_READ')">
                {{ t(scope.row.isRead ? 'mailList.actions.unread' : 'mailList.actions.read') }}
              </el-button>
              <el-button size="small" text @click="emit('action', scope.row.id, scope.row.isStarred ? 'UNSTAR' : 'STAR')">
                {{ t(scope.row.isStarred ? 'mailList.actions.unstar' : 'mailList.actions.star') }}
              </el-button>
              <el-button size="small" text @click="emit('action', scope.row.id, 'SNOOZE_24H')">{{ t('mailList.actions.snooze24h') }}</el-button>
              <el-button size="small" text @click="emit('action', scope.row.id, 'SNOOZE_7D')">{{ t('mailList.actions.snooze7d') }}</el-button>
              <el-button size="small" text @click="emit('customSnooze', scope.row.id)">{{ t('mailList.actions.customSnooze') }}</el-button>
              <el-button size="small" text @click="emit('action', scope.row.id, 'UNSNOOZE')">{{ t('mailList.actions.unsnooze') }}</el-button>
              <el-button size="small" text @click="emit('action', scope.row.id, 'MOVE_ARCHIVE')">{{ t('mailList.actions.archive') }}</el-button>
              <el-button size="small" text @click="emit('action', scope.row.id, 'MOVE_SPAM')">{{ t('mailList.actions.spam') }}</el-button>
              <el-button size="small" text @click="emit('action', scope.row.id, 'MOVE_TRASH')">{{ t('mailList.actions.trash') }}</el-button>
              <template v-if="scope.row.folderType === 'INBOX' || scope.row.folderType === 'SPAM'">
                <el-button size="small" text type="danger" @click="emit('action', scope.row.id, 'BLOCK_SENDER')">
                  {{ t('mailList.actions.blockSender') }}
                </el-button>
                <el-button size="small" text type="success" @click="emit('action', scope.row.id, 'TRUST_SENDER')">
                  {{ t('mailList.actions.trustSender') }}
                </el-button>
                <el-button size="small" text type="danger" @click="emit('action', scope.row.id, 'BLOCK_DOMAIN')">
                  {{ t('mailList.actions.blockDomain') }}
                </el-button>
                <el-button size="small" text type="success" @click="emit('action', scope.row.id, 'TRUST_DOMAIN')">
                  {{ t('mailList.actions.trustDomain') }}
                </el-button>
              </template>
              <el-button
                v-if="scope.row.folderType === 'SPAM'"
                size="small"
                text
                type="primary"
                @click="emit('action', scope.row.id, 'REPORT_NOT_PHISHING')"
              >
                {{ t('mailList.actions.reportNotPhishing') }}
              </el-button>
              <el-button
                v-else-if="scope.row.folderType !== 'SENT' && scope.row.folderType !== 'DRAFTS'"
                size="small"
                text
                type="danger"
                @click="emit('action', scope.row.id, 'REPORT_PHISHING')"
              >
                {{ t('mailList.actions.reportPhishing') }}
              </el-button>
            </template>
          </div>
        </template>
      </el-table-column>
    </el-table>
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
  margin-bottom: 10px;
}

.subject-btn {
  all: unset;
  cursor: pointer;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.subject {
  font-weight: 500;
}

.subject.unread {
  font-weight: 700;
}

.label-tag {
  margin-left: 2px;
}

.row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
</style>
