import './globals.css';
import { Toaster } from 'react-hot-toast';
import { QueryProvider } from '@/components/QueryProvider';

export const metadata = {
  title: 'Domus Pacis',
  description: 'Domus Pacis Retreat Centre',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>
        <QueryProvider>
          {children}
          <Toaster position="top-right" />
        </QueryProvider>
      </body>
    </html>
  );
}
