import { api, toQuery, type Page } from './client';

export interface MoodEntry {
  id: number;
  /** 1 (very low) to 5 (very good). */
  mood: number;
  /** 1 (exhausted) to 5 (full of energy). */
  energy?: number;
  recordedAt: string;
  note?: string;
}

export interface MoodPayload {
  mood: number;
  energy?: number | null;
  recordedAt: string;
  note?: string | null;
}

export interface MoodDay {
  date: string;
  mood: number;
  energy?: number;
  count: number;
}

export const listMood = (filters: { from?: string; to?: string; page?: number; size?: number }) =>
  api.get<Page<MoodEntry>>(`/mood${toQuery(filters)}`);

export const moodDaily = (from?: string, to?: string) => api.get<MoodDay[]>(`/mood/daily${toQuery({ from, to })}`);

export const createMood = (payload: MoodPayload) => api.post<MoodEntry>('/mood', payload);

export const updateMood = (id: number, payload: MoodPayload) => api.put<MoodEntry>(`/mood/${id}`, payload);

export const deleteMood = (id: number) => api.delete(`/mood/${id}`);
