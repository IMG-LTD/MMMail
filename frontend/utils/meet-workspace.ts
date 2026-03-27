import type {
  MeetAccessLevel,
  MeetParticipantItem,
  MeetParticipantRole,
  MeetParticipantStatus,
  MeetRoomStatus,
  MeetSignalEventItem
} from '~/types/api'

type TagType = 'success' | 'warning' | 'danger' | 'info'

export function formatMeetTime(value: string | null): string {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString()
}

export function formatMeetDuration(seconds: number): string {
  if (seconds <= 0) {
    return '00:00'
  }
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const remainderSeconds = seconds % 60
  if (hours > 0) {
    return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${remainderSeconds
      .toString()
      .padStart(2, '0')}`
  }
  return `${minutes.toString().padStart(2, '0')}:${remainderSeconds.toString().padStart(2, '0')}`
}

export function formatMeetSignalTarget(signal: MeetSignalEventItem, broadcastLabel: string): string {
  return signal.toParticipantId || broadcastLabel
}

export function meetQualityTagType(qualityScore: number): TagType {
  if (qualityScore >= 85) {
    return 'success'
  }
  if (qualityScore >= 60) {
    return 'warning'
  }
  return 'danger'
}

export function meetRoomStatusTagType(status: MeetRoomStatus): TagType {
  return status === 'ACTIVE' ? 'success' : 'info'
}

export function meetParticipantRoleTagType(role: MeetParticipantRole): TagType {
  if (role === 'HOST') {
    return 'danger'
  }
  if (role === 'CO_HOST') {
    return 'warning'
  }
  return 'info'
}

export function meetWorkspaceAccessLevelKey(accessLevel: MeetAccessLevel): string {
  return `meet.workspace.accessLevel.${accessLevel}`
}

export function meetWorkspaceRoomStatusKey(status: MeetRoomStatus): string {
  return `meet.workspace.roomStatus.${status}`
}

export function meetWorkspaceRoleKey(role: MeetParticipantRole): string {
  return `meet.workspace.participants.role.${role}`
}

export function meetWorkspaceParticipantStatusKey(status: MeetParticipantStatus): string {
  return `meet.workspace.participants.status.${status}`
}

export function meetWorkspaceBooleanKey(value: boolean): string {
  return `meet.workspace.boolean.${value ? 'true' : 'false'}`
}

export function meetWorkspaceMediaKey(enabled: boolean, kind: 'audio' | 'video' | 'screen'): string {
  return `meet.workspace.participants.media.${kind}.${enabled ? 'on' : 'off'}`
}

export function meetSignalTypeLabelKey(signalType: MeetSignalEventItem['signalType']): string {
  return `meet.workspace.signals.type.${signalType}`
}

export function meetWorkspaceParticipantSummary(participants: MeetParticipantItem[]): MeetParticipantItem | null {
  return participants.find((item) => item.self) ?? null
}
