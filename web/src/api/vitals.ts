import { api, toQuery, type Page } from './client';

export type VitalType = 'WEIGHT' | 'BLOOD_PRESSURE' | 'HEART_RATE' | 'BLOOD_GLUCOSE' | 'SLEEP';
export type GlucoseContext = 'FASTING' | 'BEFORE_MEAL' | 'AFTER_MEAL' | 'BEDTIME' | 'RANDOM';
export type Flag = 'CRITICAL_LOW' | 'LOW' | 'NORMAL' | 'HIGH' | 'CRITICAL_HIGH';

export interface VitalReading {
  id: number;
  type: VitalType;
  value: number;
  secondaryValue?: number;
  unit: string;
  glucoseContext?: GlucoseContext;
  measuredAt: string;
  notes?: string;
  flag?: Flag;
}

export interface VitalPayload {
  type: VitalType;
  value: number;
  secondaryValue?: number | null;
  glucoseContext?: GlucoseContext | null;
  measuredAt: string;
  notes?: string | null;
}

export interface TrendPoint {
  date: string;
  average: number;
  min: number;
  max: number;
  averageSecondary?: number;
  count: number;
  flagged: number;
}

export interface VitalSummary {
  type: VitalType;
  unit: string;
  latest?: VitalReading;
  average30d?: number;
  averageSecondary30d?: number;
  count30d: number;
  flagged30d: number;
}

export interface WeeklyChange {
  type: VitalType;
  count: number;
  average?: number;
  averageSecondary?: number;
  previousCount: number;
  previousAverage?: number;
  previousAverageSecondary?: number;
  /** This week minus the week before; absent when either week has no readings. */
  change?: number;
  changeSecondary?: number;
}

export interface VitalFilters {
  type?: VitalType;
  from?: string;
  to?: string;
  flagged?: boolean;
  page?: number;
  size?: number;
}

export const listVitals = (filters: VitalFilters) =>
  api.get<Page<VitalReading>>(`/vitals${toQuery({ ...filters })}`);

export const vitalTrend = (type: VitalType, from?: string, to?: string) =>
  api.get<TrendPoint[]>(`/vitals/trend${toQuery({ type, from, to })}`);

export const vitalSummary = () => api.get<VitalSummary[]>('/vitals/summary');

export const weeklyChange = () => api.get<WeeklyChange[]>('/vitals/weekly-change');

export const createVital = (payload: VitalPayload) => api.post<VitalReading>('/vitals', payload);

export const updateVital = (id: number, payload: VitalPayload) => api.put<VitalReading>(`/vitals/${id}`, payload);

export const deleteVital = (id: number) => api.delete(`/vitals/${id}`);
