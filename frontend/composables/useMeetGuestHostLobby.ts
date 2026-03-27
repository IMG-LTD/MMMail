import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { Ref } from 'vue'
import type { MeetGuestRequestItem, MeetRoomItem } from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import { useMeetGuestApi } from '~/composables/useMeetGuestApi'
import { buildMeetGuestJoinLink } from '~/utils/meet-guest'

interface UseMeetGuestHostLobbyOptions {
  room: Ref<MeetRoomItem | null>
  afterRequestResolved?: () => Promise<void>
}

export function useMeetGuestHostLobby(options: UseMeetGuestHostLobbyOptions) {
  const { t } = useI18n()
  const requestUrl = useRequestURL()
  const {
    listMeetGuestRequests,
    approveMeetGuestRequest,
    rejectMeetGuestRequest
  } = useMeetGuestApi()

  const requests = ref<MeetGuestRequestItem[]>([])
  const loading = ref(false)
  const actionRequestId = ref('')
  const joinLink = computed(() => buildMeetGuestJoinLink(options.room.value?.joinCode, requestUrl.origin))

  function reset(): void {
    requests.value = []
    actionRequestId.value = ''
  }

  async function refresh(showSuccess = false): Promise<void> {
    const roomId = options.room.value?.roomId
    if (!roomId) {
      reset()
      return
    }
    loading.value = true
    try {
      requests.value = await listMeetGuestRequests(roomId)
      if (showSuccess) {
        ElMessage.success(t('meet.guest.host.messages.refreshed'))
      }
    } catch (error) {
      ElMessage.error((error as Error).message || t('meet.guest.host.messages.loadFailed'))
    } finally {
      loading.value = false
    }
  }

  async function approve(requestId: string): Promise<void> {
    await resolveRequest(requestId, 'approve')
  }

  async function reject(requestId: string): Promise<void> {
    await resolveRequest(requestId, 'reject')
  }

  async function copyLink(): Promise<void> {
    if (!joinLink.value) {
      ElMessage.warning(t('meet.guest.host.linkUnavailable'))
      return
    }
    try {
      if (typeof navigator === 'undefined' || !navigator.clipboard?.writeText) {
        throw new Error(t('meet.guest.host.messages.clipboardUnavailable'))
      }
      await navigator.clipboard.writeText(joinLink.value)
      ElMessage.success(t('meet.guest.host.messages.linkCopied'))
    } catch (error) {
      ElMessage.error((error as Error).message || t('meet.guest.host.messages.copyFailed'))
    }
  }

  async function resolveRequest(requestId: string, action: 'approve' | 'reject'): Promise<void> {
    const roomId = options.room.value?.roomId
    if (!roomId) {
      ElMessage.warning(t('meet.guest.host.messages.noRoom'))
      return
    }
    actionRequestId.value = requestId
    try {
      const updated = action === 'approve'
        ? await approveMeetGuestRequest(roomId, requestId)
        : await rejectMeetGuestRequest(roomId, requestId)
      requests.value = requests.value.map((item) => (item.requestId === requestId ? updated : item))
      await options.afterRequestResolved?.()
      await refresh(false)
      ElMessage.success(
        action === 'approve'
          ? t('meet.guest.host.messages.approved')
          : t('meet.guest.host.messages.rejected')
      )
    } catch (error) {
      ElMessage.error(
        (error as Error).message
          || t(
            action === 'approve'
              ? 'meet.guest.host.messages.approveFailed'
              : 'meet.guest.host.messages.rejectFailed'
          )
      )
    } finally {
      actionRequestId.value = ''
    }
  }

  return {
    actionRequestId,
    approve,
    copyLink,
    joinLink,
    loading,
    refresh,
    reject,
    requests,
    reset
  }
}
