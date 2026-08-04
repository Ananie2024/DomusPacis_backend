'use client';
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { taxApi, TaxRecordDto, CreateTaxRuleRequest } from '@/lib/api/taxApi';
import { formatCurrency, formatDate } from '@/lib/utils';
import { PageHeader, PageLoader, StatusBadge, KpiTile, EmptyState, Modal } from '@/components/ui';
import {
  Receipt, AlertTriangle, CheckCircle,
  Plus, Calculator, Send, DollarSign,
  Edit, Shield,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import toast from 'react-hot-toast';

const TABS      = ['Records', 'Rules', 'Calculator'] as const;
type Tab        = typeof TABS[number];
const TAX_TYPES = ['VAT', 'WITHHOLDING_TAX', 'INCOME_TAX', 'PAYE'];

// ── Schemas ───────────────────────────────────────────────────────────────────
const ruleSchema = z.object({
  taxType:       z.string().min(1, 'Tax type required'),
  rate:          z.number().min(0).max(100, 'Rate must be 0–100'),
  description:   z.string().optional(),
  effectiveFrom: z.string().min(1, 'Effective from required'),
  effectiveTo:   z.string().optional(),
});

const calcSchema = z.object({
  amount:  z.number().positive('Amount must be positive'),
  taxType: z.string().min(1, 'Tax type required'),
});

const amendSchema = z.object({
  newTaxableAmount: z.number().positive('Must be positive'),
});

type RuleForm  = z.infer<typeof ruleSchema>;
type CalcForm  = z.infer<typeof calcSchema>;
type AmendForm = z.infer<typeof amendSchema>;

export default function TaxPage() {
  const [tab,          setTab]          = useState<Tab>('Records');
  const [year,         setYear]         = useState(new Date().getFullYear());
  const [computePeriod,setComputePeriod]= useState(new Date().toISOString().slice(0, 7));
  const [selectedId,   setSelectedId]   = useState<string | null>(null);
  const [amendTarget,  setAmendTarget]  = useState<TaxRecordDto | null>(null);
  const [ruleModal,    setRuleModal]    = useState(false);
  const [calcResult,   setCalcResult]   = useState<{ taxAmount: number; totalWithTax: number } | null>(null);

  const qc = useQueryClient();

  // ── Queries ────────────────────────────────────────────────────────────────
  const { data: records, isLoading: recLoading } = useQuery({
    queryKey: ['tax-records', year],
    queryFn:  () => taxApi.listByYear(year),
    enabled:  tab === 'Records',
  });

  const { data: rules, isLoading: rulesLoading } = useQuery({
    queryKey: ['tax-rules'],
    queryFn:  taxApi.listRules,
    enabled:  tab === 'Rules',
  });

  // ── Forms ──────────────────────────────────────────────────────────────────
  const ruleForm  = useForm<RuleForm> ({ resolver: zodResolver(ruleSchema),  defaultValues: { rate: 18 } });
  const calcForm  = useForm<CalcForm> ({ resolver: zodResolver(calcSchema),  defaultValues: { taxType: 'VAT', amount: 0 } });
  const amendForm = useForm<AmendForm>({ resolver: zodResolver(amendSchema) });

  // ── Mutations ──────────────────────────────────────────────────────────────
  const computeVat = useMutation({
    mutationFn: () => taxApi.computeVat(computePeriod),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['tax-records'] }); toast.success('VAT record computed'); },
    onError:   () => toast.error('Failed to compute VAT'),
  });

  const submit = useMutation({
    mutationFn: (id: string) => taxApi.submit(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['tax-records'] }); toast.success('Tax record submitted to RRA'); },
    onError:   () => toast.error('Failed to submit'),
  });

  const markPaid = useMutation({
    mutationFn: (id: string) => taxApi.markPaid(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['tax-records'] }); toast.success('Marked as paid'); },
    onError:   () => toast.error('Failed to mark as paid'),
  });

  const amend = useMutation({
    mutationFn: ({ id, amount }: { id: string; amount: number }) =>
      taxApi.amend(id, amount),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['tax-records'] });
      toast.success('Tax record amended');
      setAmendTarget(null);
      amendForm.reset();
    },
    onError: () => toast.error('Failed to amend record'),
  });

  const createRule = useMutation({
    mutationFn: (req: CreateTaxRuleRequest) => taxApi.createRule(req),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['tax-rules'] });
      toast.success('Tax rule created');
      setRuleModal(false);
      ruleForm.reset();
    },
    onError: () => toast.error('Failed to create rule'),
  });

  const calculate = useMutation({
    mutationFn: (d: CalcForm) => taxApi.calculate(d.amount, d.taxType),
    onSuccess: (res) => setCalcResult({ taxAmount: res.taxAmount, totalWithTax: res.totalWithTax }),
    onError:   () => toast.error('Calculation failed'),
  });

  // ── Derived KPIs from live records ─────────────────────────────────────────
  const allRecords = records ?? [];
  const totals = allRecords.reduce((acc, r) => ({
    taxableAmount: acc.taxableAmount + Number(r.taxableAmount),
    taxDue:        acc.taxDue        + Number(r.taxDue),
    vatDue:        r.taxType === 'VAT'             ? acc.vatDue   + Number(r.taxDue) : acc.vatDue,
    whtDue:        r.taxType === 'WITHHOLDING_TAX' ? acc.whtDue   + Number(r.taxDue) : acc.whtDue,
  }), { taxableAmount: 0, taxDue: 0, vatDue: 0, whtDue: 0 });

  return (
    <div className="max-w-7xl mx-auto">
      <PageHeader
        title="Tax & Compliance"
        subtitle="VAT, withholding tax and Rwanda Revenue Authority reporting"
      />

      {/* ── Tabs ────────────────────────────────────────────────────────────── */}
      <div className="flex gap-1 mb-6 bg-stone-100 p-1 rounded-xl w-fit">
        {TABS.map(t => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${
              tab === t ? 'bg-white text-stone-900 shadow-sm' : 'text-stone-500 hover:text-stone-700'
            }`}
          >
            {t}
          </button>
        ))}
      </div>

      {/* ══════════════════════════════════════════════════════════════════════
          RECORDS TAB
      ══════════════════════════════════════════════════════════════════════ */}
      {tab === 'Records' && (
        <>
          {/* KPI tiles — derived from live data */}
          <div className="grid grid-cols-3 gap-4 mb-6">
            <KpiTile
              title="Total VAT Due"
              value={formatCurrency(totals.vatDue)}
              icon={<Receipt size={18} />}
              accent="bg-blue-50 text-blue-600"
            />
            <KpiTile
              title="Withholding Tax Due"
              value={formatCurrency(totals.whtDue)}
              icon={<Receipt size={18} />}
              accent="bg-purple-50 text-purple-600"
            />
            <KpiTile
              title="Total Tax Liability"
              value={formatCurrency(totals.taxDue)}
              icon={<AlertTriangle size={18} />}
              accent="bg-amber-50 text-amber-600"
            />
          </div>

          {/* Year selector + compute */}
          <div className="card mb-5 flex flex-wrap items-center gap-4">
            <div className="flex items-center gap-2">
              <label className="label mb-0 text-sm">Year:</label>
              <select
                value={year}
                onChange={e => setYear(Number(e.target.value))}
                className="input w-24"
              >
                {[2023, 2024, 2025, 2026].map(y => (
                  <option key={y} value={y}>{y}</option>
                ))}
              </select>
            </div>
            <div className="flex items-center gap-2">
              <label className="label mb-0 text-sm">Compute VAT for:</label>
              <input
                type="month"
                value={computePeriod}
                onChange={e => setComputePeriod(e.target.value)}
                className="input w-40"
              />
              <button
                onClick={() => computeVat.mutate()}
                disabled={computeVat.isPending}
                className="btn-secondary text-sm"
              >
                <Calculator size={14} />
                {computeVat.isPending ? 'Computing…' : 'Compute VAT'}
              </button>
            </div>
          </div>

          {/* Records table */}
          {recLoading ? <PageLoader /> : (
            allRecords.length === 0 ? (
              <div className="card text-center py-12">
                <Receipt size={40} className="text-stone-300 mx-auto mb-3" />
                <p className="text-stone-500">No tax records for {year}.</p>
                <p className="text-stone-400 text-sm mt-1">Use &quot;Compute VAT&quot; above to generate records.</p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                {allRecords.map(r => (
                  <div
                    key={r.id}
                    onClick={() => setSelectedId(r.id === selectedId ? null : r.id)}
                    className={cn(
                      'card cursor-pointer transition-all duration-200 border-2',
                      selectedId === r.id
                        ? 'border-gold-400 shadow-gold'
                        : 'border-transparent hover:border-stone-200'
                    )}
                  >
                    <div className="flex items-start justify-between mb-3">
                      <div>
                        <h3 className="font-display text-lg text-stone-900">{r.period}</h3>
                        <div className="text-stone-400 text-xs mt-0.5">
                          {r.taxType.replace('_', ' ')}
                          {r.filedAt && ` · Filed ${formatDate(r.filedAt)}`}
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        <StatusBadge status={r.status} />
                        {r.status === 'PAID' && (
                          <CheckCircle size={16} className="text-green-500" />
                        )}
                      </div>
                    </div>

                    <div className="grid grid-cols-3 gap-3 text-sm mb-4">
                      <div>
                        <div className="text-stone-400 text-xs mb-0.5">Taxable Amount</div>
                        <div className="font-semibold text-stone-900">{formatCurrency(Number(r.taxableAmount))}</div>
                      </div>
                      <div>
                        <div className="text-stone-400 text-xs mb-0.5">Rate</div>
                        <div className="font-semibold text-stone-900">{Number(r.taxRate)}%</div>
                      </div>
                      <div>
                        <div className="text-stone-400 text-xs mb-0.5">Tax Due</div>
                        <div className="font-semibold text-gold-700">{formatCurrency(Number(r.taxDue))}</div>
                      </div>
                    </div>

                    {/* Action buttons */}
                    <div className="flex flex-wrap gap-2" onClick={e => e.stopPropagation()}>
                      {r.status === 'DRAFT' && (
                        <>
                          <button
                            onClick={() => submit.mutate(r.id)}
                            disabled={submit.isPending}
                            className="btn-ghost text-xs border border-stone-200 hover:bg-blue-50 hover:text-blue-600"
                          >
                            <Send size={12} /> Submit to RRA
                          </button>
                          <button
                            onClick={() => { setAmendTarget(r); amendForm.setValue('newTaxableAmount', Number(r.taxableAmount)); }}
                            className="btn-ghost text-xs border border-stone-200"
                          >
                            <Edit size={12} /> Amend
                          </button>
                        </>
                      )}
                      {r.status === 'SUBMITTED' && (
                        <button
                          onClick={() => markPaid.mutate(r.id)}
                          disabled={markPaid.isPending}
                          className="btn-ghost text-xs border border-stone-200 hover:bg-green-50 hover:text-green-600"
                        >
                          <DollarSign size={12} /> Mark Paid
                        </button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )
          )}
        </>
      )}

      {/* ══════════════════════════════════════════════════════════════════════
          RULES TAB
      ══════════════════════════════════════════════════════════════════════ */}
      {tab === 'Rules' && (
        <>
          <div className="flex justify-end mb-4">
            <button onClick={() => setRuleModal(true)} className="btn-primary">
              <Plus size={14} /> Create Rule
            </button>
          </div>

          {rulesLoading ? <PageLoader /> : (
            (rules ?? []).length === 0 ? (
              <EmptyState
                icon={<Shield size={40} />}
                title="No tax rules configured"
                description="Create tax rules to enable automatic tax computation."
                action={<button onClick={() => setRuleModal(true)} className="btn-primary"><Plus size={14} /> Create Rule</button>}
              />
            ) : (
              <div className="table-container">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Tax Type</th>
                      <th>Rate</th>
                      <th>Description</th>
                      <th>Effective From</th>
                      <th>Effective To</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {rules?.map(rule => (
                      <tr key={rule.id}>
                        <td>
                          <span className="px-2 py-0.5 bg-gold-100 text-gold-700 rounded-full text-xs font-medium">
                            {rule.taxType.replace('_', ' ')}
                          </span>
                        </td>
                        <td className="font-semibold text-stone-900">{Number(rule.rate)}%</td>
                        <td className="text-stone-600">{rule.description ?? '—'}</td>
                        <td>{rule.effectiveFrom ? formatDate(rule.effectiveFrom) : '—'}</td>
                        <td>{rule.effectiveTo  ? formatDate(rule.effectiveTo)  : 'Ongoing'}</td>
                        <td><StatusBadge status={rule.isActive ? 'ACTIVE' : 'INACTIVE'} /></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )
          )}
        </>
      )}

      {/* ══════════════════════════════════════════════════════════════════════
          CALCULATOR TAB
      ══════════════════════════════════════════════════════════════════════ */}
      {tab === 'Calculator' && (
        <div className="max-w-lg">
          <div className="card">
            <h3 className="font-display text-lg text-stone-900 mb-5">Tax Calculator</h3>
            <form onSubmit={calcForm.handleSubmit(d => calculate.mutate(d))} className="space-y-4">
              <div>
                <label className="label">Amount (RWF)</label>
                <input
                  type="number"
                  step="0.01"
                  {...calcForm.register('amount', { valueAsNumber: true })}
                  className="input"
                  placeholder="Enter amount"
                />
                {calcForm.formState.errors.amount && (
                  <p className="form-error">{calcForm.formState.errors.amount.message}</p>
                )}
              </div>
              <div>
                <label className="label">Tax Type</label>
                <select {...calcForm.register('taxType')} className="input">
                  {TAX_TYPES.map(t => (
                    <option key={t} value={t}>{t.replace('_', ' ')}</option>
                  ))}
                </select>
              </div>
              <button type="submit" disabled={calculate.isPending} className="btn-primary w-full justify-center">
                <Calculator size={15} />
                {calculate.isPending ? 'Calculating…' : 'Calculate Tax'}
              </button>
            </form>

            {calcResult && (
              <div className="mt-6 p-5 bg-ivory-100 rounded-xl border border-gold-200 space-y-3">
                <h4 className="font-medium text-stone-800 text-sm">Calculation Result</h4>
                <div className="grid grid-cols-2 gap-4 text-sm">
                  <div>
                    <div className="text-stone-400 text-xs mb-0.5">Tax Amount</div>
                    <div className="font-display text-xl text-gold-700">{formatCurrency(Number(calcResult.taxAmount))}</div>
                  </div>
                  <div>
                    <div className="text-stone-400 text-xs mb-0.5">Total with Tax</div>
                    <div className="font-display text-xl text-stone-900">{formatCurrency(Number(calcResult.totalWithTax))}</div>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* ══════════════════════════════════════════════════════════════════════
          AMEND MODAL
      ══════════════════════════════════════════════════════════════════════ */}
      <Modal
        open={!!amendTarget}
        onClose={() => { setAmendTarget(null); amendForm.reset(); }}
        title={`Amend Tax Record — ${amendTarget?.period}`}
      >
        <form onSubmit={amendForm.handleSubmit(d =>
          amendTarget && amend.mutate({ id: amendTarget.id, amount: d.newTaxableAmount })
        )} className="space-y-4">
          <div>
            <div className="text-stone-500 text-sm mb-3">
              Current taxable amount: <strong>{amendTarget && formatCurrency(Number(amendTarget.taxableAmount))}</strong>
            </div>
            <label className="label">New Taxable Amount (RWF)</label>
            <input
              type="number"
              step="0.01"
              {...amendForm.register('newTaxableAmount', { valueAsNumber: true })}
              className="input"
            />
            {amendForm.formState.errors.newTaxableAmount && (
              <p className="form-error">{amendForm.formState.errors.newTaxableAmount.message}</p>
            )}
          </div>
          <div className="flex gap-3 pt-2">
            <button type="button" onClick={() => setAmendTarget(null)} className="btn-secondary flex-1 justify-center">Cancel</button>
            <button type="submit" disabled={amend.isPending} className="btn-primary flex-1 justify-center">
              {amend.isPending ? 'Saving…' : 'Save Amendment'}
            </button>
          </div>
        </form>
      </Modal>

      {/* ══════════════════════════════════════════════════════════════════════
          CREATE RULE MODAL
      ══════════════════════════════════════════════════════════════════════ */}
      <Modal open={ruleModal} onClose={() => { setRuleModal(false); ruleForm.reset(); }} title="Create Tax Rule">
        <form onSubmit={ruleForm.handleSubmit(d => createRule.mutate({
          taxType:       d.taxType,
          rate:          d.rate,
          description:   d.description,
          effectiveFrom: d.effectiveFrom,
          effectiveTo:   d.effectiveTo || undefined,
        }))} className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="label">Tax Type</label>
              <select {...ruleForm.register('taxType')} className="input">
                <option value="">Select…</option>
                {TAX_TYPES.map(t => (
                  <option key={t} value={t}>{t.replace('_', ' ')}</option>
                ))}
              </select>
              {ruleForm.formState.errors.taxType && (
                <p className="form-error">{ruleForm.formState.errors.taxType.message}</p>
              )}
            </div>
            <div>
              <label className="label">Rate (%)</label>
              <input
                type="number"
                step="0.01"
                {...ruleForm.register('rate', { valueAsNumber: true })}
                className="input"
              />
              {ruleForm.formState.errors.rate && (
                <p className="form-error">{ruleForm.formState.errors.rate.message}</p>
              )}
            </div>
          </div>
          <div>
            <label className="label">Description <span className="text-stone-400 font-normal">(optional)</span></label>
            <input {...ruleForm.register('description')} className="input" />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="label">Effective From</label>
              <input type="date" {...ruleForm.register('effectiveFrom')} className="input" />
              {ruleForm.formState.errors.effectiveFrom && (
                <p className="form-error">{ruleForm.formState.errors.effectiveFrom.message}</p>
              )}
            </div>
            <div>
              <label className="label">Effective To <span className="text-stone-400 font-normal">(optional)</span></label>
              <input type="date" {...ruleForm.register('effectiveTo')} className="input" />
            </div>
          </div>
          <div className="flex gap-3 pt-2">
            <button type="button" onClick={() => { setRuleModal(false); ruleForm.reset(); }} className="btn-secondary flex-1 justify-center">Cancel</button>
            <button type="submit" disabled={createRule.isPending} className="btn-primary flex-1 justify-center">
              {createRule.isPending ? 'Saving…' : 'Create Rule'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
