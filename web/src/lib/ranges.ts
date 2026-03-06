import type { RangeMetric, ReferenceRange } from '../api/ranges';
import type { VitalType } from '../api/vitals';
import type { Band } from '../components/charts/TrendChart';

/** The bands to shade on a vital's chart: [main, secondary]. */
export function bandsFor(type: VitalType, ranges: ReferenceRange[] | undefined): [Band?, Band?] {
  if (!ranges) return [];
  const find = (metric: RangeMetric) => {
    const r = ranges.find((x) => x.metric === metric);
    return r ? { low: r.low, high: r.high } : undefined;
  };
  switch (type) {
    case 'HEART_RATE':
      return [find('HEART_RATE')];
    case 'SLEEP':
      return [find('SLEEP')];
    case 'BLOOD_PRESSURE':
      return [find('SYSTOLIC'), find('DIASTOLIC')];
    case 'BLOOD_GLUCOSE':
      return [find('GLUCOSE_FASTING')];
    default:
      return [];
  }
}
