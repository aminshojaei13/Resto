import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { AuthUser } from '../types';
import { apiClient, setUnauthorizedHandler } from '../api/apiClient';
import { clearSession, hasToken, setAccessToken } from './session';

interface AuthContextValue {
  /** The person the server says is signed in, or null when nobody is. */
  user: AuthUser | null;
  /** True while the profile is still being read on start-up. */
  isRestoring: boolean;
  isSignedIn: boolean;
  isPlatformAdmin: boolean;
  /** True when the signed-in person may perform the given action. */
  can: (permission: string) => boolean;
  signIn: (user: AuthUser) => void;
  signOut: () => Promise<void>;
  refresh: () => Promise<void>;
  /** Replaces the cached identity after a profile change. */
  setUser: (user: AuthUser) => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUserState] = useState<AuthUser | null>(null);
  const [isRestoring, setIsRestoring] = useState<boolean>(() => hasToken());

  const setUser = useCallback((next: AuthUser | null) => {
    setUserState(next);
  }, []);

  const signIn = useCallback((next: AuthUser) => {
    if (next.token) setAccessToken(next.token);
    setIsRestoring(false);
    setUserState(next);
  }, []);

  const signOut = useCallback(async () => {
    try {
      await apiClient.logout();
    } finally {
      // The local session is cleared here as well as in the client, so the
      // token is gone even if the network call never completed.
      clearSession();
      setUserState(null);
    }
  }, []);

  const refresh = useCallback(async () => {
    if (!hasToken()) {
      setUserState(null);
      return;
    }

    try {
      setUserState(await apiClient.getProfile());
    } catch (error) {
      // A rejected or expired session must never leave a name on screen.
      clearSession();
      setUserState(null);
    } finally {
      setIsRestoring(false);
    }
  }, []);

  // On start-up: if a token exists, confirm it with the server rather than
  // trusting anything cached in the browser.
  useEffect(() => {
    if (hasToken()) {
      void refresh();
    } else {
      setIsRestoring(false);
    }
  }, [refresh]);

  // A 401 from anywhere in the app ends the session immediately.
  useEffect(() => {
    setUnauthorizedHandler(() => setUserState(null));
    return () => setUnauthorizedHandler(null);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isRestoring,
      isSignedIn: Boolean(user),
      isPlatformAdmin: Boolean(user?.isPlatformAdmin),
      can: (permission: string) => Boolean(user?.permissions?.includes(permission)),
      signIn,
      signOut,
      refresh,
      setUser,
    }),
    [user, isRestoring, signIn, signOut, refresh, setUser]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = (): AuthContextValue => {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error('useAuth must be used inside an AuthProvider');
  }

  return context;
};
