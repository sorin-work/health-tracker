import { api, toQuery } from './client';

export interface WaterEntry {
  id: number;
  amountMl: number;
  loggedAt: string;
}

export interface WaterDay {
  date: string;
  totalMl: number;
  entries: WaterEntry[];
}

export interface WaterTotal {
  date: string;
  totalMl: number;
}

export const waterForDay = (date?: string) => api.get<WaterDay>(`/water${toQuery({ date })}`);

export const waterDaily = (from?: string, to?: string) =>
  api.get<WaterTotal[]>(`/water/daily${toQuery({ from, to })}`);

export const addWater = (amountMl: number, loggedAt?: string) => api.post<WaterEntry>('/water', { amountMl, loggedAt });

export const deleteWater = (id: number) => api.delete(`/water/${id}`);
