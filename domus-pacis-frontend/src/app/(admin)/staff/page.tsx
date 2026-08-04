'use client';
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  staffApi,
  EmployeeResponse,
  CreateEmployeeRequest,
  CreateRoleRequest,
  CreateScheduleRequest,
} from '@/lib/api/staffApi';
import { formatCurrency, formatDate } from '@/lib/utils';
import {
  PageHeader, Pagination, PageLoader,
  SearchBar, EmptyState, Modal, StatusBadge,
} from '@/components/ui';
import {
  UserCog, Plus, Trash2, CalendarDays,
  DollarSign, Shield, ChevronDown,
} from 'lucide-react';
import toast from 'react-hot-toast';

// ── Constants ──────────────────────────────────────────────────────────────────
const TABS         = ['Employees', 'Schedules', 'Payroll', 'Roles'] as const;
type Tab           = typeof TABS[number];
const CONTRACT_TYPES = ['FULL_TIME', 'PART_TIME', 'CONTRACT'];
const DEPARTMENTS    = ['Front Desk', 'Housekeeping', 'Catering', 'Maintenance', 'Security', 'Administration', 'Finance'];
const DAYS_OF_WEEK   = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

// ── Schemas ────────────────────────────────────────────────────────────────────
const employeeSchema = z.object({
  fullName:     z.string().min(2, 'Full name required'),
  nationalId:   z.string().min(3, 'National ID required'),
  phone:        z.string().min(10, 'Valid phone required'),
  roleId:       z.string().uuid('Please select a role'),
  department:   z.string().min(1, 'Department required'),
  contractType: z.string().min(1, 'Contract type required'),
  hireDate:     z.string().min(1, 'Hire date required'),
  baseSalary:   z.number().positive('Salary must be positive'),
  bankAccount:  z.string().optional(),
});

const roleSchema = z.object({
  title:       z.string().min(2, 'Title required'),
  description: z.string().min(2, 'Description required'),
  permissions: z.string(), // comma-separated, split before sending
});

const salarySchema = z.object({
  newSalary: z.number().positive('Must be positive'),
});

const payrollPeriodSchema = z.object({
  period: z.string().regex(/^\d{4}-\d{2}$/, 'Format: YYYY-MM'),
});

type EmployeeForm      = z.infer<typeof employeeSchema>;
type RoleForm          = z.infer<typeof roleSchema>;
type SalaryForm        = z.infer<typeof salarySchema>;
type PayrollPeriodForm = z.infer<typeof payrollPeriodSchema>;

function getInitialsFromFullName(fullName: string) {
  const parts = fullName.trim().split(' ');
  return parts.length >= 2
    ? (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
    : fullName.slice(0, 2).toUpperCase();
}

export default function StaffPage() {
  const [tab,          setTab]          = useState<Tab>('Employees');
  const [page,         setPage]         = useState(0);
  const [search,       setSearch]       = useState('');
  const [empModal,     setEmpModal]     = useState(false);
  const [roleModal,    setRoleModal]    = useState(false);
  const [salaryTarget, setSalaryTarget] = useState<EmployeeResponse | null>(null);
  const [scheduleEmp,  setScheduleEmp]  = useState<EmployeeResponse | null>(null);
  const [payrollPeriod,setPayrollPeriod]= useState(new Date().toISOString().slice(0, 7));
  const [payPeriodModal, setPayPeriodModal] = useState(false);

  const qc = useQueryClient();

  // ── Queries ────────────────────────────────────────────────────────────────
  const { data: employees, isLoading: empLoading } = useQuery({
    queryKey: ['staff', page, search],
    queryFn: () =>
      search
        ? staffApi.searchEmployees(search, { page, size: 15 })
        : staffApi.listEmployees({ page, size: 15 }),
    enabled: tab === 'Employees',
  });

  const { data: roles } = useQuery({
    queryKey: ['staff-roles'],
    queryFn:  staffApi.getRoles,
  });

  const { data: payrollRecords, isLoading: payLoading } = useQuery({
    queryKey: ['payroll', payrollPeriod],
    queryFn:  () => staffApi.getPayrollByPeriod(payrollPeriod),
    enabled:  tab === 'Payroll',
  });

  const { data: weekSchedules, isLoading: schedLoading } = useQuery({
    queryKey: ['schedules-week', payrollPeriod],
    queryFn:  () => staffApi.getSchedulesByWeek(payrollPeriod + '-01'),
    enabled:  tab === 'Schedules',
  });

  // ── Forms ──────────────────────────────────────────────────────────────────
  const empForm    = useForm<EmployeeForm>   ({ resolver: zodResolver(employeeSchema),      defaultValues: { contractType: 'FULL_TIME', baseSalary: 0 } });
  const roleForm   = useForm<RoleForm>       ({ resolver: zodResolver(roleSchema) });
  const salaryForm = useForm<SalaryForm>     ({ resolver: zodResolver(salarySchema) });
  const payForm    = useForm<PayrollPeriodForm>({ resolver: zodResolver(payrollPeriodSchema), defaultValues: { period: payrollPeriod } });

  // ── Mutations ──────────────────────────────────────────────────────────────
  const createEmployee = useMutation({
    mutationFn: (req: CreateEmployeeRequest) => staffApi.createEmployee(req),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['staff'] });
      toast.success('Employee added');
      setEmpModal(false);
      empForm.reset();
    },
    onError: () => toast.error('Failed to add employee'),
  });

  const terminate = useMutation({
    mutationFn: (id: string) => staffApi.terminateEmployee(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['staff'] }); toast.success('Employee terminated'); },
    onError:   () => toast.error('Failed to terminate employee'),
  });

  const updateSalary = useMutation({
    mutationFn: ({ id, salary }: { id: string; salary: number }) =>
      staffApi.updateSalary(id, salary),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['staff'] });
      toast.success('Salary updated');
      setSalaryTarget(null);
      salaryForm.reset();
    },
    onError: () => toast.error('Failed to update salary'),
  });

  const createRole = useMutation({
    mutationFn: (req: CreateRoleRequest) => staffApi.createRole(req),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['staff-roles'] });
      toast.success('Role created');
      setRoleModal(false);
      roleForm.reset();
    },
    onError: () => toast.error('Failed to create role'),
  });

  const computeAll = useMutation({
    mutationFn: (period: string) => staffApi.computeAllPayroll(period),
    onSuccess: (records) => {
      qc.invalidateQueries({ queryKey: ['payroll'] });
      toast.success(`Payroll computed for ${records.length} employees`);
      setPayPeriodModal(false);
    },
    onError: () => toast.error('Failed to compute payroll'),
  });

  const approveAll = useMutation({
    mutationFn: (period: string) => staffApi.approveAllPayroll(period),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['payroll'] }); toast.success('All payroll records approved'); },
    onError:   () => toast.error('Failed to approve payroll'),
  });

  const markPaid = useMutation({
    mutationFn: (id: string) => staffApi.markPayrollPaid(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['payroll'] }); toast.success('Marked as paid'); },
    onError:   () => toast.error('Failed to mark as paid'),
  });

  const deleteSchedule = useMutation({
    mutationFn: (id: string) => staffApi.deleteSchedule(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['schedules-week'] }); toast.success('Schedule deleted'); },
    onError:   () => toast.error('Failed to delete schedule'),
  });

  return (
    <div className="max-w-7xl mx-auto">
      <PageHeader
        title="Staff Management"
        subtitle="Employees, roles, schedules and payroll"
        actions={
          <div className="flex gap-2">
            {tab === 'Employees' && (
              <button onClick={() => setEmpModal(true)} className="btn-primary">
                <Plus size={15} /> Add Employee
              </button>
            )}
            {tab === 'Roles' && (
              <button onClick={() => setRoleModal(true)} className="btn-primary">
                <Plus size={15} /> Create Role
              </button>
            )}
            {tab === 'Payroll' && (
              <button onClick={() => setPayPeriodModal(true)} className="btn-primary">
                <DollarSign size={15} /> Compute Payroll
              </button>
            )}
          </div>
        }
      />

      {/* ── Tabs ────────────────────────────────────────────────────────────── */}
      <div className="flex gap-1 mb-6 bg-stone-100 p-1 rounded-xl w-fit">
        {TABS.map(t => (
          <button
            key={t}
            onClick={() => { setTab(t); setPage(0); }}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${
              tab === t ? 'bg-white text-stone-900 shadow-sm' : 'text-stone-500 hover:text-stone-700'
            }`}
          >
            {t}
          </button>
        ))}
      </div>

      {/* ══════════════════════════════════════════════════════════════════════
          EMPLOYEES TAB
      ══════════════════════════════════════════════════════════════════════ */}
      {tab === 'Employees' && (
        <>
          <div className="card mb-4 flex flex-wrap gap-3 items-center">
            <SearchBar
              value={search}
              onChange={v => { setSearch(v); setPage(0); }}
              placeholder="Search by name…"
            />
          </div>

          {empLoading ? <PageLoader /> : (
            <div className="table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Employee</th>
                    <th>Department</th>
                    <th>Role</th>
                    <th>Contract</th>
                    <th>Base Salary</th>
                    <th>Hire Date</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {employees?.content.length === 0 ? (
                    <tr><td colSpan={8}>
                      <EmptyState icon={<UserCog size={40} />} title="No employees found" />
                    </td></tr>
                  ) : employees?.content.map(e => (
                    <tr key={e.id}>
                      <td>
                        <div className="flex items-center gap-3">
                          <div className="w-8 h-8 rounded-full bg-burgundy-100 text-burgundy-700 flex items-center justify-center text-xs font-bold flex-shrink-0">
                            {getInitialsFromFullName(e.fullName)}
                          </div>
                          <div>
                            <div className="font-medium text-stone-900">{e.fullName}</div>
                            <div className="text-xs text-stone-400">{e.phone}</div>
                          </div>
                        </div>
                      </td>
                      <td className="text-stone-600">{e.department}</td>
                      <td className="text-stone-600">{e.role ?? '—'}</td>
                      <td>
                        <span className="px-2 py-0.5 bg-stone-100 text-stone-600 rounded-full text-xs">
                          {e.contractType.replace('_', ' ')}
                        </span>
                      </td>
                      <td className="font-medium">{formatCurrency(e.baseSalary)}</td>
                      <td className="text-stone-500">{formatDate(e.hireDate)}</td>
                      <td><StatusBadge status={e.isActive ? 'ACTIVE' : 'INACTIVE'} /></td>
                      <td>
                        <div className="flex items-center gap-1">
                          {/* Update salary */}
                          <button
                            onClick={() => { setSalaryTarget(e); salaryForm.setValue('newSalary', e.baseSalary); }}
                            className="p-1.5 rounded-lg hover:bg-gold-50 text-stone-400 hover:text-gold-600 transition-colors"
                            title="Update salary"
                          >
                            <DollarSign size={14} />
                          </button>
                          {/* View schedules */}
                          <button
                            onClick={() => { setScheduleEmp(e); setTab('Schedules'); }}
                            className="p-1.5 rounded-lg hover:bg-blue-50 text-stone-400 hover:text-blue-600 transition-colors"
                            title="View schedules"
                          >
                            <CalendarDays size={14} />
                          </button>
                          {/* Terminate */}
                          {e.isActive && (
                            <button
                              onClick={() => { if (confirm(`Terminate ${e.fullName}?`)) terminate.mutate(e.id); }}
                              className="p-1.5 rounded-lg hover:bg-red-50 text-stone-400 hover:text-red-500 transition-colors"
                              title="Terminate"
                            >
                              <Trash2 size={14} />
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {employees && (
                <div className="p-4 border-t border-stone-100">
                  <Pagination page={page} totalPages={employees.totalPages} onPageChange={setPage} />
                </div>
              )}
            </div>
          )}
        </>
      )}

      {/* ══════════════════════════════════════════════════════════════════════
          SCHEDULES TAB
      ══════════════════════════════════════════════════════════════════════ */}
      {tab === 'Schedules' && (
        <>
          <div className="card mb-4 flex items-center gap-3">
            <label className="label mb-0">Week starting:</label>
            <input
              type="date"
              value={payrollPeriod + '-01'}
              onChange={e => setPayrollPeriod(e.target.value.slice(0, 7))}
              className="input w-44"
            />
          </div>

          {schedLoading ? <PageLoader /> : (
            <div className="table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Employee</th>
                    <th>Week Start</th>
                    <th>Shifts</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {(weekSchedules ?? []).length === 0 ? (
                    <tr><td colSpan={4}>
                      <EmptyState icon={<CalendarDays size={40} />} title="No schedules for this week" />
                    </td></tr>
                  ) : weekSchedules?.map(s => (
                    <tr key={s.id}>
                      <td className="font-medium text-stone-900">{s.employeeName}</td>
                      <td>{formatDate(s.weekStartDate)}</td>
                      <td>
                        <div className="flex flex-wrap gap-1">
                          {s.shifts.map(sh => (
                            <span key={sh.id} className="px-2 py-0.5 bg-blue-50 text-blue-700 rounded text-xs">
                              {sh.dayOfWeek.slice(0, 3)} {sh.startTime}–{sh.endTime}
                            </span>
                          ))}
                        </div>
                      </td>
                      <td>
                        <button
                          onClick={() => { if (confirm('Delete this schedule?')) deleteSchedule.mutate(s.id); }}
                          className="p-1.5 rounded-lg hover:bg-red-50 text-stone-400 hover:text-red-500 transition-colors"
                        >
                          <Trash2 size={14} />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}

      {/* ══════════════════════════════════════════════════════════════════════
          PAYROLL TAB
      ══════════════════════════════════════════════════════════════════════ */}
      {tab === 'Payroll' && (
        <>
          <div className="card mb-4 flex items-center gap-3">
            <label className="label mb-0">Period:</label>
            <input
              type="month"
              value={payrollPeriod}
              onChange={e => setPayrollPeriod(e.target.value)}
              className="input w-40"
            />
            {(payrollRecords ?? []).some(r => r.status === 'DRAFT') && (
              <button
                onClick={() => approveAll.mutate(payrollPeriod)}
                disabled={approveAll.isPending}
                className="btn-secondary text-sm"
              >
                Approve All
              </button>
            )}
          </div>

          {payLoading ? <PageLoader /> : (
            <div className="table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Employee</th>
                    <th>Period</th>
                    <th>Gross</th>
                    <th>Deductions</th>
                    <th>Tax Withheld</th>
                    <th>Net Pay</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {(payrollRecords ?? []).length === 0 ? (
                    <tr><td colSpan={8}>
                      <EmptyState
                        icon={<DollarSign size={40} />}
                        title="No payroll records"
                        description={`No records for ${payrollPeriod}. Use 'Compute Payroll' to generate.`}
                      />
                    </td></tr>
                  ) : payrollRecords?.map(r => (
                    <tr key={r.id}>
                      <td className="font-medium text-stone-900">{r.employeeName}</td>
                      <td className="text-stone-500">{r.period}</td>
                      <td>{formatCurrency(r.grossSalary)}</td>
                      <td className="text-red-500">{formatCurrency(r.deductions)}</td>
                      <td className="text-orange-500">{formatCurrency(r.taxWithheld)}</td>
                      <td className="font-semibold text-green-700">{formatCurrency(r.netSalary)}</td>
                      <td><StatusBadge status={r.status} /></td>
                      <td>
                        <div className="flex items-center gap-1">
                          {r.status === 'APPROVED' && (
                            <button
                              onClick={() => markPaid.mutate(r.id)}
                              className="text-xs px-2.5 py-1.5 rounded-lg bg-green-50 text-green-700 border border-green-100 hover:bg-green-100 transition-colors"
                            >
                              Mark Paid
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}

      {/* ══════════════════════════════════════════════════════════════════════
          ROLES TAB
      ══════════════════════════════════════════════════════════════════════ */}
      {tab === 'Roles' && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {(roles ?? []).length === 0 ? (
            <div className="col-span-full">
              <EmptyState icon={<Shield size={40} />} title="No roles defined yet" />
            </div>
          ) : roles?.map(r => (
            <div key={r.id} className="card">
              <div className="flex items-center gap-3 mb-3">
                <div className="w-9 h-9 rounded-xl bg-gold-50 text-gold-600 flex items-center justify-center">
                  <Shield size={16} />
                </div>
                <div>
                  <div className="font-medium text-stone-900">{r.title}</div>
                  <div className="text-stone-400 text-xs">{r.description}</div>
                </div>
              </div>
              <div className="flex flex-wrap gap-1.5">
                {r.permissions.map(p => (
                  <span key={p} className="px-2 py-0.5 bg-stone-100 text-stone-600 rounded text-xs">
                    {p}
                  </span>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* ══════════════════════════════════════════════════════════════════════
          ADD EMPLOYEE MODAL
      ══════════════════════════════════════════════════════════════════════ */}
      <Modal open={empModal} onClose={() => { setEmpModal(false); empForm.reset(); }} title="Add Employee">
        <form onSubmit={empForm.handleSubmit(d => createEmployee.mutate(d))} className="space-y-4">
          <div>
            <label className="label">Full Name</label>
            <input {...empForm.register('fullName')} className="input" placeholder="Jean Habimana" />
            {empForm.formState.errors.fullName && <p className="form-error">{empForm.formState.errors.fullName.message}</p>}
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="label">National ID</label>
              <input {...empForm.register('nationalId')} className="input" />
              {empForm.formState.errors.nationalId && <p className="form-error">{empForm.formState.errors.nationalId.message}</p>}
            </div>
            <div>
              <label className="label">Phone</label>
              <input {...empForm.register('phone')} className="input" placeholder="+250 78 000 0000" />
              {empForm.formState.errors.phone && <p className="form-error">{empForm.formState.errors.phone.message}</p>}
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="label">Department</label>
              <select {...empForm.register('department')} className="input">
                <option value="">Select…</option>
                {DEPARTMENTS.map(d => <option key={d}>{d}</option>)}
              </select>
              {empForm.formState.errors.department && <p className="form-error">{empForm.formState.errors.department.message}</p>}
            </div>
            <div>
              <label className="label">Role</label>
              <select {...empForm.register('roleId')} className="input">
                <option value="">Select…</option>
                {roles?.map(r => <option key={r.id} value={r.id}>{r.title}</option>)}
              </select>
              {empForm.formState.errors.roleId && <p className="form-error">{empForm.formState.errors.roleId.message}</p>}
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="label">Contract Type</label>
              <select {...empForm.register('contractType')} className="input">
                {CONTRACT_TYPES.map(c => <option key={c} value={c}>{c.replace('_', ' ')}</option>)}
              </select>
            </div>
            <div>
              <label className="label">Hire Date</label>
              <input type="date" {...empForm.register('hireDate')} className="input" />
              {empForm.formState.errors.hireDate && <p className="form-error">{empForm.formState.errors.hireDate.message}</p>}
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="label">Base Salary (RWF)</label>
              <input type="number" {...empForm.register('baseSalary', { valueAsNumber: true })} className="input" />
              {empForm.formState.errors.baseSalary && <p className="form-error">{empForm.formState.errors.baseSalary.message}</p>}
            </div>
            <div>
              <label className="label">Bank Account <span className="text-stone-400 font-normal">(optional)</span></label>
              <input {...empForm.register('bankAccount')} className="input" />
            </div>
          </div>
          <div className="flex gap-3 pt-2">
            <button type="button" onClick={() => { setEmpModal(false); empForm.reset(); }} className="btn-secondary flex-1 justify-center">Cancel</button>
            <button type="submit" disabled={createEmployee.isPending} className="btn-primary flex-1 justify-center">
              {createEmployee.isPending ? 'Saving…' : 'Save Employee'}
            </button>
          </div>
        </form>
      </Modal>

      {/* ══════════════════════════════════════════════════════════════════════
          UPDATE SALARY MODAL
      ══════════════════════════════════════════════════════════════════════ */}
      <Modal
        open={!!salaryTarget}
        onClose={() => { setSalaryTarget(null); salaryForm.reset(); }}
        title={`Update Salary — ${salaryTarget?.fullName}`}
      >
        <form onSubmit={salaryForm.handleSubmit(d =>
          salaryTarget && updateSalary.mutate({ id: salaryTarget.id, salary: d.newSalary })
        )} className="space-y-4">
          <div>
            <label className="label">Current Salary</label>
            <div className="text-stone-500 text-sm">{salaryTarget && formatCurrency(salaryTarget.baseSalary)}</div>
          </div>
          <div>
            <label className="label">New Salary (RWF)</label>
            <input type="number" {...salaryForm.register('newSalary', { valueAsNumber: true })} className="input" />
            {salaryForm.formState.errors.newSalary && <p className="form-error">{salaryForm.formState.errors.newSalary.message}</p>}
          </div>
          <div className="flex gap-3 pt-2">
            <button type="button" onClick={() => setSalaryTarget(null)} className="btn-secondary flex-1 justify-center">Cancel</button>
            <button type="submit" disabled={updateSalary.isPending} className="btn-primary flex-1 justify-center">
              {updateSalary.isPending ? 'Saving…' : 'Update Salary'}
            </button>
          </div>
        </form>
      </Modal>

      {/* ══════════════════════════════════════════════════════════════════════
          CREATE ROLE MODAL
      ══════════════════════════════════════════════════════════════════════ */}
      <Modal open={roleModal} onClose={() => { setRoleModal(false); roleForm.reset(); }} title="Create Role">
        <form onSubmit={roleForm.handleSubmit(d => createRole.mutate({
          title:       d.title,
          description: d.description,
          permissions: d.permissions.split(',').map(p => p.trim()).filter(Boolean),
        }))} className="space-y-4">
          <div>
            <label className="label">Title</label>
            <input {...roleForm.register('title')} className="input" placeholder="e.g. Front Desk Officer" />
            {roleForm.formState.errors.title && <p className="form-error">{roleForm.formState.errors.title.message}</p>}
          </div>
          <div>
            <label className="label">Description</label>
            <input {...roleForm.register('description')} className="input" />
            {roleForm.formState.errors.description && <p className="form-error">{roleForm.formState.errors.description.message}</p>}
          </div>
          <div>
            <label className="label">Permissions <span className="text-stone-400 font-normal">(comma-separated)</span></label>
            <input {...roleForm.register('permissions')} className="input" placeholder="VIEW_BOOKINGS, MANAGE_BOOKINGS" />
          </div>
          <div className="flex gap-3 pt-2">
            <button type="button" onClick={() => { setRoleModal(false); roleForm.reset(); }} className="btn-secondary flex-1 justify-center">Cancel</button>
            <button type="submit" disabled={createRole.isPending} className="btn-primary flex-1 justify-center">
              {createRole.isPending ? 'Saving…' : 'Create Role'}
            </button>
          </div>
        </form>
      </Modal>

      {/* ══════════════════════════════════════════════════════════════════════
          COMPUTE PAYROLL MODAL
      ══════════════════════════════════════════════════════════════════════ */}
      <Modal open={payPeriodModal} onClose={() => setPayPeriodModal(false)} title="Compute Payroll">
        <form onSubmit={payForm.handleSubmit(d => computeAll.mutate(d.period))} className="space-y-4">
          <p className="text-stone-500 text-sm">
            This will compute payroll for all active employees for the selected period.
            Existing records for this period will be overwritten.
          </p>
          <div>
            <label className="label">Period (YYYY-MM)</label>
            <input type="month" {...payForm.register('period')} className="input w-44" />
            {payForm.formState.errors.period && <p className="form-error">{payForm.formState.errors.period.message}</p>}
          </div>
          <div className="flex gap-3 pt-2">
            <button type="button" onClick={() => setPayPeriodModal(false)} className="btn-secondary flex-1 justify-center">Cancel</button>
            <button type="submit" disabled={computeAll.isPending} className="btn-primary flex-1 justify-center">
              {computeAll.isPending ? 'Computing…' : 'Compute Payroll'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}