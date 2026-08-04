import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios';
import Cookies from 'js-cookie';

/**
 * Use a relative baseURL so that every request is served by the Next.js
 * rewrite proxy (see next.config.js → async rewrites).  This avoids CORS
 * "Network Error" issues entirely because the browser never makes a
 * cross-origin call.
 */
const API_BASE_URL = '/api/v1';

/**
 * `secure` cookies are only sent over HTTPS.  On localhost (HTTP) the
 * browser will silently drop them, so we disable the flag in development.
 */
const isSecureContext = typeof window !== 'undefined' && window.location.protocol === 'https:';
const cookieSecure = isSecureContext;
const cookieSameSite = isSecureContext ? 'none' : 'lax';

const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  },
});

apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = Cookies.get('access_token') ||
                  (typeof window !== 'undefined' ? localStorage.getItem('access_token') : null);
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (error: unknown) => void;
}> = [];

const processQueue = (error: unknown, token: string | null = null) => {
  failedQueue.forEach((promise) => {
    if (error) {
      promise.reject(error);
    } else if (token) {
      promise.resolve(token);
    }
  });
  failedQueue = [];
};

apiClient.interceptors.response.use(
  (response: AxiosResponse) => response,
  async (error) => {
    const originalRequest = error.config as AxiosRequestConfig & { _retry?: boolean };

    // ── Skip token refresh for login / register / refresh endpoints ────
    const isAuthEndpoint =
      originalRequest.url?.includes('/auth/login') ||
      originalRequest.url?.includes('/auth/register') ||
      originalRequest.url?.includes('/auth/refresh');

    if (error.response?.status === 401 && !originalRequest._retry && !isAuthEndpoint) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((token) => {
            if (originalRequest.headers) {
              (originalRequest.headers as Record<string, string>)['Authorization'] = `Bearer ${token}`;
            }
            return apiClient(originalRequest);
          })
          .catch((err) => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const refreshToken = Cookies.get('refresh_token') ||
                             (typeof window !== 'undefined' ? localStorage.getItem('refresh_token') : null);

        if (!refreshToken) {
          processQueue(error, null);
          if (typeof window !== 'undefined') {
            Cookies.remove('access_token');
            localStorage.removeItem('access_token');
            window.location.href = '/login';
          }
          return Promise.reject(error);
        }

        const { data } = await axios.post(`${API_BASE_URL}/auth/refresh`, { refreshToken });
        const newToken = data.data?.accessToken || data.accessToken;

        Cookies.set('access_token', newToken, { expires: 1, secure: cookieSecure, sameSite: cookieSameSite });
        if (typeof window !== 'undefined') localStorage.setItem('access_token', newToken);

        if (originalRequest.headers) {
          (originalRequest.headers as Record<string, string>)['Authorization'] = `Bearer ${newToken}`;
        }

        processQueue(null, newToken);
        return apiClient(originalRequest);
      } catch (refreshErr) {
        processQueue(refreshErr, null);
        if (typeof window !== 'undefined') {
          Cookies.remove('access_token');
          Cookies.remove('refresh_token');
          localStorage.removeItem('access_token');
          localStorage.removeItem('refresh_token');
          window.location.href = '/login';
        }
        return Promise.reject(refreshErr);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
