import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import type {
  CreateMeetRoomRequest,
  MeetAccessLevel,
  MeetAccessOverview,
  MeetRoomItem
} from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import { useMeetApi } from '~/composables/useMeetApi'
import { meetWorkspaceUnlocked } from '~/utils/meet-access'

type EnterpriseAccessPayload = {
  companyName: string
  requestedSeats: number
  note?: string
}

export function useMeetRoomLifecycle() {
  const { t } = useI18n()
  const {
    activateMeetAccess,
    createRoom,
    endRoom,
    getAccessOverview,
    getCurrentRoom,
    joinMeetWaitlist,
    listRoomHistory,
    listRooms,
    requestMeetEnterpriseAccess,
    rotateJoinCode
  } = useMeetApi()

  const accessOverview = ref<MeetAccessOverview | null>(null)
  const accessLoading = ref(false)
  const accessPendingAction = ref<'' | 'waitlist' | 'activate' | 'contact-sales'>('')
  const loading = ref(false)
  const refreshing = ref(false)
  const creating = ref(false)
  const rotating = ref(false)
  const ending = ref(false)
  const rooms = ref<MeetRoomItem[]>([])
  const currentRoom = ref<MeetRoomItem | null>(null)
  const history = ref<MeetRoomItem[]>([])
  const nowEpoch = ref(Date.now())
  const roomTimer = ref<ReturnType<typeof setInterval> | null>(null)

  const isActive = computed(() => currentRoom.value?.status === 'ACTIVE')
  const workspaceUnlocked = computed(() => meetWorkspaceUnlocked(accessOverview.value))

  function clearRoomTicker(): void {
    if (!roomTimer.value) {
      return
    }
    clearInterval(roomTimer.value)
    roomTimer.value = null
  }

  function startRoomTicker(): void {
    clearRoomTicker()
    if (!isActive.value) {
      return
    }
    roomTimer.value = setInterval(() => {
      nowEpoch.value = Date.now()
    }, 1000)
  }

  function resetWorkspaceState(): void {
    rooms.value = []
    currentRoom.value = null
    history.value = []
    clearRoomTicker()
  }

  function currentDurationSeconds(): number {
    if (!currentRoom.value) {
      return 0
    }
    if (currentRoom.value.status !== 'ACTIVE') {
      return currentRoom.value.durationSeconds
    }
    const startedAt = new Date(currentRoom.value.startedAt).getTime()
    return Math.max(0, Math.floor((nowEpoch.value - startedAt) / 1000))
  }

  async function loadRoomRecords(showMessage: boolean): Promise<void> {
    const [roomList, current, historyList] = await Promise.all([
      listRooms(undefined, 20),
      getCurrentRoom(),
      listRoomHistory(20)
    ])
    rooms.value = roomList
    currentRoom.value = current
    history.value = historyList
    nowEpoch.value = Date.now()
    startRoomTicker()
    if (showMessage) {
      ElMessage.success(t('meet.workspace.messages.refreshed'))
    }
  }

  async function refreshWorkspace(showMessage = false): Promise<void> {
    if (showMessage) {
      refreshing.value = true
    } else {
      loading.value = true
    }
    try {
      await loadRoomRecords(showMessage)
    } catch (error) {
      ElMessage.error((error as Error).message || t('meet.workspace.messages.loadWorkspaceFailed'))
    } finally {
      loading.value = false
      refreshing.value = false
    }
  }

  async function loadAccessState(): Promise<void> {
    accessLoading.value = true
    try {
      accessOverview.value = await getAccessOverview()
      if (workspaceUnlocked.value) {
        await refreshWorkspace(false)
      } else {
        resetWorkspaceState()
      }
    } catch (error) {
      ElMessage.error((error as Error).message || t('meet.access.loadFailed'))
    } finally {
      accessLoading.value = false
    }
  }

  async function onJoinWaitlist(note?: string): Promise<void> {
    accessPendingAction.value = 'waitlist'
    try {
      accessOverview.value = await joinMeetWaitlist({ note })
      ElMessage.success(t('meet.access.waitlist.done'))
    } catch (error) {
      ElMessage.error((error as Error).message || t('meet.access.loadFailed'))
    } finally {
      accessPendingAction.value = ''
    }
  }

  async function onActivateAccess(): Promise<void> {
    accessPendingAction.value = 'activate'
    try {
      accessOverview.value = await activateMeetAccess()
      ElMessage.success(t('meet.access.activated'))
      if (workspaceUnlocked.value) {
        await refreshWorkspace(false)
      }
    } catch (error) {
      ElMessage.error((error as Error).message || t('meet.access.loadFailed'))
    } finally {
      accessPendingAction.value = ''
    }
  }

  async function onRequestEnterpriseAccess(payload: EnterpriseAccessPayload): Promise<void> {
    accessPendingAction.value = 'contact-sales'
    try {
      accessOverview.value = await requestMeetEnterpriseAccess(payload)
      ElMessage.success(t('meet.access.enterprise.done'))
    } catch (error) {
      ElMessage.error((error as Error).message || t('meet.access.loadFailed'))
    } finally {
      accessPendingAction.value = ''
    }
  }

  async function onCreateRoom(payload: CreateMeetRoomRequest): Promise<string | null> {
    const topic = payload.topic.trim()
    if (topic.length < 3) {
      ElMessage.warning(t('meet.workspace.messages.topicInvalid'))
      return null
    }
    creating.value = true
    try {
      currentRoom.value = await createRoom({
        topic,
        accessLevel: payload.accessLevel as MeetAccessLevel,
        maxParticipants: payload.maxParticipants
      })
      await loadRoomRecords(false)
      ElMessage.success(t('meet.workspace.messages.roomCreated'))
      return currentRoom.value.roomId
    } catch (error) {
      ElMessage.error((error as Error).message || t('meet.workspace.messages.createRoomFailed'))
      return null
    } finally {
      creating.value = false
    }
  }

  async function onRotateJoinCode(): Promise<void> {
    if (!currentRoom.value || !isActive.value) {
      ElMessage.warning(t('meet.workspace.messages.noActiveRoom'))
      return
    }
    rotating.value = true
    try {
      currentRoom.value = await rotateJoinCode(currentRoom.value.roomId)
      rooms.value = await listRooms(undefined, 20)
      ElMessage.success(t('meet.workspace.messages.joinCodeRotated'))
    } catch (error) {
      ElMessage.error((error as Error).message || t('meet.workspace.messages.rotateJoinCodeFailed'))
    } finally {
      rotating.value = false
    }
  }

  async function onEndRoom(): Promise<string | null> {
    if (!currentRoom.value || !isActive.value) {
      ElMessage.warning(t('meet.workspace.messages.noActiveRoom'))
      return null
    }
    try {
      await ElMessageBox.confirm(
        t('meet.workspace.messages.endRoomConfirmMessage'),
        t('meet.workspace.messages.endRoomConfirmTitle'),
        {
          type: 'warning',
          confirmButtonText: t('meet.workspace.lifecycle.actions.endRoom'),
          cancelButtonText: t('common.actions.cancel')
        }
      )
    } catch {
      return null
    }
    const endedRoomId = currentRoom.value.roomId
    ending.value = true
    try {
      await endRoom(endedRoomId)
      await loadRoomRecords(false)
      ElMessage.success(t('meet.workspace.messages.roomEnded'))
      return endedRoomId
    } catch (error) {
      ElMessage.error((error as Error).message || t('meet.workspace.messages.endRoomFailed'))
      return null
    } finally {
      ending.value = false
    }
  }

  onMounted(() => {
    void loadAccessState()
  })

  onBeforeUnmount(() => {
    clearRoomTicker()
  })

  return {
    accessOverview,
    accessLoading,
    accessPendingAction,
    loading,
    refreshing,
    creating,
    rotating,
    ending,
    rooms,
    currentRoom,
    history,
    isActive,
    workspaceUnlocked,
    currentDurationSeconds,
    loadAccessState,
    refreshWorkspace,
    onJoinWaitlist,
    onActivateAccess,
    onRequestEnterpriseAccess,
    onCreateRoom,
    onRotateJoinCode,
    onEndRoom
  }
}
