import type {
  ApiResponse,
  CreateMeetRoomRequest,
  JoinMeetRoomRequest,
  JoinMeetWaitlistRequest,
  MeetAccessOverview,
  MeetQualitySnapshotItem,
  MeetSignalEventItem,
  MeetSignalType,
  MeetParticipantItem,
  MeetPruneInactiveResult,
  MeetRoomItem,
  PruneMeetInactiveParticipantsRequest,
  ReportMeetQualityRequest,
  RequestMeetEnterpriseAccessRequest,
  UpdateMeetParticipantMediaRequest
} from '~/types/api'

export function useMeetApi() {
  const { $apiClient } = useNuxtApp()

  async function getAccessOverview(): Promise<MeetAccessOverview> {
    const response = await $apiClient.get<ApiResponse<MeetAccessOverview>>('/api/v1/meet/access/overview')
    return response.data.data
  }

  async function joinMeetWaitlist(payload: JoinMeetWaitlistRequest): Promise<MeetAccessOverview> {
    const response = await $apiClient.post<ApiResponse<MeetAccessOverview>>('/api/v1/meet/access/waitlist', payload)
    return response.data.data
  }

  async function activateMeetAccess(): Promise<MeetAccessOverview> {
    const response = await $apiClient.post<ApiResponse<MeetAccessOverview>>('/api/v1/meet/access/activate')
    return response.data.data
  }

  async function requestMeetEnterpriseAccess(payload: RequestMeetEnterpriseAccessRequest): Promise<MeetAccessOverview> {
    const response = await $apiClient.post<ApiResponse<MeetAccessOverview>>('/api/v1/meet/access/contact-sales', payload)
    return response.data.data
  }

  async function listRooms(status?: string, limit = 20): Promise<MeetRoomItem[]> {
    const response = await $apiClient.get<ApiResponse<MeetRoomItem[]>>('/api/v1/meet/rooms', {
      params: {
        status: status || undefined,
        limit
      }
    })
    return response.data.data
  }

  async function getCurrentRoom(): Promise<MeetRoomItem | null> {
    const response = await $apiClient.get<ApiResponse<MeetRoomItem | null>>('/api/v1/meet/rooms/current')
    return response.data.data
  }

  async function listRoomHistory(limit = 20): Promise<MeetRoomItem[]> {
    const response = await $apiClient.get<ApiResponse<MeetRoomItem[]>>('/api/v1/meet/rooms/history', {
      params: { limit }
    })
    return response.data.data
  }

  async function createRoom(payload: CreateMeetRoomRequest): Promise<MeetRoomItem> {
    const response = await $apiClient.post<ApiResponse<MeetRoomItem>>('/api/v1/meet/rooms', payload)
    return response.data.data
  }

  async function rotateJoinCode(roomId: string): Promise<MeetRoomItem> {
    const response = await $apiClient.post<ApiResponse<MeetRoomItem>>(`/api/v1/meet/rooms/${roomId}/join-code/rotate`)
    return response.data.data
  }

  async function endRoom(roomId: string): Promise<MeetRoomItem> {
    const response = await $apiClient.post<ApiResponse<MeetRoomItem>>(`/api/v1/meet/rooms/${roomId}/end`)
    return response.data.data
  }

  async function listParticipants(roomId: string): Promise<MeetParticipantItem[]> {
    const response = await $apiClient.get<ApiResponse<MeetParticipantItem[]>>(`/api/v1/meet/rooms/${roomId}/participants`)
    return response.data.data
  }

  async function joinRoom(roomId: string, payload: JoinMeetRoomRequest): Promise<MeetParticipantItem> {
    const response = await $apiClient.post<ApiResponse<MeetParticipantItem>>(
      `/api/v1/meet/rooms/${roomId}/participants/join`,
      payload
    )
    return response.data.data
  }

  async function leaveParticipant(roomId: string, participantId: string): Promise<MeetParticipantItem> {
    const response = await $apiClient.post<ApiResponse<MeetParticipantItem>>(
      `/api/v1/meet/rooms/${roomId}/participants/${participantId}/leave`
    )
    return response.data.data
  }

  async function heartbeatParticipant(roomId: string, participantId: string): Promise<MeetParticipantItem> {
    const response = await $apiClient.post<ApiResponse<MeetParticipantItem>>(
      `/api/v1/meet/rooms/${roomId}/participants/${participantId}/heartbeat`
    )
    return response.data.data
  }

  async function updateParticipantMediaState(
    roomId: string,
    participantId: string,
    payload: UpdateMeetParticipantMediaRequest
  ): Promise<MeetParticipantItem> {
    const response = await $apiClient.post<ApiResponse<MeetParticipantItem>>(
      `/api/v1/meet/rooms/${roomId}/participants/${participantId}/media`,
      payload
    )
    return response.data.data
  }

  async function reportQuality(
    roomId: string,
    participantId: string,
    payload: ReportMeetQualityRequest
  ): Promise<MeetQualitySnapshotItem> {
    const response = await $apiClient.post<ApiResponse<MeetQualitySnapshotItem>>(
      `/api/v1/meet/rooms/${roomId}/participants/${participantId}/quality`,
      payload
    )
    return response.data.data
  }

  async function listQualitySnapshots(roomId: string, limit = 50): Promise<MeetQualitySnapshotItem[]> {
    const response = await $apiClient.get<ApiResponse<MeetQualitySnapshotItem[]>>(
      `/api/v1/meet/rooms/${roomId}/quality`,
      { params: { limit } }
    )
    return response.data.data
  }

  async function updateParticipantRole(roomId: string, participantId: string, role: 'CO_HOST' | 'PARTICIPANT'): Promise<MeetParticipantItem> {
    const response = await $apiClient.post<ApiResponse<MeetParticipantItem>>(
      `/api/v1/meet/rooms/${roomId}/participants/${participantId}/role`,
      { role }
    )
    return response.data.data
  }

  async function removeParticipant(roomId: string, participantId: string): Promise<MeetParticipantItem> {
    const response = await $apiClient.post<ApiResponse<MeetParticipantItem>>(
      `/api/v1/meet/rooms/${roomId}/participants/${participantId}/remove`
    )
    return response.data.data
  }

  async function pruneInactiveParticipants(roomId: string, payload: PruneMeetInactiveParticipantsRequest): Promise<MeetPruneInactiveResult> {
    const response = await $apiClient.post<ApiResponse<MeetPruneInactiveResult>>(
      `/api/v1/meet/rooms/${roomId}/participants/prune-inactive`,
      payload
    )
    return response.data.data
  }

  async function transferHost(roomId: string, targetParticipantId: string): Promise<MeetParticipantItem> {
    const response = await $apiClient.post<ApiResponse<MeetParticipantItem>>(
      `/api/v1/meet/rooms/${roomId}/host/transfer`,
      { targetParticipantId }
    )
    return response.data.data
  }

  async function sendSignal(
    roomId: string,
    signalType: MeetSignalType,
    fromParticipantId: string,
    payload: string,
    toParticipantId?: string
  ): Promise<MeetSignalEventItem> {
    const endpoint = signalType === 'OFFER'
      ? 'offer'
      : signalType === 'ANSWER'
        ? 'answer'
        : 'ice'
    const response = await $apiClient.post<ApiResponse<MeetSignalEventItem>>(
      `/api/v1/meet/rooms/${roomId}/signals/${endpoint}`,
      {
        fromParticipantId,
        toParticipantId: toParticipantId || undefined,
        payload
      }
    )
    return response.data.data
  }

  async function listSignals(roomId: string, afterEventSeq = 0, limit = 20): Promise<MeetSignalEventItem[]> {
    const response = await $apiClient.get<ApiResponse<MeetSignalEventItem[]>>(`/api/v1/meet/rooms/${roomId}/signals`, {
      params: {
        afterEventSeq,
        limit
      }
    })
    return response.data.data
  }

  async function streamSignals(
    roomId: string,
    afterEventSeq = 0,
    timeoutSeconds = 10,
    limit = 20
  ): Promise<MeetSignalEventItem[]> {
    const response = await $apiClient.get<ApiResponse<MeetSignalEventItem[]>>(`/api/v1/meet/rooms/${roomId}/signals/stream`, {
      params: {
        afterEventSeq,
        timeoutSeconds,
        limit
      }
    })
    return response.data.data
  }

  return {
    getAccessOverview,
    joinMeetWaitlist,
    activateMeetAccess,
    requestMeetEnterpriseAccess,
    listRooms,
    getCurrentRoom,
    listRoomHistory,
    createRoom,
    rotateJoinCode,
    endRoom,
    listParticipants,
    joinRoom,
    leaveParticipant,
    heartbeatParticipant,
    updateParticipantMediaState,
    reportQuality,
    listQualitySnapshots,
    updateParticipantRole,
    removeParticipant,
    pruneInactiveParticipants,
    transferHost,
    sendSignal,
    listSignals,
    streamSignals
  }
}
