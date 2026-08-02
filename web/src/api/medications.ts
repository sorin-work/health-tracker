import { api, toQuery } from './client';

export type DoseStatus = 'TAKEN' | 'SKIPPED' | 'MISSED' | 'PENDING';
export type ScheduleType = 'DAILY' | 'EVERY_N_DAYS' | 'WEEKLY';
export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

export interface Medication {
  id: number;
  name: string;
  dosage?: string;
  instructions?: string;
  asNeeded: boolean;
  /** "HH:mm:ss" or "HH:mm" in the user's local time. */
  times: string[];
  startDate: string;
  endDate?: string;
  active: boolean;
  scheduleType: ScheduleType;
  /** For EVERY_N_DAYS: 2 = every other day, counted from the start date. */
  intervalDays?: number;
  /** For WEEKLY. */
  daysOfWeek: DayOfWeek[];
}

export interface MedicationPayload {
  name: string;
  dosage?: string | null;
  instructions?: string | null;
  asNeeded: boolean;
  times: string[];
  startDate: string;
  endDate?: string | null;
  /** Defaults to DAILY. */
  scheduleType?: ScheduleType;
  intervalDays?: number | null;
  daysOfWeek?: DayOfWeek[] | null;
}

export interface Dose {
  medicationId: number;
  medicationName: string;
  dosage?: string;
  asNeeded: boolean;
  scheduledAt: string;
  status: DoseStatus;
  logId?: number;
  recordedAt?: string;
  note?: string;
}

export interface AdherenceStats {
  scheduled: number;
  taken: number;
  skipped: number;
  missed: number;
  pending: number;
  percent?: number;
}

export interface Adherence {
  from: string;
  to: string;
  overall: AdherenceStats;
  medications: { medicationId: number; name: string; stats: AdherenceStats }[];
  daily: { date: string; stats: AdherenceStats }[];
}

export const listMedications = (includeArchived = false) =>
  api.get<Medication[]>(`/medications${toQuery({ includeArchived })}`);

export const createMedication = (payload: MedicationPayload) => api.post<Medication>('/medications', payload);

export const updateMedication = (id: number, payload: MedicationPayload) =>
  api.put<Medication>(`/medications/${id}`, payload);

export const archiveMedication = (id: number) => api.post<Medication>(`/medications/${id}/archive`);

export const deleteMedication = (id: number) => api.delete(`/medications/${id}`);

export const dosesForDay = (date?: string) => api.get<Dose[]>(`/doses${toQuery({ date })}`);

export const recordDose = (
  medicationId: number,
  scheduledAt: string,
  status: 'TAKEN' | 'SKIPPED',
  note?: string,
) => api.post<Dose>(`/medications/${medicationId}/doses`, { scheduledAt, status, note });

export const undoDose = (logId: number) => api.delete(`/doses/${logId}`);

export const adherence = (from?: string, to?: string) =>
  api.get<Adherence>(`/doses/adherence${toQuery({ from, to })}`);
