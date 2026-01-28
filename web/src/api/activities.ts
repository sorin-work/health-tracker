import { api, toQuery, type Page } from './client';

export type ActivityType = 'WALK' | 'RUN' | 'CYCLE' | 'SWIM' | 'STRENGTH' | 'YOGA' | 'HIIT' | 'OTHER';

export interface Activity {
  id: number;
  type: ActivityType;
  startedAt: string;
  durationMinutes: number;
  distanceKm?: number;
  steps?: number;
  caloriesKcal?: number;
  notes?: string;
}

export interface ActivityPayload {
  type: ActivityType;
  startedAt: string;
  durationMinutes: number;
  distanceKm?: number | null;
  steps?: number | null;
  caloriesKcal?: number | null;
  notes?: string | null;
}

export interface WeeklyActivity {
  weekStart: string;
  sessions: number;
  activeMinutes: number;
  distanceKm: number;
  steps: number;
  caloriesKcal: number;
}

export const listActivities = (filters: {
  from?: string;
  to?: string;
  type?: ActivityType;
  page?: number;
  size?: number;
}) => api.get<Page<Activity>>(`/activities${toQuery(filters)}`);

export const weeklyActivity = (from?: string, to?: string) =>
  api.get<WeeklyActivity[]>(`/activities/weekly${toQuery({ from, to })}`);

export const createActivity = (payload: ActivityPayload) => api.post<Activity>('/activities', payload);

export const updateActivity = (id: number, payload: ActivityPayload) =>
  api.put<Activity>(`/activities/${id}`, payload);

export const deleteActivity = (id: number) => api.delete(`/activities/${id}`);
