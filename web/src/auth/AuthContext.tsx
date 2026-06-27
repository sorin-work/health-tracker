import { useQuery, useQueryClient } from '@tanstack/react-query';
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import * as authApi from '../api/auth';
import type { User } from '../api/auth';
import { getToken, setToken, setUnauthorizedHandler } from '../api/client';
import { METRIC, type UnitPrefs } from '../lib/units';

export const ME_QUERY_KEY = ['me'] as const;

interface AuthState {
  user: User | null;
  status: 'loading' | 'authenticated' | 'anonymous';
  login: (email: string, password: string) => Promise<User>;
  register: (payload: authApi.RegisterPayload) => Promise<User>;
  logout: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [hasToken, setHasToken] = useState(() => getToken() !== null);

  const me = useQuery({
    queryKey: ME_QUERY_KEY,
    queryFn: authApi.fetchMe,
    enabled: hasToken,
    staleTime: Infinity,
    retry: false,
  });

  const signOut = useCallback(() => {
    setToken(null);
    setHasToken(false);
    queryClient.clear();
  }, [queryClient]);

  // An expired token anywhere in the app sends the user back to the login page.
  useEffect(() => {
    setUnauthorizedHandler(signOut);
    return () => setUnauthorizedHandler(null);
  }, [signOut]);

  const signedIn = useCallback(
    (response: authApi.AuthResponse) => {
      // Drop anything cached for a previous user before showing the new one.
      queryClient.clear();
      setToken(response.token);
      queryClient.setQueryData(ME_QUERY_KEY, response.user);
      setHasToken(true);
      return response.user;
    },
    [queryClient],
  );

  const value = useMemo<AuthState>(() => {
    const user = hasToken ? (me.data ?? null) : null;
    let status: AuthState['status'] = 'anonymous';
    if (user) status = 'authenticated';
    else if (hasToken && me.isPending) status = 'loading';
    return {
      user,
      status,
      login: async (email, password) => signedIn(await authApi.login(email, password)),
      register: async (payload) => signedIn(await authApi.register(payload)),
      logout: signOut,
    };
  }, [hasToken, me.data, me.isPending, signedIn, signOut]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}

/** The signed-in user. Only use inside routes that require authentication. */
export function useUser(): User {
  const { user } = useAuth();
  if (!user) throw new Error('useUser called without a signed-in user');
  return user;
}

/** The signed-in user's unit preferences, or metric when nobody is signed in (or in tests). */
export function useUnitPrefs(): UnitPrefs {
  const user = useContext(AuthContext)?.user;
  return useMemo(
    () => (user ? { unitSystem: user.unitSystem ?? 'METRIC', glucoseUnit: user.glucoseUnit ?? 'MG_DL' } : METRIC),
    [user],
  );
}
