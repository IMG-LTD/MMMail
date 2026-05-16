import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const apiFile = new URL("../src/service/api/calendar.ts", import.meta.url);
const viewFile = new URL("../src/views/app/CalendarView.vue", import.meta.url);
const drawerFile = new URL("../src/views/app/calendar/CalendarEventDrawer.vue", import.meta.url);

test("calendar workspace reads events and availability from APIs", async () => {
  const [api, view] = await Promise.all([readFile(apiFile, "utf8"), readFile(viewFile, "utf8")]);

  assert.match(api, /\/api\/v2\/calendar\/events/);
  assert.match(api, /\/api\/v2\/calendar\/availability/);
  assert.match(api, /\/api\/v2\/calendar\/resources/);
  assert.match(api, /\/api\/v2\/calendar\/settings/);
  assert.doesNotMatch(api, /\/api\/v1\/calendar/);
  assert.match(view, /useAuthStore/);
  assert.match(view, /listCalendarEvents/);
  assert.match(view, /listCalendarAgenda/);
  assert.match(view, /queryCalendarAvailability/);
  assert.match(view, /latestCalendarRequest/);
  assert.match(view, /watch\(\(\) => authStore\.accessToken/);
  assert.match(view, /listCalendarEvents\(requestToken, requestRange\.from, requestRange\.to\)/);
  assert.doesNotMatch(view, /const events = \[/);
});

test("calendar workspace persists drawer drafts through v2 mutation APIs", async () => {
  const [api, view, drawer] = await Promise.all([
    readFile(apiFile, "utf8"),
    readFile(viewFile, "utf8"),
    readFile(drawerFile, "utf8"),
  ]);

  assert.match(api, /export function createCalendarEvent/);
  assert.match(api, /export function updateCalendarEvent/);
  assert.match(api, /export function deleteCalendarEvent/);
  assert.match(
    api,
    /httpClient\.post<ApiResponse<CalendarEventMutationResult>>\('\/api\/v2\/calendar\/events'/,
  );
  assert.match(
    api,
    /httpClient\.patch<ApiResponse<CalendarEventMutationResult>>\(`\/api\/v2\/calendar\/events\/\$\{eventId\}`/,
  );
  assert.match(
    api,
    /httpClient\.delete<ApiResponse<null>>\(`\/api\/v2\/calendar\/events\/\$\{eventId\}`/,
  );
  assert.match(
    api,
    /httpClient\.patch<ApiResponse<CalendarSettings>>\('\/api\/v2\/calendar\/settings'/,
  );
  assert.match(view, /createCalendarEvent/);
  assert.match(view, /updateCalendarEvent/);
  assert.match(view, /buildCalendarEventPayload/);
  assert.match(view, /await loadCalendar\(\)/);
  assert.match(drawer, /CalendarEventDraft/);
  assert.match(drawer, /NInput/);
  assert.match(drawer, /v-model:value="draft\.title"/);
  assert.match(drawer, /emit\('save', \{ \.\.\.draft \}\)/);
  assert.doesNotMatch(view, /Calendar save requires an API endpoint/);
});
