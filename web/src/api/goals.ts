import { api, toQuery } from './client';

export type GoalType = 'WEIGHT' | 'DAILY_STEPS' | 'WEEKLY_ACTIVE_MINUTES' | 'WEEKLY_WORKOUTS' | 'SLEEP_HOURS' | 'DAILY_WATER';

export interface Goal {
  id: number;
  type: GoalType;
  unit: string;
  targetValue: number;
  startValue?: number;
  startDate: string;
  targetDate?: string;
  notes?: string;
  archived: boolean;
  achievedAt?: string;
  currentValue?: number;
  progressPercent: number;
  achieved: boolean;
  onTrack?: boolean;
}

export interface GoalPayload {
  type: GoalType;
  targetValue: number;
  startValue?: number | null;
  targetDate?: string | null;
  notes?: string | null;
}

export const listGoals = (includeArchived = false) => api.get<Goal[]>(`/goals${toQuery({ includeArchived })}`);

export const createGoal = (payload: GoalPayload) => api.post<Goal>('/goals', payload);

export const updateGoal = (id: number, payload: GoalPayload) => api.put<Goal>(`/goals/${id}`, payload);

export const archiveGoal = (id: number) => api.post<Goal>(`/goals/${id}/archive`);

export const restoreGoal = (id: number) => api.post<Goal>(`/goals/${id}/restore`);

export const deleteGoal = (id: number) => api.delete(`/goals/${id}`);
