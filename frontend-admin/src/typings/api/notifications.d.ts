declare namespace Api {
  namespace Notifications {
    interface Notification {
      id: string;
      title: string;
      body: string;
      product: string;
      severity: string;
      status: string;
      createdAt: string;
      readAt: string | null;
    }

    interface Query {
      limit?: number;
      unreadOnly?: boolean;
      status?: string;
      includeSnoozed?: boolean;
    }

    interface PatchPayload {
      status: string;
    }

    interface Subscription {
      id: string;
      product: string;
      channel: string;
      enabled: boolean;
    }

    type RealtimeEventType = 'notification' | 'badge-update' | 'subscription-changed';

    interface RealtimePayload {
      eventType: string;
      operation: string;
      operationId: string | null;
      requestedCount: number;
      affectedCount: number;
      sessionId: string | null;
      createdAt: string;
    }

    interface RealtimeEvent {
      type: RealtimeEventType;
      channel: string;
      seq: number;
      payload: RealtimePayload;
    }

    interface RealtimeReplay {
      events: RealtimeEvent[];
      nextCursor: number;
    }

    interface RealtimeQuery {
      cursor?: number;
      limit?: number;
    }
  }
}
