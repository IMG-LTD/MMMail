<script setup lang="ts">
import { computed, watch } from 'vue'
import type {
  CreateMeetRoomRequest,
  JoinMeetRoomRequest,
  ReportMeetQualityRequest
} from '~/types/api'
import { useMeetDiagnosticsWorkspace } from '~/composables/useMeetDiagnosticsWorkspace'
import { useI18n } from '~/composables/useI18n'
import { useMeetCollaborationWorkspace } from '~/composables/useMeetCollaborationWorkspace'
import { useMeetRoomLifecycle } from '~/composables/useMeetRoomLifecycle'

const { t } = useI18n()
const roomLifecycle = useMeetRoomLifecycle()
const collaboration = useMeetCollaborationWorkspace(roomLifecycle.currentRoom)
const diagnostics = useMeetDiagnosticsWorkspace({
  currentRoom: roomLifecycle.currentRoom,
  joinedRoomId: collaboration.joinedRoomId,
  selfParticipant: collaboration.selfParticipant,
  participants: collaboration.participants
})

useHead(() => ({
  title: t('page.meet.title')
}))

watch(
  [() => roomLifecycle.workspaceUnlocked.value, () => roomLifecycle.currentRoom.value?.roomId || ''],
  ([unlocked]) => {
    if (!unlocked) {
      collaboration.handleRoomEnded(collaboration.joinedRoomId.value || null)
      diagnostics.clear()
      return
    }
    void onRefreshMeetContext()
  },
  { immediate: true }
)

async function onRefreshMeetContext(): Promise<void> {
  await collaboration.refreshFromContext()
  await diagnostics.refreshFromContext()
}

async function onRefreshWorkspace(): Promise<void> {
  await roomLifecycle.refreshWorkspace(true)
  await onRefreshMeetContext()
}

async function onCreateRoom(payload: CreateMeetRoomRequest): Promise<void> {
  const roomId = await roomLifecycle.onCreateRoom(payload)
  if (roomId) {
    await onRefreshMeetContext()
  }
}

async function onJoinRoom(roomId: string, payload: JoinMeetRoomRequest): Promise<void> {
  const joined = await collaboration.onJoinRoom(roomId, payload)
  if (joined) {
    await diagnostics.refreshFromContext()
  }
}

async function onEndRoom(): Promise<void> {
  const endedRoomId = await roomLifecycle.onEndRoom()
  collaboration.handleRoomEnded(endedRoomId)
  diagnostics.clear()
  await onRefreshMeetContext()
}

async function onToggleAudio(): Promise<void> {
  await collaboration.onUpdateMediaState({
    audioEnabled: !collaboration.mediaState.audioEnabled,
    videoEnabled: collaboration.mediaState.videoEnabled,
    screenSharing: collaboration.mediaState.screenSharing
  })
}

async function onToggleVideo(): Promise<void> {
  await collaboration.onUpdateMediaState({
    audioEnabled: collaboration.mediaState.audioEnabled,
    videoEnabled: !collaboration.mediaState.videoEnabled,
    screenSharing: collaboration.mediaState.screenSharing
  })
}

async function onToggleScreen(): Promise<void> {
  await collaboration.onUpdateMediaState({
    audioEnabled: collaboration.mediaState.audioEnabled,
    videoEnabled: collaboration.mediaState.videoEnabled,
    screenSharing: !collaboration.mediaState.screenSharing
  })
}

async function onReportQuality(payload: ReportMeetQualityRequest): Promise<void> {
  await diagnostics.onReportQuality(payload)
}

async function onSendSignal(payload: { signalType: 'OFFER' | 'ANSWER' | 'ICE'; toParticipantId?: string; payload: string }): Promise<void> {
  await diagnostics.onSendSignal(payload)
}

async function onLeaveRoom(): Promise<void> {
  const left = await collaboration.onLeaveSelf()
  if (left) {
    diagnostics.clear()
  }
}

const workspaceReady = computed(() => roomLifecycle.workspaceUnlocked.value)
</script>

<template>
  <div class="meet-access-stack">
    <MeetAccessRail
      :overview="roomLifecycle.accessOverview.value"
      :loading="roomLifecycle.accessLoading.value"
      :pending-action="roomLifecycle.accessPendingAction.value"
      @refresh="roomLifecycle.loadAccessState"
      @activate="roomLifecycle.onActivateAccess"
      @join-waitlist="roomLifecycle.onJoinWaitlist"
      @contact-sales="roomLifecycle.onRequestEnterpriseAccess"
    />

    <section v-if="!workspaceReady" class="mm-card meet-locked-state">
      <h2 class="mm-section-title">{{ t('meet.access.lockedTitle') }}</h2>
      <p>{{ t('meet.access.lockedDescription') }}</p>
    </section>

    <section v-else class="meet-shell">
      <MeetRoomLifecyclePanel
        :current-room="roomLifecycle.currentRoom.value"
        :current-duration-seconds="roomLifecycle.currentDurationSeconds()"
        :participant-room-label="collaboration.participantRoomLabel.value"
        :is-active="roomLifecycle.isActive.value"
        :creating="roomLifecycle.creating.value"
        :refreshing="roomLifecycle.refreshing.value"
        :joining="collaboration.joining.value"
        :leaving="collaboration.leaving.value"
        :heartbeating="collaboration.heartbeating.value"
        :rotating="roomLifecycle.rotating.value"
        :ending="roomLifecycle.ending.value"
        @refresh="onRefreshWorkspace"
        @create-room="onCreateRoom"
        @join-room="onJoinRoom"
        @leave-room="onLeaveRoom"
        @heartbeat="collaboration.onHeartbeat"
        @rotate-join-code="roomLifecycle.onRotateJoinCode"
        @end-room="onEndRoom"
      />

      <MeetGuestLobbyPanel
        :room="roomLifecycle.currentRoom.value"
        :join-link="collaboration.guestJoinLink.value"
        :requests="collaboration.guestRequests.value"
        :loading="collaboration.guestRequestsLoading.value"
        :action-request-id="collaboration.guestRequestActionId.value"
        @refresh="collaboration.onRefreshGuestLobby"
        @copy-link="collaboration.onCopyGuestJoinLink"
        @approve="collaboration.onApproveGuestRequest"
        @reject="collaboration.onRejectGuestRequest"
      />

      <MeetMediaControlsPanel
        :disabled="!collaboration.selfParticipant.value"
        :loading="collaboration.savingMediaState.value"
        :audio-enabled="collaboration.mediaState.audioEnabled"
        :video-enabled="collaboration.mediaState.videoEnabled"
        :screen-sharing="collaboration.mediaState.screenSharing"
        @toggle-audio="onToggleAudio"
        @toggle-video="onToggleVideo"
        @toggle-screen="onToggleScreen"
      />

      <MeetQualityPanel
        :snapshots="diagnostics.qualitySnapshots.value"
        :disabled="!collaboration.selfParticipant.value"
        :loading="diagnostics.loadingQuality.value"
        :reporting="diagnostics.reportingQuality.value"
        @report="onReportQuality"
      />

      <MeetSignalPanel
        :events="diagnostics.signalEvents.value"
        :signal-cursor="diagnostics.signalCursor.value"
        :target-options="diagnostics.signalTargetOptions.value"
        :disabled="!collaboration.selfParticipant.value"
        :sending="diagnostics.sendingSignal.value"
        :polling="diagnostics.pollingSignals.value"
        :streaming="diagnostics.streamingSignals.value"
        :live-signal-stream-enabled="diagnostics.liveSignalStreamEnabled.value"
        @send="onSendSignal"
        @poll="diagnostics.onPollSignals"
        @stream-once="diagnostics.onStreamSignals"
        @toggle-live="diagnostics.onToggleLiveSignalStream"
      />

      <MeetParticipantsPanel
        :participants="collaboration.participants.value"
        :can-manage-participants="collaboration.canManageParticipants.value"
        :can-transfer-host="collaboration.canTransferHost.value"
        :last-prune-result="collaboration.lastPruneResult.value"
        :updating-role="collaboration.updatingRole.value"
        :removing="collaboration.removing.value"
        :transferring="collaboration.transferring.value"
        :pruning="collaboration.pruning.value"
        @set-co-host="collaboration.onSetCoHost"
        @revoke-co-host="collaboration.onRevokeCoHost"
        @remove-participant="collaboration.onRemoveParticipant"
        @transfer-host="collaboration.onTransferHost"
        @prune="collaboration.onPruneInactiveParticipants"
      />

      <MeetRoomTablesPanel :rooms="roomLifecycle.rooms.value" :history="roomLifecycle.history.value" />
    </section>
  </div>
</template>

<style scoped>
.meet-access-stack {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.meet-locked-state {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 22px;
}

.meet-locked-state p {
  margin: 0;
  color: var(--el-text-color-secondary);
}

.meet-shell {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
</style>
