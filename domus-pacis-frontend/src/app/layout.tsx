import './globals.css';
import { Toaster } from 'react-hot-toast';
import { QueryProvider } from '@/components/QueryProvider';
import { BackendWakeup } from '@/components/BackendWakeup';

export const metadata = {
  title: 'Domus Pacis',
  description: 'Domus Pacis Retreat Centre',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>
        <BackendWakeup />
        <QueryProvider>
          {children}
          <Toaster position="top-right" />
        </QueryProvider>
      </body>
    </html>
  );
}
