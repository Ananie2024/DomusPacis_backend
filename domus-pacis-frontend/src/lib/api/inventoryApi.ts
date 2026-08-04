import apiClient from './client';
import { ApiResponse } from '../types';

// ── Types matching controller DTOs exactly ────────────────────────────────────

export interface SupplierDto {
  id:            string;
  name:          string;
  contactPerson: string;
  phone:         string;
  email:         string;
  isActive:      boolean;
}

export interface InventoryItemDto {
  id:           string;
  name:         string;
  category:     string;
  unit:         string;
  currentStock: number;
  reorderLevel: number;
  unitCost:     number;
  supplierName: string | null;
  lowStock:     boolean;
}

export interface MovementDto {
  id:            string;
  itemId:        string;
  itemName:      string;
  movementType:  string;
  quantity:      number;
  movementDate:  string;
  referenceNote: string | null;
}

export interface MenuItemDto {
  id:          string;
  name:        string;
  category:    string;
  description: string;
  unitPrice:   number;
  isAvailable: boolean;
}

export interface FoodOrderDto {
  id:               string;
  customerId:       string;
  bookingId:        string | null;
  status:           string;
  totalAmount:      number;
  deliveryLocation: string;
  orderedAt:        string;
  itemCount:        number;
}

export interface CreateSupplierRequest {
  name:                    string;
  contactPerson?:          string;
  phone?:                  string;
  email?:                  string;
  address?:                string;
  taxIdentificationNumber?: string;
}

export interface CreateItemRequest {
  name:         string;
  category:     string;   // ItemCategory enum value e.g. 'FOOD', 'BEVERAGES'
  unit:         string;
  reorderLevel: number;
  unitCost:     number;
  supplierId?:  string;
}

export interface RecordMovementRequest {
  itemId:        string;
  movementType:  string;   // 'RECEIPT' | 'CONSUMPTION' | 'ADJUSTMENT' | 'WASTE'
  quantity:      number;
  referenceNote?: string;
  recordedById?:  string;
}

export interface CreateMenuItemRequest {
  name:          string;
  category:      string;
  description?:  string;
  unitPrice:     number;
  ingredientIds?: string[];
}

export interface PlaceFoodOrderRequest {
  customerId:       string;
  bookingId?:       string;
  itemQuantities:   Record<string, number>;   // { menuItemId: quantity }
  deliveryLocation?: string;
}

export interface InventoryPage<T> {
  content:       T[];
  totalElements: number;
  totalPages:    number;
  number:        number;
  size:          number;
  first:         boolean;
  last:          boolean;
}

export const inventoryApi = {

  // ── Suppliers ──────────────────────────────────────────────────────────────

  // POST /api/v1/inventory/suppliers
  createSupplier: async (req: CreateSupplierRequest): Promise<SupplierDto> => {
    const { data } = await apiClient.post<ApiResponse<SupplierDto>>('/inventory/suppliers', req);
    return data.data;
  },

  // GET /api/v1/inventory/suppliers?page=&size=
  listSuppliers: async (params?: { page?: number; size?: number }): Promise<InventoryPage<SupplierDto>> => {
    const { data } = await apiClient.get<ApiResponse<InventoryPage<SupplierDto>>>(
      '/inventory/suppliers', { params }
    );
    return data.data;
  },

  // GET /api/v1/inventory/suppliers/{id}
  getSupplierById: async (id: string): Promise<SupplierDto> => {
    const { data } = await apiClient.get<ApiResponse<SupplierDto>>(`/inventory/suppliers/${id}`);
    return data.data;
  },

  // DELETE /api/v1/inventory/suppliers/{id}/deactivate
  deactivateSupplier: async (id: string): Promise<SupplierDto> => {
    const { data } = await apiClient.delete<ApiResponse<SupplierDto>>(
      `/inventory/suppliers/${id}/deactivate`
    );
    return data.data;
  },

  // ── Inventory Items ────────────────────────────────────────────────────────

  // POST /api/v1/inventory/items
  createItem: async (req: CreateItemRequest): Promise<InventoryItemDto> => {
    const { data } = await apiClient.post<ApiResponse<InventoryItemDto>>('/inventory/items', req);
    return data.data;
  },

  // GET /api/v1/inventory/items?page=&size=
  listItems: async (params?: { page?: number; size?: number }): Promise<InventoryPage<InventoryItemDto>> => {
    const { data } = await apiClient.get<ApiResponse<InventoryPage<InventoryItemDto>>>(
      '/inventory/items', { params }
    );
    return data.data;
  },

  // GET /api/v1/inventory/items/{id}
  getItemById: async (id: string): Promise<InventoryItemDto> => {
    const { data } = await apiClient.get<ApiResponse<InventoryItemDto>>(`/inventory/items/${id}`);
    return data.data;
  },

  // GET /api/v1/inventory/items/low-stock
  getLowStockItems: async (): Promise<InventoryItemDto[]> => {
    const { data } = await apiClient.get<ApiResponse<InventoryItemDto[]>>('/inventory/items/low-stock');
    return data.data;
  },

  // ── Stock Movements ────────────────────────────────────────────────────────

  // POST /api/v1/inventory/movements
  recordMovement: async (req: RecordMovementRequest): Promise<MovementDto> => {
    const { data } = await apiClient.post<ApiResponse<MovementDto>>('/inventory/movements', req);
    return data.data;
  },

  // GET /api/v1/inventory/movements/item/{itemId}?page=&size=
  listMovementsByItem: async (itemId: string, params?: { page?: number; size?: number }): Promise<InventoryPage<MovementDto>> => {
    const { data } = await apiClient.get<ApiResponse<InventoryPage<MovementDto>>>(
      `/inventory/movements/item/${itemId}`, { params }
    );
    return data.data;
  },

  // ── Menu Items ─────────────────────────────────────────────────────────────

  // POST /api/v1/inventory/menu-items
  createMenuItem: async (req: CreateMenuItemRequest): Promise<MenuItemDto> => {
    const { data } = await apiClient.post<ApiResponse<MenuItemDto>>('/inventory/menu-items', req);
    return data.data;
  },

  // GET /api/v1/inventory/menu-items?page=&size=
  listMenuItems: async (params?: { page?: number; size?: number }): Promise<InventoryPage<MenuItemDto>> => {
    const { data } = await apiClient.get<ApiResponse<InventoryPage<MenuItemDto>>>(
      '/inventory/menu-items', { params }
    );
    return data.data;
  },

  // GET /api/v1/inventory/menu-items/available
  listAvailableMenuItems: async (): Promise<MenuItemDto[]> => {
    const { data } = await apiClient.get<ApiResponse<MenuItemDto[]>>('/inventory/menu-items/available');
    return data.data;
  },

  // GET /api/v1/inventory/menu-items/{id}
  getMenuItemById: async (id: string): Promise<MenuItemDto> => {
    const { data } = await apiClient.get<ApiResponse<MenuItemDto>>(`/inventory/menu-items/${id}`);
    return data.data;
  },

  // PATCH /api/v1/inventory/menu-items/{id}/toggle
  toggleMenuItemAvailability: async (id: string): Promise<MenuItemDto> => {
    const { data } = await apiClient.patch<ApiResponse<MenuItemDto>>(
      `/inventory/menu-items/${id}/toggle`
    );
    return data.data;
  },

  // ── Food Orders ────────────────────────────────────────────────────────────

  // POST /api/v1/inventory/food-orders
  placeOrder: async (req: PlaceFoodOrderRequest): Promise<FoodOrderDto> => {
    const { data } = await apiClient.post<ApiResponse<FoodOrderDto>>('/inventory/food-orders', req);
    return data.data;
  },

  // GET /api/v1/inventory/food-orders?page=&size=
  listOrders: async (params?: { page?: number; size?: number }): Promise<InventoryPage<FoodOrderDto>> => {
    const { data } = await apiClient.get<ApiResponse<InventoryPage<FoodOrderDto>>>(
      '/inventory/food-orders', { params }
    );
    return data.data;
  },

  // GET /api/v1/inventory/food-orders/{id}
  getOrderById: async (id: string): Promise<FoodOrderDto> => {
    const { data } = await apiClient.get<ApiResponse<FoodOrderDto>>(`/inventory/food-orders/${id}`);
    return data.data;
  },

  // PATCH /api/v1/inventory/food-orders/{id}/status?status=PREPARING
  updateOrderStatus: async (id: string, status: string): Promise<FoodOrderDto> => {
    const { data } = await apiClient.patch<ApiResponse<FoodOrderDto>>(
      `/inventory/food-orders/${id}/status`, null, { params: { status } }
    );
    return data.data;
  },

  // PATCH /api/v1/inventory/food-orders/{id}/cancel
  cancelOrder: async (id: string): Promise<FoodOrderDto> => {
    const { data } = await apiClient.patch<ApiResponse<FoodOrderDto>>(
      `/inventory/food-orders/${id}/cancel`
    );
    return data.data;
  },
};