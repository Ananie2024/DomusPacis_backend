import apiClient from './client';
import { ApiResponse, AuthResponse, LoginRequest, UserProfile } from '../types';

export const authApi = {
  login: async (credentials: LoginRequest): Promise<AuthResponse> => {
    const { data } = await apiClient.post<ApiResponse<AuthResponse>>('/auth/login', credentials);
    return data.data;
  },

  register: async (payload: {
    firstName: string;
    lastName:  string;
    email:     string;
    password:  string;
    phone?:    string;
  }): Promise<AuthResponse> => {
    const { data } = await apiClient.post<ApiResponse<AuthResponse>>('/auth/register', payload);
    return data.data;
  },

  logout: async (): Promise<void> => {
    await apiClient.post('/auth/logout');
  },

  refreshToken: async (refreshToken: string): Promise<{ accessToken: string }> => {
    const { data } = await apiClient.post<ApiResponse<{ accessToken: string }>>('/auth/refresh', { refreshToken });
    return data.data;
  },

  me: async (): Promise<UserProfile> => {
    const { data } = await apiClient.get<ApiResponse<UserProfile>>('/auth/me');
    return data.data;
  },

  changePassword: async (payload: { currentPassword: string; newPassword: string }): Promise<void> => {
    await apiClient.put('/auth/change-password', payload);
  },

  forgotPassword: async (email: string): Promise<void> => {
    await apiClient.post('/auth/password-reset/initiate', null, { params: { email } });
  },

  resetPassword: async (payload: { token: string; newPassword: string }): Promise<void> => {
    await apiClient.post('/auth/reset-password', payload);
  },
};