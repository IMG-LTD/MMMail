<script setup lang="ts">
import type { MeetRoomItem } from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import {
  formatMeetDuration,
  formatMeetTime,
  meetRoomStatusTagType,
  meetWorkspaceAccessLevelKey,
  meetWorkspaceRoomStatusKey
} from '~/utils/meet-workspace'

interface Props {
  rooms: MeetRoomItem[]
  history: MeetRoomItem[]
}

defineProps<Props>()

const { t } = useI18n()
</script>

<template>
  <section class="meet-room-table-grid">
    <article class="mm-card meet-room-table-card">
      <h2 class="mm-section-title">{{ t('meet.workspace.rooms.title') }}</h2>
      <el-table :data="rooms" style="width: 100%">
        <el-table-column prop="roomCode" :label="t('meet.workspace.tables.columns.room')" min-width="120" />
        <el-table-column prop="topic" :label="t('meet.workspace.tables.columns.topic')" min-width="180" />
        <el-table-column :label="t('meet.workspace.tables.columns.access')" min-width="110">
          <template #default="scope">{{ t(meetWorkspaceAccessLevelKey(scope.row.accessLevel)) }}</template>
        </el-table-column>
        <el-table-column :label="t('meet.workspace.tables.columns.status')" min-width="110">
          <template #default="scope">
            <el-tag :type="meetRoomStatusTagType(scope.row.status)" effect="plain">
              {{ t(meetWorkspaceRoomStatusKey(scope.row.status)) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="maxParticipants" :label="t('meet.workspace.tables.columns.max')" min-width="100" />
        <el-table-column :label="t('meet.workspace.tables.columns.startedAt')" min-width="180">
          <template #default="scope">{{ formatMeetTime(scope.row.startedAt) }}</template>
        </el-table-column>
      </el-table>
    </article>

    <article class="mm-card meet-room-table-card">
      <h2 class="mm-section-title">{{ t('meet.workspace.history.title') }}</h2>
      <el-table :data="history" style="width: 100%">
        <el-table-column prop="roomCode" :label="t('meet.workspace.tables.columns.room')" min-width="120" />
        <el-table-column prop="topic" :label="t('meet.workspace.tables.columns.topic')" min-width="180" />
        <el-table-column :label="t('meet.workspace.tables.columns.access')" min-width="110">
          <template #default="scope">{{ t(meetWorkspaceAccessLevelKey(scope.row.accessLevel)) }}</template>
        </el-table-column>
        <el-table-column :label="t('meet.workspace.tables.columns.startedAt')" min-width="180">
          <template #default="scope">{{ formatMeetTime(scope.row.startedAt) }}</template>
        </el-table-column>
        <el-table-column :label="t('meet.workspace.tables.columns.endedAt')" min-width="180">
          <template #default="scope">{{ formatMeetTime(scope.row.endedAt) }}</template>
        </el-table-column>
        <el-table-column :label="t('meet.workspace.tables.columns.duration')" min-width="120">
          <template #default="scope">{{ formatMeetDuration(scope.row.durationSeconds) }}</template>
        </el-table-column>
      </el-table>
    </article>
  </section>
</template>

<style scoped>
.meet-room-table-grid {
  display: grid;
  gap: 16px;
}

.meet-room-table-card {
  padding: 16px;
}
</style>
