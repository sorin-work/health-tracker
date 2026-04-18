const TOKEN_KEY = 'health-tracker.token';

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly fields: Record<string, string>;

  constructor(status: number, message: string, code = 'error', fields: Record<string, string> = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.fields = fields;
  }
}

export function getToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

export function setToken(token: string | null): void {
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token);
    else localStorage.removeItem(TOKEN_KEY);
  } catch {
    // Storage can be unavailable (private mode); the session then lasts until reload.
  }
}

type UnauthorizedListener = () => void;
let onUnauthorized: UnauthorizedListener | null = null;

/** Called when a signed-in request comes back 401, e.g. because the token expired. */
export function setUnauthorizedHandler(listener: UnauthorizedListener | null): void {
  onUnauthorized = listener;
}

type Method = 'GET' | 'POST' | 'PUT' | 'DELETE';

async function send(method: Method, path: string, body?: unknown): Promise<Response> {
  const headers: Record<string, string> = {};
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';

  const res = await fetch(`/api${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  if (!res.ok) {
    const data = await res.json().catch(() => null);
    if (res.status === 401 && token) {
      setToken(null);
      onUnauthorized?.();
    }
    throw new ApiError(
      res.status,
      data?.message ?? `Request failed (${res.status})`,
      data?.error,
      data?.fields ?? {},
    );
  }
  return res;
}

async function request<T>(method: Method, path: string, body?: unknown): Promise<T> {
  const res = await send(method, path, body);
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

export const api = {
  get: <T>(path: string) => request<T>('GET', path),
  post: <T>(path: string, body?: unknown) => request<T>('POST', path, body ?? {}),
  put: <T>(path: string, body: unknown) => request<T>('PUT', path, body),
  delete: (path: string) => request<void>('DELETE', path),
  /** Fetches a file and returns it with the server's suggested name. */
  download: async (path: string): Promise<{ blob: Blob; filename: string | null }> => {
    const res = await send('GET', path);
    const disposition = res.headers.get('Content-Disposition') ?? '';
    const match = /filename="?([^";]+)"?/.exec(disposition);
    return { blob: await res.blob(), filename: match ? match[1] : null };
  },
};

/** Builds `?a=1&b=2`, skipping empty values. Returns '' when nothing is set. */
export function toQuery(params: Record<string, string | number | boolean | null | undefined>): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value === undefined || value === null || value === '' || value === false) continue;
    search.set(key, String(value));
  }
  const query = search.toString();
  return query ? `?${query}` : '';
}

export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  total: number;
}
