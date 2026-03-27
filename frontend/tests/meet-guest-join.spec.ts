import { describe, expect, it } from 'vitest'
import type { MeetGuestRequestItem, MeetGuestSession } from '../types/api'
import {
  buildMeetGuestJoinLink,
  canUseMeetGuestJoin,
  meetGuestSessionBadgeType,
  pendingMeetGuestRequests,
  resolveMeetGuestViewState
} from '../utils/meet-guest'

function request(overrides: Partial<MeetGuestRequestItem> = {}): MeetGuestRequestItem {
  return {
    requestId: '1',
    roomId: '1',
    roomCode: 'MR-TEST',
    roomStatus: 'ACTIVE',
    displayName: 'Guest',
    audioEnabled: true,
    videoEnabled: true,
    status: 'PENDING',
    requestToken: 'GJ-TEST',
    guestSessionToken: null,
    participantId: null,
    requestedAt: '2026-03-12T00:00:00Z',
    approvedAt: null,
    rejectedAt: null,
    ...overrides
  }
}

function session(overrides: Partial<MeetGuestSession> = {}): MeetGuestSession {
  return {
    roomId: '1',
    roomCode: 'MR-TEST',
    topic: 'Test room',
    sessionStatus: 'WAITING',
    selfParticipant: null,
    participants: [],
    ...overrides
  }
}

describe('meet guest utils', () => {
  it('builds public join links only when join code exists', () => {
    expect(buildMeetGuestJoinLink('JC-ABCD', 'http://127.0.0.1:3003')).toBe('http://127.0.0.1:3003/meet/join/JC-ABCD')
    expect(buildMeetGuestJoinLink('JC-ABCD', 'http://127.0.0.1:3003/')).toBe('http://127.0.0.1:3003/meet/join/JC-ABCD')
    expect(buildMeetGuestJoinLink()).toBe('')
  })

  it('resolves whether host room can expose guest links', () => {
    expect(canUseMeetGuestJoin({ status: 'ACTIVE', accessLevel: 'PUBLIC', joinCode: 'JC-ABCD' })).toBe(true)
    expect(canUseMeetGuestJoin({ status: 'ACTIVE', accessLevel: 'PRIVATE', joinCode: 'JC-ABCD' })).toBe(false)
  })

  it('filters pending requests and derives guest view states', () => {
    expect(pendingMeetGuestRequests([request(), request({ requestId: '2', status: 'APPROVED' })])).toHaveLength(1)
    expect(resolveMeetGuestViewState(request(), null)).toBe('WAITING')
    expect(resolveMeetGuestViewState(request({ status: 'APPROVED' }), null)).toBe('WAITING')
    expect(resolveMeetGuestViewState(request({ status: 'REJECTED' }), null)).toBe('REJECTED')
    expect(resolveMeetGuestViewState(request({ status: 'LEFT' }), null)).toBe('LEFT')
    expect(resolveMeetGuestViewState(request({ status: 'APPROVED' }), session({ sessionStatus: 'ACTIVE' }))).toBe('ACTIVE')
    expect(resolveMeetGuestViewState(request({ status: 'APPROVED' }), session({ sessionStatus: 'ROOM_ENDED' }))).toBe('ROOM_ENDED')
  })

  it('maps session state to badge type', () => {
    expect(meetGuestSessionBadgeType('ACTIVE')).toBe('success')
    expect(meetGuestSessionBadgeType('WAITING')).toBe('warning')
    expect(meetGuestSessionBadgeType('REMOVED')).toBe('danger')
    expect(meetGuestSessionBadgeType('LEFT')).toBe('info')
  })
})
