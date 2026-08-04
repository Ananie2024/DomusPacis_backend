'use client';

import { useQuery } from '@tanstack/react-query';
import { startOfYear, endOfMonth, format } from 'date-fns';
import {
  AreaChart, Area, BarChart, Bar, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer, PieChart, Pie, Cell,
} from 'recharts';
import {
  CalendarDays, Users, DollarSign,
  TrendingUp, CheckCircle, Clock, AlertTriangle,
} from 'lucide-react';
import { analyticsApi } from '@/lib/api/analyticsApi';
import { KpiTile, PageLoader, PageHeader } from '@/components/ui';
import { formatCurrency, formatDate } from '@/lib/utils';

// ── Date helpers ─────────────────────────────────────────────────────────────
const today      = new Date();
const FROM_YEAR  = format(startOfYear(today), 'yyyy-MM-dd');
const TO_TODAY   = format(today, 'yyyy-MM-dd');
const YEAR       = today.getFullYear();

// ── Palette (mirrors old mock colours) ───────────────────────────────────────
const PIE_COLOURS = ['#d4a017', '#7e1b38', '#e87575', '#6ba3be', '#4caf8a', '#9c6fd6'];

export default function DashboardPage() {

  const { data: kpis, isLoading: kpiLoading } = useQuery({
    queryKey: ['dashboard-kpis'],
    queryFn:  () => analyticsApi.getOverviewKpis(),
  });

  const { data: monthlyRevenue = [], isLoading: revenueLoading } = useQuery({
    queryKey: ['monthly-revenue', YEAR],
    queryFn:  () => analyticsApi.getMonthlyRevenue(YEAR),
  });

  const { data: servicePopularity = [], isLoading: popularityLoading } = useQuery({
    queryKey: ['service-popularity', FROM_YEAR, TO_TODAY],
    queryFn:  () => analyticsApi.getServicePopularity(FROM_YEAR, TO_TODAY),
  });

  const { data: revenueBySource = [], isLoading: sourceLoading } = useQuery({
    queryKey: ['revenue-by-source', FROM_YEAR, TO_TODAY],
    queryFn:  () => analyticsApi.getRevenueBySource(FROM_YEAR, TO_TODAY),
  });

  const { data: customerActivity, isLoading: customerLoading } = useQuery({
    queryKey: ['customer-activity', FROM_YEAR, TO_TODAY],
    queryFn:  () => analyticsApi.getCustomerActivity(FROM_YEAR, TO_TODAY),
  });

  const isLoading =
    kpiLoading || revenueLoading || popularityLoading || sourceLoading || customerLoading;

  if (isLoading) return <PageLoader />;

  // ── Derived chart data ────────────────────────────────────────────────────
  // Monthly revenue: shorten "2025-01" → "Jan"
  const revenueChartData = monthlyRevenue.map((r) => ({
    month:    format(new Date(r.month + '-01'), 'MMM'),
    revenue:  r.revenue,
    expenses: r.expenses,
    profit:   r.profit,
  }));

  // Service popularity → pie-compatible shape (compute % from booking counts)
  const totalBookings = servicePopularity.reduce((sum, s) => sum + s.bookings, 0);
  const popularityChartData = servicePopularity.map((s, i) => ({
    name:  s.assetName || s.assetType || 'Unknown',
    value: totalBookings > 0 ? (s.bookings / totalBookings) * 100 : 0,
    color: PIE_COLOURS[i % PIE_COLOURS.length],
  }));

  // Revenue by source → bar chart
  const totalRevenue = revenueBySource.reduce((sum, s) => sum + s.amount, 0);
  const sourceChartData = revenueBySource.map((s) => ({
    name:   s.source.replace('_', ' '),
    amount: s.amount,
    pct:    totalRevenue > 0 ? (s.amount / totalRevenue) * 100 : 0,
  }));

  return (
    <div className="max-w-7xl mx-auto space-y-6">
      <PageHeader
        title="Dashboard"
        subtitle={`Welcome back — ${formatDate(today.toISOString(), 'EEEE, MMMM d yyyy')}`}
      />

      {/* ── KPI row 1 ── */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <KpiTile
          title="Monthly Revenue"
          value={formatCurrency(kpis?.monthlyRevenue ?? 0)}
          icon={<DollarSign size={18} />}
          change={kpis?.revenueGrowth}
          changeLabel="vs last month"
          accent="bg-gold-50 text-gold-600"
        />
        <KpiTile
          title="Confirmed Bookings"
          value={kpis?.confirmedBookings ?? 0}
          icon={<CheckCircle size={18} />}
          accent="bg-green-50 text-green-600"
        />
        <KpiTile
          title="Pending Bookings"
          value={kpis?.pendingBookings ?? 0}
          icon={<Clock size={18} />}
          accent="bg-amber-50 text-amber-600"
        />
        <KpiTile
          title="Total Customers"
          value={(customerActivity?.totalCustomers ?? kpis?.totalCustomers ?? 0).toLocaleString()}
          icon={<Users size={18} />}
          accent="bg-blue-50 text-blue-600"
        />
      </div>

      {/* ── KPI row 2 ── */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <KpiTile
          title="Occupancy Rate"
          value={`${kpis?.occupancyRate ?? 0}%`}
          icon={<CalendarDays size={18} />}
          accent="bg-purple-50 text-purple-600"
        />
        <KpiTile
          title="Today's Check-ins"
          value={kpis?.todayCheckIns ?? 0}
          icon={<TrendingUp size={18} />}
          accent="bg-teal-50 text-teal-600"
        />
        <KpiTile
          title="Monthly Expenses"
          value={formatCurrency(kpis?.monthlyExpenses ?? 0)}
          icon={<DollarSign size={18} />}
          accent="bg-rose-50 text-rose-600"
        />
        <KpiTile
          title="Low Stock Alerts"
          value={kpis?.lowStockItems ?? 0}
          icon={<AlertTriangle size={18} />}
          accent="bg-red-50 text-red-600"
        />
      </div>

      {/* ── Charts row ── */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

        {/* Revenue vs Expenses area chart */}
        <div className="lg:col-span-2 card">
          <h3 className="font-display text-lg text-stone-900 mb-5">Revenue vs Expenses</h3>
          {revenueChartData.length === 0 ? (
            <EmptyChart message="No revenue data for this year yet." />
          ) : (
            <ResponsiveContainer width="100%" height={240}>
              <AreaChart data={revenueChartData} margin={{ top: 0, right: 0, left: 0, bottom: 0 }}>
                <defs>
                  <linearGradient id="rev" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%"  stopColor="#d4a017" stopOpacity={0.2} />
                    <stop offset="95%" stopColor="#d4a017" stopOpacity={0} />
                  </linearGradient>
                  <linearGradient id="exp" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%"  stopColor="#7e1b38" stopOpacity={0.15} />
                    <stop offset="95%" stopColor="#7e1b38" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0ece6" />
                <XAxis dataKey="month" tick={{ fontSize: 11, fill: '#a8a29e' }} axisLine={false} tickLine={false} />
                <YAxis tickFormatter={(v) => `${(v / 1e6).toFixed(1)}M`} tick={{ fontSize: 10, fill: '#a8a29e' }} axisLine={false} tickLine={false} />
                <Tooltip formatter={(v: number) => formatCurrency(v)} contentStyle={{ borderRadius: 8, border: '1px solid #e7e5e4', fontSize: 12 }} />
                <Area type="monotone" dataKey="revenue"  stroke="#d4a017" strokeWidth={2} fill="url(#rev)"  name="Revenue" />
                <Area type="monotone" dataKey="expenses" stroke="#7e1b38" strokeWidth={2} fill="url(#exp)" name="Expenses" />
              </AreaChart>
            </ResponsiveContainer>
          )}
        </div>

        {/* Service popularity pie */}
        <div className="card">
          <h3 className="font-display text-lg text-stone-900 mb-5">Service Popularity</h3>
          {popularityChartData.length === 0 ? (
            <EmptyChart message="No bookings recorded yet." />
          ) : (
            <>
              <ResponsiveContainer width="100%" height={180}>
                <PieChart>
                  <Pie
                    data={popularityChartData}
                    cx="50%" cy="50%"
                    innerRadius={50} outerRadius={78}
                    dataKey="value"
                    paddingAngle={3}
                  >
                    {popularityChartData.map((s, i) => (
                      <Cell key={i} fill={s.color} />
                    ))}
                  </Pie>
                  <Tooltip formatter={(v: number) => `${v.toFixed(1)}%`} contentStyle={{ borderRadius: 8, fontSize: 12 }} />
                </PieChart>
              </ResponsiveContainer>
              <div className="space-y-2 mt-2">
                {popularityChartData.map((s) => (
                  <div key={s.name} className="flex items-center justify-between text-sm">
                    <div className="flex items-center gap-2">
                      <span className="w-2.5 h-2.5 rounded-full" style={{ background: s.color }} />
                      <span className="text-stone-600">{s.name}</span>
                    </div>
                    <span className="font-medium text-stone-800">{s.value.toFixed(1)}%</span>
                  </div>
                ))}
              </div>
            </>
          )}
        </div>
      </div>

      {/* ── Bottom row ── */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">

        {/* Revenue by source bar chart */}
        <div className="card">
          <h3 className="font-display text-lg text-stone-900 mb-5">Revenue by Source</h3>
          {sourceChartData.length === 0 ? (
            <EmptyChart message="No revenue source data available." />
          ) : (
            <ResponsiveContainer width="100%" height={200}>
              <BarChart data={sourceChartData} margin={{ top: 0, right: 0, left: 0, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0ece6" vertical={false} />
                <XAxis dataKey="name" tick={{ fontSize: 11, fill: '#a8a29e' }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fontSize: 10, fill: '#a8a29e' }} axisLine={false} tickLine={false} tickFormatter={(v) => `${(v / 1e6).toFixed(1)}M`} />
                <Tooltip formatter={(v: number) => formatCurrency(v)} contentStyle={{ borderRadius: 8, fontSize: 12 }} />
                <Bar dataKey="amount" fill="#d4a017" radius={[4, 4, 0, 0]} name="Revenue" />
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>

        {/* Customer activity summary */}
        <div className="card">
          <h3 className="font-display text-lg text-stone-900 mb-5">Customer Activity</h3>
          {!customerActivity ? (
            <EmptyChart message="No customer data available." />
          ) : (
            <div className="grid grid-cols-2 gap-4 h-full content-center py-4">
              <CustomerStat label="Total Customers"     value={customerActivity.totalCustomers     ?? 0} color="text-blue-600"   bg="bg-blue-50" />
              <CustomerStat label="New This Period"     value={customerActivity.newCustomers       ?? 0} color="text-green-600"  bg="bg-green-50" />
              <CustomerStat label="Returning"           value={customerActivity.returningCustomers ?? 0} color="text-amber-600"  bg="bg-amber-50" />
              <CustomerStat label="Loyal Customers"     value={customerActivity.loyalCustomers     ?? 0} color="text-purple-600" bg="bg-purple-50" />
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

// ── Small helpers ─────────────────────────────────────────────────────────────

function EmptyChart({ message }: { message: string }) {
  return (
    <div className="flex items-center justify-center h-40 text-stone-400 text-sm">
      {message}
    </div>
  );
}

function CustomerStat({
  label, value, color, bg,
}: {
  label: string; value: number; color: string; bg: string;
}) {
  return (
    <div className={`${bg} rounded-xl p-4 flex flex-col gap-1`}>
      <span className="text-xs text-stone-500">{label}</span>
      <span className={`text-2xl font-semibold ${color}`}>{value.toLocaleString()}</span>
    </div>
  );
}