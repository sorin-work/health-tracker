import { api, toQuery, type Page } from './client';

export interface JournalEntry {
  id: number;
  entryDate: string;
  title?: string;
  body: string;
  createdAt: string;
  updatedAt: string;
}

export interface JournalPayload {
  entryDate: string;
  title?: string | null;
  body: string;
}

export const listJournal = (filters: { from?: string; to?: string; q?: string; page?: number; size?: number }) =>
  api.get<Page<JournalEntry>>(`/journal${toQuery(filters)}`);

export const createJournalEntry = (payload: JournalPayload) => api.post<JournalEntry>('/journal', payload);

export const updateJournalEntry = (id: number, payload: JournalPayload) =>
  api.put<JournalEntry>(`/journal/${id}`, payload);

export const deleteJournalEntry = (id: number) => api.delete(`/journal/${id}`);
