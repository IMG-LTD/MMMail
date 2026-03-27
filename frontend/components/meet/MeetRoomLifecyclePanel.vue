<script setup lang="ts">
import { reactive } from 'vue'
import type {
  CreateMeetRoomRequest,
  JoinMeetRoomRequest,
  MeetRoomItem
} from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import {
  formatMeetDuration,
  formatMeetTime,
  meetRoomStatusTagType,
  meetWorkspaceAccessLevelKey,
  meetWorkspaceRoomStatusKey
} from '~/utils/meet-workspace'

interface Props {
  currentRoom: MeetRoomItem | null
  currentDurationSeconds: number
  participantRoomLabel: string
  isActive: boolean
  creating: boolean
  refreshing: boolean
  joining: boolean
  leaving: boolean
  heartbeating: boolean
  rotating: boolean
  ending: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{
  refresh: []
  createRoom: [payload: CreateMeetRoomRequest]
  joinRoom: [roomId: string, payload: JoinMeetRoomRequest]
  leaveRoom: []
  heartbeat: []
  rotateJoinCode: []
  endRoom: []
}>()

const { t } = useI18n()

const createForm = reactive<CreateMeetRoomRequest>({
  topic: '',
  accessLevel: 'PRIVATE',
  maxParticipants: 20
})

const joinForm = reactive({
  roomId: '',
  displayName: ''
})

function onCreateRoom(): void {
  emit('createRoom', {
    topic: createForm.topic,
    accessLevel: createForm.accessLevel,
    maxParticipants: createForm.maxParticipants
  })
}

function onJoinRoom(): void {
  emit('joinRoom', joinForm.roomId, { displayName: joinForm.displayName })
}
</script>

<template>
  <section class="meet-lifecycle-grid">
    <article class="mm-card meet-lifecycle-card">
      <div class="meet-lifecycle-head">
        <div>
          <h2 class="mm-section-title">{{ t('meet.workspace.lifecycle.createTitle') }}</h2>
          <p class="mm-muted">{{ t('meet.workspace.subtitle') }}</p>
        </div>
        <el-button :loading="refreshing" @click="emit('refresh')">{{ t('common.actions.refresh') }}</el-button>
      </div>
      <div class="meet-form-grid">
        <el-input
          v-model="createForm.topic"
          :placeholder="t('meet.workspace.lifecycle.topicPlaceholder')"
          maxlength="128"
          show-word-limit
        />
        <el-select v-model="createForm.accessLevel" :placeholder="t('meet.workspace.lifecycle.accessPlaceholder')">
          <el-option :label="t('meet.workspace.accessLevel.PRIVATE')" value="PRIVATE" />
          <el-option :label="t('meet.workspace.accessLevel.PUBLIC')" value="PUBLIC" />
        </el-select>
        <el-input-number v-model="createForm.maxParticipants" :min="2" :max="200" />
        <el-button type="primary" :loading="creating" @click="onCreateRoom">{{ t('common.actions.create') }}</el-button>
      </div>
    </article>

    <article class="mm-card meet-lifecycle-card">
      <h2 class="mm-section-title">{{ t('meet.workspace.lifecycle.joinTitle') }}</h2>
      <div class="meet-form-grid">
        <el-input v-model="joinForm.roomId" :placeholder="t('meet.workspace.lifecycle.roomIdPlaceholder')" />
        <el-input
          v-model="joinForm.displayName"
          :placeholder="t('meet.workspace.lifecycle.displayNamePlaceholder')"
          maxlength="64"
        />
        <el-button type="success" :loading="joining" @click="onJoinRoom">
          {{ t('meet.workspace.lifecycle.actions.join') }}
        </el-button>
        <el-button :loading="leaving" @click="emit('leaveRoom')">
          {{ t('meet.workspace.lifecycle.actions.leave') }}
        </el-button>
        <el-button :loading="heartbeating" @click="emit('heartbeat')">
          {{ t('meet.workspace.lifecycle.actions.heartbeat') }}
        </el-button>
      </div>
      <p class="meet-lifecycle-tip">
        {{ t('meet.workspace.lifecycle.participantRoom', { room: participantRoomLabel }) }}
      </p>
    </article>

    <article class="mm-card meet-current-card">
      <div class="meet-lifecycle-head">
        <h2 class="mm-section-title">{{ t('meet.workspace.lifecycle.currentTitle') }}</h2>
        <div class="meet-current-actions">
          <el-button type="warning" :disabled="!isActive" :loading="rotating" @click="emit('rotateJoinCode')">
            {{ t('meet.workspace.lifecycle.actions.rotateJoinCode') }}
          </el-button>
          <el-button type="danger" :disabled="!isActive" :loading="ending" @click="emit('endRoom')">
            {{ t('meet.workspace.lifecycle.actions.endRoom') }}
          </el-button>
        </div>
      </div>
      <div v-if="currentRoom" class="meet-current-grid">
        <div class="meet-current-item">
          <span>{{ t('meet.workspace.lifecycle.current.status') }}</span>
          <el-tag :type="meetRoomStatusTagType(currentRoom.status)" effect="plain">
            {{ t(meetWorkspaceRoomStatusKey(currentRoom.status)) }}
          </el-tag>
        </div>
        <div class="meet-current-item">
          <span>{{ t('meet.workspace.lifecycle.current.room') }}</span>
          <strong>{{ currentRoom.roomCode }}</strong>
        </div>
        <div class="meet-current-item">
          <span>{{ t('meet.workspace.lifecycle.current.topic') }}</span>
          <strong>{{ currentRoom.topic }}</strong>
        </div>
        <div class="meet-current-item">
          <span>{{ t('meet.workspace.lifecycle.current.access') }}</span>
          <strong>{{ t(meetWorkspaceAccessLevelKey(currentRoom.accessLevel)) }}</strong>
        </div>
        <div class="meet-current-item">
          <span>{{ t('meet.workspace.lifecycle.current.joinCode') }}</span>
          <strong>{{ currentRoom.joinCode || '-' }}</strong>
        </div>
        <div class="meet-current-item">
          <span>{{ t('meet.workspace.lifecycle.current.duration') }}</span>
          <strong>{{ formatMeetDuration(currentDurationSeconds) }}</strong>
        </div>
        <div class="meet-current-item">
          <span>{{ t('meet.workspace.lifecycle.current.startedAt') }}</span>
          <strong>{{ formatMeetTime(currentRoom.startedAt) }}</strong>
        </div>
        <div class="meet-current-item">
          <span>{{ t('meet.workspace.lifecycle.current.maxParticipants') }}</span>
          <strong>{{ currentRoom.maxParticipants }}</strong>
        </div>
      </div>
      <el-empty v-else :description="t('meet.workspace.lifecycle.current.empty')" />
    </article>
  </section>
</template>

<style scoped>
.meet-lifecycle-grid {
  display: grid;
  gap: 16px;
}

.meet-lifecycle-card,
.meet-current-card {
  padding: 16px;
}

.meet-lifecycle-head,
.meet-current-actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.meet-form-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.meet-lifecycle-tip {
  margin: 10px 0 0;
  font-size: 12px;
  color: #64748b;
}

.meet-current-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.meet-current-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px;
  border-radius: 12px;
  background: #f8fafc;
}

.meet-current-item span {
  font-size: 12px;
  color: #64748b;
}

@media (max-width: 1200px) {
  .meet-form-grid,
  .meet-current-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .meet-form-grid,
  .meet-current-grid {
    grid-template-columns: 1fr;
  }
}
</style>
