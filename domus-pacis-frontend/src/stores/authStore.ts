import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import Cookies from 'js-cookie';
import { UserProfile } from '@/lib/types';

/**
 * `secure` cookies are only sent over HTTPS.  On localhost (HTTP) the
 * browser will silently drop them, so we disable the flag in development.
 */
const isSecureContext = typeof window !== 'undefined' && window.location.protocol === 'https:';
const cookieSecure = isSecureContext;
const cookieSameSite = isSecureContext ? 'none' : 'lax';

interface AuthState {
  user:            UserProfile | null;
  accessToken:     string | null;
  refreshToken:    string | null;
  isAuthenticated: boolean;

  setAuth:    (user: UserProfile, accessToken: string, refreshToken: string) => void;
  clearAuth:  () => void;
  updateUser: (user: Partial<UserProfile>) => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user:            null,
      accessToken:     null,
      refreshToken:    null,
      isAuthenticated: false,

      setAuth: (user, accessToken, refreshToken) => {
        Cookies.set('access_token',  accessToken,  { expires: 1, secure: cookieSecure, sameSite: cookieSameSite });
        Cookies.set('refresh_token', refreshToken, { expires: 7, secure: cookieSecure, sameSite: cookieSameSite });
        if (typeof window !== 'undefined') {
          localStorage.setItem('access_token',  accessToken);
          localStorage.setItem('refresh_token', refreshToken);
        }
        set({ user, accessToken, refreshToken, isAuthenticated: true });
      },

      clearAuth: () => {
        Cookies.remove('access_token');
        Cookies.remove('refresh_token');
        if (typeof window !== 'undefined') {
          localStorage.removeItem('access_token');
          localStorage.removeItem('refresh_token');
        }
        set({ user: null, accessToken: null, refreshToken: null, isAuthenticated: false });
      },

      updateUser: (partial) =>
        set((state) => ({ user: state.user ? { ...state.user, ...partial } : null })),
    }),
    {
      name:       'domus-pacis-auth',
      partialize: (s) => ({
        user:            s.user,
        accessToken:     s.accessToken,
        refreshToken:    s.refreshToken,
        isAuthenticated: s.isAuthenticated,
      }),
    }
  )
);