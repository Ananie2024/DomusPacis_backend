'use client';
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { bookingApi, BookingResponse, BookingStatus } from '@/lib/api/bookingApi';
import { formatCurrency, formatDate } from '@/lib/utils';
import {
  PageHeader, StatusBadge, Pagination, PageLoader,
  SearchBar, EmptyState, Modal,
} from '@/components/ui';
import {
  CalendarDays, Plus, Eye, CheckCircle,
  XCircle, LogIn, Flag, CalendarClock, Trash2,
} from 'lucide-react';
import toast from 'react-hot-toast';
import Link from 'next/link';

const STATUS_OPTIONS: (BookingStatus | 'All')[] = [
  'All', 'PENDING', 'CONFIRMED', 'CHECKED_IN', 'COMPLETED', 'CANCELLED',
];

export default function BookingsPage() {
  const [page,     setPage]     = useState(0);
  const [status,   setStatus]   = useState<BookingStatus | 'All'>('All');
  const [selected, setSelected] = useState<string | null>(null);
  const [overrideOpen, setOverrideOpen] = useState<string | null>(null);
  const [overrideCheckIn,  setOverrideCheckIn]  = useState('');
  const [overrideCheckOut, setOverrideCheckOut] = useState('');

  const qc = useQueryClient();

  // ── List — filter by status when not 'All' ────────────────────────────────
  const { data, isLoading } = useQuery({
    queryKey: ['bookings', page, status],
    queryFn: () =>
      status === 'All'
        ? bookingApi.listAll({ page, size: 15, sort: 'createdAt,desc' })
        : bookingApi.listByStatus(status, { page, size: 15 }),
  });

  // ── Detail ─────────────────────────────────────────────────────────────────
  const { data: detail } = useQuery({
    queryKey: ['booking', selected],
    queryFn: () => bookingApi.getById(selected!),
    enabled: !!selected,
  });

  // ── Mutations ──────────────────────────────────────────────────────────────
  const confirmBooking = useMutation({
    mutationFn: (id: string) => bookingApi.confirm(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['bookings'] }); toast.success('Booking confirmed'); },
    onError:   () => toast.error('Failed to confirm booking'),
  });

  const checkIn = useMutation({
    mutationFn: (id: string) => bookingApi.checkIn(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['bookings'] }); toast.success('Guest checked in'); },
    onError:   () => toast.error('Failed to check in'),
  });

  const complete = useMutation({
    mutationFn: (id: string) => bookingApi.complete(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['bookings'] }); toast.success('Booking completed'); },
    onError:   () => toast.error('Failed to complete booking'),
  });

  const cancel = useMutation({
    mutationFn: (id: string) => bookingApi.cancel(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['bookings'] }); toast.success('Booking cancelled'); },
    onError:   () => toast.error('Failed to cancel booking'),
  });

  const remove = useMutation({
    mutationFn: (id: string) => bookingApi.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['bookings'] });
      toast.success('Booking deleted');
    },
    onError: (err: unknown) => {
      const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      toast.error(message ?? 'Failed to delete booking');
    },
  });

  const overrideDates = useMutation({
    mutationFn: ({ id, checkIn, checkOut }: { id: string; checkIn: string; checkOut: string }) =>
      bookingApi.overrideDates(id, checkIn, checkOut),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['bookings'] });
      toast.success('Dates updated');
      setOverrideOpen(null);
    },
    onError: () => toast.error('Failed to update dates'),
  });

  return (
    <div className="max-w-7xl mx-auto">
      <PageHeader
        title="Booking Management"
        subtitle="View, confirm and manage all reservations"
        actions={
          <Link href="/booking" target="_blank" className="btn-primary">
            <Plus size={15} /> New Booking
          </Link>
        }
      />

      {/* ── Filters ─────────────────────────────────────────────────────────── */}
      <div className="card mb-5 flex flex-wrap items-center gap-3">
        <div className="flex flex-wrap gap-2">
          {STATUS_OPTIONS.map((s) => (
            <button
              key={s}
              onClick={() => { setStatus(s); setPage(0); }}
              className={`px-3 py-1.5 rounded-full text-xs font-medium border transition-all ${
                status === s
                  ? 'bg-gold-600 text-white border-gold-600'
                  : 'bg-white text-stone-600 border-stone-200 hover:border-gold-300'
              }`}
            >
              {s.replace(/_/g, ' ')}
            </button>
          ))}
        </div>
      </div>

      {/* ── Table ───────────────────────────────────────────────────────────── */}
      {isLoading ? <PageLoader /> : (
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Reference</th>
                <th>Guest</th>
                <th>Asset</th>
                <th>Check-in</th>
                <th>Check-out</th>
                <th>Amount</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {data?.content.length === 0 ? (
                <tr>
                  <td colSpan={8}>
                    <EmptyState
                      icon={<CalendarDays size={40} />}
                      title="No bookings found"
                      description="Try a different status filter."
                    />
                  </td>
                </tr>
              ) : (
                data?.content.map((b) => (
                  <tr key={b.id}>
                    <td className="font-mono text-xs text-stone-600">{b.id}</td>
                    <td>
                      <div className="font-medium text-stone-900">{b.customerName}</div>
                    </td>
                    <td className="text-stone-700">{b.serviceAssetName}</td>
                    <td>{formatDate(b.checkInDate)}</td>
                    <td>{formatDate(b.checkOutDate)}</td>
                    <td className="font-medium">{formatCurrency(b.totalAmount)}</td>
                    <td><StatusBadge status={b.status} /></td>
                    <td>
                      <div className="flex items-center gap-1">

                        {/* View */}
                        <button
                          onClick={() => setSelected(b.id)}
                          className="p-1.5 rounded-lg hover:bg-stone-100 text-stone-400 transition-colors"
                          title="View details"
                        >
                          <Eye size={14} />
                        </button>

                        {/* Confirm — PENDING only */}
                        {b.status === 'PENDING' && (
                          <button
                            onClick={() => confirmBooking.mutate(b.id)}
                            className="p-1.5 rounded-lg hover:bg-green-50 text-green-600 transition-colors"
                            title="Confirm"
                          >
                            <CheckCircle size={14} />
                          </button>
                        )}

                        {/* Check-in — CONFIRMED only */}
                        {b.status === 'CONFIRMED' && (
                          <button
                            onClick={() => checkIn.mutate(b.id)}
                            className="p-1.5 rounded-lg hover:bg-blue-50 text-blue-600 transition-colors"
                            title="Check in"
                          >
                            <LogIn size={14} />
                          </button>
                        )}

                        {/* Complete — CHECKED_IN only */}
                        {b.status === 'CHECKED_IN' && (
                          <button
                            onClick={() => complete.mutate(b.id)}
                            className="p-1.5 rounded-lg hover:bg-purple-50 text-purple-600 transition-colors"
                            title="Complete"
                          >
                            <Flag size={14} />
                          </button>
                        )}

                        {/* Override dates — PENDING or CONFIRMED */}
                        {(b.status === 'PENDING' || b.status === 'CONFIRMED') && (
                          <button
                            onClick={() => {
                              setOverrideCheckIn(b.checkInDate);
                              setOverrideCheckOut(b.checkOutDate);
                              setOverrideOpen(b.id);
                            }}
                            className="p-1.5 rounded-lg hover:bg-amber-50 text-amber-600 transition-colors"
                            title="Override dates"
                          >
                            <CalendarClock size={14} />
                          </button>
                        )}

                        {/* Cancel — PENDING or CONFIRMED */}
                        {(b.status === 'PENDING' || b.status === 'CONFIRMED') && (
                          <button
                            onClick={() => { if (window.confirm(`Cancel booking ${b.id}?`)) cancel.mutate(b.id); }}
                            className="p-1.5 rounded-lg hover:bg-red-50 text-red-500 transition-colors"
                            title="Cancel"
                          >
                            <XCircle size={14} />
                          </button>
                        )}

                        {/* Delete — admin only */}
                        <button
                          onClick={() => {
                            if (window.confirm(`Delete booking ${b.id}? This action cannot be undone.`)) {
                              remove.mutate(b.id);
                            }
                          }}
                          className="p-1.5 rounded-lg hover:bg-red-50 text-stone-400 hover:text-red-600 transition-colors"
                          title="Delete booking"
                        >
                          <Trash2 size={14} />
                        </button>

                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
          {data && (
            <div className="p-4 border-t border-stone-100">
              <Pagination page={page} totalPages={data.totalPages} onPageChange={setPage} />
            </div>
          )}
        </div>
      )}

      {/* ── Detail modal ─────────────────────────────────────────────────────── */}
      <Modal open={!!selected} onClose={() => setSelected(null)} title="Booking Details">
        {detail ? (
          <div className="space-y-4 text-sm">
            <div className="grid grid-cols-2 gap-3">
              {([
                ['Reference',     detail.id],
                ['Status',        detail.status],
                ['Guest',         detail.customerName],
                ['Asset',         detail.serviceAssetName],
                ['Check-in',      formatDate(detail.checkInDate)],
                ['Check-out',     formatDate(detail.checkOutDate)],
                ['Guests',        detail.numberOfGuests],
                ['Total',         formatCurrency(detail.totalAmount)],
              ] as [string, string | number][]).map(([label, value]) => (
                <div key={label}>
                  <div className="text-stone-400 text-xs">{label}</div>
                  <div className="font-medium text-stone-800 mt-0.5">
                    {label === 'Status'
                      ? <StatusBadge status={String(value)} />
                      : String(value)}
                  </div>
                </div>
              ))}
            </div>
            {detail.specialRequests && (
              <div className="bg-stone-50 rounded-xl p-3">
                <div className="text-stone-400 text-xs mb-1">Special Requests</div>
                <p className="text-stone-700">{detail.specialRequests}</p>
              </div>
            )}
          </div>
        ) : <PageLoader />}
      </Modal>

      {/* ── Override dates modal ─────────────────────────────────────────────── */}
      <Modal open={!!overrideOpen} onClose={() => setOverrideOpen(null)} title="Override Booking Dates">
        <div className="space-y-4">
          <div>
            <label className="label">New Check-in Date</label>
            <input
              type="date"
              value={overrideCheckIn}
              onChange={e => setOverrideCheckIn(e.target.value)}
              className="input"
            />
          </div>
          <div>
            <label className="label">New Check-out Date</label>
            <input
              type="date"
              value={overrideCheckOut}
              onChange={e => setOverrideCheckOut(e.target.value)}
              className="input"
            />
          </div>
          <div className="flex gap-3 pt-2">
            <button onClick={() => setOverrideOpen(null)} className="btn-secondary flex-1 justify-center">
              Cancel
            </button>
            <button
              onClick={() => overrideOpen && overrideDates.mutate({
                id: overrideOpen,
                checkIn: overrideCheckIn,
                checkOut: overrideCheckOut,
              })}
              disabled={overrideDates.isPending}
              className="btn-primary flex-1 justify-center"
            >
              {overrideDates.isPending ? 'Saving…' : 'Update Dates'}
            </button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
