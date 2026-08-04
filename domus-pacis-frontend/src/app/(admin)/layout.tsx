'use client';
import { AdminSidebar } from '@/components/layout/AdminSidebar';
import { AdminHeader }  from '@/components/layout/AdminHeader';
import { useUIStore }   from '@/stores/uiStore';
import { cn }           from '@/lib/utils';

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const { sidebarOpen, setSidebar } = useUIStore();

  return (
    <div className="min-h-screen bg-stone-50 flex">

      {/* Mobile overlay — tap to close sidebar */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 z-30 bg-stone-950/50 lg:hidden"
          onClick={() => setSidebar(false)}
        />
      )}

      <AdminSidebar />

      <div className={cn(
        'flex-1 flex flex-col min-w-0 transition-all duration-300',
        // Desktop: shift content based on sidebar state
        sidebarOpen ? 'lg:ml-64' : 'lg:ml-16',
        // Mobile: never shift — sidebar floats over content
        'ml-0',
      )}>
        <AdminHeader />
        <main className="flex-1 p-4 lg:p-6 overflow-auto">
          {children}
        </main>
      </div>

    </div>
  );
}