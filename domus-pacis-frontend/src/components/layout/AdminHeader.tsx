'use client';

import { Menu, Bell, User } from 'lucide-react';
import { useUIStore } from '@/stores/uiStore';
import { useAuthStore } from '@/stores/authStore';

export function AdminHeader() {
  const { toggleSidebar } = useUIStore();
  const { user } = useAuthStore();

  return (
    <header className="sticky top-0 z-20 bg-white border-b border-stone-200 px-4 py-3 flex items-center justify-between shadow-xs">
      <div className="flex items-center gap-3">
        <button
          onClick={toggleSidebar}
          className="p-2 rounded-lg text-stone-600 hover:bg-stone-100 transition-colors"
          title="Toggle Navigation Menu"
          aria-label="Toggle Navigation Menu"
        >
          <Menu className="w-5 h-5" />
        </button>
        <span className="font-display font-medium text-stone-800 text-sm hidden sm:inline-block">
          Domus Pacis Management Platform
        </span>
      </div>

      <div className="flex items-center gap-3">
        <div className="relative">
          <button
            className="p-2 rounded-lg text-stone-500 hover:bg-stone-100 transition-colors"
            title="Notifications"
          >
            <Bell className="w-4 h-4" />
          </button>
        </div>

        <div className="flex items-center gap-2 pl-2 border-l border-stone-200">
          <div className="w-8 h-8 rounded-full bg-stone-900 text-amber-400 font-semibold flex items-center justify-center text-xs">
            {user?.firstName ? user.firstName[0].toUpperCase() : <User className="w-4 h-4" />}
          </div>
          <div className="hidden md:block text-left text-xs">
            <p className="font-medium text-stone-900 leading-tight">
              {user?.firstName ? `${user.firstName} ${user.lastName || ''}` : 'Administrator'}
            </p>
            <p className="text-stone-500 text-[10px] uppercase tracking-wider font-semibold">
              {user?.role || 'ADMIN'}
            </p>
          </div>
        </div>
      </div>
    </header>
  );
}
