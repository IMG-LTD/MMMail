import { ElMessage } from 'element-plus'
import { computed, onBeforeUnmount, ref, type Ref } from 'vue'
import type {
  MeetParticipantItem,
  MeetQualitySnapshotItem,
  MeetRoomItem,
  MeetSignalEventItem,
  MeetSignalType,
  ReportMeetQualityRequest
} from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import { useMeetApi } from '~/composables/useMeetApi'

interface SignalPayload {
  signalType: MeetSignalType
  toParticipantId?: string
  payload: string
}

interface Options {
  currentRoom: Ref<MeetRoomItem | null>
  joinedRoomId: Ref<string>
  selfParticipant: Ref<MeetParticipantItem | null>
  participants: Ref<MeetParticipantItem[]>
}

export function useMeetDiagnosticsWorkspace(options: Options) {
  const { t } = useI18n()
  const { listQualitySnapshots, listSignals, reportQuality, sendSignal, streamSignals } = useMeetApi()

  const sendingSignal = ref(false)
  const pollingSignals = ref(false)
  const streamingSignals = ref(false)
  const loadingQuality = ref(false)
  const reportingQuality = ref(false)
  const liveSignalStreamEnabled = ref(false)
  const qualitySnapshots = ref<MeetQualitySnapshotItem[]>([])
  const signalEvents = ref<MeetSignalEventItem[]>([])
  const signalCursor = ref(0)
  const signalStreamTimer = ref<ReturnType<typeof setTimeout> | null>(null)

  const signalTargetOptions = computed(() => options.participants.value.filter((item) => !item.self))

  function resolveParticipantRoomId(): string | null {
    if (options.currentRoom.value?.status === 'ACTIVE') {
      return options.currentRoom.value.roomId
    }
    return options.joinedRoomId.value || null
  }

  function appendSignalEvents(items: MeetSignalEventItem[]): void {
    if (items.length === 0) {
      return
    }
    signalEvents.value = [...signalEvents.value, ...items].slice(-200)
    const lastEvent = items[items.length - 1]
    const parsed = Number.parseInt(lastEvent.eventSeq, 10)
    if (!Number.isNaN(parsed)) {
      signalCursor.value = Math.max(signalCursor.value, parsed)
    }
  }

  function clearSignalStreamTimer(): void {
    if (!signalStreamTimer.value) {
      return
    }
    clearTimeout(signalStreamTimer.value)
    signalStreamTimer.value = null
  }

  function stopLiveSignalStream(): void {
    clearSignalStreamTimer()
  }

  function clear(): void {
    qualitySnapshots.value = []
    signalEvents.value = []
    signalCursor.value = 0
    stopLiveSignalStream()
  }

  async function refreshQualitySnapshots(roomId?: string, silent = false): Promise<void> {
    const targetRoomId = roomId || resolveParticipantRoomId()
    if (!targetRoomId || !options.selfParticipant.value) {
      qualitySnapshots.value = []
      return
    }
    if (!silent) {
      loadingQuality.value = true
    }
    try {
      qualitySnapshots.value = await listQualitySnapshots(targetRoomId, 100)
    } catch (error) {
      if (!silent) {
        ElMessage.error((error as Error).message || t('meet.workspace.messages.loadQualityFailed'))
      }
    } finally {
      loadingQuality.value = false
    }
  }

  async function onReportQuality(payload: ReportMeetQualityRequest): Promise<void> {
    const roomId = resolveParticipantRoomId()
    if (!roomId || !options.selfParticipant.value) {
      ElMessage.warning(t('meet.workspace.messages.joinBeforeReportQuality'))
      return
    }
    if (payload.jitterMs < 0 || payload.jitterMs > 5000) {
      ElMessage.warning(t('meet.workspace.messages.jitterInvalid'))
      return
    }
    if (payload.packetLossPercent < 0 || payload.packetLossPercent > 100) {
      ElMessage.warning(t('meet.workspace.messages.packetLossInvalid'))
      return
    }
    if (payload.roundTripMs < 0 || payload.roundTripMs > 10000) {
      ElMessage.warning(t('meet.workspace.messages.rttInvalid'))
      return
    }
    reportingQuality.value = true
    try {
      await reportQuality(roomId, options.selfParticipant.value.participantId, payload)
      await refreshQualitySnapshots(roomId, true)
      ElMessage.success(t('meet.workspace.messages.qualityReported'))
    } catch (error) {
      ElMessage.error((error as Error).message || t('meet.workspace.messages.reportQualityFailed'))
    } finally {
      reportingQuality.value = false
    }
  }

  async function onSendSignal({ signalType, toParticipantId, payload }: SignalPayload): Promise<void> {
    const roomId = resolveParticipantRoomId()
    if (!roomId || !options.selfParticipant.value) {
      ElMessage.warning(t('meet.workspace.messages.joinBeforeSendSignals'))
      return
    }
    const normalizedPayload = payload.trim()
    if (!normalizedPayload) {
      ElMessage.warning(t('meet.workspace.messages.signalPayloadRequired'))
      return
    }
    sendingSignal.value = true
    try {
      const event = await sendSignal(roomId, signalType, options.selfParticipant.value.participantId, normalizedPayload, toParticipantId)
      appendSignalEvents([event])
      await refreshFromContext()
      ElMessage.success(t('meet.workspace.messages.signalSent', { signalType }))
    } catch (error) {
      ElMessage.error((error as Error).message || t('meet.workspace.messages.sendSignalFailed', { signalType }))
    } finally {
      sendingSignal.value = false
    }
  }

  async function onPollSignals(silent = false): Promise<void> {
    const roomId = resolveParticipantRoomId()
    if (!roomId || !options.selfParticipant.value) {
      if (!silent) {
        ElMessage.warning(t('meet.workspace.messages.joinBeforePollSignals'))
      }
      return
    }
    pollingSignals.value = true
    try {
      const events = await listSignals(roomId, signalCursor.value, 50)
      appendSignalEvents(events)
      if (!silent) {
        ElMessage.success(t('meet.workspace.messages.signalsFetched', { count: events.length }))
      }
    } catch (error) {
      ElMessage.error((error as Error).message || t('meet.workspace.messages.pollSignalsFailed'))
    } finally {
      pollingSignals.value = false
    }
  }

  async function onStreamSignals(silent = false): Promise<void> {
    const roomId = resolveParticipantRoomId()
    if (!roomId || !options.selfParticipant.value) {
      if (!silent) {
        ElMessage.warning(t('meet.workspace.messages.joinBeforeStreamSignals'))
      }
      return
    }
    streamingSignals.value = true
    try {
      const events = await streamSignals(roomId, signalCursor.value, 5, 50)
      appendSignalEvents(events)
      if (!silent) {
        ElMessage.success(t('meet.workspace.messages.streamSignalsFetched', { count: events.length }))
      }
    } catch (error) {
      ElMessage.error((error as Error).message || t('meet.workspace.messages.streamSignalsFailed'))
    } finally {
      streamingSignals.value = false
    }
  }

  function scheduleSignalStream(delayMs = 200): void {
    clearSignalStreamTimer()
    signalStreamTimer.value = setTimeout(async () => {
      signalStreamTimer.value = null
      if (!liveSignalStreamEnabled.value || !options.selfParticipant.value) {
        return
      }
      await onStreamSignals(true)
      if (liveSignalStreamEnabled.value && options.selfParticipant.value) {
        scheduleSignalStream(200)
      }
    }, delayMs)
  }

  async function onToggleLiveSignalStream(enabled: boolean): Promise<void> {
    liveSignalStreamEnabled.value = enabled
    if (!enabled) {
      stopLiveSignalStream()
      return
    }
    if (!options.selfParticipant.value) {
      ElMessage.warning(t('meet.workspace.messages.joinBeforeLiveStream'))
      liveSignalStreamEnabled.value = false
      return
    }
    await onStreamSignals(true)
    scheduleSignalStream(0)
    ElMessage.success(t('meet.workspace.messages.liveStreamEnabled'))
  }

  async function refreshFromContext(): Promise<void> {
    if (!options.selfParticipant.value) {
      clear()
      return
    }
    await refreshQualitySnapshots(undefined, true)
    if (liveSignalStreamEnabled.value) {
      await onStreamSignals(true)
      scheduleSignalStream(0)
      return
    }
    await onPollSignals(true)
  }

  onBeforeUnmount(() => {
    stopLiveSignalStream()
  })

  return {
    sendingSignal,
    pollingSignals,
    streamingSignals,
    loadingQuality,
    reportingQuality,
    liveSignalStreamEnabled,
    qualitySnapshots,
    signalEvents,
    signalCursor,
    signalTargetOptions,
    clear,
    refreshFromContext,
    onReportQuality,
    onSendSignal,
    onPollSignals,
    onStreamSignals,
    onToggleLiveSignalStream
  }
}
