import { api, toQuery, type Page } from './client';

export interface Symptom {
  id: number;
  name: string;
  severity: number;
  occurredAt: string;
  durationMinutes?: number;
  notes?: string;
}

export interface SymptomPayload {
  name: string;
  severity: number;
  occurredAt: string;
  durationMinutes?: number | null;
  notes?: string | null;
}

export const listSymptoms = (filters: { from?: string; to?: string; name?: string; page?: number; size?: number }) =>
  api.get<Page<Symptom>>(`/symptoms${toQuery(filters)}`);

export const symptomNames = () => api.get<string[]>('/symptoms/names');

export const createSymptom = (payload: SymptomPayload) => api.post<Symptom>('/symptoms', payload);

export const updateSymptom = (id: number, payload: SymptomPayload) => api.put<Symptom>(`/symptoms/${id}`, payload);

export const deleteSymptom = (id: number) => api.delete(`/symptoms/${id}`);
