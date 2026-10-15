import type { Symptom } from '../api/symptoms';

/** How many series the symptom chart has colours for. */
export const SERIES_LIMIT = 5;
export const OTHER = 'Other';

/**
 * The most frequent symptoms get their own colour; the rest share one "Other" series so no entry
 * silently drops off the chart.
 */
export function symptomSeriesNames(symptoms: Symptom[]): string[] {
  const counts = new Map<string, number>();
  for (const s of symptoms) counts.set(s.name, (counts.get(s.name) ?? 0) + 1);
  const byFrequency = [...counts.keys()].sort((a, b) => counts.get(b)! - counts.get(a)! || a.localeCompare(b));
  if (byFrequency.length <= SERIES_LIMIT) return byFrequency;
  return [...byFrequency.slice(0, SERIES_LIMIT - 1), OTHER];
}
