'use client';
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { usersApi, AdminUser, CreateUserRequest } from '@/lib/api/usersApi';
import { UserRole } from '@/lib/types';
import { formatDate, formatDateRelative, getInitials, cn } from '@/lib/utils';
import {
  PageHeader, Pagination, PageLoader,
  SearchBar, EmptyState, Modal, StatusBadge,
} from '@/components/ui';
import {
  ShieldCheck, Plus, Edit, Trash2, Eye, EyeOff,
  RotateCcw, UserX, UserCheck, KeyRound, Copy, Check,
} from 'lucide-react';
import toast from 'react-hot-toast';

// ─── Zod schema ───────────────────────────────────────────────────────────────
const createUserSchema = z.object({
  firstName:        z.string().min(2, 'First name required'),
  lastName:         z.string().min(2, 'Last name required'),
  email:            z.string().email('Valid email required'),
  phone:            z.string().optional(),
  role:             z.nativeEnum(UserRole),
  password:         z.string().min(8, 'Min 8 characters')
                              .regex(/[A-Z]/, 'Must contain uppercase')
                              .regex(/[0-9]/, 'Must contain a number'),
  confirmPassword:  z.string(),
  sendWelcomeEmail: z.boolean().optional(),
}).refine(d => d.password === d.confirmPassword, {
  message: 'Passwords do not match',
  path: ['confirmPassword'],
});

const editUserSchema = z.object({
  firstName: z.string().min(2),
  lastName:  z.string().min(2),
  email:     z.string().email(),
  phone:     z.string().optional(),
  role:      z.nativeEnum(UserRole),
});

type CreateForm = z.infer<typeof createUserSchema>;
type EditForm   = z.infer<typeof editUserSchema>;

// ─── Role config ──────────────────────────────────────────────────────────────
const ROLE_META: Record<UserRole, { label: string; color: string; description: string }> = {
  [UserRole.ADMIN]:    { label: 'Admin',    color: 'bg-red-100 text-red-700 border-red-200',       description: 'Full system access'         },
  [UserRole.MANAGER]:  { label: 'Manager',  color: 'bg-purple-100 text-purple-700 border-purple-200', description: 'Operations & staff management' },
  [UserRole.FINANCE]:  { label: 'Finance',  color: 'bg-blue-100 text-blue-700 border-blue-200',    description: 'Financial & tax modules'    },
  [UserRole.STAFF]:    { label: 'Staff',    color: 'bg-teal-100 text-teal-700 border-teal-200',    description: 'Bookings & front desk'      },
  [UserRole.CUSTOMER]: { label: 'Customer', color: 'bg-stone-100 text-stone-600 border-stone-200', description: 'Public booking access'      },
};

const ALL_ROLES  = Object.values(UserRole);
const STAFF_ROLES = ALL_ROLES.filter(r => r !== UserRole.CUSTOMER);



// ─── Sub-components ───────────────────────────────────────────────────────────
function RoleBadge({ role }: { role: UserRole }) {
  const m = ROLE_META[role];
  return (
    <span className={cn('badge text-xs', m.color)}>
      {m.label}
    </span>
  );
}

function UserAvatar({ user, size = 'md' }: { user: AdminUser; size?: 'sm' | 'md' | 'lg' }) {
  const sz = { sm: 'w-7 h-7 text-[10px]', md: 'w-9 h-9 text-xs', lg: 'w-12 h-12 text-sm' }[size];
  const bg = user.role === UserRole.ADMIN    ? 'bg-red-600' :
             user.role === UserRole.MANAGER  ? 'bg-purple-600' :
             user.role === UserRole.FINANCE  ? 'bg-blue-600' :
             user.role === UserRole.STAFF    ? 'bg-teal-600' :
                                               'bg-stone-500';
  return (
    <div className={cn('rounded-full flex items-center justify-center text-white font-bold flex-shrink-0', sz, bg)}>
      {getInitials(user.firstName, user.lastName)}
    </div>
  );
}

// ─── Temporary password copy helper ──────────────────────────────────────────
function TempPasswordDisplay({ password }: { password: string }) {
  const [copied, setCopied] = useState(false);
  const copy = () => {
    navigator.clipboard.writeText(password);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };
  return (
    <div className="mt-4 p-4 bg-amber-50 border border-amber-200 rounded-xl">
      <p className="text-amber-800 text-xs font-medium mb-2">Temporary password generated:</p>
      <div className="flex items-center gap-2">
        <code className="flex-1 bg-white border border-amber-200 rounded-lg px-3 py-2 text-sm font-mono text-stone-800">
          {password}
        </code>
        <button onClick={copy} className="p-2 rounded-lg bg-amber-100 hover:bg-amber-200 text-amber-700 transition-colors">
          {copied ? <Check size={14} /> : <Copy size={14} />}
        </button>
      </div>
      <p className="text-amber-600 text-xs mt-2">Share this securely — it will only be shown once.</p>
    </div>
  );
}

// ─── Main page ────────────────────────────────────────────────────────────────
export default function UsersPage() {
  const [page,        setPage]        = useState(0);
  const [search,      setSearch]      = useState('');
  const [roleFilter,  setRoleFilter]  = useState<UserRole | ''>('');
  const [activeFilter,setActiveFilter]= useState<'all' | 'active' | 'inactive'>('all');
  const [createOpen,  setCreateOpen]  = useState(false);
  const [editTarget,  setEditTarget]  = useState<AdminUser | null>(null);
  const [viewTarget,  setViewTarget]  = useState<AdminUser | null>(null);
  const [showPwd,     setShowPwd]     = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [tempPwd,     setTempPwd]     = useState<string | null>(null);
  const [deleteTarget,setDeleteTarget]= useState<AdminUser | null>(null);

  const qc = useQueryClient();

  // Fetch from live database
  const { data, isLoading, isError } = useQuery({
    queryKey: ['users', page, search, roleFilter, activeFilter],
    queryFn: () => usersApi.getUsers({
      page, size: 12,
      search:   search    || undefined,
      role:     roleFilter || undefined,
      isActive: activeFilter === 'all' ? undefined : activeFilter === 'active',
    }),
  });

  // ── Mutations ────────────────────────────────────────────────────────────
  const createMutation = useMutation({
    mutationFn: (p: CreateUserRequest) => usersApi.createUser(p),
    onSuccess: (user) => {
      qc.invalidateQueries({ queryKey: ['users'] });
      toast.success(`User ${user.firstName} ${user.lastName} created`);
      setCreateOpen(false);
      createForm.reset();
    },
    onError: () => toast.error('Failed to create user — check API connection'),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: EditForm }) =>
      usersApi.updateUser(id, payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['users'] });
      toast.success('User updated');
      setEditTarget(null);
    },
    onError: () => toast.error('Failed to update user'),
  });

  const toggleActiveMutation = useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) =>
      active ? usersApi.deactivateUser(id) : usersApi.activateUser(id),
    onSuccess: (_, { active }) => {
      qc.invalidateQueries({ queryKey: ['users'] });
      toast.success(active ? 'User deactivated' : 'User activated');
    },
    onError: () => toast.error('Failed to update user status'),
  });

  const resetPwdMutation = useMutation({
    mutationFn: (user: AdminUser) => usersApi.resetPassword(user.id, user.email),
    onSuccess: (res) => {
      setTempPwd(res.temporaryPassword);
      toast.success('Password reset email sent to user');
    },
    onError: () => toast.error('Failed to reset password'),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => usersApi.deleteUser(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['users'] });
      toast.success('User deleted');
      setDeleteTarget(null);
    },
    onError: () => toast.error('Failed to delete user'),
  });

  // ── Forms ────────────────────────────────────────────────────────────────
  const createForm = useForm<CreateForm>({
    resolver: zodResolver(createUserSchema),
    defaultValues: { role: UserRole.STAFF, sendWelcomeEmail: true },
  });

  const editForm = useForm<EditForm>({
    resolver: zodResolver(editUserSchema),
  });

  const openEdit = (user: AdminUser) => {
    setEditTarget(user);
    editForm.reset({
      firstName: user.firstName,
      lastName:  user.lastName,
      email:     user.email,
      phone:     user.phone ?? '',
      role:      user.role,
    });
  };

  // ── Derived stats ────────────────────────────────────────────────────────
  const users  = data?.content ?? [];
  const counts = ALL_ROLES.reduce((acc, r) => {
    acc[r] = users.filter(u => u.role === r).length;
    return acc;
  }, {} as Record<UserRole, number>);
  const activeCount   = users.filter(u => u.isActive).length;
  const inactiveCount = users.filter(u => !u.isActive).length;

  return (
    <div className="max-w-7xl mx-auto">
      <PageHeader
        title="User Management"
        subtitle="Create and manage admin, staff and customer accounts"
        actions={
          <button onClick={() => setCreateOpen(true)} className="btn-primary">
            <Plus size={15} /> New User
          </button>
        }
      />

      {/* ── Stats strip ─────────────────────────────────────────────────── */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3 mb-6">
        {ALL_ROLES.map(role => {
          const m = ROLE_META[role];
          return (
            <button
              key={role}
              onClick={() => setRoleFilter(roleFilter === role ? '' : role)}
              className={cn(
                'card py-3 px-4 text-left transition-all duration-150 hover:shadow-card-hover',
                roleFilter === role && 'ring-2 ring-gold-400'
              )}
            >
              <div className="font-display text-2xl text-stone-900">{counts[role]}</div>
              <span className={cn('badge mt-1 text-[10px]', m.color)}>{m.label}</span>
            </button>
          );
        })}
      </div>

      {/* ── Filters ─────────────────────────────────────────────────────── */}
      <div className="card mb-5 flex flex-wrap items-center gap-3">
        <SearchBar
          value={search}
          onChange={v => { setSearch(v); setPage(0); }}
          placeholder="Search name or email…"
        />

        <select
          value={roleFilter}
          onChange={e => { setRoleFilter(e.target.value as UserRole | ''); setPage(0); }}
          className="input w-40"
        >
          <option value="">All Roles</option>
          {ALL_ROLES.map(r => (
            <option key={r} value={r}>{ROLE_META[r].label}</option>
          ))}
        </select>

        <div className="flex gap-1 bg-stone-100 p-1 rounded-lg">
          {(['all', 'active', 'inactive'] as const).map(f => (
            <button
              key={f}
              onClick={() => { setActiveFilter(f); setPage(0); }}
              className={cn(
                'px-3 py-1.5 rounded-md text-xs font-medium capitalize transition-all',
                activeFilter === f
                  ? 'bg-white text-stone-900 shadow-sm'
                  : 'text-stone-500 hover:text-stone-700'
              )}
            >
              {f === 'all' ? `All (${users.length})` :
               f === 'active' ? `Active (${activeCount})` :
               `Inactive (${inactiveCount})`}
            </button>
          ))}
        </div>
      </div>

      {/* ── Table ───────────────────────────────────────────────────────── */}
      {isLoading ? <PageLoader /> : isError ? (
        <div className="card text-center py-16">
          <p className="text-red-500 font-medium mb-2">Could not load users</p>
          <p className="text-stone-400 text-sm">Ensure the backend service is reachable and you are logged in with administrator privileges.</p>
        </div>
      ) : (
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>User</th>
                <th>Role</th>
                <th>Phone</th>
                <th>Status</th>
                <th>Last Login</th>
                <th>Created</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.length === 0 ? (
                <tr>
                  <td colSpan={7}>
                    <EmptyState
                      icon={<ShieldCheck size={40} />}
                      title="No users found"
                      description="Try adjusting your filters or create a new user."
                      action={
                        <button onClick={() => setCreateOpen(true)} className="btn-primary">
                          <Plus size={14} /> Create User
                        </button>
                      }
                    />
                  </td>
                </tr>
              ) : (
                users.map((user) => (
                  <tr key={user.id} className={cn(!user.isActive && 'opacity-60')}>
                    <td>
                      <div className="flex items-center gap-3">
                        <UserAvatar user={user} />
                        <div>
                          <div className="font-medium text-stone-900 leading-tight">
                            {user.firstName} {user.lastName}
                          </div>
                          <div className="text-xs text-stone-400">{user.email}</div>
                        </div>
                      </div>
                    </td>
                    <td><RoleBadge role={user.role} /></td>
                    <td className="text-stone-500 text-sm">{user.phone ?? '—'}</td>
                    <td>
                      <StatusBadge status={user.isActive ? 'ACTIVE' : 'INACTIVE'} />
                    </td>
                    <td className="text-stone-500 text-sm">
                      {user.lastLoginAt ? formatDateRelative(user.lastLoginAt) : 'Never'}
                    </td>
                    <td className="text-stone-500 text-sm">{formatDate(user.createdAt)}</td>
                    <td>
                      <div className="flex items-center gap-1">
                        {/* View */}
                        <button
                          onClick={() => setViewTarget(user)}
                          title="View details"
                          className="p-1.5 rounded-lg hover:bg-stone-100 text-stone-400 hover:text-stone-700 transition-colors"
                        >
                          <Eye size={14} />
                        </button>
                        {/* Edit */}
                        <button
                          onClick={() => openEdit(user)}
                          title="Edit user"
                          className="p-1.5 rounded-lg hover:bg-blue-50 text-stone-400 hover:text-blue-600 transition-colors"
                        >
                          <Edit size={14} />
                        </button>
                        {/* Toggle active */}
                        <button
                          onClick={() => toggleActiveMutation.mutate({ id: user.id, active: user.isActive })}
                          title={user.isActive ? 'Deactivate' : 'Activate'}
                          className={cn(
                            'p-1.5 rounded-lg transition-colors',
                            user.isActive
                              ? 'hover:bg-amber-50 text-stone-400 hover:text-amber-600'
                              : 'hover:bg-green-50 text-stone-400 hover:text-green-600'
                          )}
                        >
                          {user.isActive ? <UserX size={14} /> : <UserCheck size={14} />}
                        </button>
                        {/* Reset password */}
                        <button
                          onClick={() => { setViewTarget(user); resetPwdMutation.mutate(user); }}
                          title="Reset password"
                          className="p-1.5 rounded-lg hover:bg-purple-50 text-stone-400 hover:text-purple-600 transition-colors"
                        >
                          <KeyRound size={14} />
                        </button>
                        {/* Delete */}
                        <button
                          onClick={() => setDeleteTarget(user)}
                          title="Delete user"
                          className="p-1.5 rounded-lg hover:bg-red-50 text-stone-400 hover:text-red-500 transition-colors"
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

          CREATE USER MODAL
     {/* ══════════════════════════════════════════════════════════════════ */}
      <Modal
        open={createOpen}
        onClose={() => { setCreateOpen(false); createForm.reset(); setTempPwd(null); }}
        title="Create New User"
      >
        <form
          onSubmit={createForm.handleSubmit(d =>
            createMutation.mutate({
              firstName: d.firstName,
              lastName:  d.lastName,
              email:     d.email,
              phone:     d.phone,
              role:      d.role,
              password:  d.password,
              sendWelcomeEmail: d.sendWelcomeEmail,
            })
          )}
          className="space-y-4"
        >
          {/* Name row */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="label">First Name</label>
              <input {...createForm.register('firstName')} className="input" placeholder="Jean" />
              {createForm.formState.errors.firstName && (
                <p className="form-error">{createForm.formState.errors.firstName.message}</p>
              )}
            </div>
            <div>
              <label className="label">Last Name</label>
              <input {...createForm.register('lastName')} className="input" placeholder="Habimana" />
              {createForm.formState.errors.lastName && (
                <p className="form-error">{createForm.formState.errors.lastName.message}</p>
              )}
            </div>
          </div>

          {/* Email + Phone */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="label">Email Address</label>
              <input type="email" {...createForm.register('email')} className="input" placeholder="jean@domuspacis.rw" />
              {createForm.formState.errors.email && (
                <p className="form-error">{createForm.formState.errors.email.message}</p>
              )}
            </div>
            <div>
              <label className="label">Phone <span className="text-stone-400 font-normal">(optional)</span></label>
              <input {...createForm.register('phone')} className="input" placeholder="+250 78 000 0000" />
            </div>
          </div>

          {/* Role */}
          <div>
            <label className="label">Role</label>
            <div className="grid grid-cols-2 gap-2">
              {ALL_ROLES.map(role => {
                const m = ROLE_META[role];
                const selected = createForm.watch('role') === role;
                return (
                  <label
                    key={role}
                    className={cn(
                      'flex items-start gap-3 p-3 rounded-xl border-2 cursor-pointer transition-all',
                      selected
                        ? 'border-gold-400 bg-gold-50'
                        : 'border-stone-200 hover:border-stone-300'
                    )}
                  >
                    <input
                      type="radio"
                      value={role}
                      {...createForm.register('role')}
                      className="mt-0.5 accent-gold-600"
                    />
                    <div>
                      <div className={cn('badge text-[10px] mb-0.5', m.color)}>{m.label}</div>
                      <div className="text-xs text-stone-500">{m.description}</div>
                    </div>
                  </label>
                );
              })}
            </div>
          </div>

          {/* Password */}
          <div>
            <label className="label">Password</label>
            <div className="relative">
              <input
                type={showPwd ? 'text' : 'password'}
                {...createForm.register('password')}
                className="input pr-10"
                placeholder="Min 8 chars, 1 uppercase, 1 number"
              />
              <button
                type="button"
                onClick={() => setShowPwd(v => !v)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-stone-400 hover:text-stone-600"
              >
                {showPwd ? <EyeOff size={15} /> : <Eye size={15} />}
              </button>
            </div>
            {createForm.formState.errors.password && (
              <p className="form-error">{createForm.formState.errors.password.message}</p>
            )}
          </div>

          <div>
            <label className="label">Confirm Password</label>
            <div className="relative">
              <input
                type={showConfirm ? 'text' : 'password'}
                {...createForm.register('confirmPassword')}
                className="input pr-10"
                placeholder="Repeat password"
              />
              <button
                type="button"
                onClick={() => setShowConfirm(v => !v)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-stone-400 hover:text-stone-600"
              >
                {showConfirm ? <EyeOff size={15} /> : <Eye size={15} />}
              </button>
            </div>
            {createForm.formState.errors.confirmPassword && (
              <p className="form-error">{createForm.formState.errors.confirmPassword.message}</p>
            )}
          </div>

          {/* Send welcome email toggle */}
          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="checkbox"
              {...createForm.register('sendWelcomeEmail')}
              className="rounded accent-gold-600 w-4 h-4"
            />
            <span className="text-sm text-stone-700">
              Send welcome email with login instructions
            </span>
          </label>

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
              disabled={createMutation.isPending}
              className="btn-primary flex-1 justify-center"
            >
              {createMutation.isPending ? 'Creating…' : 'Create User'}
            </button>
          </div>
        </form>
      </Modal>

      {/* ══════════════════════════════════════════════════════════════════
          EDIT USER MODAL
      ══════════════════════════════════════════════════════════════════ */}
      <Modal
        open={!!editTarget}
        onClose={() => setEditTarget(null)}
        title={`Edit — ${editTarget?.firstName} ${editTarget?.lastName}`}
      >
        {editTarget && (
          <form
            onSubmit={editForm.handleSubmit(d =>
              updateMutation.mutate({ id: editTarget.id, payload: d })
            )}
            className="space-y-4"
          >
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="label">First Name</label>
                <input {...editForm.register('firstName')} className="input" />
                {editForm.formState.errors.firstName && (
                  <p className="form-error">{editForm.formState.errors.firstName.message}</p>
                )}
              </div>
              <div>
                <label className="label">Last Name</label>
                <input {...editForm.register('lastName')} className="input" />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="label">Email</label>
                <input type="email" {...editForm.register('email')} className="input" />
                {editForm.formState.errors.email && (
                  <p className="form-error">{editForm.formState.errors.email.message}</p>
                )}
              </div>
              <div>
                <label className="label">Phone</label>
                <input {...editForm.register('phone')} className="input" />
              </div>
            </div>

            <div>
              <label className="label">Role</label>
              <div className="grid grid-cols-2 gap-2">
                {ALL_ROLES.map(role => {
                  const m = ROLE_META[role];
                  const selected = editForm.watch('role') === role;
                  return (
                    <label
                      key={role}
                      className={cn(
                        'flex items-start gap-3 p-3 rounded-xl border-2 cursor-pointer transition-all',
                        selected
                          ? 'border-gold-400 bg-gold-50'
                          : 'border-stone-200 hover:border-stone-300'
                      )}
                    >
                      <input type="radio" value={role} {...editForm.register('role')} className="mt-0.5 accent-gold-600" />
                      <div>
                        <div className={cn('badge text-[10px] mb-0.5', m.color)}>{m.label}</div>
                        <div className="text-xs text-stone-500">{m.description}</div>
                      </div>
                    </label>
                  );
                })}
              </div>
            </div>

            <div className="flex gap-3 pt-2">
              <button type="button" onClick={() => setEditTarget(null)} className="btn-secondary flex-1 justify-center">
                Cancel
              </button>
              <button type="submit" disabled={updateMutation.isPending} className="btn-primary flex-1 justify-center">
                {updateMutation.isPending ? 'Saving…' : 'Save Changes'}
              </button>
            </div>
          </form>
        )}
      </Modal>

      {/* ══════════════════════════════════════════════════════════════════
          VIEW / RESET PASSWORD MODAL
      ══════════════════════════════════════════════════════════════════ */}
      <Modal
        open={!!viewTarget}
        onClose={() => { setViewTarget(null); setTempPwd(null); }}
        title="User Details"
      >
        {viewTarget && (
          <div className="space-y-5">
            {/* Avatar + name */}
            <div className="flex items-center gap-4">
              <UserAvatar user={viewTarget} size="lg" />
              <div>
                <h3 className="font-display text-xl text-stone-900">
                  {viewTarget.firstName} {viewTarget.lastName}
                </h3>
                <p className="text-stone-400 text-sm">{viewTarget.email}</p>
                <div className="flex items-center gap-2 mt-1.5">
                  <RoleBadge role={viewTarget.role} />
                  <StatusBadge status={viewTarget.isActive ? 'ACTIVE' : 'INACTIVE'} />
                </div>
              </div>
            </div>

            <div className="gold-divider" />

            {/* Info grid */}
            <div className="grid grid-cols-2 gap-4 text-sm">
              {[
                ['Phone',      viewTarget.phone ?? '—'],
                ['Created',    formatDate(viewTarget.createdAt)],
                ['Last Login', viewTarget.lastLoginAt ? formatDateRelative(viewTarget.lastLoginAt) : 'Never'],
                ['User ID',    viewTarget.id],
              ].map(([label, value]) => (
                <div key={label}>
                  <div className="text-stone-400 text-xs mb-0.5">{label}</div>
                  <div className="font-medium text-stone-800 break-all">{value}</div>
                </div>
              ))}
            </div>

            {/* Role description */}
            <div className="bg-stone-50 rounded-xl p-3 text-sm">
              <div className="text-stone-400 text-xs mb-1">Access Level</div>
              <div className="text-stone-700">{ROLE_META[viewTarget.role].description}</div>
            </div>

            {/* Temp password display (after reset) */}
            {tempPwd && <TempPasswordDisplay password={tempPwd} />}

            {/* Actions */}
            <div className="flex flex-wrap gap-2 pt-1">
              <button
                onClick={() => { openEdit(viewTarget); setViewTarget(null); }}
                className="btn-secondary flex-1 justify-center text-sm"
              >
                <Edit size={13} /> Edit User
              </button>
              <button
                onClick={() => {
                  setTempPwd(null);
                  resetPwdMutation.mutate(viewTarget);
                }}
                disabled={resetPwdMutation.isPending}
                className="btn-ghost border border-stone-200 flex-1 justify-center text-sm text-purple-600 hover:bg-purple-50"
              >
                <RotateCcw size={13} />
                {resetPwdMutation.isPending ? 'Resetting…' : 'Reset Password'}
              </button>
              <button
                onClick={() => toggleActiveMutation.mutate({ id: viewTarget.id, active: viewTarget.isActive })}
                className={cn(
                  'btn-ghost border border-stone-200 flex-1 justify-center text-sm',
                  viewTarget.isActive ? 'text-amber-600 hover:bg-amber-50' : 'text-green-600 hover:bg-green-50'
                )}
              >
                {viewTarget.isActive ? <><UserX size={13} /> Deactivate</> : <><UserCheck size={13} /> Activate</>}
              </button>
            </div>
          </div>
        )}
      </Modal>

      {/* ══════════════════════════════════════════════════════════════════
          DELETE CONFIRMATION MODAL
      ══════════════════════════════════════════════════════════════════ */}
      <Modal
        open={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        title="Delete User"
      >
        {deleteTarget && (
          <div>
            <div className="flex items-center gap-4 mb-5 p-4 bg-red-50 rounded-xl border border-red-100">
              <UserAvatar user={deleteTarget} />
              <div>
                <div className="font-medium text-stone-900">{deleteTarget.firstName} {deleteTarget.lastName}</div>
                <div className="text-xs text-stone-500">{deleteTarget.email}</div>
              </div>
            </div>
            <p className="text-stone-600 text-sm mb-2">
              Are you sure you want to permanently delete this user? This action <strong>cannot be undone</strong>.
            </p>
            <p className="text-stone-400 text-xs mb-6">
              All associated data (session tokens, preferences) will be removed. Booking and transaction records will be preserved.
            </p>
            <div className="flex gap-3">
              <button
                onClick={() => setDeleteTarget(null)}
                className="btn-secondary flex-1 justify-center"
              >
                Cancel
              </button>
              <button
                onClick={() => deleteMutation.mutate(deleteTarget.id)}
                disabled={deleteMutation.isPending}
                className="btn-danger flex-1 justify-center"
              >
                <Trash2 size={14} />
                {deleteMutation.isPending ? 'Deleting…' : 'Delete User'}
              </button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
