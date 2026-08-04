import { create } from 'zustand';

interface UIState {
  sidebarOpen:   boolean;
  theme:         'light' | 'dark';
  toggleSidebar: () => void;
  setSidebar:    (open: boolean) => void;
  toggleTheme:   () => void;
}

export const useUIStore = create<UIState>((set) => ({
  sidebarOpen: typeof window !== 'undefined' ? window.innerWidth >= 1024 : false,
  theme:       'light',
  toggleSidebar: () => set((s) => ({ sidebarOpen: !s.sidebarOpen })),
  setSidebar:    (open) => set({ sidebarOpen: open }),
  toggleTheme:   () => set((s) => ({ theme: s.theme === 'light' ? 'dark' : 'light' })),
}));