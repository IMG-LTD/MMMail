declare namespace Api {
  namespace Calendar {
    interface Event {
      id: string;
      seriesId: string;
      rrule: string | null;
      occurrenceStartAt: string;
      recurrenceUntil: string | null;
      title: string;
      location: string | null;
      startAt: string;
      endAt: string;
      allDay: boolean;
      timezone: string;
      reminderMinutes: number | null;
      attendeeCount: number;
      updatedAt: string;
      shared: boolean;
      ownerEmail: string | null;
      sharePermission: string;
      canEdit: boolean;
      canDelete: boolean;
    }

    interface AttendeeInput {
      displayName?: string;
      email: string;
    }

    interface EventPayload {
      allDay?: boolean;
      attendees: AttendeeInput[];
      description?: string;
      endAt: string;
      location?: string;
      rrule?: string;
      rdate?: string[];
      exdate?: string[];
      reminderMinutes?: number | null;
      startAt: string;
      timezone?: string;
      title: string;
    }

    interface AvailabilityPayload {
      startAt: string;
      endAt: string;
      attendeeEmails: string[];
      excludeEventId?: string | number | null;
    }

    interface Availability {
      summary: {
        attendeeCount: number;
        busyCount: number;
        freeCount: number;
        unknownCount: number;
        hasConflicts: boolean;
      };
    }

    interface Settings {
      defaultTimezone: string;
      weekStartsOn: string;
      workingHours: string[];
    }

    interface SubscriptionPayload {
      url: string;
      label: string;
      authMode?: string;
      color?: string;
    }

    interface Subscription {
      id: string;
      url: string;
      label: string;
      authMode: string;
      color: string | null;
      syncStatus: string;
      lastSyncAt: string | null;
      lastError: string | null;
      nextSyncAt: string | null;
      createdAt: string;
      updatedAt: string;
    }

    interface SubscriptionSync {
      jobId: string;
      subscriptionId: string;
      syncStatus: string;
      totalCount: number;
      importedCount: number;
      eventIds: string[];
      syncedAt: string;
    }
  }
}
