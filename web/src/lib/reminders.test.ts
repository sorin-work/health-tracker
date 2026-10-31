import { describe, expect, it } from 'vitest';
import type { Goal } from '../api/goals';
import type { Dose, Medication } from '../api/medications';
import { buildReminders } from './reminders';
import { METRIC } from './units';

const now = new Date('2026-11-03T12:00:00');

function dose(name: string, at: string, status: Dose['status'] = 'PENDING'): Dose {
  return { medicationId: name.length, medicationName: name, asNeeded: false, scheduledAt: new Date(at).toISOString(), status };
}

const lisinopril = { id: 2, name: 'Lisinopril', daysOfSupply: 3, lowSupply: true } as Medication;

const goal: Goal = {
  id: 5,
  type: 'WEIGHT',
  unit: 'kg',
  targetValue: 82,
  startDate: '2026-06-01',
  targetDate: '2026-11-10',
  archived: false,
  progressPercent: 71.5,
  achieved: false,
};

describe('buildReminders', () => {
  it('lists pending doses, refills and close goal dates, overdue first', () => {
    const reminders = buildReminders(
      [
        dose('Metformin', '2026-11-03T20:00:00'),
        dose('Lisinopril', '2026-11-03T08:00:00'),
        dose('Atorvastatin', '2026-11-03T21:30:00', 'TAKEN'),
      ],
      [lisinopril],
      [goal],
      now,
      METRIC,
    );

    expect(reminders.map((r) => r.title)).toEqual(['Lisinopril', 'Metformin', 'Refill Lisinopril', 'Reach 82.0 kg']);
    expect(reminders[0].overdue).toBe(true);
    expect(reminders[2]).toMatchObject({ due: '2026-11-06', detail: 'lasts 3 more days' });
    expect(reminders[3].detail).toBe('72% there');
  });

  it('leaves out goals that are far off, achieved or archived', () => {
    const goals = [
      { ...goal, id: 1, targetDate: '2027-01-01' },
      { ...goal, id: 2, achieved: true },
      { ...goal, id: 3, archived: true },
      { ...goal, id: 4, targetDate: undefined },
    ];

    expect(buildReminders([], [], goals, now, METRIC)).toEqual([]);
  });
});
