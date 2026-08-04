import apiClient from './client';
import { ApiResponse } from '../types';

// ── Raw backend response types (mirrors StatisticsService Java records) ───────

interface BackendOverviewKpi {
  todayCheckIns:      number;
  pendingBookings:    number;
  confirmedBookings:  number;
  totalCustomers:     number;
  activeStaff:        number;
  lowStockAlerts:     number;
  monthlyRevenue:     number;
  monthlyExpenses:    number;
  monthlyNetIncome:   number;
}

interface BackendMonthlyRevenueStat {
  period:    string;   // e.g. "2025-01"
  revenue:   number;
  expenses:  number;
  netIncome: number;
}

interface BackendRevenueBySource {
  sourceType: string;   // "BOOKING" | "FOOD_SERVICE" | "OTHER"
  amount:     number;
}

interface BackendServicePopularityStat {
  serviceName:  string;
  bookingCount: number;
}

interface BackendCustomerActivitySummary {
  totalCustomers: number;
  newCustomers:   number;
  totalBookings:  number;
}

// ── Frontend-facing types (what the dashboard expects) ────────────────────────

export interface OverviewKpi {
  todayCheckIns:      number;
  todayCheckOuts:     number;
  pendingBookings:    number;
  confirmedBookings:  number;
  monthlyRevenue:     number;
  monthlyExpenses:    number;
  lowStockItems:      number;
  totalCustomers:     number;
  occupancyRate:      number;
  revenueGrowth:      number;
}

export interface OccupancyStats {
  from:           string;
  to:             string;
  overallRate:    number;
  byAssetType:    { assetType: string; rate: number }[];
}

export interface MonthlyRevenueStat {
  month:    string;   // e.g. "2025-01"
  revenue:  number;
  expenses: number;
  profit:   number;
}

export interface RevenueBySource {
  source:  string;   // "BOOKING" | "FOOD_SERVICE" | "OTHER"
  amount:  number;
  percentage: number;
}

export interface ServicePopularityStat {
  assetType:   string;
  assetName:   string;
  bookings:    number;
  revenue:     number;
  percentage:  number;
}

export interface CustomerActivitySummary {
  totalCustomers:    number;
  newCustomers:      number;
  returningCustomers: number;
  loyalCustomers:    number;
}

export const analyticsApi = {

  // GET /api/v1/analytics/overview
  getOverviewKpis: async (): Promise<OverviewKpi> => {
    const { data } = await apiClient.get<ApiResponse<BackendOverviewKpi>>('/analytics/overview');
    const b = data.data;
    return {
      todayCheckIns:      b.todayCheckIns,
      todayCheckOuts:     0,                    // not provided by backend
      pendingBookings:    b.pendingBookings,
      confirmedBookings:  b.confirmedBookings,
      monthlyRevenue:     b.monthlyRevenue,
      monthlyExpenses:    b.monthlyExpenses,
      lowStockItems:      b.lowStockAlerts,
      totalCustomers:     b.totalCustomers,
      occupancyRate:      0,                    // not provided by overview endpoint
      revenueGrowth:      0,                    // not provided by overview endpoint
    };
  },

  // GET /api/v1/analytics/occupancy?from=&to=
  getOccupancy: async (from: string, to: string): Promise<OccupancyStats> => {
    const { data } = await apiClient.get<ApiResponse<OccupancyStats>>('/analytics/occupancy', {
      params: { from, to },
    });
    return data.data;
  },

  // GET /api/v1/analytics/revenue/monthly?year=
  getMonthlyRevenue: async (year?: number): Promise<MonthlyRevenueStat[]> => {
    const { data } = await apiClient.get<ApiResponse<BackendMonthlyRevenueStat[]>>('/analytics/revenue/monthly', {
      params: year ? { year } : {},
    });
    return data.data.map((b) => ({
      month:    b.period,
      revenue:  b.revenue,
      expenses: b.expenses,
      profit:   b.netIncome,
    }));
  },

  // GET /api/v1/analytics/revenue/by-source?from=&to=
  getRevenueBySource: async (from: string, to: string): Promise<RevenueBySource[]> => {
    const { data } = await apiClient.get<ApiResponse<BackendRevenueBySource[]>>('/analytics/revenue/by-source', {
      params: { from, to },
    });
    return data.data.map((b) => ({
      source:     b.sourceType,
      amount:     b.amount,
      percentage: 0,   // not provided by backend; computed on frontend if needed
    }));
  },

  // GET /api/v1/analytics/services/popularity?from=&to=
  getServicePopularity: async (from: string, to: string): Promise<ServicePopularityStat[]> => {
    const { data } = await apiClient.get<ApiResponse<BackendServicePopularityStat[]>>('/analytics/services/popularity', {
      params: { from, to },
    });
    return data.data.map((b) => ({
      assetType:  '',
      assetName:  b.serviceName,
      bookings:   b.bookingCount,
      revenue:    0,       // not provided by backend
      percentage: 0,       // computed below
    }));
  },

  // GET /api/v1/analytics/customers?from=&to=
  getCustomerActivity: async (from: string, to: string): Promise<CustomerActivitySummary> => {
    const { data } = await apiClient.get<ApiResponse<BackendCustomerActivitySummary>>('/analytics/customers', {
      params: { from, to },
    });
    const b = data.data;
    return {
      totalCustomers:     b.totalCustomers,
      newCustomers:       b.newCustomers,
      returningCustomers: 0,   // not provided by backend
      loyalCustomers:     0,   // not provided by backend
    };
  },
};
