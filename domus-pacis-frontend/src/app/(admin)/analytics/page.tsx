'use client';
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  LineChart, Line, BarChart, Bar, AreaChart, Area,
  XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell, Legend,
} from 'recharts';
import {
  analyticsApi,
  MonthlyRevenueStat,
  ServicePopularityStat,
} from '@/lib/api/analyticsApi';
import { PageHeader, PageLoader, KpiTile } from '@/components/ui';
import { formatCurrency } from '@/lib/utils';
import { BarChart3, TrendingUp, Users, Percent } from 'lucide-react';
import { format, subMonths } from 'date-fns';

const PIE_COLORS = ['#d4a017', '#7e1b38', '#e87575', '#6ba3be'];

// Default date range — last 6 months
const DEFAULT_FROM = format(subMonths(new Date(), 6), 'yyyy-MM-dd');
const DEFAULT_TO   = format(new Date(), 'yyyy-MM-dd');

export default function AnalyticsPage() {
  const [from, setFrom] = useState(DEFAULT_FROM);
  const [to,   setTo]   = useState(DEFAULT_TO);
  const [year, setYear] = useState(new Date().getFullYear());

  // ── Queries ────────────────────────────────────────────────────────────────
  const { data: kpis, isLoading: kpisLoading } = useQuery({
    queryKey: ['analytics-overview'],
    queryFn:  analyticsApi.getOverviewKpis,
  });

  const { data: monthly, isLoading: monthlyLoading } = useQuery({
    queryKey: ['analytics-monthly', year],
    queryFn:  () => analyticsApi.getMonthlyRevenue(year),
  });

  const { data: popularity } = useQuery({
    queryKey: ['analytics-popularity', from, to],
    queryFn:  () => analyticsApi.getServicePopularity(from, to),
  });

  const { data: customers } = useQuery({
    queryKey: ['analytics-customers', from, to],
    queryFn:  () => analyticsApi.getCustomerActivity(from, to),
  });

  const { data: occupancy } = useQuery({
    queryKey: ['analytics-occupancy', from, to],
    queryFn:  () => analyticsApi.getOccupancy(from, to),
  });

  // ── Derived display values (fall back to zeros while loading) ──────────────
  const kpi = kpis ?? {
    occupancyRate: 0, revenueGrowth: 0,
    totalCustomers: 0, monthlyRevenue: 0,
  };

  // Normalise monthly data — backend returns "2025-01", chart needs short label
  const monthlyChart: MonthlyRevenueStat[] = (monthly ?? []).map(m => ({
    ...m,
    month: m.month.slice(5), // "2025-01" → "01"
  }));

  // Normalise service popularity for pie chart
  const popularityChart: ServicePopularityStat[] = popularity ?? [];

  // Customer segments from backend
  const customerSegments = customers
    ? [
        { segment: 'New',       count: customers.newCustomers,       pct: Math.round((customers.newCustomers / Math.max(customers.totalCustomers, 1)) * 100) },
        { segment: 'Returning', count: customers.returningCustomers, pct: Math.round((customers.returningCustomers / Math.max(customers.totalCustomers, 1)) * 100) },
        { segment: 'Loyal',     count: customers.loyalCustomers,     pct: Math.round((customers.loyalCustomers / Math.max(customers.totalCustomers, 1)) * 100) },
      ]
    : [];

  // Occupancy by asset type for line chart
  const occupancyByType = occupancy?.byAssetType ?? [];

  if (kpisLoading) return <PageLoader />;

  return (
    <div className="max-w-7xl mx-auto space-y-6">
      <PageHeader
        title="Analytics"
        subtitle="Occupancy, revenue trends and service performance"
        actions={
          <div className="flex items-center gap-2 text-sm">
            <input type="date" value={from} onChange={e => setFrom(e.target.value)} className="input w-36" />
            <span className="text-stone-400">to</span>
            <input type="date" value={to}   onChange={e => setTo(e.target.value)}   className="input w-36" />
          </div>
        }
      />

      {/* ── KPI tiles ──────────────────────────────────────────────────────── */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <KpiTile
          title="Avg Occupancy"
          value={`${kpi.occupancyRate}%`}
          icon={<Percent size={18} />}
          accent="bg-gold-50 text-gold-600"
        />
        <KpiTile
          title="Revenue Growth"
          value={`+${kpi.revenueGrowth}%`}
          icon={<TrendingUp size={18} />}
          accent="bg-green-50 text-green-600"
          change={kpi.revenueGrowth}
        />
        <KpiTile
          title="Total Customers"
          value={kpi.totalCustomers.toLocaleString()}
          icon={<Users size={18} />}
          accent="bg-blue-50 text-blue-600"
        />
        <KpiTile
          title="Monthly Revenue"
          value={formatCurrency(kpi.monthlyRevenue)}
          icon={<BarChart3 size={18} />}
          accent="bg-purple-50 text-purple-600"
        />
      </div>

      {/* ── Occupancy by asset type ─────────────────────────────────────────── */}
      <div className="card">
        <div className="flex items-center justify-between mb-5">
          <h3 className="font-display text-lg text-stone-900">Occupancy Rate by Service (%)</h3>
          {occupancy && (
            <span className="text-sm text-stone-500">
              Overall: <strong className="text-stone-800">{occupancy.overallRate}%</strong>
            </span>
          )}
        </div>
        {occupancyByType.length > 0 ? (
          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={occupancyByType} margin={{ top: 0, right: 10, left: 0, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0ece6" vertical={false} />
              <XAxis dataKey="assetType" tick={{ fontSize: 11, fill: '#a8a29e' }} axisLine={false} tickLine={false} />
              <YAxis domain={[0, 100]} tickFormatter={v => `${v}%`} tick={{ fontSize: 10, fill: '#a8a29e' }} axisLine={false} tickLine={false} />
              <Tooltip formatter={(v: number) => `${v}%`} contentStyle={{ borderRadius: 8, fontSize: 12 }} />
              <Bar dataKey="rate" fill="#d4a017" radius={[4, 4, 0, 0]} name="Occupancy %" />
            </BarChart>
          </ResponsiveContainer>
        ) : (
          <p className="text-stone-400 text-sm py-10 text-center">No occupancy data for selected range.</p>
        )}
      </div>

      {/* ── Monthly revenue trend ───────────────────────────────────────────── */}
      <div className="card">
        <div className="flex items-center justify-between mb-5">
          <h3 className="font-display text-lg text-stone-900">Revenue Trend (RWF)</h3>
          <select
            value={year}
            onChange={e => setYear(Number(e.target.value))}
            className="input w-28 text-sm"
          >
            {[2023, 2024, 2025, 2026].map(y => <option key={y} value={y}>{y}</option>)}
          </select>
        </div>
        {monthlyLoading ? <PageLoader /> : (
          <ResponsiveContainer width="100%" height={240}>
            <AreaChart data={monthlyChart} margin={{ top: 0, right: 10, left: 0, bottom: 0 }}>
              <defs>
                <linearGradient id="profGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%"  stopColor="#d4a017" stopOpacity={0.25} />
                  <stop offset="95%" stopColor="#d4a017" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0ece6" />
              <XAxis dataKey="month" tick={{ fontSize: 11, fill: '#a8a29e' }} axisLine={false} tickLine={false} />
              <YAxis tickFormatter={v => `${(v / 1e6).toFixed(1)}M`} tick={{ fontSize: 10, fill: '#a8a29e' }} axisLine={false} tickLine={false} />
              <Tooltip formatter={(v: number) => formatCurrency(v)} contentStyle={{ borderRadius: 8, fontSize: 12 }} />
              <Legend wrapperStyle={{ fontSize: 11 }} />
              <Area type="monotone" dataKey="profit"   stroke="#d4a017" strokeWidth={2} fill="url(#profGrad)" name="Net Profit" />
              <Line type="monotone" dataKey="revenue"  stroke="#6ba3be" strokeWidth={1.5} dot={false} name="Revenue" />
              <Line type="monotone" dataKey="expenses" stroke="#e87575" strokeWidth={1.5} dot={false} name="Expenses" strokeDasharray="3 2" />
            </AreaChart>
          </ResponsiveContainer>
        )}
      </div>

      {/* ── Service popularity & customer segments ──────────────────────────── */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">

        {/* Service popularity */}
        <div className="card">
          <h3 className="font-display text-lg text-stone-900 mb-5">Service Popularity</h3>
          {popularityChart.length > 0 ? (
            <div className="flex items-center gap-6">
              <ResponsiveContainer width={160} height={160}>
                <PieChart>
                  <Pie
                    data={popularityChart}
                    cx="50%" cy="50%"
                    innerRadius={45} outerRadius={72}
                    dataKey="percentage"
                    paddingAngle={2}
                  >
                    {popularityChart.map((_, i) => (
                      <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />
                    ))}
                  </Pie>
                </PieChart>
              </ResponsiveContainer>
              <div className="flex-1 space-y-3">
                {popularityChart.map((s, i) => (
                  <div key={s.assetType}>
                    <div className="flex items-center justify-between text-sm mb-1">
                      <div className="flex items-center gap-2">
                        <span className="w-2.5 h-2.5 rounded-full" style={{ background: PIE_COLORS[i % PIE_COLORS.length] }} />
                        <span className="text-stone-700">{s.assetName}</span>
                      </div>
                      <span className="font-semibold text-stone-900">{s.percentage}%</span>
                    </div>
                    <div className="h-1.5 bg-stone-100 rounded-full overflow-hidden">
                      <div
                        className="h-full rounded-full"
                        style={{ width: `${s.percentage}%`, background: PIE_COLORS[i % PIE_COLORS.length] }}
                      />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <p className="text-stone-400 text-sm py-10 text-center">No popularity data for selected range.</p>
          )}
        </div>

        {/* Customer segments */}
        <div className="card">
          <h3 className="font-display text-lg text-stone-900 mb-5">Customer Segments</h3>
          {customerSegments.length > 0 ? (
            <>
              <ResponsiveContainer width="100%" height={180}>
                <BarChart data={customerSegments} layout="vertical" margin={{ left: 40 }}>
                  <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke="#f0ece6" />
                  <XAxis type="number" tick={{ fontSize: 10, fill: '#a8a29e' }} axisLine={false} tickLine={false} />
                  <YAxis dataKey="segment" type="category" tick={{ fontSize: 11, fill: '#78716c' }} axisLine={false} tickLine={false} />
                  <Tooltip formatter={(v: number) => [`${v} guests`, 'Count']} contentStyle={{ borderRadius: 8, fontSize: 12 }} />
                  <Bar dataKey="count" fill="#d4a017" radius={[0, 6, 6, 0]} name="Customers" />
                </BarChart>
              </ResponsiveContainer>
              <div className="grid grid-cols-3 gap-3 mt-4">
                {customerSegments.map(seg => (
                  <div key={seg.segment} className="text-center">
                    <div className="font-display text-2xl text-stone-900">{seg.count}</div>
                    <div className="text-stone-400 text-xs">{seg.segment}</div>
                    <div className="text-gold-600 text-xs font-medium">{seg.pct}%</div>
                  </div>
                ))}
              </div>
            </>
          ) : (
            <p className="text-stone-400 text-sm py-10 text-center">No customer data for selected range.</p>
          )}
        </div>
      </div>
    </div>
  );
}