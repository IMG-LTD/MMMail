import test from 'node:test'
import assert from 'node:assert/strict'
import vm from 'node:vm'
import { createRequire } from 'node:module'
import { readFile } from 'node:fs/promises'

const require = createRequire(import.meta.url)
const ts = require('typescript')

const layoutFile = new URL('../src/views/app/calendar/calendar-layout.ts', import.meta.url)
const helperFile = new URL('../src/views/app/calendar/calendar-view-helpers.ts', import.meta.url)

async function loadTsModule(fileUrl) {
  const source = await readFile(fileUrl, 'utf8')
  const { outputText } = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2022
    }
  })

  const module = { exports: {} }
  vm.runInNewContext(outputText, { exports: module.exports, module }, { filename: fileUrl.pathname })
  return module.exports
}

test('calendar layout expands slot coverage for cross-midnight events', async () => {
  const { resolveCalendarTimeSlotHours } = await loadTsModule(layoutFile)
  const hours = resolveCalendarTimeSlotHours([
    { endAt: '2026-05-15T01:15:00', startAt: '2026-05-14T23:30:00' }
  ], 9)

  assert.equal(hours[0], 0)
  assert.equal(hours.at(-1), 23)
})

test('calendar layout splits cross-midnight events across visible days', async () => {
  const { resolvePositionedCalendarEvents } = await loadTsModule(layoutFile)
  const positioned = resolvePositionedCalendarEvents([
    {
      allDay: false,
      endAt: '2026-05-15T01:15:00',
      id: 'night-shift',
      shared: false,
      startAt: '2026-05-14T23:30:00',
      title: 'Night shift'
    }
  ], {
    days: [
      { key: '2026-05-14' },
      { key: '2026-05-15' }
    ],
    firstSlotHour: 0,
    selectedEventId: ''
  })

  assert.equal(positioned.length, 2)
  assert.deepEqual(positioned.map(item => item.style.gridColumn), ['2', '3'])
  assert.deepEqual(positioned.map(item => item.style.gridRow), ['25 / span 1', '2 / span 2'])
})

test('calendar date helpers keep local wall-clock dates across DST windows', async () => {
  const previousTimezone = process.env.TZ
  process.env.TZ = 'America/New_York'
  try {
    const { addDays, formatDateKey, formatLocalDateTime, startOfWeek } = await loadTsModule(helperFile)
    const springForwardStart = new Date(2026, 2, 7, 0, 0, 0)
    const focusedSunday = new Date(2026, 2, 8, 12, 0, 0)

    assert.equal(formatLocalDateTime(addDays(springForwardStart, 2)), '2026-03-09T00:00:00')
    assert.equal(formatDateKey(startOfWeek(focusedSunday)), '2026-03-02')
  } finally {
    process.env.TZ = previousTimezone
  }
})

test('calendar date arithmetic avoids Date setter mutation at DST boundaries', async () => {
  const [helpers, layout] = await Promise.all([
    readFile(helperFile, 'utf8'),
    readFile(layoutFile, 'utf8')
  ])

  assert.doesNotMatch(helpers, /\.(?:setDate|setHours)\(/)
  assert.doesNotMatch(layout, /\.(?:setDate|setHours)\(/)
})
