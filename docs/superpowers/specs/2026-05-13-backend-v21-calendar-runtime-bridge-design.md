# Backend v2.1 Calendar Runtime Bridge Design

## Purpose

MMMail v2.1 frontend Calendar already consumes `/api/v2/calendar/*` routes, while the backend has mature real calendar behavior under `/api/v1/calendar/*`. This slice closes that gap by adding a v2 Calendar runtime bridge that reuses the existing real Calendar services and removes the frontend save placeholder.

The slice is intentionally narrow: Calendar only. It must not add fake API success paths, mock business data, or broad Mail/Drive/Docs bridges.

## Current Context

- Frontend Calendar calls `/api/v2/calendar/events`, `/api/v2/calendar/availability`, `/api/v2/calendar/resources`, `/api/v2/calendar/bookings`, and `/api/v2/calendar/settings`.
- `CalendarView.vue` currently shows `Calendar save requires an API endpoint in the next backend slice.` for saves.
- Backend v1 Calendar already supports event CRUD, agenda, availability, import/export, and sharing through `CalendarController`, `CalendarService`, and `CalendarAvailabilityService`.
- Backend v2 access entitlement gates now enforce `/api/v2/*` contract metadata before controller fallback.
- The v2 contract catalog marks `calendar:read/write` as Community and `calendar:resources:*` as Premium.

## Goals

1. Provide real `/api/v2/calendar/*` runtime endpoints for Community calendar event and availability workflows.
2. Reuse existing v1 Calendar domain services, validation, persistence, and authorization behavior.
3. Keep Premium resource booking endpoints entitlement-gated in Community instead of returning fake resource data.
4. Wire frontend Calendar save to real v2 create/update APIs and reload after success.
5. Preserve existing v1 Calendar release-blocking tests and v2 access gate behavior.

## Non-Goals

- No broad v2 bridge for Mail, Drive, Docs, Sheets, Pass, Collaboration, Command Center, Notifications, Admin, or Settings beyond Calendar settings.
- No new calendar schema migration unless an existing real setting field requires it.
- No fake room/resource catalog.
- No bypass of `V21ApiAccessGateInterceptor`.
- No hosted worker extraction or queue integration.

## Backend API Design

Add a v2 Calendar controller under `/api/v2/calendar`.

### Community Runtime Endpoints

- `GET /api/v2/calendar/events`
  - Default behavior returns `List<CalendarEventItemVo>`.
  - When `view=agenda`, returns `List<CalendarAgendaItemVo>`.
  - Uses existing `from`, `to`, and `days` query semantics where applicable.
- `POST /api/v2/calendar/events`
  - Creates an event with `CreateCalendarEventRequest`.
  - Reuses `CalendarService.createEvent`.
- `PATCH /api/v2/calendar/events/{eventId}`
  - Updates an event with `UpdateCalendarEventRequest`.
  - Reuses `CalendarService.updateEvent`.
- `DELETE /api/v2/calendar/events/{eventId}`
  - Deletes an event.
  - Reuses `CalendarService.deleteEvent`.
- `POST /api/v2/calendar/availability`
  - Uses `QueryCalendarAvailabilityRequest`.
  - Reuses `CalendarAvailabilityService.queryAvailability`.
- `GET /api/v2/calendar/settings`
  - Returns a stable `CalendarSettings` payload for frontend runtime use.
- `PATCH /api/v2/calendar/settings`
  - Accepts the same settings shape and returns the normalized settings payload.

### Premium-Gated Endpoints

- `GET /api/v2/calendar/resources`
- `POST /api/v2/calendar/bookings`

These remain contract-listed Premium routes. In Community they should be stopped by the v2 entitlement gate with `V2_ENTITLEMENT_REQUIRED` before business execution. The slice should not create fake resources or bookings.

## Frontend Design

Extend `frontend-v2/src/service/api/calendar.ts` with:

- `createCalendarEvent(token, body)`
- `updateCalendarEvent(token, eventId, body)`
- `deleteCalendarEvent(token, eventId)`

Modify `CalendarView.vue` so `saveEventDraft()`:

- builds a request body from the current selected or draft event state,
- calls create for new events and update for existing events,
- clears `calendarSaveError` on success,
- closes the drawer,
- reloads calendar data,
- shows the backend error message on failure.

The placeholder save message must be removed from production source.

## Data Flow

```text
CalendarView.vue
  -> calendar.ts v2 client
  -> /api/v2/calendar/*
  -> V21ApiAccessGateInterceptor
  -> V21CalendarController
  -> CalendarService / CalendarAvailabilityService
  -> existing MyBatis mappers and calendar tables
```

The v2 controller is an API compatibility bridge, not a new domain service. Existing service methods remain the source of business truth.

## Error and Access Behavior

- Missing or invalid authentication returns `UNAUTHORIZED`.
- Community access is allowed for event read/write routes.
- Premium resources and bookings return `V2_ENTITLEMENT_REQUIRED` in Community.
- Missing events return `CALENDAR_EVENT_NOT_FOUND`.
- Invalid time ranges, timezone values, or request bodies return `INVALID_ARGUMENT`.
- Shared-event edit/delete behavior remains controlled by existing CalendarService authorization rules.

## Tests

Add backend coverage:

- `BackendV21CalendarRuntimeBridgeTest`
  - creates a v2 event,
  - lists it via v2 events,
  - reads agenda via `view=agenda`,
  - queries availability and sees the event as a busy overlap,
  - updates the event via `PATCH`,
  - deletes the event via `DELETE`,
  - verifies Premium resource routes are denied by entitlement in Community.

Add or update frontend contract coverage:

- Calendar API client exposes create/update/delete v2 methods.
- `CalendarView.vue` no longer contains the placeholder save error.
- Save behavior references real calendar client methods and reloads after success.

Regression coverage must include:

- `CalendarReleaseBlockingIntegrationTest`
- `BackendV21AccessEntitlementGatesTest`
- `BackendV21CalendarRuntimeBridgeTest`
- `frontend-v2` test suite
- backend compile

## Acceptance Criteria

- `/api/v2/calendar/events` supports real list, agenda, create, update, and delete paths.
- `/api/v2/calendar/availability` returns real availability based on persisted events.
- `/api/v2/calendar/resources` and `/api/v2/calendar/bookings` do not fake success in Community.
- Frontend Calendar save no longer displays the placeholder backend-slice error.
- Existing v1 Calendar behavior does not regress.
- All targeted backend and frontend validations pass before implementation is considered complete.

## Scope Boundary

This specification is sized for one implementation plan. If implementation reveals that Calendar settings need persistent user preference storage beyond the existing model, settings persistence should become a follow-up slice rather than expanding this slice.
