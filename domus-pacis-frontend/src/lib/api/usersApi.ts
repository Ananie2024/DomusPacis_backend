import apiClient from './client';
import { ApiResponse, PaginatedResponse, UserRole } from '@/lib/types';

export interface AdminUser {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  isActive: boolean;
  phone?: string;
  createdAt: string;
  lastLoginAt?: string;
}

export interface CreateUserRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  phone?: string;
  sendWelcomeEmail?: boolean;
}

export interface UpdateUserRequest {
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  phone?: string;
}

export interface ResetPasswordResponse {
  temporaryPassword: string;
}

type RawUser = Omit<AdminUser, 'role' | 'createdAt'> & {
  role: string;
  createdAt?: string;
};

function normalizeUser(user: RawUser): AdminUser {
  return {
    ...user,
    role: user.role as UserRole,
    createdAt: user.createdAt ?? new Date().toISOString(),
  };
}

export const usersApi = {
  getUsers: async (params?: {
    page?: number;
    size?: number;
    search?: string;
    role?: UserRole;
    isActive?: boolean;
  }): Promise<PaginatedResponse<AdminUser>> => {
    const { data } = await apiClient.get<ApiResponse<PaginatedResponse<RawUser>>>('/auth/admin/users', { params });
    return {
      ...data.data,
      content: data.data.content.map(normalizeUser),
    };
  },

  createUser: async (request: CreateUserRequest): Promise<AdminUser> => {
    const { phone: _phone, sendWelcomeEmail: _sendWelcomeEmail, ...payload } = request;
    const { data } = await apiClient.post<ApiResponse<RawUser>>('/auth/admin/users', payload);
    return normalizeUser(data.data);
  },

  updateUser: async (id: string, request: UpdateUserRequest): Promise<AdminUser> => {
    const { phone: _phone, ...payload } = request;
    const { data } = await apiClient.put<ApiResponse<RawUser>>(`/auth/admin/users/${id}`, payload);
    return normalizeUser(data.data);
  },

  activateUser: async (id: string): Promise<void> => {
    await apiClient.patch(`/auth/admin/users/${id}/activate`);
  },

  deactivateUser: async (id: string): Promise<void> => {
    await apiClient.delete(`/auth/admin/users/${id}`);
  },

  deleteUser: async (id: string): Promise<void> => {
    await apiClient.delete(`/auth/admin/users/${id}`);
  },

  resetPassword: async (id: string, _email?: string): Promise<ResetPasswordResponse> => {
    const { data } = await apiClient.post<ApiResponse<ResetPasswordResponse>>(`/auth/admin/users/${id}/reset-password`);
    return data.data;
  },
};
