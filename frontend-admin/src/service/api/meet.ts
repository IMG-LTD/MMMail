import { request } from '../request';

export function readMeetAccessOverview() {
  return request<Api.Meet.AccessOverview>({ url: '/api/v1/meet/access/overview' });
}

export function joinMeetWaitlist(data: Api.Meet.WaitlistPayload = {}) {
  return request<Api.Meet.AccessOverview>({
    url: '/api/v1/meet/access/waitlist',
    method: 'post',
    data
  });
}

export function requestMeetEnterpriseAccess(data: Api.Meet.EnterpriseAccessPayload) {
  return request<Api.Meet.AccessOverview>({
    url: '/api/v1/meet/access/contact-sales',
    method: 'post',
    data
  });
}

export function activateMeetAccess() {
  return request<Api.Meet.AccessOverview>({
    url: '/api/v1/meet/access/activate',
    method: 'post'
  });
}

export function listMeetRooms(params: Api.Meet.RoomParams = {}) {
  return request<Api.Meet.Room[]>({ url: '/api/v1/meet/rooms', params });
}

export function createMeetRoom(data: Api.Meet.RoomPayload) {
  return request<Api.Meet.Room>({
    url: '/api/v1/meet/rooms',
    method: 'post',
    data
  });
}

export function readCurrentMeetRoom() {
  return request<Api.Meet.Room | null>({ url: '/api/v1/meet/rooms/current' });
}

export function listMeetRoomHistory(params: Api.Meet.HistoryParams = {}) {
  return request<Api.Meet.Room[]>({ url: '/api/v1/meet/rooms/history', params });
}

export function rotateMeetJoinCode(roomId: string) {
  return request<Api.Meet.Room>({
    url: `/api/v1/meet/rooms/${roomId}/join-code/rotate`,
    method: 'post'
  });
}

export function endMeetRoom(roomId: string) {
  return request<Api.Meet.Room>({
    url: `/api/v1/meet/rooms/${roomId}/end`,
    method: 'post'
  });
}

export function listMeetParticipants(roomId: string) {
  return request<Api.Meet.Participant[]>({ url: `/api/v1/meet/rooms/${roomId}/participants` });
}

export function joinMeetRoom(roomId: string, data: Api.Meet.JoinPayload) {
  return request<Api.Meet.Participant>({
    url: `/api/v1/meet/rooms/${roomId}/participants/join`,
    method: 'post',
    data
  });
}

export function leaveMeetParticipant(roomId: string, participantId: string) {
  return request<Api.Meet.Participant>({
    url: `/api/v1/meet/rooms/${roomId}/participants/${participantId}/leave`,
    method: 'post'
  });
}

export function heartbeatMeetParticipant(roomId: string, participantId: string) {
  return request<Api.Meet.Participant>({
    url: `/api/v1/meet/rooms/${roomId}/participants/${participantId}/heartbeat`,
    method: 'post'
  });
}

export function updateMeetParticipantMedia(roomId: string, participantId: string, data: Api.Meet.MediaPayload) {
  return request<Api.Meet.Participant>({
    url: `/api/v1/meet/rooms/${roomId}/participants/${participantId}/media`,
    method: 'post',
    data
  });
}

export function reportMeetParticipantQuality(roomId: string, participantId: string, data: Api.Meet.QualityPayload) {
  return request<Api.Meet.QualitySnapshot>({
    url: `/api/v1/meet/rooms/${roomId}/participants/${participantId}/quality`,
    method: 'post',
    data
  });
}

export function listMeetRoomQuality(roomId: string, params: Api.Meet.HistoryParams = {}) {
  return request<Api.Meet.QualitySnapshot[]>({ url: `/api/v1/meet/rooms/${roomId}/quality`, params });
}

export function listMeetGuestRequests(roomId: string) {
  return request<Api.Meet.GuestRequest[]>({ url: `/api/v1/meet/rooms/${roomId}/guest-requests` });
}

export function approveMeetGuestRequest(roomId: string, requestId: string) {
  return request<Api.Meet.GuestRequest>({
    url: `/api/v1/meet/rooms/${roomId}/guest-requests/${requestId}/approve`,
    method: 'post'
  });
}

export function rejectMeetGuestRequest(roomId: string, requestId: string) {
  return request<Api.Meet.GuestRequest>({
    url: `/api/v1/meet/rooms/${roomId}/guest-requests/${requestId}/reject`,
    method: 'post'
  });
}

export function readPublicMeetJoinOverview(joinCode: string) {
  return request<Api.Meet.GuestJoinOverview>({ url: `/api/v1/public/meet/join/${joinCode}` });
}

export function submitPublicMeetGuestRequest(joinCode: string, data: Api.Meet.GuestRequestPayload) {
  return request<Api.Meet.GuestRequest>({
    url: `/api/v1/public/meet/join/${joinCode}/requests`,
    method: 'post',
    data
  });
}

export function readPublicMeetGuestRequest(requestToken: string) {
  return request<Api.Meet.GuestRequest>({ url: `/api/v1/public/meet/requests/${requestToken}` });
}

export function readPublicMeetGuestSession(guestSessionToken: string) {
  return request<Api.Meet.GuestSession>({ url: `/api/v1/public/meet/sessions/${guestSessionToken}` });
}

export function heartbeatPublicMeetGuestSession(guestSessionToken: string) {
  return request<Api.Meet.GuestSession>({
    url: `/api/v1/public/meet/sessions/${guestSessionToken}/heartbeat`,
    method: 'post'
  });
}

export function updatePublicMeetGuestMedia(guestSessionToken: string, data: Api.Meet.MediaPayload) {
  return request<Api.Meet.GuestSession>({
    url: `/api/v1/public/meet/sessions/${guestSessionToken}/media`,
    method: 'post',
    data
  });
}

export function leavePublicMeetGuestSession(guestSessionToken: string) {
  return request<Api.Meet.GuestSession>({
    url: `/api/v1/public/meet/sessions/${guestSessionToken}/leave`,
    method: 'post'
  });
}

export function sendMeetSignalOffer(roomId: string, data: Api.Meet.SignalPayload) {
  return request<Api.Meet.SignalEvent>({
    url: `/api/v1/meet/rooms/${roomId}/signals/offer`,
    method: 'post',
    data
  });
}

export function sendMeetSignalAnswer(roomId: string, data: Api.Meet.SignalPayload) {
  return request<Api.Meet.SignalEvent>({
    url: `/api/v1/meet/rooms/${roomId}/signals/answer`,
    method: 'post',
    data
  });
}

export function sendMeetSignalIce(roomId: string, data: Api.Meet.SignalPayload) {
  return request<Api.Meet.SignalEvent>({
    url: `/api/v1/meet/rooms/${roomId}/signals/ice`,
    method: 'post',
    data
  });
}

export function listMeetSignals(roomId: string, params: Api.Meet.SignalParams = {}) {
  return request<Api.Meet.SignalEvent[]>({ url: `/api/v1/meet/rooms/${roomId}/signals`, params });
}

export function streamMeetSignals(roomId: string, params: Api.Meet.StreamSignalParams = {}) {
  return request<Api.Meet.SignalEvent[]>({ url: `/api/v1/meet/rooms/${roomId}/signals/stream`, params });
}
