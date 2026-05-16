declare namespace Api {
  namespace Meet {
    interface WaitlistPayload {
      note?: string;
    }

    interface EnterpriseAccessPayload {
      companyName: string;
      requestedSeats: number;
      note?: string;
    }

    interface RoomParams {
      status?: string;
      limit?: number;
    }

    interface HistoryParams {
      limit?: number;
    }

    interface RoomPayload {
      accessLevel: string;
      maxParticipants: number;
      topic: string;
    }

    interface JoinPayload {
      displayName: string;
    }

    interface MediaPayload {
      audioEnabled: boolean;
      videoEnabled: boolean;
      screenSharing: boolean;
    }

    interface QualityPayload {
      jitterMs: number;
      packetLossPercent: number;
      roundTripMs: number;
    }

    interface RolePayload {
      role: string;
    }

    interface SignalPayload {
      fromParticipantId: string;
      toParticipantId?: string;
      payload: string;
    }

    interface SignalParams {
      afterEventSeq?: string;
      limit?: number;
    }

    interface StreamSignalParams extends SignalParams {
      timeoutSeconds?: number;
    }

    interface GuestRequestPayload {
      displayName: string;
      audioEnabled: boolean;
      videoEnabled: boolean;
    }

    interface Participant {
      participantId: string;
      roomId: string;
      userId: string;
      displayName: string;
      role: string;
      status: string;
      audioEnabled: boolean;
      videoEnabled: boolean;
      screenSharing: boolean;
      joinedAt: string;
      leftAt: string;
      lastHeartbeatAt: string;
      self: boolean;
      canManageParticipants: boolean;
      canTransferHost: boolean;
    }

    interface QualitySnapshot {
      snapshotId: string;
      roomId: string;
      participantId: string;
      jitterMs: number;
      packetLossPercent: number;
      roundTripMs: number;
      qualityScore: number;
      createdAt: string;
    }

    interface GuestRequest {
      requestId: string;
      roomId: string;
      roomCode: string;
      roomStatus: string;
      displayName: string;
      audioEnabled: boolean;
      videoEnabled: boolean;
      status: string;
      requestToken: string;
      guestSessionToken: string;
      participantId: string;
      requestedAt: string;
      approvedAt: string;
      rejectedAt: string;
    }

    interface GuestJoinOverview {
      roomId: string;
      roomCode: string;
      topic: string;
      joinCode: string;
      accessLevel: string;
      roomStatus: string;
      guestJoinEnabled: boolean;
      lobbyEnabled: boolean;
      activeParticipants: number;
      maxParticipants: number;
    }

    interface GuestParticipantView {
      participantId: string;
      displayName: string;
      role: string;
      status: string;
      audioEnabled: boolean;
      videoEnabled: boolean;
      screenSharing: boolean;
      self: boolean;
    }

    interface GuestSession {
      roomId: string;
      roomCode: string;
      topic: string;
      sessionStatus: string;
      selfParticipant: GuestParticipantView;
      participants: GuestParticipantView[];
    }

    interface SignalEvent {
      eventSeq: string;
      roomId: string;
      signalType: string;
      fromParticipantId: string;
      toParticipantId: string;
      payload: string;
      createdAt: string;
    }
  }
}
