import type {
  MeetGuestRequestItem,
  MeetGuestSession,
  MeetGuestSessionStatus,
  MeetRoomItem
} from '~/types/api'

type SessionBadgeType = 'info' | 'warning' | 'success' | 'danger'
type GuestViewState = 'FORM' | 'WAITING' | 'REJECTED' | 'ACTIVE' | 'LEFT' | 'REMOVED' | 'ROOM_ENDED'

export function buildMeetGuestJoinLink(joinCode?: string | null, origin = ''): string {
  if (!joinCode) {
    return ''
  }
  const normalizedOrigin = origin.endsWith('/') ? origin.slice(0, -1) : origin
  const path = `/meet/join/${joinCode}`
  return normalizedOrigin ? `${normalizedOrigin}${path}` : path
}

export function canUseMeetGuestJoin(room: Pick<MeetRoomItem, 'status' | 'accessLevel' | 'joinCode'> | null): boolean {
  if (!room?.joinCode) {
    return false
  }
  return room.status === 'ACTIVE' && room.accessLevel === 'PUBLIC'
}

export function pendingMeetGuestRequests(requests: MeetGuestRequestItem[]): MeetGuestRequestItem[] {
  return requests.filter(request => request.status === 'PENDING')
}

export function resolveMeetGuestViewState(
  request: MeetGuestRequestItem | null,
  session: MeetGuestSession | null
): GuestViewState {
  const sessionStatus = session?.sessionStatus
  if (sessionStatus === 'ACTIVE') {
    return 'ACTIVE'
  }
  if (sessionStatus === 'REJECTED') {
    return 'REJECTED'
  }
  if (sessionStatus === 'LEFT') {
    return 'LEFT'
  }
  if (sessionStatus === 'REMOVED') {
    return 'REMOVED'
  }
  if (sessionStatus === 'ROOM_ENDED') {
    return 'ROOM_ENDED'
  }
  if (request?.status === 'REJECTED') {
    return 'REJECTED'
  }
  if (request?.status === 'LEFT') {
    return 'LEFT'
  }
  if (request?.status === 'APPROVED') {
    return 'WAITING'
  }
  if (request?.status === 'PENDING' || sessionStatus === 'WAITING') {
    return 'WAITING'
  }
  return 'FORM'
}

export function meetGuestSessionBadgeType(status?: MeetGuestSessionStatus | null): SessionBadgeType {
  if (status === 'ACTIVE') {
    return 'success'
  }
  if (status === 'WAITING') {
    return 'warning'
  }
  if (status === 'REJECTED' || status === 'REMOVED') {
    return 'danger'
  }
  return 'info'
}
