import type { AppLocale } from "@/locales";

const BYTE_UNIT = 1024;
const PERCENT_FACTOR = 100;

export function formatV211Number(value: number, locale: AppLocale, maximumFractionDigits = 0) {
  return new Intl.NumberFormat(locale, { maximumFractionDigits }).format(value);
}

export function formatV211DateTime(
  value: string | number | Date,
  locale: AppLocale,
  timeZone?: string,
) {
  return new Intl.DateTimeFormat(locale, {
    dateStyle: "medium",
    timeStyle: "short",
    timeZone,
  }).format(new Date(value));
}

export function formatV211Percent(value: number, locale: AppLocale) {
  return new Intl.NumberFormat(locale, {
    maximumFractionDigits: 1,
    style: "percent",
  }).format(value / PERCENT_FACTOR);
}

export function formatV211Bytes(value: number, locale: AppLocale) {
  const units = ["B", "KB", "MB", "GB", "TB"];
  let nextValue = value;
  let unitIndex = 0;

  while (nextValue >= BYTE_UNIT && unitIndex < units.length - 1) {
    nextValue /= BYTE_UNIT;
    unitIndex += 1;
  }

  return `${formatV211Number(nextValue, locale, nextValue >= 10 ? 0 : 1)} ${units[unitIndex]}`;
}
