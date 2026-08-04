import apiClient from './client';
import { ApiResponse } from '../types';

// ── Types matching TaxController DTOs exactly ─────────────────────────────────

export interface TaxRecordDto {
  id:             string;
  period:         string;   // "YYYY-MM"
  taxType:        string;   // "VAT" | "WITHHOLDING" etc.
  taxableAmount:  number;
  taxRate:        number;
  taxDue:         number;
  status:         string;   // "DRAFT" | "SUBMITTED" | "PAID"
  filedAt:        string | null;
}

export interface TaxRuleDto {
  id:            string;
  taxType:       string;
  rate:          number;
  description:   string;
  effectiveFrom: string;
  effectiveTo:   string | null;
  isActive:      boolean;
}

export interface TaxCalcResult {
  amount:        number;
  taxType:       string;
  taxAmount:     number;
  totalWithTax:  number;
}

export interface CreateTaxRuleRequest {
  taxType:       string;
  rate:          number;
  description?:  string;
  effectiveFrom: string;   // ISO date e.g. '2025-01-01'
  effectiveTo?:  string;
}

export const taxApi = {

  // ── Tax Records ────────────────────────────────────────────────────────────

  // POST /api/v1/tax/records/compute-vat?period=YYYY-MM
  computeVat: async (period: string): Promise<TaxRecordDto> => {
    const { data } = await apiClient.post<ApiResponse<TaxRecordDto>>(
      '/tax/records/compute-vat', null, { params: { period } }
    );
    return data.data;
  },

  // GET /api/v1/tax/records?year=2025
  listByYear: async (year: number): Promise<TaxRecordDto[]> => {
    const { data } = await apiClient.get<ApiResponse<TaxRecordDto[]>>(
      '/tax/records', { params: { year } }
    );
    return data.data;
  },

  // GET /api/v1/tax/records/{id}
  getById: async (id: string): Promise<TaxRecordDto> => {
    const { data } = await apiClient.get<ApiResponse<TaxRecordDto>>(`/tax/records/${id}`);
    return data.data;
  },

  // PATCH /api/v1/tax/records/{id}/submit
  submit: async (id: string): Promise<TaxRecordDto> => {
    const { data } = await apiClient.patch<ApiResponse<TaxRecordDto>>(`/tax/records/${id}/submit`);
    return data.data;
  },

  // PATCH /api/v1/tax/records/{id}/amend?newTaxableAmount=
  amend: async (id: string, newTaxableAmount: number): Promise<TaxRecordDto> => {
    const { data } = await apiClient.patch<ApiResponse<TaxRecordDto>>(
      `/tax/records/${id}/amend`, null, { params: { newTaxableAmount } }
    );
    return data.data;
  },

  // PATCH /api/v1/tax/records/{id}/pay
  markPaid: async (id: string): Promise<TaxRecordDto> => {
    const { data } = await apiClient.patch<ApiResponse<TaxRecordDto>>(`/tax/records/${id}/pay`);
    return data.data;
  },

  // ── Tax Rules ──────────────────────────────────────────────────────────────

  // GET /api/v1/tax/rules
  listRules: async (): Promise<TaxRuleDto[]> => {
    const { data } = await apiClient.get<ApiResponse<TaxRuleDto[]>>('/tax/rules');
    return data.data;
  },

  // POST /api/v1/tax/rules
  createRule: async (req: CreateTaxRuleRequest): Promise<TaxRuleDto> => {
    const { data } = await apiClient.post<ApiResponse<TaxRuleDto>>('/tax/rules', req);
    return data.data;
  },

  // ── Utility ────────────────────────────────────────────────────────────────

  // GET /api/v1/tax/calculate?amount=&taxType=
  calculate: async (amount: number, taxType: string): Promise<TaxCalcResult> => {
    const { data } = await apiClient.get<ApiResponse<TaxCalcResult>>(
      '/tax/calculate', { params: { amount, taxType } }
    );
    return data.data;
  },
};