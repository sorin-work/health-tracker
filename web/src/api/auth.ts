import { api } from './client';

export interface User {
  id: number;
  email: string;
  displayName: string;
  timezone: string;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  expiresAt: string;
  user: User;
}

export interface RegisterPayload {
  email: string;
  password: string;
  displayName: string;
  timezone?: string;
}

export const login = (email: string, password: string) =>
  api.post<AuthResponse>('/auth/login', { email, password });

export const register = (payload: RegisterPayload) => api.post<AuthResponse>('/auth/register', payload);

export const fetchMe = () => api.get<User>('/me');

export const updateProfile = (payload: { displayName: string; timezone: string }) =>
  api.put<User>('/me', payload);
