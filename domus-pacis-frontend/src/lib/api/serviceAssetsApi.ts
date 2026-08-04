import apiClient from './client';
import { ApiResponse, AssetType } from '../types';

export interface ServiceAssetResponse {
  id:          string;
  assetType:   AssetType;
  name:        string;
  description: string;
  capacity:    number;
  pricePerUnit: number;
  pricingUnit: string;
  isAvailable: boolean;
}

export interface CreateServiceAssetRequest {
  assetType:   string;
  name:        string;
  description?: string;
  capacity?:   number;
  pricePerUnit: number;
  pricingUnit: string;
  roomNumber?: string;
  roomType?: string;
  floor?: number;
  hallCode?: string;
  projectorAvailable?: boolean;
  audioSystemAvailable?: boolean;
  seatingLayout?: string;
  isIndoor?: boolean;
  hasStage?: boolean;
  numberOfBeds?: number;
  includesChapel?: boolean;
  includesCatering?: boolean;
}

export interface ServiceAssetPage {
  content:       ServiceAssetResponse[];
  totalElements: number;
  totalPages:    number;
  number:        number;
  size:          number;
  first:         boolean;
  last:          boolean;
}

export const serviceAssetsApi = {
  listAll: async (params?: { page?: number; size?: number }): Promise<ServiceAssetPage> => {
    const { data } = await apiClient.get<ApiResponse<ServiceAssetPage>>('/service-assets', { params });
    return data.data;
  },
  listAvailable: async (): Promise<ServiceAssetResponse[]> => {
    const { data } = await apiClient.get<ApiResponse<ServiceAssetResponse[]>>('/service-assets/available');
    return data.data;
  },
  search: async (q: string, params?: { page?: number; size?: number }): Promise<ServiceAssetPage> => {
    const { data } = await apiClient.get<ApiResponse<ServiceAssetPage>>('/service-assets/search', { params: { q, ...params } });
    return data.data;
  },
  getById: async (id: string): Promise<ServiceAssetResponse> => {
    const { data } = await apiClient.get<ApiResponse<ServiceAssetResponse>>(`/service-assets/${id}`);
    return data.data;
  },
  create: async (req: CreateServiceAssetRequest): Promise<ServiceAssetResponse> => {
    const { data } = await apiClient.post<ApiResponse<ServiceAssetResponse>>('/service-assets', req);
    return data.data;
  },
  setAvailability: async (id: string, available: boolean): Promise<ServiceAssetResponse> => {
    const { data } = await apiClient.patch<ApiResponse<ServiceAssetResponse>>(`/service-assets/${id}/availability`, null, { params: { available } });
    return data.data;
  },
  delete: async (id: string): Promise<void> => {
    await apiClient.delete(`/service-assets/${id}`);
  },
};
