import { describe, expect, it } from 'vitest'
import type { MeetAccessOverview } from '../types/api'
import {
  meetAccessStateTagType,
  meetShouldShowActivateAction,
  meetShouldShowWaitlistAction,
  meetWorkspaceUnlocked
} from '../utils/meet-access'

function overview(overrides: Partial<MeetAccessOverview>): MeetAccessOverview {
  return {
    planCode: 'FREE',
    planName: 'Free',
    eligibleForInstantAccess: false,
    accessGranted: false,
    waitlistRequested: false,
    salesContactRequested: false,
    accessState: 'LOCKED',
    recommendedAction: 'JOIN_WAITLIST',
    companyName: null,
    requestedSeats: null,
    requestNote: null,
    waitlistRequestedAt: null,
    accessGrantedAt: null,
    salesContactRequestedAt: null,
    ...overrides
  }
}

describe('meet access utils', () => {
  it('maps access state to tag type', () => {
    expect(meetAccessStateTagType('LOCKED')).toBe('info')
    expect(meetAccessStateTagType('WAITLISTED')).toBe('warning')
    expect(meetAccessStateTagType('GRANTED')).toBe('success')
  })

  it('resolves workspace unlock and waitlist actions', () => {
    expect(meetWorkspaceUnlocked(overview({ accessGranted: true, accessState: 'GRANTED' }))).toBe(true)
    expect(meetShouldShowWaitlistAction(overview({}))).toBe(true)
    expect(meetShouldShowWaitlistAction(overview({ waitlistRequested: true, accessState: 'WAITLISTED' }))).toBe(false)
  })

  it('shows activate action only for instantly eligible plans', () => {
    expect(meetShouldShowActivateAction(overview({ eligibleForInstantAccess: true, recommendedAction: 'ACTIVATE' }))).toBe(true)
    expect(meetShouldShowActivateAction(overview({ eligibleForInstantAccess: false }))).toBe(false)
  })
})
