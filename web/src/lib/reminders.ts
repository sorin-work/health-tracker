import type { Goal } from '../api/goals';
import type { Dose, Medication } from '../api/medications';
import { addDays, toISODate } from './dates';
import { describeGoal } from './format';
import type { UnitPrefs } from './units';

export interface Reminder {
  key: string;
  kind: 'dose' | 'refill' | 'goal';
  /** When it is due: an ISO instant for doses, a `YYYY-MM-DD` day otherwise. */
  due: string;
  title: string;
  detail?: string;
  overdue?: boolean;
}

/** Days ahead that a goal's target date shows up in the list. */
export const GOAL_HORIZON_DAYS = 14;

/**
 * What is coming up: today's doses that still need taking, medications to refill and goals whose
 * target date is close. Overdue doses come first, then everything in time order.
 */
export function buildReminders(
  doses: Dose[],
  lowSupply: Medication[],
  goals: Goal[],
  now: Date,
  prefs: UnitPrefs,
): Reminder[] {
  const today = toISODate(now);
  const reminders: Reminder[] = [];

  for (const d of doses) {
    if (d.status !== 'PENDING' || d.asNeeded) continue;
    reminders.push({
      key: `dose-${d.medicationId}-${d.scheduledAt}`,
      kind: 'dose',
      due: d.scheduledAt,
      title: d.medicationName,
      detail: d.dosage,
      overdue: new Date(d.scheduledAt) < now,
    });
  }

  for (const m of lowSupply) {
    const days = m.daysOfSupply ?? 0;
    reminders.push({
      key: `refill-${m.id}`,
      kind: 'refill',
      due: addDays(today, days),
      title: `Refill ${m.name}`,
      detail: m.daysOfSupply === undefined ? 'running low' : days === 0 ? 'runs out today' : `lasts ${days} more ${days === 1 ? 'day' : 'days'}`,
    });
  }

  const horizon = addDays(today, GOAL_HORIZON_DAYS);
  for (const g of goals) {
    if (g.archived || g.achieved || !g.targetDate || g.targetDate < today || g.targetDate > horizon) continue;
    reminders.push({
      key: `goal-${g.id}`,
      kind: 'goal',
      due: g.targetDate,
      title: describeGoal(g.type, g.targetValue, prefs),
      detail: `${Math.round(g.progressPercent)}% there`,
    });
  }

  const sortKey = (r: Reminder) => (r.kind === 'dose' ? r.due : `${r.due}T23:59:59`);
  return reminders.sort((a, b) => Number(!!b.overdue) - Number(!!a.overdue) || sortKey(a).localeCompare(sortKey(b)));
}
