<script setup lang="ts">
import { computed } from 'vue'
import type { MailSummary } from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import { buildMailListModel } from '~/utils/mail-list'
import MailListSignals from '~/components/business/mail-list/MailListSignals.vue'
import MailListQuickActions from '~/components/business/mail-list/MailListQuickActions.vue'

const props = defineProps<{
  mail: MailSummary
  selected: boolean
}>()

const emit = defineEmits<{
  toggleSelection: [checked: boolean]
  open: []
  action: [action: string]
  undo: []
  customSnooze: []
}>()

const { locale, t } = useI18n()
const model = computed(() => buildMailListModel(props.mail))
const subjectText = computed(() => model.value.subjectText || t('mailList.subjectFallback'))
const isUnread = computed(() => !props.mail.isRead && !props.mail.isDraft)
const rowLabel = computed(() => `${model.value.senderName || props.mail.peerEmail} ${subjectText.value}`.trim())
const formattedSentAt = computed(() => new Date(props.mail.sentAt).toLocaleString(locale.value))

function onSelectionChange(event: Event): void {
  emit('toggleSelection', (event.target as HTMLInputElement).checked)
}
</script>

<template>
  <article :class="['mail-row', { unread: isUnread, selected: props.selected }]">
    <div class="leading">
      <input
        :checked="props.selected"
        :aria-label="rowLabel"
        class="selection"
        data-testid="mail-row-select"
        type="checkbox"
        @change="onSelectionChange"
      >
      <span v-if="isUnread" class="unread-dot" aria-hidden="true" />
      <button
        class="avatar"
        type="button"
        :aria-label="rowLabel"
        @click="emit('open')"
      >
        {{ model.avatarText }}
      </button>
    </div>

    <button
      class="content"
      type="button"
      :aria-label="rowLabel"
      @click="emit('open')"
    >
      <span class="line-one">
        <strong class="sender">{{ model.senderName || props.mail.peerEmail }}</strong>
        <span class="subject">{{ subjectText }}</span>
      </span>
      <span class="signals-row">
        <MailListSignals :signals="model.identitySignals" />
        <MailListSignals :signals="model.taskSignals" />
      </span>
      <span class="line-two">
        <span class="preview">{{ model.previewText }}</span>
      </span>
      <MailListSignals :signals="model.conversationSignals" compact />
    </button>

    <div class="trailing">
      <time class="sent-at" :datetime="props.mail.sentAt">{{ formattedSentAt }}</time>
      <MailListQuickActions
        :mail="props.mail"
        :subject-text="subjectText"
        @action="emit('action', $event)"
        @undo="emit('undo')"
        @custom-snooze="emit('customSnooze')"
      />
    </div>
  </article>
</template>

<style scoped>
.mail-row {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 14px;
  align-items: start;
  padding: 14px 16px;
  border: 1px solid var(--el-border-color-light, #ebeef5);
  border-radius: 14px;
  background: var(--el-bg-color, #fff);
}

.mail-row.unread {
  border-color: color-mix(in srgb, var(--el-color-primary, #409eff) 32%, var(--el-border-color-light, #dcdfe6));
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--el-color-primary, #409eff) 14%, transparent);
}

.mail-row.selected {
  background: color-mix(in srgb, var(--el-color-primary, #409eff) 5%, white);
}

.leading {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-top: 4px;
}

.selection {
  margin: 0;
}

.unread-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: var(--el-color-primary, #409eff);
}

.avatar {
  width: 36px;
  height: 36px;
  border: 0;
  border-radius: 999px;
  background: color-mix(in srgb, var(--el-color-primary, #409eff) 18%, white);
  color: var(--el-color-primary-dark-2, #337ecc);
  font-weight: 700;
  cursor: pointer;
}

.content {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
  padding: 0;
  border: 0;
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.line-one,
.line-two,
.signals-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.sender {
  font-size: 14px;
  color: var(--el-text-color-primary, #303133);
}

.subject {
  font-size: 14px;
  color: var(--el-text-color-primary, #303133);
}

.mail-row.unread .subject,
.mail-row.unread .sender {
  font-weight: 700;
}

.preview {
  min-width: 0;
  color: var(--el-text-color-regular, #606266);
  line-height: 1.5;
  word-break: break-word;
}

.trailing {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
  min-width: 180px;
}

.sent-at {
  color: var(--el-text-color-secondary, #909399);
  font-size: 12px;
  white-space: nowrap;
}

@media (max-width: 900px) {
  .mail-row {
    grid-template-columns: 1fr;
  }

  .leading,
  .trailing {
    padding-top: 0;
  }

  .trailing {
    align-items: flex-start;
    min-width: 0;
  }
}
</style>
