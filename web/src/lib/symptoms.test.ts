import { describe, expect, it } from 'vitest';
import type { Symptom } from '../api/symptoms';
import { symptomSeriesNames } from './symptoms';

const entry = (name: string, id: number): Symptom => ({
  id,
  name,
  severity: 3,
  occurredAt: '2026-08-01T10:00:00Z',
  tags: [],
});

describe('symptomSeriesNames', () => {
  it('keeps every symptom when there are five or fewer', () => {
    expect(symptomSeriesNames(['Nausea', 'Headache', 'Headache'].map(entry))).toEqual(['Headache', 'Nausea']);
  });

  it('puts the least frequent ones into Other instead of dropping them', () => {
    const names = ['A', 'A', 'A', 'B', 'B', 'C', 'C', 'D', 'D', 'E', 'F'];

    expect(symptomSeriesNames(names.map(entry))).toEqual(['A', 'B', 'C', 'D', 'Other']);
  });
});
