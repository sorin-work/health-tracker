import { api } from './client';

export type RangeMetric =
  | 'HEART_RATE'
  | 'SYSTOLIC'
  | 'DIASTOLIC'
  | 'GLUCOSE_FASTING'
  | 'GLUCOSE_NON_FASTING'
  | 'SLEEP';

export interface ReferenceRange {
  metric: RangeMetric;
  label: string;
  unit: string;
  low: number;
  high: number;
  criticalLow?: number;
  criticalHigh?: number;
  defaultLow: number;
  defaultHigh: number;
  custom: boolean;
}

export const listRanges = () => api.get<ReferenceRange[]>('/ranges');

export const updateRange = (metric: RangeMetric, low: number, high: number) =>
  api.put<ReferenceRange>(`/ranges/${metric}`, { low, high });

export const resetRange = (metric: RangeMetric) => api.delete(`/ranges/${metric}`);
