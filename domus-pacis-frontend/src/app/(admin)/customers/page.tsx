'use client';
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  customerApi,
  CustomerResponse,
  CreateCustomerRequest,
  UpdateCustomerRequest,
} from '@/lib/api/customerApi';
import { formatCurrency, formatDate, getInitialsFromFullName, formatAddress } from '@/lib/utils';
import { PageHeader, Pagination, PageLoader, SearchBar, EmptyState, Modal } from '@/components/ui';
import { Users, Plus, Eye, Edit, Trash2 } from 'lucide-react';
import toast from 'react-hot-toast';

// ── Zod schema aligned with CreateCustomerRequest / UpdateCustomerRequest ─────
const customerSchema = z.object({
  fullName:     z.string().min(2, 'Full name required'),
  email:        z.string().email('Valid email required'),
  phone:        z.string().min(10, 'Valid phone required'),
  address:      z.any().optional(),
  nationality:  z.string().optional(),
  idNumber:     z.string().optional(),
});

type CustomerForm = z.infer<typeof customerSchema>;

export default function CustomersPage() {
  const [page,      setPage]      = useState(0);
  const [search,    setSearch]    = useState('');
  const [createOpen,setCreateOpen]= useState(false);
  const [editTarget,setEditTarget]= useState<CustomerResponse | null>(null);
  const [viewTarget,setViewTarget]= useState<CustomerResponse | null>(null);

  const qc = useQueryClient();

  // ── List — use search endpoint when query present ─────────────────────────
  const { data, isLoading } = useQuery({
    queryKey: ['customers', page, search],
    queryFn: () =>
      search
        ? customerApi.search(search, { page, size: 15 })
        : customerApi.listAll({ page, size: 15 }),
  });

  // ── Create form ───────────────────────────────────────────────────────────
  const createForm = useForm<CustomerForm>({ resolver: zodResolver(customerSchema) });

  // ── Edit form ─────────────────────────────────────────────────────────────
  const editForm = useForm<CustomerForm>({ resolver: zodResolver(customerSchema) });

  const openEdit = (c: CustomerResponse) => {
    setEditTarget(c);
    editForm.reset({
      fullName:    c.fullName,
      email:       c.email,
      phone:       c.phone,
      address:     c.address ?? undefined,
      nationality: c.nationality ?? '',
      idNumber:    c.idNumber   ?? '',
    });
  };

  // ── Mutations ─────────────────────────────────────────────────────────────
  const create = useMutation({
    mutationFn: (req: CreateCustomerRequest) => customerApi.createCustomer(req),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['customers'] });
      toast.success('Customer created');
      setCreateOpen(false);
      createForm.reset();
    },
    onError: () => toast.error('Failed to create customer'),
  });

  const update = useMutation({
    mutationFn: ({ id, req }: { id: string; req: UpdateCustomerRequest }) =>
      customerApi.updateCustomer(id, req),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['customers'] });
      toast.success('Customer updated');
      setEditTarget(null);
    },
    onError: () => toast.error('Failed to update customer'),
  });

  const remove = useMutation({
    mutationFn: (id: string) => customerApi.deleteCustomer(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['customers'] });
      toast.success('Customer deleted');
    },
    onError: (err: unknown) => {
      const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      toast.error(message ?? 'Failed to delete customer');
    },
  });

  // ── Shared form fields renderer ───────────────────────────────────────────
  const renderFields = (form: typeof createForm) => (
    <>
      <div>
        <label className="label">Full Name</label>
        <input {...form.register('fullName')} className="input" placeholder="Jane Doe" />
        {form.formState.errors.fullName && (
          <p className="form-error">{form.formState.errors.fullName.message}</p>
        )}
      </div>
      <div>
        <label className="label">Email</label>
        <input type="email" {...form.register('email')} className="input" />
        {form.formState.errors.email && (
          <p className="form-error">{form.formState.errors.email.message}</p>
        )}
      </div>
      <div>
        <label className="label">Phone</label>
        <input type="tel" {...form.register('phone')} className="input" placeholder="+250 78 000 0000" />
        {form.formState.errors.phone && (
          <p className="form-error">{form.formState.errors.phone.message}</p>
        )}
      </div>
      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="label">Nationality</label>
          <input {...form.register('nationality')} className="input" />
        </div>
        <div>
          <label className="label">ID / Passport Number</label>
          <input {...form.register('idNumber')} className="input" />
        </div>
      </div>
      <div>
        <label className="label">Address</label>
        <input {...form.register('address')} className="input" placeholder="Street, City, Country" />
      </div>
    </>
  );

  return (
    <div className="max-w-7xl mx-auto">
      <PageHeader
        title="Customer Management"
        subtitle="View and manage all guest profiles"
        actions={
          <button onClick={() => setCreateOpen(true)} className="btn-primary">
            <Plus size={15} /> Add Customer
          </button>
        }
      />

      {/* ── Search bar ──────────────────────────────────────────────────────── */}
      <div className="card mb-5">
        <SearchBar
          value={search}
          onChange={v => { setSearch(v); setPage(0); }}
          placeholder="Search by name, email or phone…"
        />
      </div>

      {/* ── Table ───────────────────────────────────────────────────────────── */}
      {isLoading ? <PageLoader /> : (
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Customer</th>
                <th>Phone</th>
                <th>Nationality</th>
                <th>Bookings</th>
                <th>Total Spent</th>
                <th>Since</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {data?.content.length === 0 ? (
                <tr>
                  <td colSpan={7}>
                    <EmptyState icon={<Users size={40} />} title="No customers found" />
                  </td>
                </tr>
              ) : (
                data?.content.map((c) => (
                  <tr key={c.id}>
                    <td>
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-full bg-gold-100 text-gold-700 flex items-center justify-center text-xs font-bold flex-shrink-0">
                          {getInitialsFromFullName(c.fullName)}
                        </div>
                        <div>
                          <div className="font-medium text-stone-900">
                            {c.fullName}
                          </div>
                          <div className="text-xs text-stone-400">{c.email}</div>
                        </div>
                      </div>
                    </td>
                    <td className="text-stone-600">{c.phone}</td>
                    <td className="text-stone-600">{c.nationality ?? '—'}</td>
                    <td className="font-medium">{c.totalBookings ?? 0}</td>
                    <td className="font-medium">{formatCurrency(c.totalSpent ?? 0)}</td>
                    <td className="text-stone-500">{formatDate(c.createdAt)}</td>
                    <td>
                      <div className="flex items-center gap-1">
                        <button
                          onClick={() => setViewTarget(c)}
                          className="p-1.5 rounded-lg hover:bg-stone-100 text-stone-400 transition-colors"
                          title="View"
                        >
                          <Eye size={14} />
                        </button>
                        <button
                          onClick={() => openEdit(c)}
                          className="p-1.5 rounded-lg hover:bg-blue-50 text-stone-400 hover:text-blue-600 transition-colors"
                          title="Edit"
                        >
                          <Edit size={14} />
                        </button>
                        <button
                          onClick={() => { if (confirm('Delete this customer?')) remove.mutate(c.id); }}
                          className="p-1.5 rounded-lg hover:bg-red-50 text-stone-400 hover:text-red-500 transition-colors"
                          title="Delete"
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

      {/* ── Create modal ─────────────────────────────────────────────────────── */}
      <Modal
        open={createOpen}
        onClose={() => { setCreateOpen(false); createForm.reset(); }}
        title="Add New Customer"
      >
        <form
          onSubmit={createForm.handleSubmit(d => create.mutate(d))}
          className="space-y-4"
        >
          {renderFields(createForm)}
          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={() => { setCreateOpen(false); createForm.reset(); }}
              className="btn-secondary flex-1 justify-center"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={createForm.formState.isSubmitting}
              className="btn-primary flex-1 justify-center"
            >
              {createForm.formState.isSubmitting ? 'Saving…' : 'Save Customer'}
            </button>
          </div>
        </form>
      </Modal>

      {/* ── Edit modal ───────────────────────────────────────────────────────── */}
      <Modal
        open={!!editTarget}
        onClose={() => setEditTarget(null)}
        title={`Edit — ${editTarget?.fullName ?? ''}`}
      >
        <form
          onSubmit={editForm.handleSubmit(d =>
            editTarget && update.mutate({ id: editTarget.id, req: d })
          )}
          className="space-y-4"
        >
          {renderFields(editForm)}
          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={() => setEditTarget(null)}
              className="btn-secondary flex-1 justify-center"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={update.isPending}
              className="btn-primary flex-1 justify-center"
            >
              {update.isPending ? 'Saving…' : 'Save Changes'}
            </button>
          </div>
        </form>
      </Modal>

      {/* ── View modal ───────────────────────────────────────────────────────── */}
      <Modal
        open={!!viewTarget}
        onClose={() => setViewTarget(null)}
        title="Customer Profile"
      >
        {viewTarget && (
          <div className="space-y-4 text-sm">
            <div className="flex items-center gap-4 mb-2">
              <div className="w-12 h-12 rounded-full bg-gold-100 text-gold-700 flex items-center justify-center text-base font-bold">
                {getInitialsFromFullName(viewTarget.fullName)}
              </div>
              <div>
                <div className="font-display text-lg text-stone-900">
                  {viewTarget.fullName}
                </div>
                <div className="text-stone-400 text-xs">{viewTarget.email}</div>
              </div>
            </div>
            <div className="gold-divider" />
            <div className="grid grid-cols-2 gap-4">
              {([
                ['Phone',        viewTarget.phone],
                ['Nationality',  viewTarget.nationality ?? '—'],
                ['ID Number',    viewTarget.idNumber    ?? '—'],
                ['Address',      formatAddress(viewTarget.address)],
                ['Bookings',     viewTarget.totalBookings ?? 0],
                ['Total Spent',  formatCurrency(viewTarget.totalSpent ?? 0)],
                ['Member Since', formatDate(viewTarget.createdAt)],
              ] as [string, string | number][]).map(([label, value]) => (
                <div key={label}>
                  <div className="text-stone-400 text-xs mb-0.5">{label}</div>
                  <div className="font-medium text-stone-800">{String(value)}</div>
                </div>
              ))}
            </div>
            <div className="flex gap-2 pt-2">
              <button
                onClick={() => { setViewTarget(null); openEdit(viewTarget); }}
                className="btn-secondary flex-1 justify-center text-sm"
              >
                <Edit size={13} /> Edit
              </button>
              <button
                onClick={() => {
                  if (confirm('Delete this customer?')) {
                    remove.mutate(viewTarget.id);
                    setViewTarget(null);
                  }
                }}
                className="btn-danger flex-1 justify-center text-sm"
              >
                <Trash2 size={13} /> Delete
              </button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
