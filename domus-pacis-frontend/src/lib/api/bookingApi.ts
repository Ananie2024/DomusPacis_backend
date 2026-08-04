import apiClient from './client';
import { ApiResponse } from '../types';

// ── Types matching BookingDtos exactly ────────────────────────────────────────

export type BookingStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'CHECKED_IN'
  | 'COMPLETED'
  | 'CANCELLED';

export interface BookingResponse {
  id:               string;
  customerId:       string;
  customerName:     string;
  serviceAssetId:   string;
  serviceAssetName: string;
  checkInDate:      string;
  checkOutDate:     string;
  numberOfGuests:   number;
  status:           BookingStatus;
  specialRequests?: string;
  totalAmount:      number;
  createdAt:        string;
}

export interface AvailabilityResponse {
  assetId:   string;
  checkIn:   string;
  checkOut:  string;
  available: boolean;
}

export interface CreateBookingRequest {
  serviceAssetId: string;
  checkInDate:    string;
  checkOutDate:   string;
  numberOfGuests: number;
  specialRequests?: string;
  firstName:      string;
  lastName:       string;
  email:          string;
  phone:          string;
}

export interface CancelBookingRequest {
  reason?: string;
}

export interface OverrideDatesRequest {
  checkIn:  string;
  checkOut: string;
}

export interface BookingPage {
  content:       BookingResponse[];
  totalElements: number;
  totalPages:    number;
  number:        number;
  size:          number;
  first:         boolean;
  last:          boolean;
}

export const bookingApi = {

  // POST /api/v1/bookings
  createBooking: async (req: CreateBookingRequest): Promise<BookingResponse> => {
    const { data } = await apiClient.post<ApiResponse<BookingResponse>>('/bookings', req);
    return data.data;
  },

  // GET /api/v1/bookings?page=&size=&sort=
  listAll: async (params?: { page?: number; size?: number; sort?: string }): Promise<BookingPage> => {
    const { data } = await apiClient.get<ApiResponse<BookingPage>>('/bookings', { params });
    return data.data;
  },

  // GET /api/v1/bookings/{id}
  getById: async (id: string): Promise<BookingResponse> => {
    const { data } = await apiClient.get<ApiResponse<BookingResponse>>(`/bookings/${id}`);
    return data.data;
  },

  // GET /api/v1/bookings/customer/{customerId}
  listByCustomer: async (customerId: string, params?: { page?: number; size?: number }): Promise<BookingPage> => {
    const { data } = await apiClient.get<ApiResponse<BookingPage>>(
      `/bookings/customer/${customerId}`, { params }
    );
    return data.data;
  },

  // GET /api/v1/bookings/status/{status}
  listByStatus: async (status: BookingStatus, params?: { page?: number; size?: number }): Promise<BookingPage> => {
    const { data } = await apiClient.get<ApiResponse<BookingPage>>(
      `/bookings/status/${status}`, { params }
    );
    return data.data;
  },

  // GET /api/v1/bookings/availability?assetId=&checkIn=&checkOut=
  checkAvailability: async (assetId: string, checkIn: string, checkOut: string): Promise<AvailabilityResponse> => {
    const { data } = await apiClient.get<ApiResponse<AvailabilityResponse>>('/bookings/availability', {
      params: { assetId, checkIn, checkOut },
    });
    return data.data;
  },

  // PATCH /api/v1/bookings/{id}/confirm
  confirm: async (id: string): Promise<BookingResponse> => {
    const { data } = await apiClient.patch<ApiResponse<BookingResponse>>(`/bookings/${id}/confirm`);
    return data.data;
  },

  // PATCH /api/v1/bookings/{id}/check-in
  checkIn: async (id: string): Promise<BookingResponse> => {
    const { data } = await apiClient.patch<ApiResponse<BookingResponse>>(`/bookings/${id}/check-in`);
    return data.data;
  },

  // PATCH /api/v1/bookings/{id}/complete
  complete: async (id: string): Promise<BookingResponse> => {
    const { data } = await apiClient.patch<ApiResponse<BookingResponse>>(`/bookings/${id}/complete`);
    return data.data;
  },

  // PATCH /api/v1/bookings/{id}/cancel
  cancel: async (id: string, reason?: string): Promise<BookingResponse> => {
    const { data } = await apiClient.patch<ApiResponse<BookingResponse>>(
      `/bookings/${id}/cancel`,
      reason ? { reason } : {}
    );
    return data.data;
  },

  // PATCH /api/v1/bookings/{id}/override-dates
  overrideDates: async (id: string, checkIn: string, checkOut: string): Promise<BookingResponse> => {
    const { data } = await apiClient.patch<ApiResponse<BookingResponse>>(
      `/bookings/${id}/override-dates`, { checkIn, checkOut }
    );
    return data.data;
  },

  // DELETE /api/v1/bookings/{id}
  delete: async (id: string): Promise<void> => {
    await apiClient.delete(`/bookings/${id}`);
  },
};
