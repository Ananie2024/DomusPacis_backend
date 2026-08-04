import { ReactNode } from 'react';
import { Search, X } from 'lucide-react';
import { cn } from '@/lib/utils';

export function PageHeader({
  title,
  subtitle,
  actions,
}: {
  title: string;
  subtitle?: string;
  actions?: ReactNode;
}) {
  return (
    <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <h1 className="font-display text-2xl text-stone-900 sm:text-3xl">{title}</h1>
        {subtitle && <p className="mt-1 text-sm text-stone-500">{subtitle}</p>}
      </div>
      {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
    </div>
  );
}

export function PageLoader() {
  return (
    <div className="card flex min-h-48 items-center justify-center">
      <div className="h-8 w-8 animate-spin rounded-full border-2 border-stone-200 border-t-yellow-500" />
    </div>
  );
}

export function KpiTile({
  title,
  value,
  subtitle,
  icon,
  trend,
  change,
  changeLabel,
  accent,
}: {
  title: string;
  value: ReactNode;
  subtitle?: ReactNode;
  icon?: ReactNode;
  trend?: ReactNode;
  change?: number;
  changeLabel?: string;
  accent?: string;
}) {
  const trendContent = trend ?? (
    typeof change === 'number' ? (
      <span className={change >= 0 ? 'text-green-600' : 'text-red-600'}>
        {change >= 0 ? '+' : ''}{change}% {changeLabel}
      </span>
    ) : null
  );

  return (
    <div className="kpi-tile">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-xs font-medium uppercase text-stone-500">{title}</p>
          <div className="mt-2 text-2xl font-semibold text-stone-900">{value}</div>
        </div>
        {icon && <div className={cn('rounded-lg bg-yellow-50 p-2 text-yellow-700', accent)}>{icon}</div>}
      </div>
      {(subtitle || trendContent) && (
        <div className="flex items-center justify-between gap-2 text-xs text-stone-500">
          {subtitle && <span>{subtitle}</span>}
          {trendContent && <span>{trendContent}</span>}
        </div>
      )}
    </div>
  );
}

export function StatusBadge({ status }: { status: string }) {
  const normalized = status.toUpperCase();
  const tone =
    ['ACTIVE', 'CONFIRMED', 'PAID', 'COMPLETED', 'PROCESSED', 'FILED'].includes(normalized)
      ? 'bg-green-100 text-green-700 border-green-200'
      : ['PENDING', 'PARTIAL', 'CHECKED_IN'].includes(normalized)
        ? 'bg-amber-100 text-amber-700 border-amber-200'
        : ['INACTIVE', 'CANCELLED', 'UNPAID', 'OVERDUE'].includes(normalized)
          ? 'bg-red-100 text-red-700 border-red-200'
          : 'bg-stone-100 text-stone-700 border-stone-200';

  return <span className={cn('badge', tone)}>{normalized.replace(/_/g, ' ')}</span>;
}

export function Pagination({
  page,
  totalPages,
  onPageChange,
}: {
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}) {
  if (totalPages <= 1) return null;

  return (
    <div className="flex items-center justify-between gap-3">
      <button
        className="btn-secondary px-3 py-2 disabled:cursor-not-allowed disabled:opacity-50"
        disabled={page <= 0}
        onClick={() => onPageChange(Math.max(0, page - 1))}
      >
        Previous
      </button>
      <span className="text-sm text-stone-500">
        Page {page + 1} of {totalPages}
      </span>
      <button
        className="btn-secondary px-3 py-2 disabled:cursor-not-allowed disabled:opacity-50"
        disabled={page >= totalPages - 1}
        onClick={() => onPageChange(Math.min(totalPages - 1, page + 1))}
      >
        Next
      </button>
    </div>
  );
}

export function SearchBar({
  value,
  onChange,
  placeholder = 'Search...',
}: {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
}) {
  return (
    <div className="relative min-w-64 flex-1">
      <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-stone-400" />
      <input
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
        className="input pl-9 pr-9"
      />
      {value && (
        <button
          type="button"
          onClick={() => onChange('')}
          className="absolute right-3 top-1/2 -translate-y-1/2 text-stone-400 hover:text-stone-600"
          aria-label="Clear search"
        >
          <X size={14} />
        </button>
      )}
    </div>
  );
}

export function EmptyState({
  icon,
  title,
  description,
  action,
}: {
  icon?: ReactNode;
  title: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <div className="flex flex-col items-center justify-center px-4 py-12 text-center">
      {icon && <div className="mb-3 text-stone-300">{icon}</div>}
      <h3 className="text-base font-semibold text-stone-800">{title}</h3>
      {description && <p className="mt-1 max-w-md text-sm text-stone-500">{description}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}

export function Modal({
  open,
  onClose,
  title,
  children,
}: {
  open: boolean;
  onClose: () => void;
  title: string;
  children: ReactNode;
}) {
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-stone-950/50 p-4">
      <div className="w-full max-w-2xl rounded-2xl bg-white p-6 shadow-xl">
        <div className="mb-5 flex items-center justify-between gap-3">
          <h2 className="font-display text-xl text-stone-900">{title}</h2>
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg p-2 text-stone-400 hover:bg-stone-100 hover:text-stone-700"
            aria-label="Close modal"
          >
            <X size={18} />
          </button>
        </div>
        <div className="max-h-[75vh] overflow-y-auto pr-1">{children}</div>
      </div>
    </div>
  );
}
