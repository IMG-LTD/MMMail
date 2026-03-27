<script setup lang="ts">
import { reactive } from 'vue'
import type { MeetParticipantItem, MeetSignalEventItem, MeetSignalType } from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import {
  formatMeetSignalTarget,
  formatMeetTime,
  meetSignalTypeLabelKey
} from '~/utils/meet-workspace'

interface SignalPayload {
  signalType: MeetSignalType
  toParticipantId?: string
  payload: string
}

interface Props {
  events: MeetSignalEventItem[]
  signalCursor: number
  targetOptions: MeetParticipantItem[]
  disabled: boolean
  sending: boolean
  polling: boolean
  streaming: boolean
  liveSignalStreamEnabled: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{
  send: [payload: SignalPayload]
  poll: []
  streamOnce: []
  toggleLive: [enabled: boolean]
}>()

const { t } = useI18n()

const form = reactive({
  toParticipantId: '',
  payload: '{"sdp":"demo"}'
})

function onSend(signalType: MeetSignalType): void {
  emit('send', {
    signalType,
    toParticipantId: form.toParticipantId || undefined,
    payload: form.payload
  })
}

function onToggleLive(enabled: string | number | boolean): void {
  emit('toggleLive', Boolean(enabled))
}
</script>

<template>
  <section class="mm-card meet-signal-card">
    <h2 class="mm-section-title">{{ t('meet.workspace.signals.title') }}</h2>
    <div class="meet-signal-grid">
      <el-select v-model="form.toParticipantId" clearable :placeholder="t('meet.workspace.signals.targetPlaceholder')">
        <el-option :label="t('meet.workspace.signals.broadcast')" value="" />
        <el-option
          v-for="item in targetOptions"
          :key="item.participantId"
          :label="`${item.displayName} (${item.participantId})`"
          :value="item.participantId"
        />
      </el-select>
      <el-input
        v-model="form.payload"
        type="textarea"
        :rows="3"
        maxlength="8192"
        show-word-limit
        :placeholder="t('meet.workspace.signals.payloadPlaceholder')"
      />
    </div>
    <div class="meet-signal-actions">
      <el-button type="primary" :disabled="disabled" :loading="sending" @click="onSend('OFFER')">
        {{ t('meet.workspace.signals.actions.offer') }}
      </el-button>
      <el-button type="success" :disabled="disabled" :loading="sending" @click="onSend('ANSWER')">
        {{ t('meet.workspace.signals.actions.answer') }}
      </el-button>
      <el-button type="warning" :disabled="disabled" :loading="sending" @click="onSend('ICE')">
        {{ t('meet.workspace.signals.actions.ice') }}
      </el-button>
      <el-button :disabled="disabled" :loading="polling" @click="emit('poll')">
        {{ t('meet.workspace.signals.actions.poll') }}
      </el-button>
      <el-button :disabled="disabled" :loading="streaming" @click="emit('streamOnce')">
        {{ t('meet.workspace.signals.actions.streamOnce') }}
      </el-button>
      <el-switch
        :model-value="liveSignalStreamEnabled"
        :disabled="disabled"
        :active-text="t('meet.workspace.signals.live')"
        :inactive-text="t('meet.workspace.signals.manual')"
        @change="onToggleLive"
      />
    </div>
    <p class="meet-signal-meta">
      {{ t('meet.workspace.signals.meta', {
        cursor: signalCursor,
        count: events.length,
        mode: liveSignalStreamEnabled ? t('meet.workspace.signals.live') : t('meet.workspace.signals.manual')
      }) }}
    </p>
    <el-table :data="props.events" style="width: 100%">
      <el-table-column prop="eventSeq" :label="t('meet.workspace.signals.columns.seq')" min-width="90" />
      <el-table-column :label="t('meet.workspace.signals.columns.type')" min-width="110">
        <template #default="scope">{{ t(meetSignalTypeLabelKey(scope.row.signalType)) }}</template>
      </el-table-column>
      <el-table-column prop="fromParticipantId" :label="t('meet.workspace.signals.columns.from')" min-width="120" />
      <el-table-column :label="t('meet.workspace.signals.columns.to')" min-width="170">
        <template #default="scope">{{ formatMeetSignalTarget(scope.row, t('meet.workspace.signals.broadcast')) }}</template>
      </el-table-column>
      <el-table-column :label="t('meet.workspace.signals.columns.payload')" min-width="220">
        <template #default="scope">{{ scope.row.payload }}</template>
      </el-table-column>
      <el-table-column :label="t('meet.workspace.signals.columns.createdAt')" min-width="180">
        <template #default="scope">{{ formatMeetTime(scope.row.createdAt) }}</template>
      </el-table-column>
    </el-table>
  </section>
</template>

<style scoped>
.meet-signal-card {
  padding: 16px;
}

.meet-signal-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 10px;
}

.meet-signal-actions {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.meet-signal-meta {
  margin: 12px 0 0;
  font-size: 12px;
  color: #64748b;
}

@media (max-width: 900px) {
  .meet-signal-grid {
    grid-template-columns: 1fr;
  }
}
</style>
