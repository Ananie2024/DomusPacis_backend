import apiClient from './client';
import { ApiResponse, Address } from '../types';

// ── Types matching CustomerDtos exactly ───────────────────────────────────────

export interface CustomerResponse {
  id:           string;
  userId?:      string;
  fullName:     string;
  email:        string;
  phone:        string;
  address?:     Address;
  nationality?: string;
  idNumber?:    string;
  segment?:     string;
  totalBookings?: number;
  totalSpent?:   number;
  createdAt:    string;
}

export interface CustomerSummaryResponse {
  id:      string;
  fullName: string;
  email:    string;
  phone:    string;
  segment?: string;
}

export interface CreateCustomerRequest {
  fullName:     string;
  email:        string;
  phone:        string;
  address?:     Address;
  nationality?: string;
  idNumber?:    string;
}

export interface UpdateCustomerRequest {
  fullName:     string;
  email:        string;
  phone:        string;
  address?:     Address;
  nationality?: string;
  idNumber?:    string;
  segment?:     string;
  notes?:       string;
}

export interface CustomerPage {
  content:       CustomerResponse[];
  totalElements: number;
  totalPages:    number;
  number:        number;
  size:          number;
  first:         boolean;
  last:          boolean;
}

export interface CustomerSummaryPage {
  content:       CustomerSummaryResponse[];
  totalElements: number;
  totalPages:    number;
  number:        number;
  size:          number;
  first:         boolean;
  last:          boolean;
}

export const customerApi = {

  // GET /api/v1/customers?page=&size=
  listAll: async (params?: { page?: number; size?: number }): Promise<CustomerPage> => {
    const { data } = await apiClient.get<ApiResponse<CustomerPage>>('/customers', { params });
    return data.data;
  },

  // GET /api/v1/customers/search?q=&page=&size=
  search: async (q: string, params?: { page?: number; size?: number }): Promise<CustomerPage> => {
    const { data } = await apiClient.get<ApiResponse<CustomerSummaryPage>>('/customers/search', {
      params: { q, ...params },
    });
    return {
      ...data.data,
      content: data.data.content.map((customer) => ({
        ...customer,
        totalBookings: 0,
        totalSpent: 0,
        createdAt: new Date().toISOString(),
      })),
    };
  },

  // GET /api/v1/customers/{id}
  getById: async (id: string): Promise<CustomerResponse> => {
    const { data } = await apiClient.get<ApiResponse<CustomerResponse>>(`/customers/${id}`);
    return data.data;
  },

  // GET /api/v1/customers/by-user/{userId}
  getByUserId: async (userId: string): Promise<CustomerResponse> => {
    const { data } = await apiClient.get<ApiResponse<CustomerResponse>>(`/customers/by-user/${userId}`);
    return data.data;
  },

  // POST /api/v1/customers
  createCustomer: async (req: CreateCustomerRequest): Promise<CustomerResponse> => {
    const { data } = await apiClient.post<ApiResponse<CustomerResponse>>('/customers', req);
    return data.data;
  },

  // PUT /api/v1/customers/{id}
  updateCustomer: async (id: string, req: UpdateCustomerRequest): Promise<CustomerResponse> => {
    const { data } = await apiClient.put<ApiResponse<CustomerResponse>>(`/customers/${id}`, req);
    return data.data;
  },

  // DELETE /api/v1/customers/{id}
  deleteCustomer: async (id: string): Promise<void> => {
    await apiClient.delete(`/customers/${id}`);
  },
};
