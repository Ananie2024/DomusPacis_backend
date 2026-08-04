'use client';
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  inventoryApi,
  CreateItemRequest,
  RecordMovementRequest,
  CreateMenuItemRequest,
  CreateSupplierRequest,
} from '@/lib/api/inventoryApi';
import { formatCurrency, formatDate } from '@/lib/utils';
import { PageHeader, Pagination, PageLoader, EmptyState, Modal, StatusBadge } from '@/components/ui';
import {
  Package, Plus, AlertTriangle,
  ArrowDownCircle, ArrowUpCircle,
  UtensilsCrossed, Truck, ShoppingBag,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import toast from 'react-hot-toast';

// ── Constants ─────────────────────────────────────────────────────────────────
const TABS = ['Items', 'Menu', 'Orders', 'Suppliers'] as const;
type Tab = typeof TABS[number];

const ITEM_CATEGORIES = [
  'FOOD', 'BEVERAGES', 'CLEANING', 'KITCHEN_EQUIPMENT',
  'EVENT_EQUIPMENT', 'STATIONERY', 'LINEN',
];

const MENU_CATEGORIES = ['BREAKFAST', 'LUNCH', 'DINNER', 'SNACKS', 'BEVERAGES'];
const MOVEMENT_TYPES  = ['RECEIPT', 'CONSUMPTION', 'ADJUSTMENT', 'WASTE'];

// ── Schemas ───────────────────────────────────────────────────────────────────
const itemSchema = z.object({
  name:         z.string().min(2, 'Name required'),
  category:     z.string().min(1, 'Category required'),
  unit:         z.string().min(1, 'Unit required'),
  reorderLevel: z.number().min(0),
  unitCost:     z.number().positive('Cost must be positive'),
  supplierId:   z.string().uuid().optional().or(z.literal('')),
});

const movementSchema = z.object({
  movementType:  z.string().min(1),
  quantity:      z.number().positive('Quantity must be positive'),
  referenceNote: z.string().optional(),
});

const menuItemSchema = z.object({
  name:        z.string().min(2, 'Name required'),
  category:    z.string().min(1, 'Category required'),
  description: z.string().optional(),
  unitPrice:   z.number().positive('Price must be positive'),
});

const supplierSchema = z.object({
  name:          z.string().min(2, 'Name required'),
  contactPerson: z.string().optional(),
  phone:         z.string().optional(),
  email:         z.string().email().optional().or(z.literal('')),
  address:       z.string().optional(),
  taxIdentificationNumber: z.string().optional(),
});

type ItemForm     = z.infer<typeof itemSchema>;
type MovementForm = z.infer<typeof movementSchema>;
type MenuForm     = z.infer<typeof menuItemSchema>;
type SupplierForm = z.infer<typeof supplierSchema>;

export default function InventoryPage() {
  const [tab,          setTab]          = useState<Tab>('Items');
  const [page,         setPage]         = useState(0);
  const [showLowStock, setShowLowStock] = useState(false);
  const [itemModal,    setItemModal]    = useState(false);
  const [moveModal,    setMoveModal]    = useState<string | null>(null);
  const [menuModal,    setMenuModal]    = useState(false);
  const [supplierModal,setSupplierModal]= useState(false);

  const qc = useQueryClient();

  // ── Queries ────────────────────────────────────────────────────────────────
  const { data: items, isLoading: itemsLoading } = useQuery({
    queryKey: ['inventory-items', page],
    queryFn:  () => inventoryApi.listItems({ page, size: 20 }),
    enabled:  tab === 'Items' && !showLowStock,
  });

  const { data: lowStockItems, isLoading: lowLoading } = useQuery({
    queryKey: ['inventory-low-stock'],
    queryFn:  inventoryApi.getLowStockItems,
    enabled:  showLowStock,
  });

  const { data: menuItems, isLoading: menuLoading } = useQuery({
    queryKey: ['menu-items', page],
    queryFn:  () => inventoryApi.listMenuItems({ page, size: 20 }),
    enabled:  tab === 'Menu',
  });

  const { data: orders, isLoading: ordersLoading } = useQuery({
    queryKey: ['food-orders', page],
    queryFn:  () => inventoryApi.listOrders({ page, size: 15 }),
    enabled:  tab === 'Orders',
  });

  const { data: suppliers, isLoading: suppliersLoading } = useQuery({
    queryKey: ['suppliers', page],
    queryFn:  () => inventoryApi.listSuppliers({ page, size: 15 }),
    enabled:  tab === 'Suppliers',
  });

  // ── Forms ──────────────────────────────────────────────────────────────────
  const itemForm     = useForm<ItemForm>    ({ resolver: zodResolver(itemSchema),     defaultValues: { reorderLevel: 0, unitCost: 0 } });
  const moveForm     = useForm<MovementForm>({ resolver: zodResolver(movementSchema), defaultValues: { movementType: 'RECEIPT', quantity: 1 } });
  const menuForm     = useForm<MenuForm>    ({ resolver: zodResolver(menuItemSchema), defaultValues: { unitPrice: 0 } });
  const supplierForm = useForm<SupplierForm>({ resolver: zodResolver(supplierSchema) });

  // ── Mutations ──────────────────────────────────────────────────────────────
  const createItem = useMutation({
    mutationFn: (req: CreateItemRequest) => inventoryApi.createItem(req),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['inventory-items'] });
      toast.success('Item added');
      setItemModal(false);
      itemForm.reset();
    },
    onError: () => toast.error('Failed to add item'),
  });

  const recordMovement = useMutation({
    mutationFn: (req: RecordMovementRequest) => inventoryApi.recordMovement(req),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['inventory-items'] });
      qc.invalidateQueries({ queryKey: ['inventory-low-stock'] });
      toast.success('Movement recorded');
      setMoveModal(null);
      moveForm.reset();
    },
    onError: () => toast.error('Failed to record movement'),
  });

  const createMenuItem = useMutation({
    mutationFn: (req: CreateMenuItemRequest) => inventoryApi.createMenuItem(req),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['menu-items'] });
      toast.success('Menu item created');
      setMenuModal(false);
      menuForm.reset();
    },
    onError: () => toast.error('Failed to create menu item'),
  });

  const toggleMenu = useMutation({
    mutationFn: (id: string) => inventoryApi.toggleMenuItemAvailability(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['menu-items'] }),
    onError:   () => toast.error('Failed to toggle availability'),
  });

  const createSupplier = useMutation({
    mutationFn: (req: CreateSupplierRequest) => inventoryApi.createSupplier(req),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['suppliers'] });
      toast.success('Supplier added');
      setSupplierModal(false);
      supplierForm.reset();
    },
    onError: () => toast.error('Failed to add supplier'),
  });

  const cancelOrder = useMutation({
    mutationFn: (id: string) => inventoryApi.cancelOrder(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['food-orders'] }); toast.success('Order cancelled'); },
    onError:   () => toast.error('Failed to cancel order'),
  });

  const deactivateSupplier = useMutation({
    mutationFn: (id: string) => inventoryApi.deactivateSupplier(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['suppliers'] }); toast.success('Supplier deactivated'); },
    onError:   () => toast.error('Failed to deactivate supplier'),
  });

  // ── Display items (normal or low-stock filtered) ───────────────────────────
  const displayItems   = showLowStock ? lowStockItems ?? [] : (items?.content ?? []);
  const isItemsLoading = showLowStock ? lowLoading : itemsLoading;

  return (
    <div className="max-w-7xl mx-auto">
      <PageHeader
        title="Inventory Management"
        subtitle="Stock levels, movements, menu and suppliers"
        actions={
          <div className="flex gap-2">
            {tab === 'Items'     && <button onClick={() => setItemModal(true)}     className="btn-primary"><Plus size={14} /> Add Item</button>}
            {tab === 'Menu'      && <button onClick={() => setMenuModal(true)}     className="btn-primary"><Plus size={14} /> Add Menu Item</button>}
            {tab === 'Suppliers' && <button onClick={() => setSupplierModal(true)} className="btn-primary"><Plus size={14} /> Add Supplier</button>}
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
          ITEMS TAB
      ══════════════════════════════════════════════════════════════════════ */}
      {tab === 'Items' && (
        <>
          <div className="card mb-4 flex items-center gap-3">
            <label className="flex items-center gap-2 text-sm text-stone-600 cursor-pointer">
              <input
                type="checkbox"
                checked={showLowStock}
                onChange={e => setShowLowStock(e.target.checked)}
                className="rounded accent-red-500"
              />
              <AlertTriangle size={14} className="text-red-500" />
              Show Low Stock Only
            </label>
          </div>

          {isItemsLoading ? <PageLoader /> : (
            <div className="table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Item</th>
                    <th>Category</th>
                    <th>Unit</th>
                    <th>Stock</th>
                    <th>Reorder At</th>
                    <th>Unit Cost</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {displayItems.length === 0 ? (
                    <tr><td colSpan={8}>
                      <EmptyState icon={<Package size={40} />} title="No items found" />
                    </td></tr>
                  ) : displayItems.map(item => (
                    <tr key={item.id}>
                      <td>
                        <div className="font-medium text-stone-900">{item.name}</div>
                        {item.supplierName && (
                          <div className="text-xs text-stone-400">{item.supplierName}</div>
                        )}
                      </td>
                      <td>
                        <span className="px-2 py-0.5 rounded-full bg-stone-100 text-stone-600 text-xs">
                          {item.category.replace('_', ' ')}
                        </span>
                      </td>
                      <td className="text-stone-600">{item.unit}</td>
                      <td>
                        <span className={cn('font-semibold', item.lowStock ? 'text-red-600' : 'text-green-700')}>
                          {item.currentStock}
                        </span>
                      </td>
                      <td className="text-stone-500">{item.reorderLevel}</td>
                      <td>{formatCurrency(item.unitCost)}</td>
                      <td>
                        {item.lowStock ? (
                          <span className="flex items-center gap-1 text-xs text-red-600 font-medium">
                            <AlertTriangle size={12} /> Low Stock
                          </span>
                        ) : (
                          <span className="text-xs text-green-600 font-medium">OK</span>
                        )}
                      </td>
                      <td>
                        <div className="flex items-center gap-1">
                          <button
                            onClick={() => { moveForm.setValue('movementType', 'RECEIPT'); setMoveModal(item.id); }}
                            className="p-1.5 rounded-lg hover:bg-green-50 text-green-600 transition-colors"
                            title="Add stock"
                          >
                            <ArrowDownCircle size={14} />
                          </button>
                          <button
                            onClick={() => { moveForm.setValue('movementType', 'CONSUMPTION'); setMoveModal(item.id); }}
                            className="p-1.5 rounded-lg hover:bg-amber-50 text-amber-600 transition-colors"
                            title="Consume stock"
                          >
                            <ArrowUpCircle size={14} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {!showLowStock && items && (
                <div className="p-4 border-t border-stone-100">
                  <Pagination page={page} totalPages={items.totalPages} onPageChange={setPage} />
                </div>
              )}
            </div>
          )}
        </>
      )}

      {/* ══════════════════════════════════════════════════════════════════════
          MENU TAB
      ══════════════════════════════════════════════════════════════════════ */}
      {tab === 'Menu' && (
        menuLoading ? <PageLoader /> : (
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Category</th>
                  <th>Description</th>
                  <th>Price</th>
                  <th>Available</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {menuItems?.content.length === 0 ? (
                  <tr><td colSpan={6}>
                    <EmptyState icon={<UtensilsCrossed size={40} />} title="No menu items yet" />
                  </td></tr>
                ) : menuItems?.content.map(m => (
                  <tr key={m.id}>
                    <td className="font-medium text-stone-900">{m.name}</td>
                    <td>
                      <span className="px-2 py-0.5 bg-stone-100 text-stone-600 rounded-full text-xs">
                        {m.category}
                      </span>
                    </td>
                    <td className="text-stone-500 text-sm">{m.description ?? '—'}</td>
                    <td className="font-medium">{formatCurrency(m.unitPrice)}</td>
                    <td><StatusBadge status={m.isAvailable ? 'ACTIVE' : 'INACTIVE'} /></td>
                    <td>
                      <button
                        onClick={() => toggleMenu.mutate(m.id)}
                        className="text-xs px-3 py-1.5 rounded-lg border border-stone-200 hover:bg-stone-50 text-stone-600 transition-colors"
                      >
                        {m.isAvailable ? 'Disable' : 'Enable'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {menuItems && (
              <div className="p-4 border-t border-stone-100">
                <Pagination page={page} totalPages={menuItems.totalPages} onPageChange={setPage} />
              </div>
            )}
          </div>
        )
      )}

      {/* ══════════════════════════════════════════════════════════════════════
          ORDERS TAB
      ══════════════════════════════════════════════════════════════════════ */}
      {tab === 'Orders' && (
        ordersLoading ? <PageLoader /> : (
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Order ID</th>
                  <th>Customer</th>
                  <th>Items</th>
                  <th>Total</th>
                  <th>Location</th>
                  <th>Ordered At</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {orders?.content.length === 0 ? (
                  <tr><td colSpan={8}>
                    <EmptyState icon={<ShoppingBag size={40} />} title="No food orders" />
                  </td></tr>
                ) : orders?.content.map(o => (
                  <tr key={o.id}>
                    <td className="font-mono text-xs text-stone-500">{o.id.slice(0, 8)}…</td>
                    <td className="font-mono text-xs text-stone-500">{o.customerId.slice(0, 8)}…</td>
                    <td className="text-stone-600">{o.itemCount} item{o.itemCount !== 1 ? 's' : ''}</td>
                    <td className="font-medium">{formatCurrency(o.totalAmount)}</td>
                    <td className="text-stone-500">{o.deliveryLocation ?? '—'}</td>
                    <td>{formatDate(o.orderedAt)}</td>
                    <td><StatusBadge status={o.status} /></td>
                    <td>
                      {o.status !== 'CANCELLED' && o.status !== 'DELIVERED' && (
                        <button
                          onClick={() => { if (confirm('Cancel this order?')) cancelOrder.mutate(o.id); }}
                          className="text-xs px-2.5 py-1.5 rounded-lg hover:bg-red-50 text-red-500 border border-red-100 transition-colors"
                        >
                          Cancel
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {orders && (
              <div className="p-4 border-t border-stone-100">
                <Pagination page={page} totalPages={orders.totalPages} onPageChange={setPage} />
              </div>
            )}
          </div>
        )
      )}

      {/* ══════════════════════════════════════════════════════════════════════
          SUPPLIERS TAB
      ══════════════════════════════════════════════════════════════════════ */}
      {tab === 'Suppliers' && (
        suppliersLoading ? <PageLoader /> : (
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Supplier</th>
                  <th>Contact</th>
                  <th>Phone</th>
                  <th>Email</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {suppliers?.content.length === 0 ? (
                  <tr><td colSpan={6}>
                    <EmptyState icon={<Truck size={40} />} title="No suppliers yet" />
                  </td></tr>
                ) : suppliers?.content.map(s => (
                  <tr key={s.id}>
                    <td className="font-medium text-stone-900">{s.name}</td>
                    <td className="text-stone-600">{s.contactPerson ?? '—'}</td>
                    <td className="text-stone-600">{s.phone ?? '—'}</td>
                    <td className="text-stone-500 text-sm">{s.email ?? '—'}</td>
                    <td><StatusBadge status={s.isActive ? 'ACTIVE' : 'INACTIVE'} /></td>
                    <td>
                      {s.isActive && (
                        <button
                          onClick={() => { if (confirm('Deactivate this supplier?')) deactivateSupplier.mutate(s.id); }}
                          className="text-xs px-2.5 py-1.5 rounded-lg hover:bg-amber-50 text-amber-600 border border-amber-100 transition-colors"
                        >
                          Deactivate
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {suppliers && (
              <div className="p-4 border-t border-stone-100">
                <Pagination page={page} totalPages={suppliers.totalPages} onPageChange={setPage} />
              </div>
            )}
          </div>
        )
      )}

      {/* ══════════════════════════════════════════════════════════════════════
          ADD ITEM MODAL
      ══════════════════════════════════════════════════════════════════════ */}
      <Modal open={itemModal} onClose={() => { setItemModal(false); itemForm.reset(); }} title="Add Inventory Item">
        <form onSubmit={itemForm.handleSubmit(d => createItem.mutate({
          name:         d.name,
          category:     d.category,
          unit:         d.unit,
          reorderLevel: d.reorderLevel,
          unitCost:     d.unitCost,
          supplierId:   d.supplierId || undefined,
        }))} className="space-y-4">
          <div>
            <label className="label">Item Name</label>
            <input {...itemForm.register('name')} className="input" />
            {itemForm.formState.errors.name && <p className="form-error">{itemForm.formState.errors.name.message}</p>}
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="label">Category</label>
              <select {...itemForm.register('category')} className="input">
                <option value="">Select…</option>
                {ITEM_CATEGORIES.map(c => (
                  <option key={c} value={c}>{c.replace('_', ' ')}</option>
                ))}
              </select>
              {itemForm.formState.errors.category && <p className="form-error">{itemForm.formState.errors.category.message}</p>}
            </div>
            <div>
              <label className="label">Unit</label>
              <input {...itemForm.register('unit')} placeholder="kg, litres, pcs…" className="input" />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="label">Reorder Level</label>
              <input type="number" step="0.01" {...itemForm.register('reorderLevel', { valueAsNumber: true })} className="input" />
            </div>
            <div>
              <label className="label">Unit Cost (RWF)</label>
              <input type="number" step="0.01" {...itemForm.register('unitCost', { valueAsNumber: true })} className="input" />
              {itemForm.formState.errors.unitCost && <p className="form-error">{itemForm.formState.errors.unitCost.message}</p>}
            </div>
          </div>
          <div className="flex gap-3 pt-2">
            <button type="button" onClick={() => { setItemModal(false); itemForm.reset(); }} className="btn-secondary flex-1 justify-center">Cancel</button>
            <button type="submit" disabled={createItem.isPending} className="btn-primary flex-1 justify-center">
              {createItem.isPending ? 'Saving…' : 'Save Item'}
            </button>
          </div>
        </form>
      </Modal>

      {/* ══════════════════════════════════════════════════════════════════════
          STOCK MOVEMENT MODAL
      ══════════════════════════════════════════════════════════════════════ */}
      <Modal
        open={!!moveModal}
        onClose={() => { setMoveModal(null); moveForm.reset(); }}
        title={moveForm.watch('movementType') === 'RECEIPT' ? 'Add Stock' : 'Record Consumption'}
      >
        <form onSubmit={moveForm.handleSubmit(d => recordMovement.mutate({
          itemId:        moveModal!,
          movementType:  d.movementType,
          quantity:      d.quantity,
          referenceNote: d.referenceNote,
        }))} className="space-y-4">
          <div>
            <label className="label">Movement Type</label>
            <select {...moveForm.register('movementType')} className="input">
              {MOVEMENT_TYPES.map(t => (
                <option key={t} value={t}>{t}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="label">Quantity</label>
            <input type="number" step="0.01" min="0.01" {...moveForm.register('quantity', { valueAsNumber: true })} className="input w-36" />
            {moveForm.formState.errors.quantity && <p className="form-error">{moveForm.formState.errors.quantity.message}</p>}
          </div>
          <div>
            <label className="label">Reference Note <span className="text-stone-400 font-normal">(optional)</span></label>
            <textarea {...moveForm.register('referenceNote')} rows={2} className="input resize-none" />
          </div>
          <div className="flex gap-3 pt-2">
            <button type="button" onClick={() => { setMoveModal(null); moveForm.reset(); }} className="btn-secondary flex-1 justify-center">Cancel</button>
            <button type="submit" disabled={recordMovement.isPending} className="btn-primary flex-1 justify-center">
              {recordMovement.isPending ? 'Saving…' : 'Record Movement'}
            </button>
          </div>
        </form>
      </Modal>

      {/* ══════════════════════════════════════════════════════════════════════
          ADD MENU ITEM MODAL
      ══════════════════════════════════════════════════════════════════════ */}
      <Modal open={menuModal} onClose={() => { setMenuModal(false); menuForm.reset(); }} title="Add Menu Item">
        <form onSubmit={menuForm.handleSubmit(d => createMenuItem.mutate(d))} className="space-y-4">
          <div>
            <label className="label">Name</label>
            <input {...menuForm.register('name')} className="input" />
            {menuForm.formState.errors.name && <p className="form-error">{menuForm.formState.errors.name.message}</p>}
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="label">Category</label>
              <select {...menuForm.register('category')} className="input">
                <option value="">Select…</option>
                {MENU_CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
              </select>
            </div>
            <div>
              <label className="label">Price (RWF)</label>
              <input type="number" step="0.01" {...menuForm.register('unitPrice', { valueAsNumber: true })} className="input" />
              {menuForm.formState.errors.unitPrice && <p className="form-error">{menuForm.formState.errors.unitPrice.message}</p>}
            </div>
          </div>
          <div>
            <label className="label">Description <span className="text-stone-400 font-normal">(optional)</span></label>
            <textarea {...menuForm.register('description')} rows={2} className="input resize-none" />
          </div>
          <div className="flex gap-3 pt-2">
            <button type="button" onClick={() => { setMenuModal(false); menuForm.reset(); }} className="btn-secondary flex-1 justify-center">Cancel</button>
            <button type="submit" disabled={createMenuItem.isPending} className="btn-primary flex-1 justify-center">
              {createMenuItem.isPending ? 'Saving…' : 'Save Menu Item'}
            </button>
          </div>
        </form>
      </Modal>

      {/* ══════════════════════════════════════════════════════════════════════
          ADD SUPPLIER MODAL
      ══════════════════════════════════════════════════════════════════════ */}
      <Modal open={supplierModal} onClose={() => { setSupplierModal(false); supplierForm.reset(); }} title="Add Supplier">
        <form onSubmit={supplierForm.handleSubmit(d => createSupplier.mutate(d))} className="space-y-4">
          <div>
            <label className="label">Supplier Name</label>
            <input {...supplierForm.register('name')} className="input" />
            {supplierForm.formState.errors.name && <p className="form-error">{supplierForm.formState.errors.name.message}</p>}
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="label">Contact Person</label>
              <input {...supplierForm.register('contactPerson')} className="input" />
            </div>
            <div>
              <label className="label">Phone</label>
              <input {...supplierForm.register('phone')} className="input" />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="label">Email</label>
              <input type="email" {...supplierForm.register('email')} className="input" />
            </div>
            <div>
              <label className="label">Tax ID <span className="text-stone-400 font-normal">(optional)</span></label>
              <input {...supplierForm.register('taxIdentificationNumber')} className="input" />
            </div>
          </div>
          <div>
            <label className="label">Address</label>
            <input {...supplierForm.register('address')} className="input" />
          </div>
          <div className="flex gap-3 pt-2">
            <button type="button" onClick={() => { setSupplierModal(false); supplierForm.reset(); }} className="btn-secondary flex-1 justify-center">Cancel</button>
            <button type="submit" disabled={createSupplier.isPending} className="btn-primary flex-1 justify-center">
              {createSupplier.isPending ? 'Saving…' : 'Save Supplier'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}