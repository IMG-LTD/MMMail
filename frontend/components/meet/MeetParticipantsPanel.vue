<script setup lang="ts">
import { ref } from 'vue'
import type { MeetParticipantItem, MeetPruneInactiveResult } from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import {
  formatMeetTime,
  meetParticipantRoleTagType,
  meetWorkspaceBooleanKey,
  meetWorkspaceMediaKey,
  meetWorkspaceParticipantStatusKey,
  meetWorkspaceRoleKey
} from '~/utils/meet-workspace'

interface Props {
  participants: MeetParticipantItem[]
  canManageParticipants: boolean
  canTransferHost: boolean
  lastPruneResult: MeetPruneInactiveResult | null
  updatingRole: boolean
  removing: boolean
  transferring: boolean
  pruning: boolean
}

defineProps<Props>()
const emit = defineEmits<{
  setCoHost: [row: MeetParticipantItem]
  revokeCoHost: [row: MeetParticipantItem]
  removeParticipant: [row: MeetParticipantItem]
  transferHost: [row: MeetParticipantItem]
  prune: [inactiveSeconds: number]
}>()

const { t } = useI18n()
const inactiveSeconds = ref(60)
</script>

<template>
  <section class="mm-card meet-participants-card">
    <h2 class="mm-section-title">{{ t('meet.workspace.participants.title') }}</h2>
    <div class="meet-participants-meta">
      <span>{{ t('meet.workspace.participants.manager') }}：{{ t(meetWorkspaceBooleanKey(canManageParticipants)) }}</span>
      <span>{{ t('meet.workspace.participants.transferHost') }}：{{ t(meetWorkspaceBooleanKey(canTransferHost)) }}</span>
    </div>
    <div class="meet-prune-panel">
      <el-input-number v-model="inactiveSeconds" :min="30" :max="86400" />
      <el-button type="warning" :disabled="!canManageParticipants" :loading="pruning" @click="emit('prune', inactiveSeconds)">
        {{ t('meet.workspace.participants.prune') }}
      </el-button>
      <span v-if="lastPruneResult" class="meet-prune-result">
        {{ t('meet.workspace.participants.pruneResult', {
          count: lastPruneResult.removedCount,
          time: formatMeetTime(lastPruneResult.executedAt)
        }) }}
      </span>
    </div>
    <el-table :data="participants" style="width: 100%">
      <el-table-column prop="displayName" :label="t('meet.workspace.participants.columns.name')" min-width="150" />
      <el-table-column :label="t('meet.workspace.participants.columns.role')" min-width="120">
        <template #default="scope">
          <el-tag :type="meetParticipantRoleTagType(scope.row.role)" effect="plain">
            {{ t(meetWorkspaceRoleKey(scope.row.role)) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('meet.workspace.participants.columns.status')" min-width="110">
        <template #default="scope">{{ t(meetWorkspaceParticipantStatusKey(scope.row.status)) }}</template>
      </el-table-column>
      <el-table-column :label="t('meet.workspace.participants.columns.media')" min-width="240">
        <template #default="scope">
          <div class="meet-media-tags">
            <el-tag size="small" :type="scope.row.audioEnabled ? 'success' : 'info'">
              {{ t(meetWorkspaceMediaKey(scope.row.audioEnabled, 'audio')) }}
            </el-tag>
            <el-tag size="small" :type="scope.row.videoEnabled ? 'success' : 'info'">
              {{ t(meetWorkspaceMediaKey(scope.row.videoEnabled, 'video')) }}
            </el-tag>
            <el-tag size="small" :type="scope.row.screenSharing ? 'warning' : 'info'">
              {{ t(meetWorkspaceMediaKey(scope.row.screenSharing, 'screen')) }}
            </el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column :label="t('meet.workspace.participants.columns.self')" min-width="90">
        <template #default="scope">{{ t(meetWorkspaceBooleanKey(scope.row.self)) }}</template>
      </el-table-column>
      <el-table-column :label="t('meet.workspace.participants.columns.heartbeat')" min-width="180">
        <template #default="scope">{{ formatMeetTime(scope.row.lastHeartbeatAt) }}</template>
      </el-table-column>
      <el-table-column :label="t('meet.workspace.participants.columns.actions')" min-width="360">
        <template #default="scope">
          <div class="meet-row-actions">
            <el-button
              v-if="canManageParticipants && !scope.row.self && scope.row.role === 'PARTICIPANT'"
              size="small"
              :loading="updatingRole"
              @click="emit('setCoHost', scope.row)"
            >
              {{ t('meet.workspace.participants.actions.setCoHost') }}
            </el-button>
            <el-button
              v-if="canManageParticipants && !scope.row.self && scope.row.role === 'CO_HOST'"
              size="small"
              :loading="updatingRole"
              @click="emit('revokeCoHost', scope.row)"
            >
              {{ t('meet.workspace.participants.actions.revokeCoHost') }}
            </el-button>
            <el-button
              v-if="canManageParticipants && !scope.row.self && scope.row.role !== 'HOST'"
              size="small"
              type="danger"
              :loading="removing"
              @click="emit('removeParticipant', scope.row)"
            >
              {{ t('meet.workspace.participants.actions.remove') }}
            </el-button>
            <el-button
              v-if="canTransferHost && !scope.row.self && scope.row.role !== 'HOST'"
              size="small"
              type="warning"
              :loading="transferring"
              @click="emit('transferHost', scope.row)"
            >
              {{ t('meet.workspace.participants.actions.transferHost') }}
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<style scoped>
.meet-participants-card {
  padding: 16px;
}

.meet-participants-meta,
.meet-prune-panel,
.meet-row-actions,
.meet-media-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.meet-participants-meta {
  margin: 12px 0 0;
  font-size: 12px;
  color: #64748b;
}

.meet-prune-panel {
  margin: 12px 0;
  align-items: center;
}

.meet-prune-result {
  font-size: 12px;
  color: #64748b;
}
</style>
