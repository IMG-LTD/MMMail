import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const apiFile = new URL('../src/service/api/calendar.ts', import.meta.url)
const viewFile = new URL('../src/views/app/CalendarView.vue', import.meta.url)

test('calendar workspace reads events and availability from APIs', async () => {
  const [api, view] = await Promise.all([
    readFile(apiFile, 'utf8'),
    readFile(viewFile, 'utf8')
  ])

  assert.match(api, /\/api\/v1\/calendar\/events/)
  assert.match(api, /\/api\/v1\/calendar\/agenda/)
  assert.match(api, /\/api\/v1\/calendar\/availability\/query/)
  assert.match(view, /useAuthStore/)
  assert.match(view, /listCalendarEvents/)
  assert.match(view, /listCalendarAgenda/)
  assert.match(view, /queryCalendarAvailability/)
  assert.match(view, /latestCalendarRequest/)
  assert.match(view, /watch\(\(\) => authStore\.accessToken/)
  assert.match(view, /listCalendarEvents\(requestToken, requestRange\.from, requestRange\.to\)/)
  assert.doesNotMatch(view, /const events = \[/)
})
