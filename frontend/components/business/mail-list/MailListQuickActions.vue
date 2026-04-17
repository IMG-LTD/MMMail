<script setup lang="ts">
import { computed } from 'vue'
import type { MailSummary } from '~/types/api'
import { useI18n } from '~/composables/useI18n'

const props = defineProps<{
  mail: MailSummary
  subjectText: string
}>()

const emit = defineEmits<{
  action: [action: string]
  undo: []
  customSnooze: []
}>()

const { t } = useI18n()

const canManageTrustPolicy = computed(() => props.mail.folderType === 'INBOX' || props.mail.folderType === 'SPAM')
const canReportPhishing = computed(() => props.mail.folderType !== 'SENT' && props.mail.folderType !== 'DRAFTS' && props.mail.folderType !== 'OUTBOX')
const isSnoozedRow = computed(() => props.mail.folderType === 'SNOOZED')
const moreActions = computed(() => {
  const items: Array<{ key: string, label: string, handler: () => void }> = [
    {
      key: 'star',
      label: t(props.mail.isStarred ? 'mailList.actions.unstar' : 'mailList.actions.star'),
      handler: () => emit('action', props.mail.isStarred ? 'UNSTAR' : 'STAR')
    },
    {
      key: 'snooze7d',
      label: t('mailList.actions.snooze7d'),
      handler: () => emit('action', 'SNOOZE_7D')
    },
    {
      key: 'customSnooze',
      label: t('mailList.actions.customSnooze'),
      handler: () => emit('customSnooze')
    }
  ]

  if (props.mail.folderType === 'SPAM') {
    items.push({
      key: 'reportNotPhishing',
      label: t('mailList.actions.reportNotPhishing'),
      handler: () => emit('action', 'REPORT_NOT_PHISHING')
    })
  } else if (canReportPhishing.value) {
    items.push({
      key: 'reportPhishing',
      label: t('mailList.actions.reportPhishing'),
      handler: () => emit('action', 'REPORT_PHISHING')
    })
  }

  if (canManageTrustPolicy.value) {
    items.push(
      {
        key: 'blockSender',
        label: t('mailList.actions.blockSender'),
        handler: () => emit('action', 'BLOCK_SENDER')
      },
      {
        key: 'trustSender',
        label: t('mailList.actions.trustSender'),
        handler: () => emit('action', 'TRUST_SENDER')
      },
      {
        key: 'blockDomain',
        label: t('mailList.actions.blockDomain'),
        handler: () => emit('action', 'BLOCK_DOMAIN')
      },
      {
        key: 'trustDomain',
        label: t('mailList.actions.trustDomain'),
        handler: () => emit('action', 'TRUST_DOMAIN')
      }
    )
  }

  items.push(
    {
      key: 'spam',
      label: t('mailList.actions.spam'),
      handler: () => emit('action', 'MOVE_SPAM')
    },
    {
      key: 'trash',
      label: t('mailList.actions.trash'),
      handler: () => emit('action', 'MOVE_TRASH')
    }
  )

  return items
})
</script>

<template>
  <div class="quick-actions" @click.stop>
    <template v-if="props.mail.folderType === 'OUTBOX'">
      <el-button
        :data-testid="`mail-action-undo-${props.mail.id}`"
        size="small"
        @click="emit('undo')"
      >
        {{ t('mailList.actions.undoSend') }}
      </el-button>
    </template>
    <template v-else>
      <el-button
        :data-testid="`mail-action-read-${props.mail.id}`"
        size="small"
        @click="emit('action', props.mail.isRead ? 'MARK_UNREAD' : 'MARK_READ')"
      >
        {{ t(props.mail.isRead ? 'mailList.actions.unread' : 'mailList.actions.read') }}
      </el-button>
      <el-button
        :data-testid="`mail-action-archive-${props.mail.id}`"
        size="small"
        @click="emit('action', 'MOVE_ARCHIVE')"
      >
        {{ t('mailList.actions.archive') }}
      </el-button>
      <el-button
        :data-testid="`mail-action-snooze-${props.mail.id}`"
        size="small"
        @click="emit('action', isSnoozedRow ? 'UNSNOOZE' : 'SNOOZE_24H')"
      >
        {{ t(isSnoozedRow ? 'mailList.actions.unsnooze' : 'mailList.actions.snooze') }}
      </el-button>
      <el-dropdown trigger="click">
        <el-button
          :data-testid="`mail-action-more-${props.mail.id}`"
          :aria-label="`${t('mailList.actions.more')} ${props.subjectText}`"
          size="small"
        >
          {{ t('mailList.actions.more') }}
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item
              v-for="item in moreActions"
              :key="item.key"
              @click="item.handler()"
            >
              {{ item.label }}
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </template>
  </div>
</template>

<style scoped>
.quick-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}
</style>
