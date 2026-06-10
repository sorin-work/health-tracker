/** Helpers for calendar dates (`YYYY-MM-DD`, local) and instants (ISO strings, UTC). */

const pad = (n: number) => String(n).padStart(2, '0');

/** A Date as a local calendar day, e.g. `2026-03-14`. */
export function toISODate(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

export function today(): string {
  return toISODate(new Date());
}

/** Parses `YYYY-MM-DD` as a local date (not UTC midnight, which can be the day before). */
export function parseISODate(iso: string): Date {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y, m - 1, d);
}

export function addDays(iso: string, days: number): string {
  const date = parseISODate(iso);
  date.setDate(date.getDate() + days);
  return toISODate(date);
}

export interface DateRange {
  from: string;
  to: string;
}

export type RangePreset = '7d' | '30d' | '90d' | '1y';

export const RANGE_PRESETS: { key: RangePreset; label: string; days: number }[] = [
  { key: '7d', label: '7 days', days: 7 },
  { key: '30d', label: '30 days', days: 30 },
  { key: '90d', label: '90 days', days: 90 },
  { key: '1y', label: '1 year', days: 365 },
];

/** The last N days, ending (and including) `end`. */
export function presetRange(preset: RangePreset, end: string = today()): DateRange {
  const days = RANGE_PRESETS.find((p) => p.key === preset)?.days ?? 30;
  return { from: addDays(end, -(days - 1)), to: end };
}

/** Which preset a range matches, if any. */
export function matchPreset(range: DateRange, end: string = today()): RangePreset | null {
  if (range.to !== end) return null;
  return RANGE_PRESETS.find((p) => presetRange(p.key, end).from === range.from)?.key ?? null;
}

const dateFormat = new Intl.DateTimeFormat(undefined, { day: 'numeric', month: 'short', year: 'numeric' });
const shortDateFormat = new Intl.DateTimeFormat(undefined, { day: 'numeric', month: 'short' });
const timeFormat = new Intl.DateTimeFormat(undefined, { hour: '2-digit', minute: '2-digit' });
const weekdayFormat = new Intl.DateTimeFormat(undefined, { weekday: 'short' });

export function formatDate(iso: string): string {
  return dateFormat.format(parseISODate(iso));
}

export function formatShortDate(iso: string): string {
  return shortDateFormat.format(parseISODate(iso));
}

/** `Mon`, `Tue`, … for a `YYYY-MM-DD` day. */
export function formatWeekday(iso: string): string {
  return weekdayFormat.format(parseISODate(iso));
}

export function formatDateTime(instant: string): string {
  const date = new Date(instant);
  return `${dateFormat.format(date)}, ${timeFormat.format(date)}`;
}

export function formatTime(instant: string): string {
  return timeFormat.format(new Date(instant));
}

/** Value for an `<input type="datetime-local">`, e.g. `2026-03-14T08:30`. */
export function toLocalInputValue(date: Date = new Date()): string {
  return `${toISODate(date)}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

/** Converts a datetime-local value back to an ISO instant. */
export function fromLocalInputValue(value: string): string {
  return new Date(value).toISOString();
}

/** `08:00:00` -> `08:00` */
export function shortTime(time: string): string {
  return time.slice(0, 5);
}
