// app/(public)/layout.tsx
import { PublicHeader } from '@/components/layout/PublicHeader';
import { PublicFooter } from '@/components/layout/PublicFooter';

export default function PublicLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="min-h-screen flex flex-col">
      <PublicHeader />
      <main className="flex-1 pt-20"> {/* Add padding top to account for fixed header */}
        {children}
      </main>
      <PublicFooter />
    </div>
  );
}