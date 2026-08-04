'use client';
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  financeApi,
  LogExpenseRequest,
  RecordPaymentRequest,
  GenerateInvoiceRequest,
} from '@/lib/api/financeApi';
import { formatCurrency, formatDate } from '@/lib/utils';
import {
  PageHeader, Pagination, PageLoader, KpiTile,
  StatusBadge, EmptyState, Modal,
} from '@/components/ui';
import {
  DollarSign, Receipt, TrendingDown, FileText,
  Plus, CheckCircle, XCircle, BarChart3,
} from 'lucide-react';
import toast from 'react-hot-toast';

const TABS = ['Invoices', 'Expenses', 'Reports'] as const;
type Tab = typeof TABS[number];

const EXPENSE_CATEGORIES = [
  'UTILITIES', 'MAINTENANCE', 'SUPPLIES', 'SALARIES',
  'FOOD_BEVERAGES', 'MARKETING', 'OTHER',
];

const PAYMENT_METHODS = ['CASH', 'BANK_TRANSFER', 'MOBILE_MONEY', 'CARD'];

// ── Zod schemas ────────────────────────────────────────────────────────────────
const expenseSchema = z.object({
  category:         z.string().min(1, 'Category required'),
  description:      z.string().min(3, 'Description required'),
  amount:           z.number().positive('Amount must be positive'),
  expenseDate:      z.string().min(1, 'Date required'),
  approvedBy:       z.string().optional(),
  receiptReference: z.string().optional(),
});

const paymentSchema = z.object({
  bookingId:            z.string().uuid('Valid booking ID required'),
  method:               z.string().min(1, 'Method required'),
  amount:               z.number().positive('Amount must be positive'),
  transactionReference: z.string().optional(),
});

const invoiceSchema = z.object({
  bookingId: z.string().uuid('Valid booking ID required'),
  taxRate:   z.number().min(0).max(100),
});

type ExpenseForm  = z.infer<typeof expenseSchema>;
type PaymentForm  = z.infer<typeof paymentSchema>;
type InvoiceForm  = z.infer<typeof invoiceSchema>;

export default function FinancePage() {
  const [tab,          setTab]          = useState<Tab>('Invoices');
  const [page,         setPage]         = useState(0);
  const [expModal,     setExpModal]     = useState(false);
  const [payModal,     setPayModal]     = useState(false);
  const [invModal,     setInvModal]     = useState(false);
  const [reportModal,  setReportModal]  = useState(false);
  const [reportPeriod, setReportPeriod] = useState(new Date().toISOString().slice(0, 7)); // 'YYYY-MM'
  const [reportYear,   setReportYear]   = useState(new Date().getFullYear());
  const [reportQ,      setReportQ]      = useState(1);

  const qc = useQueryClient();

  // ── Queries ────────────────────────────────────────────────────────────────
  const { data: invoices, isLoading: invLoading } = useQuery({
    queryKey: ['invoices', page],
    queryFn:  () => financeApi.listInvoices({ page, size: 15 }),
    enabled:  tab === 'Invoices',
  });

  const { data: expenses, isLoading: expLoading } = useQuery({
    queryKey: ['expenses', page],
    queryFn:  () => financeApi.listExpenses({ page, size: 15 }),
    enabled:  tab === 'Expenses',
  });

  const { data: reports, isLoading: repLoading } = useQuery({
    queryKey: ['reports', page],
    queryFn:  () => financeApi.listReports({ page, size: 15 }),
    enabled:  tab === 'Reports',
  });

  // ── Forms ──────────────────────────────────────────────────────────────────
  const expForm = useForm<ExpenseForm>({
    resolver: zodResolver(expenseSchema),
    defaultValues: { amount: 0, expenseDate: new Date().toISOString().slice(0, 10) },
  });

  const payForm = useForm<PaymentForm>({
    resolver: zodResolver(paymentSchema),
    defaultValues: { method: 'CASH', amount: 0 },
  });

  const invForm = useForm<InvoiceForm>({
    resolver: zodResolver(invoiceSchema),
    defaultValues: { taxRate: 18 },
  });

  // ── Mutations ──────────────────────────────────────────────────────────────
  const logExpense = useMutation({
    mutationFn: (req: LogExpenseRequest) => financeApi.logExpense(req),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['expenses'] });
      toast.success('Expense logged');
      setExpModal(false);
      expForm.reset();
    },
    onError: () => toast.error('Failed to log expense'),
  });

  const recordPayment = useMutation({
    mutationFn: (req: RecordPaymentRequest) => financeApi.recordPayment(req),
    onSuccess: () => {
      toast.success('Payment recorded');
      setPayModal(false);
      payForm.reset();
    },
    onError: () => toast.error('Failed to record payment'),
  });

  const generateInvoice = useMutation({
    mutationFn: (req: GenerateInvoiceRequest) => financeApi.generateInvoice(req),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['invoices'] });
      toast.success('Invoice generated');
      setInvModal(false);
      invForm.reset();
    },
    onError: () => toast.error('Failed to generate invoice'),
  });

  const approveExpense = useMutation({
    mutationFn: (id: string) => financeApi.approveExpense(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['expenses'] }); toast.success('Expense approved'); },
    onError:   () => toast.error('Failed to approve expense'),
  });

  const deleteExpense = useMutation({
    mutationFn: (id: string) => financeApi.deleteExpense(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['expenses'] }); toast.success('Expense deleted'); },
    onError:   () => toast.error('Failed to delete expense'),
  });

  const voidInvoice = useMutation({
    mutationFn: (id: string) => financeApi.voidInvoice(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['invoices'] }); toast.success('Invoice voided'); },
    onError:   () => toast.error('Failed to void invoice'),
  });

  const genMonthly = useMutation({
    mutationFn: () => financeApi.generateMonthlyReport(reportPeriod),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['reports'] }); toast.success('Monthly report generated'); setReportModal(false); },
    onError:   () => toast.error('Failed to generate report'),
  });

  const genQuarterly = useMutation({
    mutationFn: () => financeApi.generateQuarterlyReport(reportYear, reportQ),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['reports'] }); toast.success('Quarterly report generated'); setReportModal(false); },
    onError:   () => toast.error('Failed to generate report'),
  });

  const genAnnual = useMutation({
    mutationFn: () => financeApi.generateAnnualReport(reportYear),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['reports'] }); toast.success('Annual report generated'); setReportModal(false); },
    onError:   () => toast.error('Failed to generate report'),
  });

  return (
    <div className="max-w-7xl mx-auto">
      <PageHeader
        title="Finance Dashboard"
        subtitle="Payments, invoices, expenses and reports"
        actions={
          <div className="flex gap-2">
            <button onClick={() => setPayModal(true)} className="btn-secondary text-sm">
              <DollarSign size={14} /> Record Payment
            </button>
            <button onClick={() => setInvModal(true)} className="btn-secondary text-sm">
              <Receipt size={14} /> Generate Invoice
            </button>
            <button onClick={() => setExpModal(true)} className="btn-primary text-sm">
              <Plus size={14} /> Log Expense
            </button>
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
          INVOICES TAB
      ══════════════════════════════════════════════════════════════════════ */}
      {tab === 'Invoices' && (
        invLoading ? <PageLoader /> : (
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Invoice #</th>
                  <th>Booking</th>
                  <th>Issued</th>
                  <th>Due</th>
                  <th>Subtotal</th>
                  <th>Tax</th>
                  <th>Total</th>
                  <th>Status</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {invoices?.content.length === 0 ? (
                  <tr><td colSpan={9}>
                    <EmptyState icon={<FileText size={40} />} title="No invoices yet" />
                  </td></tr>
                ) : invoices?.content.map(inv => (
                  <tr key={inv.id}>
                    <td className="font-mono text-xs text-stone-600">{inv.invoiceNumber}</td>
                    <td className="font-mono text-xs text-stone-400">{inv.bookingId.slice(0, 8)}…</td>
                    <td>{formatDate(inv.issuedAt)}</td>
                    <td>{formatDate(inv.dueDate)}</td>
                    <td>{formatCurrency(inv.subtotal)}</td>
                    <td className="text-stone-500">{formatCurrency(inv.taxAmount)}</td>
                    <td className="font-semibold">{formatCurrency(inv.totalAmount)}</td>
                    <td><StatusBadge status={inv.status} /></td>
                    <td>
                      {inv.status !== 'VOIDED' && (
                        <button
                          onClick={() => { if (confirm('Void this invoice?')) voidInvoice.mutate(inv.id); }}
                          className="p-1.5 rounded-lg hover:bg-red-50 text-stone-400 hover:text-red-500 transition-colors"
                          title="Void invoice"
                        >
                          <XCircle size={14} />
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {invoices && (
              <div className="p-4 border-t border-stone-100">
                <Pagination page={page} totalPages={invoices.totalPages} onPageChange={setPage} />
              </div>
            )}
          </div>
        )
      )}

      {/* ══════════════════════════════════════════════════════════════════════
          EXPENSES TAB
      ══════════════════════════════════════════════════════════════════════ */}
      {tab === 'Expenses' && (
        expLoading ? <PageLoader /> : (
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Category</th>
                  <th>Description</th>
                  <th>Amount</th>
                  <th>Date</th>
                  <th>Approved By</th>
                  <th>Receipt</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {expenses?.content.length === 0 ? (
                  <tr><td colSpan={7}>
                    <EmptyState icon={<TrendingDown size={40} />} title="No expenses recorded" />
                  </td></tr>
                ) : expenses?.content.map(e => (
                  <tr key={e.id}>
                    <td>
                      <span className="px-2 py-0.5 bg-stone-100 rounded-full text-xs text-stone-600">
                        {e.category.replace('_', ' ')}
                      </span>
                    </td>
                    <td className="text-stone-700">{e.description}</td>
                    <td className="font-semibold text-red-600">{formatCurrency(e.amount)}</td>
                    <td>{formatDate(e.expenseDate)}</td>
                    <td className="text-stone-500">{e.approvedBy ?? '—'}</td>
                    <td className="text-stone-400 text-xs">{e.receiptReference ?? '—'}</td>
                    <td>
                      <div className="flex items-center gap-1">
                        {!e.approvedBy && (
                          <button
                            onClick={() => approveExpense.mutate(e.id)}
                            className="p-1.5 rounded-lg hover:bg-green-50 text-stone-400 hover:text-green-600 transition-colors"
                            title="Approve"
                          >
                            <CheckCircle size={14} />
                          </button>
                        )}
                        <button
                          onClick={() => { if (confirm('Delete this expense?')) deleteExpense.mutate(e.id); }}
                          className="p-1.5 rounded-lg hover:bg-red-50 text-stone-400 hover:text-red-500 transition-colors"
                          title="Delete"
                        >
                          <XCircle size={14} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {expenses && (
              <div className="p-4 border-t border-stone-100">
                <Pagination page={page} totalPages={expenses.totalPages} onPageChange={setPage} />
              </div>
            )}
          </div>
        )
      )}

      {/* ══════════════════════════════════════════════════════════════════════
          REPORTS TAB
      ══════════════════════════════════════════════════════════════════════ */}
      {tab === 'Reports' && (
        <>
          <div className="flex justify-end mb-4">
            <button onClick={() => setReportModal(true)} className="btn-primary">
              <BarChart3 size={14} /> Generate Report
            </button>
          </div>
          {repLoading ? <PageLoader /> : (
            <div className="table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Type</th>
                    <th>Period</th>
                    <th>Revenue</th>
                    <th>Expenses</th>
                    <th>Net Income</th>
                    <th>Generated</th>
                    <th>By</th>
                  </tr>
                </thead>
                <tbody>
                  {reports?.content.length === 0 ? (
                    <tr><td colSpan={7}>
                      <EmptyState icon={<BarChart3 size={40} />} title="No reports generated yet" />
                    </td></tr>
                  ) : reports?.content.map(r => (
                    <tr key={r.id}>
                      <td>
                        <span className="px-2 py-0.5 bg-gold-100 text-gold-700 rounded-full text-xs font-medium">
                          {r.reportType}
                        </span>
                      </td>
                      <td className="font-medium text-stone-800">{r.period}</td>
                      <td className="text-green-700 font-medium">{formatCurrency(r.totalRevenue)}</td>
                      <td className="text-red-600">{formatCurrency(r.totalExpenses)}</td>
                      <td className="font-semibold text-stone-900">{formatCurrency(r.netIncome)}</td>
                      <td className="text-stone-500">{formatDate(r.generatedAt)}</td>
                      <td className="text-stone-400 text-xs">{r.generatedBy ?? '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {reports && (
                <div className="p-4 border-t border-stone-100">
                  <Pagination page={page} totalPages={reports.totalPages} onPageChange={setPage} />
                </div>
              )}
            </div>
          )}
        </>
      )}

      {/* ══════════════════════════════════════════════════════════════════════
          LOG EXPENSE MODAL
      ══════════════════════════════════════════════════════════════════════ */}
      <Modal open={expModal} onClose={() => { setExpModal(false); expForm.reset(); }} title="Log Expense">
        <form onSubmit={expForm.handleSubmit(d => logExpense.mutate(d))} className="space-y-4">
          <div>
            <label className="label">Category</label>
            <select {...expForm.register('category')} className="input">
              <option value="">Select…</option>
              {EXPENSE_CATEGORIES.map(c => (
                <option key={c} value={c}>{c.replace('_', ' ')}</option>
              ))}
            </select>
            {expForm.formState.errors.category && (
              <p className="form-error">{expForm.formState.errors.category.message}</p>
            )}
          </div>
          <div>
            <label className="label">Description</label>
            <input {...expForm.register('description')} className="input" />
            {expForm.formState.errors.description && (
              <p className="form-error">{expForm.formState.errors.description.message}</p>
            )}
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="label">Amount (RWF)</label>
              <input type="number" {...expForm.register('amount', { valueAsNumber: true })} className="input" />
              {expForm.formState.errors.amount && (
                <p className="form-error">{expForm.formState.errors.amount.message}</p>
              )}
            </div>
            <div>
              <label className="label">Date</label>
              <input type="date" {...expForm.register('expenseDate')} className="input" />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="label">Approved By <span className="text-stone-400 font-normal">(optional)</span></label>
              <input {...expForm.register('approvedBy')} className="input" />
            </div>
            <div>
              <label className="label">Receipt Reference <span className="text-stone-400 font-normal">(optional)</span></label>
              <input {...expForm.register('receiptReference')} className="input" />
            </div>
          </div>
          <div className="flex gap-3 pt-2">
            <button type="button" onClick={() => { setExpModal(false); expForm.reset(); }} className="btn-secondary flex-1 justify-center">Cancel</button>
            <button type="submit" disabled={logExpense.isPending} className="btn-primary flex-1 justify-center">
              {logExpense.isPending ? 'Saving…' : 'Log Expense'}
            </button>
          </div>
        </form>
      </Modal>

      {/* ══════════════════════════════════════════════════════════════════════
          RECORD PAYMENT MODAL
      ══════════════════════════════════════════════════════════════════════ */}
      <Modal open={payModal} onClose={() => { setPayModal(false); payForm.reset(); }} title="Record Payment">
        <form onSubmit={payForm.handleSubmit(d => recordPayment.mutate(d))} className="space-y-4">
          <div>
            <label className="label">Booking ID</label>
            <input {...payForm.register('bookingId')} className="input font-mono text-sm" placeholder="UUID of the booking" />
            {payForm.formState.errors.bookingId && (
              <p className="form-error">{payForm.formState.errors.bookingId.message}</p>
            )}
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="label">Amount (RWF)</label>
              <input type="number" {...payForm.register('amount', { valueAsNumber: true })} className="input" />
              {payForm.formState.errors.amount && (
                <p className="form-error">{payForm.formState.errors.amount.message}</p>
              )}
            </div>
            <div>
              <label className="label">Method</label>
              <select {...payForm.register('method')} className="input">
                {PAYMENT_METHODS.map(m => (
                  <option key={m} value={m}>{m.replace('_', ' ')}</option>
                ))}
              </select>
            </div>
          </div>
          <div>
            <label className="label">Transaction Reference <span className="text-stone-400 font-normal">(optional)</span></label>
            <input {...payForm.register('transactionReference')} className="input" />
          </div>
          <div className="flex gap-3 pt-2">
            <button type="button" onClick={() => { setPayModal(false); payForm.reset(); }} className="btn-secondary flex-1 justify-center">Cancel</button>
            <button type="submit" disabled={recordPayment.isPending} className="btn-primary flex-1 justify-center">
              {recordPayment.isPending ? 'Saving…' : 'Record Payment'}
            </button>
          </div>
        </form>
      </Modal>

      {/* ══════════════════════════════════════════════════════════════════════
          GENERATE INVOICE MODAL
      ══════════════════════════════════════════════════════════════════════ */}
      <Modal open={invModal} onClose={() => { setInvModal(false); invForm.reset(); }} title="Generate Invoice">
        <form onSubmit={invForm.handleSubmit(d => generateInvoice.mutate(d))} className="space-y-4">
          <div>
            <label className="label">Booking ID</label>
            <input {...invForm.register('bookingId')} className="input font-mono text-sm" placeholder="UUID of the booking" />
            {invForm.formState.errors.bookingId && (
              <p className="form-error">{invForm.formState.errors.bookingId.message}</p>
            )}
          </div>
          <div>
            <label className="label">Tax Rate (%)</label>
            <input type="number" step="0.1" {...invForm.register('taxRate', { valueAsNumber: true })} className="input w-32" />
          </div>
          <div className="flex gap-3 pt-2">
            <button type="button" onClick={() => { setInvModal(false); invForm.reset(); }} className="btn-secondary flex-1 justify-center">Cancel</button>
            <button type="submit" disabled={generateInvoice.isPending} className="btn-primary flex-1 justify-center">
              {generateInvoice.isPending ? 'Generating…' : 'Generate Invoice'}
            </button>
          </div>
        </form>
      </Modal>

      {/* ══════════════════════════════════════════════════════════════════════
          GENERATE REPORT MODAL
      ══════════════════════════════════════════════════════════════════════ */}
      <Modal open={reportModal} onClose={() => setReportModal(false)} title="Generate Financial Report">
        <div className="space-y-5">
          {/* Monthly */}
          <div className="card border border-stone-200">
            <h4 className="font-medium text-stone-800 mb-3">Monthly Report</h4>
            <div className="flex items-center gap-3">
              <input
                type="month"
                value={reportPeriod}
                onChange={e => setReportPeriod(e.target.value)}
                className="input flex-1"
              />
              <button
                onClick={() => genMonthly.mutate()}
                disabled={genMonthly.isPending}
                className="btn-primary whitespace-nowrap"
              >
                {genMonthly.isPending ? 'Generating…' : 'Generate'}
              </button>
            </div>
          </div>

          {/* Quarterly */}
          <div className="card border border-stone-200">
            <h4 className="font-medium text-stone-800 mb-3">Quarterly Report</h4>
            <div className="flex items-center gap-3">
              <input
                type="number"
                value={reportYear}
                onChange={e => setReportYear(Number(e.target.value))}
                className="input w-24"
                placeholder="Year"
              />
              <select
                value={reportQ}
                onChange={e => setReportQ(Number(e.target.value))}
                className="input w-32"
              >
                <option value={1}>Q1 (Jan–Mar)</option>
                <option value={2}>Q2 (Apr–Jun)</option>
                <option value={3}>Q3 (Jul–Sep)</option>
                <option value={4}>Q4 (Oct–Dec)</option>
              </select>
              <button
                onClick={() => genQuarterly.mutate()}
                disabled={genQuarterly.isPending}
                className="btn-primary whitespace-nowrap"
              >
                {genQuarterly.isPending ? 'Generating…' : 'Generate'}
              </button>
            </div>
          </div>

          {/* Annual */}
          <div className="card border border-stone-200">
            <h4 className="font-medium text-stone-800 mb-3">Annual Report</h4>
            <div className="flex items-center gap-3">
              <input
                type="number"
                value={reportYear}
                onChange={e => setReportYear(Number(e.target.value))}
                className="input w-24"
                placeholder="Year"
              />
              <button
                onClick={() => genAnnual.mutate()}
                disabled={genAnnual.isPending}
                className="btn-primary whitespace-nowrap"
              >
                {genAnnual.isPending ? 'Generating…' : 'Generate'}
              </button>
            </div>
          </div>
        </div>
      </Modal>
    </div>
  );
}