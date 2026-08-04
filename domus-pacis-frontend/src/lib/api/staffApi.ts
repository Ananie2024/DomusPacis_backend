import apiClient from './client';
import { ApiResponse } from '../types';

// ── Types matching StaffDtos exactly ─────────────────────────────────────────

export interface EmployeeResponse {
  id:           string;
  fullName:     string;
  nationalId:   string;
  phone:        string;
  role:         string | null;
  department:   string;
  contractType: string;
  hireDate:     string;
  baseSalary:   number;
  isActive:     boolean;
  createdAt:    string;
}

export interface RoleResponse {
  id:          string;
  title:       string;
  description: string;
  permissions: string[];
}

export interface ShiftResponse {
  id:        string;
  dayOfWeek: string;
  startTime: string;
  endTime:   string;
}

export interface ScheduleResponse {
  id:            string;
  employeeId:    string;
  employeeName:  string;
  weekStartDate: string;
  shifts:        ShiftResponse[];
}

export interface PayrollResponse {
  id:           string;
  employeeId:   string;
  employeeName: string;
  period:       string;   // "YYYY-MM"
  grossSalary:  number;
  deductions:   number;
  netSalary:    number;
  taxWithheld:  number;
  status:       string;   // 'DRAFT' | 'APPROVED' | 'PAID'
  paidAt:       string | null;
}

export interface CreateEmployeeRequest {
  fullName:     string;
  nationalId:   string;
  phone:        string;
  roleId:       string;
  department:   string;
  contractType: string;   // 'FULL_TIME' | 'PART_TIME' | 'CONTRACT'
  hireDate:     string;   // ISO date e.g. '2024-01-15'
  baseSalary:   number;
  bankAccount?: string;
  userId?:      string;
}

export interface CreateRoleRequest {
  title:       string;
  description: string;
  permissions: string[];
}

export interface CreateScheduleRequest {
  employeeId:    string;
  weekStartDate: string;
  shifts: {
    dayOfWeek: string;
    startTime: string;
    endTime:   string;
  }[];
}

export interface EmployeePage {
  content:       EmployeeResponse[];
  totalElements: number;
  totalPages:    number;
  number:        number;
  size:          number;
  first:         boolean;
  last:          boolean;
}

export const staffApi = {

  // ── Employees ──────────────────────────────────────────────────────────────

  // GET /api/v1/staff/employees?page=&size=
  listEmployees: async (params?: { page?: number; size?: number }): Promise<EmployeePage> => {
    const { data } = await apiClient.get<ApiResponse<EmployeePage>>(
      '/staff/employees', { params }
    );
    return data.data;
  },

  // GET /api/v1/staff/employees/search?q=&page=&size=
  searchEmployees: async (q: string, params?: { page?: number; size?: number }): Promise<EmployeePage> => {
    const { data } = await apiClient.get<ApiResponse<EmployeePage>>(
      '/staff/employees/search', { params: { q, ...params } }
    );
    return data.data;
  },

  // GET /api/v1/staff/employees/{id}
  getEmployeeById: async (id: string): Promise<EmployeeResponse> => {
    const { data } = await apiClient.get<ApiResponse<EmployeeResponse>>(`/staff/employees/${id}`);
    return data.data;
  },

  // GET /api/v1/staff/employees/department/{dept}
  getByDepartment: async (dept: string): Promise<EmployeeResponse[]> => {
    const { data } = await apiClient.get<ApiResponse<EmployeeResponse[]>>(
      `/staff/employees/department/${dept}`
    );
    return data.data;
  },

  // POST /api/v1/staff/employees
  createEmployee: async (req: CreateEmployeeRequest): Promise<EmployeeResponse> => {
    const { data } = await apiClient.post<ApiResponse<EmployeeResponse>>('/staff/employees', req);
    return data.data;
  },

  // PATCH /api/v1/staff/employees/{id}/role?roleId=
  assignRole: async (id: string, roleId: string): Promise<EmployeeResponse> => {
    const { data } = await apiClient.patch<ApiResponse<EmployeeResponse>>(
      `/staff/employees/${id}/role`, null, { params: { roleId } }
    );
    return data.data;
  },

  // PATCH /api/v1/staff/employees/{id}/salary
  updateSalary: async (id: string, newSalary: number): Promise<EmployeeResponse> => {
    const { data } = await apiClient.patch<ApiResponse<EmployeeResponse>>(
      `/staff/employees/${id}/salary`, { newSalary }
    );
    return data.data;
  },

  // DELETE /api/v1/staff/employees/{id}/terminate
  terminateEmployee: async (id: string): Promise<void> => {
    await apiClient.delete(`/staff/employees/${id}/terminate`);
  },

  // ── Roles ──────────────────────────────────────────────────────────────────

  // GET /api/v1/staff/roles
  getRoles: async (): Promise<RoleResponse[]> => {
    const { data } = await apiClient.get<ApiResponse<RoleResponse[]>>('/staff/roles');
    return data.data;
  },

  // POST /api/v1/staff/roles
  createRole: async (req: CreateRoleRequest): Promise<RoleResponse> => {
    const { data } = await apiClient.post<ApiResponse<RoleResponse>>('/staff/roles', req);
    return data.data;
  },

  // ── Schedules ──────────────────────────────────────────────────────────────

  // POST /api/v1/staff/schedules
  createSchedule: async (req: CreateScheduleRequest): Promise<ScheduleResponse> => {
    const { data } = await apiClient.post<ApiResponse<ScheduleResponse>>('/staff/schedules', req);
    return data.data;
  },

  // GET /api/v1/staff/schedules/employee/{employeeId}
  getSchedulesByEmployee: async (employeeId: string): Promise<ScheduleResponse[]> => {
    const { data } = await apiClient.get<ApiResponse<ScheduleResponse[]>>(
      `/staff/schedules/employee/${employeeId}`
    );
    return data.data;
  },

  // GET /api/v1/staff/schedules/week?weekStart=YYYY-MM-DD
  getSchedulesByWeek: async (weekStart: string): Promise<ScheduleResponse[]> => {
    const { data } = await apiClient.get<ApiResponse<ScheduleResponse[]>>(
      '/staff/schedules/week', { params: { weekStart } }
    );
    return data.data;
  },

  // DELETE /api/v1/staff/schedules/{id}
  deleteSchedule: async (id: string): Promise<void> => {
    await apiClient.delete(`/staff/schedules/${id}`);
  },

  // ── Payroll ────────────────────────────────────────────────────────────────

  // POST /api/v1/staff/payroll/compute
  computePayroll: async (employeeId: string, period: string): Promise<PayrollResponse> => {
    const { data } = await apiClient.post<ApiResponse<PayrollResponse>>(
      '/staff/payroll/compute', { employeeId, period }
    );
    return data.data;
  },

  // POST /api/v1/staff/payroll/compute-all?period=YYYY-MM
  computeAllPayroll: async (period: string): Promise<PayrollResponse[]> => {
    const { data } = await apiClient.post<ApiResponse<PayrollResponse[]>>(
      '/staff/payroll/compute-all', null, { params: { period } }
    );
    return data.data;
  },

  // GET /api/v1/staff/payroll/period/{period}
  getPayrollByPeriod: async (period: string): Promise<PayrollResponse[]> => {
    const { data } = await apiClient.get<ApiResponse<PayrollResponse[]>>(
      `/staff/payroll/period/${period}`
    );
    return data.data;
  },

  // GET /api/v1/staff/payroll/{id}
  getPayrollById: async (id: string): Promise<PayrollResponse> => {
    const { data } = await apiClient.get<ApiResponse<PayrollResponse>>(`/staff/payroll/${id}`);
    return data.data;
  },

  // PATCH /api/v1/staff/payroll/{id}/approve
  approvePayroll: async (id: string): Promise<PayrollResponse> => {
    const { data } = await apiClient.patch<ApiResponse<PayrollResponse>>(
      `/staff/payroll/${id}/approve`
    );
    return data.data;
  },

  // POST /api/v1/staff/payroll/approve-all?period=YYYY-MM
  approveAllPayroll: async (period: string): Promise<PayrollResponse[]> => {
    const { data } = await apiClient.post<ApiResponse<PayrollResponse[]>>(
      '/staff/payroll/approve-all', null, { params: { period } }
    );
    return data.data;
  },

  // PATCH /api/v1/staff/payroll/{id}/pay
  markPayrollPaid: async (id: string): Promise<PayrollResponse> => {
    const { data } = await apiClient.patch<ApiResponse<PayrollResponse>>(
      `/staff/payroll/${id}/pay`
    );
    return data.data;
  },
};