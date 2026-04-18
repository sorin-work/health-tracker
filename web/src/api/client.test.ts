import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { api, ApiError, getToken, setToken, setUnauthorizedHandler, toQuery } from './client';

function jsonResponse(status: number, body: unknown, headers: Record<string, string> = {}) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json', ...headers },
  });
}

describe('toQuery', () => {
  it('skips empty values', () => {
    expect(toQuery({ a: 1, b: '', c: undefined, d: null, e: false, f: true })).toBe('?a=1&f=true');
    expect(toQuery({})).toBe('');
  });
});

describe('api client', () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    fetchMock.mockReset();
    setUnauthorizedHandler(null);
  });

  it('sends the bearer token', async () => {
    setToken('abc');
    fetchMock.mockResolvedValue(jsonResponse(200, { ok: true }));

    await api.get('/me');

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('/api/me');
    expect(init.headers.Authorization).toBe('Bearer abc');
  });

  it('turns error bodies into ApiErrors', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(400, { status: 400, error: 'validation_failed', message: 'Some fields are invalid', fields: { value: 'must not be null' } }),
    );

    const error = (await api.post('/vitals', {}).catch((e: unknown) => e)) as ApiError;

    expect(error).toBeInstanceOf(ApiError);
    expect(error.status).toBe(400);
    expect(error.code).toBe('validation_failed');
    expect(error.fields).toEqual({ value: 'must not be null' });
  });

  it('signs out when a signed-in request gets a 401', async () => {
    setToken('expired');
    const onUnauthorized = vi.fn();
    setUnauthorizedHandler(onUnauthorized);
    fetchMock.mockResolvedValue(jsonResponse(401, { error: 'unauthorized', message: 'Sign in to continue' }));

    await expect(api.get('/vitals')).rejects.toThrow('Sign in to continue');

    expect(getToken()).toBeNull();
    expect(onUnauthorized).toHaveBeenCalledOnce();
  });

  it('does not treat a failed login as an expired session', async () => {
    const onUnauthorized = vi.fn();
    setUnauthorizedHandler(onUnauthorized);
    fetchMock.mockResolvedValue(jsonResponse(401, { error: 'invalid_credentials', message: 'Email or password is incorrect' }));

    await expect(api.post('/auth/login', {})).rejects.toThrow('Email or password is incorrect');
    expect(onUnauthorized).not.toHaveBeenCalled();
  });

  it('returns undefined for 204 responses', async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }));
    await expect(api.delete('/vitals/1')).resolves.toBeUndefined();
  });

  it('reads the filename of downloads', async () => {
    fetchMock.mockResolvedValue(
      new Response('a,b\r\n', {
        status: 200,
        headers: { 'Content-Disposition': 'attachment; filename="health-tracker-vitals.csv"' },
      }),
    );

    const { filename, blob } = await api.download('/export/vitals.csv');

    expect(filename).toBe('health-tracker-vitals.csv');
    expect(await blob.text()).toBe('a,b\r\n');
  });
});
