<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type { MeetGuestRequestItem, MeetRoomItem } from '~/types/api'
import { canUseMeetGuestJoin, pendingMeetGuestRequests } from '~/utils/meet-guest'

interface Props {
  room: MeetRoomItem | null
  joinLink: string
  requests: MeetGuestRequestItem[]
  loading: boolean
  actionRequestId: string
}

const props = defineProps<Props>()
const emit = defineEmits<{
  refresh: []
  copyLink: []
  approve: [requestId: string]
  reject: [requestId: string]
}>()

const { t } = useI18n()

const canExposeJoin = computed(() => canUseMeetGuestJoin(props.room))
const pendingRequests = computed(() => pendingMeetGuestRequests(props.requests))

function requestTagType(status: MeetGuestRequestItem['status']): 'info' | 'warning' | 'success' | 'danger' {
  if (status === 'APPROVED') {
    return 'success'
  }
  if (status === 'PENDING') {
    return 'warning'
  }
  if (status === 'REJECTED') {
    return 'danger'
  }
  return 'info'
}

function accessLevelLabel(accessLevel: MeetRoomItem['accessLevel']): string {
  return t(`meet.guest.join.accessLevel.${accessLevel}`)
}
</script>

<template>
  <section class="mm-card meet-guest-lobby" v-loading="loading">
    <div class="meet-guest-lobby__hero">
      <div>
        <el-tag size="small" effect="plain" type="warning">{{ t('meet.guest.host.badge') }}</el-tag>
        <h2 class="mm-section-title">{{ t('meet.guest.host.title') }}</h2>
        <p class="meet-guest-lobby__description">{{ t('meet.guest.host.description') }}</p>
      </div>
      <div class="meet-guest-lobby__actions">
        <el-button plain @click="emit('refresh')">{{ t('common.actions.refresh') }}</el-button>
        <el-button type="primary" plain :disabled="!canExposeJoin" @click="emit('copyLink')">
          {{ t('meet.guest.host.copyLink') }}
        </el-button>
      </div>
    </div>

    <div v-if="room" class="meet-guest-lobby__metrics">
      <div class="meet-guest-lobby__metric">
        <span>{{ t('meet.guest.host.accessLabel') }}</span>
        <strong>{{ accessLevelLabel(room.accessLevel) }}</strong>
      </div>
      <div class="meet-guest-lobby__metric">
        <span>{{ t('meet.guest.host.pendingLabel') }}</span>
        <strong>{{ pendingRequests.length }}</strong>
      </div>
      <div class="meet-guest-lobby__metric">
        <span>{{ t('meet.guest.host.linkLabel') }}</span>
        <strong>{{ canExposeJoin ? t('meet.guest.host.linkReady') : t('meet.guest.host.linkDisabledShort') }}</strong>
      </div>
    </div>

    <div v-if="room" class="meet-guest-lobby__link-card" :class="{ 'is-disabled': !canExposeJoin }">
      <div>
        <p class="meet-guest-lobby__link-label">{{ t('meet.guest.host.joinLinkTitle') }}</p>
        <p class="meet-guest-lobby__link-value">{{ canExposeJoin ? joinLink : t('meet.guest.host.linkUnavailable') }}</p>
      </div>
      <a
        v-if="canExposeJoin"
        class="meet-guest-lobby__open"
        :href="joinLink"
        target="_blank"
        rel="noreferrer"
      >
        {{ t('meet.guest.host.openLink') }}
      </a>
    </div>

    <el-alert
      v-if="room && !canExposeJoin"
      type="info"
      :closable="false"
      :title="t('meet.guest.host.linkDisabledTitle')"
      :description="t('meet.guest.host.linkDisabledDescription')"
      show-icon
    />

    <div v-if="!room" class="meet-guest-lobby__empty">
      <el-empty :description="t('meet.guest.host.noRoom')" />
    </div>

    <div v-else class="meet-guest-lobby__queue">
      <div class="meet-guest-lobby__queue-head">
        <h3>{{ t('meet.guest.host.queueTitle') }}</h3>
        <span>{{ t('meet.guest.host.queueCount', { count: requests.length }) }}</span>
      </div>
      <el-table :data="requests" style="width: 100%">
        <el-table-column prop="displayName" :label="t('meet.guest.host.columns.name')" min-width="140" />
        <el-table-column :label="t('meet.guest.host.columns.preflight')" min-width="180">
          <template #default="scope">
            <div class="meet-guest-lobby__chips">
              <el-tag size="small" :type="scope.row.audioEnabled ? 'success' : 'info'">
                {{ scope.row.audioEnabled ? t('meet.guest.join.audioOn') : t('meet.guest.join.audioOff') }}
              </el-tag>
              <el-tag size="small" :type="scope.row.videoEnabled ? 'success' : 'info'">
                {{ scope.row.videoEnabled ? t('meet.guest.join.videoOn') : t('meet.guest.join.videoOff') }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="t('meet.guest.host.columns.status')" min-width="150">
          <template #default="scope">
            <el-tag :type="requestTagType(scope.row.status)" effect="plain">
              {{ t(`meet.guest.request.status.${scope.row.status}`) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('meet.guest.host.columns.requestedAt')" min-width="180">
          <template #default="scope">{{ scope.row.requestedAt.replace('T', ' ').slice(0, 16) }}</template>
        </el-table-column>
        <el-table-column :label="t('meet.guest.host.columns.actions')" min-width="220">
          <template #default="scope">
            <div class="meet-guest-lobby__row-actions">
              <el-button
                v-if="scope.row.status === 'PENDING'"
                size="small"
                type="primary"
                :loading="actionRequestId === scope.row.requestId"
                @click="emit('approve', scope.row.requestId)"
              >
                {{ t('meet.guest.host.approve') }}
              </el-button>
              <el-button
                v-if="scope.row.status === 'PENDING'"
                size="small"
                type="danger"
                plain
                :loading="actionRequestId === scope.row.requestId"
                @click="emit('reject', scope.row.requestId)"
              >
                {{ t('meet.guest.host.reject') }}
              </el-button>
              <span v-if="scope.row.status !== 'PENDING'" class="meet-guest-lobby__resolved">
                {{ t(`meet.guest.request.status.${scope.row.status}`) }}
              </span>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>

<style scoped>
.meet-guest-lobby {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 22px;
  border: 1px solid rgba(109, 74, 255, 0.12);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(245, 243, 255, 0.92)),
    radial-gradient(circle at top right, rgba(109, 74, 255, 0.08), transparent 30%);
}

.meet-guest-lobby__hero,
.meet-guest-lobby__actions,
.meet-guest-lobby__metrics,
.meet-guest-lobby__queue-head,
.meet-guest-lobby__row-actions {
  display: flex;
}

.meet-guest-lobby__hero,
.meet-guest-lobby__queue-head {
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.meet-guest-lobby__description {
  margin: 8px 0 0;
  max-width: 760px;
  line-height: 1.6;
  color: #5b5873;
}

.meet-guest-lobby__actions,
.meet-guest-lobby__row-actions {
  gap: 10px;
  align-items: center;
}

.meet-guest-lobby__metrics {
  gap: 12px;
  flex-wrap: wrap;
}

.meet-guest-lobby__metric {
  min-width: 160px;
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid rgba(109, 74, 255, 0.12);
  background: rgba(255, 255, 255, 0.88);
}

.meet-guest-lobby__metric span,
.meet-guest-lobby__link-label {
  display: block;
  font-size: 12px;
  color: #7a7598;
}

.meet-guest-lobby__metric strong {
  display: block;
  margin-top: 6px;
  font-size: 18px;
  color: #251f43;
}

.meet-guest-lobby__link-card {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  border-radius: 16px;
  border: 1px solid rgba(109, 74, 255, 0.16);
  background: rgba(255, 255, 255, 0.9);
}

.meet-guest-lobby__link-card.is-disabled {
  opacity: 0.72;
}

.meet-guest-lobby__link-value {
  margin: 6px 0 0;
  font-size: 14px;
  font-weight: 600;
  color: #342e56;
  word-break: break-all;
}

.meet-guest-lobby__open {
  align-self: center;
  color: #6d4aff;
  font-weight: 600;
}

.meet-guest-lobby__queue h3 {
  margin: 0;
  font-size: 18px;
  color: #251f43;
}

.meet-guest-lobby__chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.meet-guest-lobby__resolved {
  color: #6b6885;
  font-size: 13px;
}

@media (max-width: 900px) {
  .meet-guest-lobby__link-card {
    flex-direction: column;
  }
}
</style>
