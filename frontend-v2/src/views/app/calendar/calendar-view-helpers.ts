import type { CalendarAgendaItem, CalendarEvent } from "@/service/api/calendar";
import type { CalendarSurfaceItem } from "./calendar-types";

const DAYS_IN_WORK_WEEK = 5;
const DAYS_FROM_SUNDAY_TO_MONDAY = 6;
const MONDAY_WEEKDAY = 1;
const NO_DAY_OFFSET = 0;
const SUNDAY_WEEKDAY = 0;
const MIDNIGHT = Object.freeze({
  hour: 0,
  millisecond: 0,
  minute: 0,
  second: 0,
});

interface LocalDateOptions {
  readonly dayOffset?: number;
  readonly resetTime?: boolean;
}

export function parseDate(value?: string | null) {
  if (!value) return null;
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
}

export function startOfDay(value: Date) {
  return createLocalDate(value, { resetTime: true });
}

export function startOfWeek(value: Date) {
  const dayStart = startOfDay(value);
  return addDays(dayStart, weekStartOffset(dayStart));
}

export function addDays(value: Date, days: number) {
  return createLocalDate(value, { dayOffset: days });
}

export function formatDateKey(value: Date) {
  const month = `${value.getMonth() + 1}`.padStart(2, "0");
  const day = `${value.getDate()}`.padStart(2, "0");
  return `${value.getFullYear()}-${month}-${day}`;
}

export function formatDateWindow(startAt: string, endAt: string, allDay = false) {
  const start = parseDate(startAt);
  const end = parseDate(endAt);
  if (!start || !end) return "Time unavailable";
  if (allDay)
    return `${start.toLocaleDateString(undefined, { weekday: "short", month: "short", day: "numeric" })} · All day`;
  return `${start.toLocaleDateString(undefined, { weekday: "short", month: "short", day: "numeric" })} · ${start.toLocaleTimeString(undefined, { hour: "numeric", minute: "2-digit" })} - ${end.toLocaleTimeString(undefined, { hour: "numeric", minute: "2-digit" })}`;
}

export function formatLocalDateTime(value: Date) {
  const month = `${value.getMonth() + 1}`.padStart(2, "0");
  const day = `${value.getDate()}`.padStart(2, "0");
  const hour = `${value.getHours()}`.padStart(2, "0");
  const minute = `${value.getMinutes()}`.padStart(2, "0");
  const second = `${value.getSeconds()}`.padStart(2, "0");
  return `${value.getFullYear()}-${month}-${day}T${hour}:${minute}:${second}`;
}

export function mergeCalendarSurfaceItems(events: CalendarEvent[], agenda: CalendarAgendaItem[]) {
  const seen = new Set<string>();
  const items = [...events.map(fromEvent), ...agenda.map(fromAgenda)];
  return items
    .filter((item) => {
      if (seen.has(item.id)) return false;
      seen.add(item.id);
      return true;
    })
    .sort((left, right) => String(left.startAt).localeCompare(String(right.startAt)));
}

export function workWeekLength(mode: string) {
  return mode === "day" ? 1 : DAYS_IN_WORK_WEEK;
}

function createLocalDate(value: Date, options: LocalDateOptions = {}) {
  const time = options.resetTime ? MIDNIGHT : localTime(value);
  return new Date(
    value.getFullYear(),
    value.getMonth(),
    value.getDate() + (options.dayOffset ?? NO_DAY_OFFSET),
    time.hour,
    time.minute,
    time.second,
    time.millisecond,
  );
}

function localTime(value: Date) {
  return {
    hour: value.getHours(),
    millisecond: value.getMilliseconds(),
    minute: value.getMinutes(),
    second: value.getSeconds(),
  };
}

function weekStartOffset(value: Date) {
  return value.getDay() === SUNDAY_WEEKDAY
    ? -DAYS_FROM_SUNDAY_TO_MONDAY
    : MONDAY_WEEKDAY - value.getDay();
}

function fromEvent(item: CalendarEvent): CalendarSurfaceItem {
  return {
    allDay: item.allDay,
    attendeeCount: item.attendeeCount,
    canDelete: item.canDelete,
    canEdit: item.canEdit,
    endAt: item.endAt,
    id: item.id,
    location: item.location,
    ownerEmail: item.ownerEmail,
    reminderMinutes: item.reminderMinutes,
    shared: item.shared,
    startAt: item.startAt,
    timezone: item.timezone,
    title: item.title,
  };
}

function fromAgenda(item: CalendarAgendaItem): CalendarSurfaceItem {
  return {
    ...fromEvent({
      ...item,
      allDay: false,
      canDelete: false,
      canEdit: false,
      reminderMinutes: null,
      timezone: "",
      updatedAt: "",
    }),
  };
}
