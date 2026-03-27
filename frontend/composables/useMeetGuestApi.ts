import type {
  ApiResponse,
  MeetGuestJoinOverview,
  MeetGuestRequestItem,
  MeetGuestSession,
  SubmitMeetGuestRequestRequest,
  UpdateMeetParticipantMediaRequest
} from '~/types/api'

export function useMeetGuestApi() {
  const { $apiClient } = useNuxtApp()

  async function getMeetGuestJoinOverview(joinCode: string): Promise<MeetGuestJoinOverview> {
    const response = await $apiClient.get<ApiResponse<MeetGuestJoinOverview>>(`/api/v1/public/meet/join/${joinCode}`)
    return response.data.data
  }

  async function submitMeetGuestRequest(
    joinCode: string,
    payload: SubmitMeetGuestRequestRequest
  ): Promise<MeetGuestRequestItem> {
    const response = await $apiClient.post<ApiResponse<MeetGuestRequestItem>>(
      `/api/v1/public/meet/join/${joinCode}/requests`,
      payload
    )
    return response.data.data
  }

  async function getMeetGuestRequest(requestToken: string): Promise<MeetGuestRequestItem> {
    const response = await $apiClient.get<ApiResponse<MeetGuestRequestItem>>(`/api/v1/public/meet/requests/${requestToken}`)
    return response.data.data
  }

  async function getMeetGuestSession(sessionToken: string): Promise<MeetGuestSession> {
    const response = await $apiClient.get<ApiResponse<MeetGuestSession>>(`/api/v1/public/meet/sessions/${sessionToken}`)
    return response.data.data
  }

  async function heartbeatMeetGuestSession(sessionToken: string): Promise<MeetGuestSession> {
    const response = await $apiClient.post<ApiResponse<MeetGuestSession>>(
      `/api/v1/public/meet/sessions/${sessionToken}/heartbeat`
    )
    return response.data.data
  }

  async function updateMeetGuestSessionMedia(
    sessionToken: string,
    payload: UpdateMeetParticipantMediaRequest
  ): Promise<MeetGuestSession> {
    const response = await $apiClient.post<ApiResponse<MeetGuestSession>>(
      `/api/v1/public/meet/sessions/${sessionToken}/media`,
      payload
    )
    return response.data.data
  }

  async function leaveMeetGuestSession(sessionToken: string): Promise<MeetGuestSession> {
    const response = await $apiClient.post<ApiResponse<MeetGuestSession>>(
      `/api/v1/public/meet/sessions/${sessionToken}/leave`
    )
    return response.data.data
  }

  async function listMeetGuestRequests(roomId: string): Promise<MeetGuestRequestItem[]> {
    const response = await $apiClient.get<ApiResponse<MeetGuestRequestItem[]>>(
      `/api/v1/meet/rooms/${roomId}/guest-requests`
    )
    return response.data.data
  }

  async function approveMeetGuestRequest(roomId: string, requestId: string): Promise<MeetGuestRequestItem> {
    const response = await $apiClient.post<ApiResponse<MeetGuestRequestItem>>(
      `/api/v1/meet/rooms/${roomId}/guest-requests/${requestId}/approve`
    )
    return response.data.data
  }

  async function rejectMeetGuestRequest(roomId: string, requestId: string): Promise<MeetGuestRequestItem> {
    const response = await $apiClient.post<ApiResponse<MeetGuestRequestItem>>(
      `/api/v1/meet/rooms/${roomId}/guest-requests/${requestId}/reject`
    )
    return response.data.data
  }

  return {
    approveMeetGuestRequest,
    getMeetGuestJoinOverview,
    getMeetGuestRequest,
    getMeetGuestSession,
    heartbeatMeetGuestSession,
    leaveMeetGuestSession,
    listMeetGuestRequests,
    rejectMeetGuestRequest,
    submitMeetGuestRequest,
    updateMeetGuestSessionMedia
  }
}
