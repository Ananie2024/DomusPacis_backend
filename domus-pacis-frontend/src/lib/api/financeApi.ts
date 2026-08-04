import apiClient from './client';
import { ApiResponse } from '../types';

// ── Types matching FinanceDtos exactly ────────────────────────────────────────

export interface PaymentResponse {
  id:                  string;
  bookingId:           string;
  amount:              number;
  currency:            string;
  method:              string;
  status:              string;
  transactionReference?: string;
  paidAt?:             string;
  createdAt:           string;
}

export interface InvoiceResponse {
  id:            string;
  bookingId:     string;
  invoiceNumber: string;
  issuedAt:      string;
  dueDate:       string;
  subtotal:      number;
  taxAmount:     number;
  totalAmount:   number;
  status:        string;
}

export interface ExpenseResponse {
  id:               string;
  category:         string;
  description:      string;
  amount:           number;
  expenseDate:      string;
  approvedBy?:      string;
  receiptReference?: string;
  createdAt:        string;
}

export interface FinancialReportResponse {
  id:            string;
  reportType:    string;   // 'MONTHLY' | 'QUARTERLY' | 'ANNUAL'
  period:        string;
  totalRevenue:  number;
  totalExpenses: number;
  netIncome:     number;
  generatedAt:   string;
  generatedBy?:  string;
}

export interface RecordPaymentRequest {
  bookingId:            string;
  method:               string;   // e.g. 'CASH' | 'BANK_TRANSFER' | 'MOBILE_MONEY' | 'CARD'
  amount:               number;
  transactionReference?: string;
}

export interface GenerateInvoiceRequest {
  bookingId: string;
  taxRate:   number;
}

export interface LogExpenseRequest {
  category:         string;
  description:      string;
  amount:           number;
  expenseDate:      string;   // ISO date e.g. '2025-03-19'
  approvedBy?:      string;
  receiptReference?: string;
}

export interface FinancePage<T> {
  content:       T[];
  totalElements: number;
  totalPages:    number;
  number:        number;
  size:          number;
  first:         boolean;
  last:          boolean;
}

export const financeApi = {

  // ── Payments ───────────────────────────────────────────────────────────────

  // POST /api/v1/finance/payments
  recordPayment: async (req: RecordPaymentRequest): Promise<PaymentResponse> => {
    const { data } = await apiClient.post<ApiResponse<PaymentResponse>>('/finance/payments', req);
    return data.data;
  },

  // GET /api/v1/finance/payments/booking/{bookingId}
  getPaymentByBooking: async (bookingId: string): Promise<PaymentResponse> => {
    const { data } = await apiClient.get<ApiResponse<PaymentResponse>>(
      `/finance/payments/booking/${bookingId}`
    );
    return data.data;
  },

  // POST /api/v1/finance/payments/{paymentId}/refund
  refundPayment: async (paymentId: string): Promise<PaymentResponse> => {
    const { data } = await apiClient.post<ApiResponse<PaymentResponse>>(
      `/finance/payments/${paymentId}/refund`
    );
    return data.data;
  },

  // ── Invoices ───────────────────────────────────────────────────────────────

  // POST /api/v1/finance/invoices
  generateInvoice: async (req: GenerateInvoiceRequest): Promise<InvoiceResponse> => {
    const { data } = await apiClient.post<ApiResponse<InvoiceResponse>>('/finance/invoices', req);
    return data.data;
  },

  // GET /api/v1/finance/invoices?page=&size=
  listInvoices: async (params?: { page?: number; size?: number }): Promise<FinancePage<InvoiceResponse>> => {
    const { data } = await apiClient.get<ApiResponse<FinancePage<InvoiceResponse>>>(
      '/finance/invoices', { params }
    );
    return data.data;
  },

  // GET /api/v1/finance/invoices/{id}
  getInvoiceById: async (id: string): Promise<InvoiceResponse> => {
    const { data } = await apiClient.get<ApiResponse<InvoiceResponse>>(`/finance/invoices/${id}`);
    return data.data;
  },

  // GET /api/v1/finance/invoices/booking/{bookingId}
  getInvoiceByBooking: async (bookingId: string): Promise<InvoiceResponse> => {
    const { data } = await apiClient.get<ApiResponse<InvoiceResponse>>(
      `/finance/invoices/booking/${bookingId}`
    );
    return data.data;
  },

  // DELETE /api/v1/finance/invoices/{id}/void
  voidInvoice: async (id: string): Promise<InvoiceResponse> => {
    const { data } = await apiClient.delete<ApiResponse<InvoiceResponse>>(
      `/finance/invoices/${id}/void`
    );
    return data.data;
  },

  // ── Expenses ───────────────────────────────────────────────────────────────

  // POST /api/v1/finance/expenses
  logExpense: async (req: LogExpenseRequest): Promise<ExpenseResponse> => {
    const { data } = await apiClient.post<ApiResponse<ExpenseResponse>>('/finance/expenses', req);
    return data.data;
  },

  // GET /api/v1/finance/expenses?page=&size=
  listExpenses: async (params?: { page?: number; size?: number }): Promise<FinancePage<ExpenseResponse>> => {
    const { data } = await apiClient.get<ApiResponse<FinancePage<ExpenseResponse>>>(
      '/finance/expenses', { params }
    );
    return data.data;
  },

  // GET /api/v1/finance/expenses/{id}
  getExpenseById: async (id: string): Promise<ExpenseResponse> => {
    const { data } = await apiClient.get<ApiResponse<ExpenseResponse>>(`/finance/expenses/${id}`);
    return data.data;
  },

  // PATCH /api/v1/finance/expenses/{id}/approve
  approveExpense: async (id: string): Promise<ExpenseResponse> => {
    const { data } = await apiClient.patch<ApiResponse<ExpenseResponse>>(
      `/finance/expenses/${id}/approve`
    );
    return data.data;
  },

  // DELETE /api/v1/finance/expenses/{id}
  deleteExpense: async (id: string): Promise<void> => {
    await apiClient.delete(`/finance/expenses/${id}`);
  },

  // ── Reports ────────────────────────────────────────────────────────────────

  // POST /api/v1/finance/reports/monthly?yearMonth=2025-03
  generateMonthlyReport: async (yearMonth: string): Promise<FinancialReportResponse> => {
    const { data } = await apiClient.post<ApiResponse<FinancialReportResponse>>(
      '/finance/reports/monthly', null, { params: { yearMonth } }
    );
    return data.data;
  },

  // POST /api/v1/finance/reports/quarterly?year=2025&quarter=1
  generateQuarterlyReport: async (year: number, quarter: number): Promise<FinancialReportResponse> => {
    const { data } = await apiClient.post<ApiResponse<FinancialReportResponse>>(
      '/finance/reports/quarterly', null, { params: { year, quarter } }
    );
    return data.data;
  },

  // POST /api/v1/finance/reports/annual?year=2025
  generateAnnualReport: async (year: number): Promise<FinancialReportResponse> => {
    const { data } = await apiClient.post<ApiResponse<FinancialReportResponse>>(
      '/finance/reports/annual', null, { params: { year } }
    );
    return data.data;
  },

  // GET /api/v1/finance/reports?page=&size=
  listReports: async (params?: { page?: number; size?: number }): Promise<FinancePage<FinancialReportResponse>> => {
    const { data } = await apiClient.get<ApiResponse<FinancePage<FinancialReportResponse>>>(
      '/finance/reports', { params }
    );
    return data.data;
  },

  // GET /api/v1/finance/reports/{id}
  getReportById: async (id: string): Promise<FinancialReportResponse> => {
    const { data } = await apiClient.get<ApiResponse<FinancialReportResponse>>(`/finance/reports/${id}`);
    return data.data;
  },
};