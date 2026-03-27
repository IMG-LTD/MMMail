import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onBeforeUnmount, reactive, ref, type Ref } from 'vue'
import type {
  JoinMeetRoomRequest,
  MeetParticipantItem,
  MeetPruneInactiveResult,
  MeetRoomItem,
  UpdateMeetParticipantMediaRequest
} from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import { useMeetApi } from '~/composables/useMeetApi'
import { useMeetGuestHostLobby } from '~/composables/useMeetGuestHostLobby'

export function useMeetCollaborationWorkspace(currentRoom: Ref<MeetRoomItem | null>) {
  const { t } = useI18n()
  const {
    heartbeatParticipant,
    joinRoom,
    leaveParticipant,
    listParticipants,
    pruneInactiveParticipants,
    removeParticipant,
    transferHost,
    updateParticipantMediaState,
    updateParticipantRole
  } = useMeetApi()

  const joining = ref(false)
  const leaving = ref(false)
  const heartbeating = ref(false)
  const savingMediaState = ref(false)
  const updatingRole = ref(false)
  const removing = ref(false)
  const transferring = ref(false)
  const pruning = ref(false)

  const participants = ref<MeetParticipantItem[]>([])
  const selfParticipant = ref<MeetParticipantItem | null>(null)
  const joinedRoomId = ref('')
  const lastPruneResult = ref<MeetPruneInactiveResult | null>(null)
  const heartbeatTimer = ref<ReturnType<typeof setInterval> | null>(null)

  const mediaState = reactive({
    audioEnabled: true,
    videoEnabled: true,
    screenSharing: false
  })

  const canManageParticipants = computed(() => participants.value[0]?.canManageParticipants ?? false)
  const canTransferHost = computed(() => participants.value[0]?.canTransferHost ?? false)
  const participantRoomLabel = computed(() => currentRoom.value?.roomCode || joinedRoomId.value || '-')

  const {
    actionRequestId: guestRequestActionId,
    approve: approveGuestRequest,
    copyLink: copyGuestJoinLink,
    joinLink: guestJoinLink,
    loading: guestRequestsLoading,
    refresh: refreshGuestRequests,
    reject: rejectGuestRequest,
    requests: guestRequests,
    reset: resetGuestRequests
  } = useMeetGuestHostLobby({
    room: currentRoom,
    afterRequestResolved: async () => {
      await refreshParticipants(currentRoom.value?.roomId, true)
    }
  })

  function resolveParticipantRoomId(): string | null {
    if (currentRoom.value?.status === 'ACTIVE') {
      return currentRoom.value.roomId
    }
    return joinedRoomId.value || null
  }

  function syncMediaStateFromSelf(): void {
    if (!selfParticipant.value) {
      mediaState.audioEnabled = true
      mediaState.videoEnabled = true
      mediaState.screenSharing = false
      return
    }
    mediaState.audioEnabled = selfParticipant.value.audioEnabled
    mediaState.videoEnabled = selfParticipant.value.videoEnabled
    mediaState.screenSharing = selfParticipant.value.screenSharing
  }

  function clearHeartbeatTicker(): void {
    if (!heartbeatTimer.value) {
      return
    }
    clearInterval(heartbeatTimer.value)
    heartbeatTimer.value = null
  }

  function startHeartbeatTicker(): void {
    clearHeartbeatTicker()
    const roomId = resolveParticipantRoomId()
    if (!roomId || !selfParticipant.value) {
      return
    }
    heartbeatTimer.value = setInterval(() => {
      void onHeartbeat(true)
    }, 15000)
  }

  function upsertParticipant(updated: MeetParticipantItem): void {
    participants.value = participants.value.map((item) => (item.participantId === updated.participantId ? updated : item))
    if (updated.self) {
      selfParticipant.value = updated
      syncMediaStateFromSelf()
    }
  }

  function resetParticipantState(clearJoinedRoom = false): void {
    participants.value = []
    selfParticipant.value = null
    lastPruneResult.value = null
    clearHeartbeatTicker()
    syncMediaStateFromSelf()
    if (clearJoinedRoom) {
      joinedRoomId.value = ''
    }
  }

  async function refreshParticipants(roomId?: string, silent = false): Promise<void> {
    const targetRoomId = roomId || resolveParticipantRoomId()
    if (!targetRoomId) {
      resetParticipantState(false)
      return
    }
    try {
      const list = await listParticipants(targetRoomId)
      participants.value = list
      selfParticipant.value = list.find((item) => item.self) ?? null
      joinedRoomId.value = targetRoomId
      syncMediaStateFromSelf()
      startHeartbeatTicker()
    } catch (error) {
      if (!silent) {
        ElMessage.error((error as Error).message || t('meet.workspace.messages.loadParticipantsFailed'))
      }
    }
  }

  async function refreshFromContext(): Promise<void> {
    if (currentRoom.value?.roomId) {
      await refreshGuestRequests(false)
    } else {
      resetGuestRequests()
    }
    await refreshParticipants(undefined, true)
  }

  async function onJoinRoom(roomIdRaw: string, payload: JoinMeetRoomRequest): Promise<boolean> {
    const roomId = roomIdRaw.trim()
    const displayName = payload.displayName.trim()
    if (!roomId) {
      ElMessage.warning(t('meet.workspace.messages.roomIdRequired'))
      return false
    }
    if (displayName.length < 2) {
      ElMessage.warning(t('meet.workspace.messages.displayNameInvalid'))
      return false
    }
    joining.value = true
    try {
      selfParticipant.value = await joinRoom(roomId, { displayName })
      joinedRoomId.value = roomId
      syncMediaStateFromSelf()
      await refreshParticipants(roomId, true)
      ElMessage.success(t('meet.workspace.messages.joinedRoom'))
      return true
    } catch (error) {
      ElMessage.error((error as Error).message || t('meet.workspace.messages.joinFailed'))
      return false
    } finally {
      joining.value = false
    }
  }

  async function onHeartbeat(silent = false): Promise<void> {
    const roomId = resolveParticipantRoomId()
    if (!roomId || !selfParticipant.value) {
      return
    }
    if (!silent) {
      heartbeating.value = true
    }
    try {
      const updated = await heartbeatParticipant(roomId, selfParticipant.value.participantId)
      upsertParticipant(updated)
      if (!silent) {
        ElMessage.success(t('meet.workspace.messages.heartbeatSent'))
      }
    } catch (error) {
      if (!silent) {
        ElMessage.error((error as Error).message || t('meet.workspace.messages.heartbeatFailed'))
      }
    } finally {
      heartbeating.value = false
    }
  }

  async function onUpdateMediaState(nextState: UpdateMeetParticipantMediaRequest): Promise<void> {
    const roomId = resolveParticipantRoomId()
    if (!roomId || !selfParticipant.value) {
      ElMessage.warning(t('meet.workspace.messages.joinBeforeUpdateMedia'))
      return
    }
    savingMediaState.value = true
    try {
      const updated = await updateParticipantMediaState(roomId, selfParticipant.value.participantId, nextState)
      upsertParticipant(updated)
      ElMessage.success(t('meet.workspace.messages.mediaUpdated'))
    } catch (error) {
      ElMessage.error((error as Error).message || t('meet.workspace.messages.updateMediaFailed'))
    } finally {
      savingMediaState.value = false
    }
  }

  async function onLeaveSelf(): Promise<boolean> {
    const roomId = resolveParticipantRoomId()
    if (!roomId || !selfParticipant.value) {
      ElMessage.warning(t('meet.workspace.messages.noJoinedParticipant'))
      return false
    }
    leaving.value = true
    try {
      await leaveParticipant(roomId, selfParticipant.value.participantId)
      selfParticipant.value = null
      await refreshParticipants(roomId, true)
      clearHeartbeatTicker()
      ElMessage.success(t('meet.workspace.messages.leftRoom'))
      return true
    } catch (error) {
      ElMessage.error((error as Error).message || t('meet.workspace.messages.leaveFailed'))
      return false
    } finally {
      leaving.value = false
    }
  }

  async function onSetCoHost(row: MeetParticipantItem): Promise<void> {
    const roomId = resolveParticipantRoomId()
    if (!roomId) {
      ElMessage.warning(t('meet.workspace.messages.noActiveRoomContext'))
      return
    }
    updatingRole.value = true
    try {
      await updateParticipantRole(roomId, row.participantId, 'CO_HOST')
      await refreshParticipants(roomId, true)
      ElMessage.success(t('meet.workspace.messages.cohostGranted'))
    } catch (error) {
      ElMessage.error((error as Error).message || t('meet.workspace.messages.setCohostFailed'))
    } finally {
      updatingRole.value = false
    }
  }

  async function onRevokeCoHost(row: MeetParticipantItem): Promise<void> {
    const roomId = resolveParticipantRoomId()
    if (!roomId) {
      ElMessage.warning(t('meet.workspace.messages.noActiveRoomContext'))
      return
    }
    updatingRole.value = true
    try {
      await updateParticipantRole(roomId, row.participantId, 'PARTICIPANT')
      await refreshParticipants(roomId, true)
      ElMessage.success(t('meet.workspace.messages.cohostRevoked'))
    } catch (error) {
      ElMessage.error((error as Error).message || t('meet.workspace.messages.revokeCohostFailed'))
    } finally {
      updatingRole.value = false
    }
  }

  async function onRemoveParticipant(row: MeetParticipantItem): Promise<void> {
    const roomId = resolveParticipantRoomId()
    if (!roomId) {
      ElMessage.warning(t('meet.workspace.messages.noActiveRoomContext'))
      return
    }
    try {
      await ElMessageBox.confirm(
        t('meet.workspace.messages.removeParticipantConfirm', { name: row.displayName }),
        t('meet.workspace.messages.removeParticipantTitle'),
        { type: 'warning', confirmButtonText: t('meet.workspace.participants.actions.remove'), cancelButtonText: t('common.actions.cancel') }
      )
    } catch {
      return
    }
    removing.value = true
    try {
      await removeParticipant(roomId, row.participantId)
      await refreshParticipants(roomId, true)
      ElMessage.success(t('meet.workspace.messages.participantRemoved'))
    } catch (error) {
      ElMessage.error((error as Error).message || t('meet.workspace.messages.removeParticipantFailed'))
    } finally {
      removing.value = false
    }
  }

  async function onTransferHost(row: MeetParticipantItem): Promise<void> {
    const roomId = resolveParticipantRoomId()
    if (!roomId) {
      ElMessage.warning(t('meet.workspace.messages.noActiveRoomContext'))
      return
    }
    try {
      await ElMessageBox.confirm(
        t('meet.workspace.messages.transferHostConfirm', { name: row.displayName }),
        t('meet.workspace.messages.transferHostTitle'),
        { type: 'warning', confirmButtonText: t('meet.workspace.participants.actions.transferHost'), cancelButtonText: t('common.actions.cancel') }
      )
    } catch {
      return
    }
    transferring.value = true
    try {
      await transferHost(roomId, row.participantId)
      await refreshParticipants(roomId, true)
      ElMessage.success(t('meet.workspace.messages.hostTransferred'))
    } catch (error) {
      ElMessage.error((error as Error).message || t('meet.workspace.messages.transferHostFailed'))
    } finally {
      transferring.value = false
    }
  }

  async function onPruneInactiveParticipants(inactiveSeconds: number): Promise<void> {
    const roomId = resolveParticipantRoomId()
    if (!roomId) {
      ElMessage.warning(t('meet.workspace.messages.noActiveRoomContext'))
      return
    }
    if (inactiveSeconds < 30 || inactiveSeconds > 86400) {
      ElMessage.warning(t('meet.workspace.messages.inactiveSecondsInvalid'))
      return
    }
    try {
      await ElMessageBox.confirm(
        t('meet.workspace.messages.pruneConfirmMessage', { seconds: inactiveSeconds }),
        t('meet.workspace.messages.pruneConfirmTitle'),
        { type: 'warning', confirmButtonText: t('meet.workspace.participants.prune'), cancelButtonText: t('common.actions.cancel') }
      )
    } catch {
      return
    }
    pruning.value = true
    try {
      lastPruneResult.value = await pruneInactiveParticipants(roomId, { inactiveSeconds })
      await refreshParticipants(roomId, true)
      ElMessage.success(t('meet.workspace.messages.prunedParticipants', { count: lastPruneResult.value.removedCount }))
    } catch (error) {
      ElMessage.error((error as Error).message || t('meet.workspace.messages.pruneParticipantsFailed'))
    } finally {
      pruning.value = false
    }
  }

  function handleRoomEnded(endedRoomId: string | null): void {
    if (endedRoomId && joinedRoomId.value === endedRoomId) {
      resetParticipantState(true)
    }
    resetGuestRequests()
  }

  onBeforeUnmount(() => {
    clearHeartbeatTicker()
  })

  return {
    joining,
    leaving,
    heartbeating,
    savingMediaState,
    updatingRole,
    removing,
    transferring,
    pruning,
    participants,
    selfParticipant,
    mediaState,
    joinedRoomId,
    lastPruneResult,
    canManageParticipants,
    canTransferHost,
    participantRoomLabel,
    guestRequestActionId,
    guestJoinLink,
    guestRequestsLoading,
    guestRequests,
    refreshParticipants,
    refreshFromContext,
    onJoinRoom,
    onHeartbeat,
    onUpdateMediaState,
    onLeaveSelf,
    onSetCoHost,
    onRevokeCoHost,
    onRemoveParticipant,
    onTransferHost,
    onPruneInactiveParticipants,
    onRefreshGuestLobby: refreshGuestRequests,
    onCopyGuestJoinLink: copyGuestJoinLink,
    onApproveGuestRequest: approveGuestRequest,
    onRejectGuestRequest: rejectGuestRequest,
    handleRoomEnded
  }
}
