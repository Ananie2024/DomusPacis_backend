import Link from 'next/link';

export default function NotFound() {
  return (
    <div className="min-h-screen bg-stone-950 flex items-center justify-center px-4">
      <div className="text-center">
        <div className="font-display text-[8rem] text-stone-800 leading-none select-none">404</div>
        <h2 className="font-display text-3xl text-white mb-3">Page Not Found</h2>
        <p className="text-stone-400 mb-8">The page you are looking for does not exist or has been moved.</p>
        <Link href="/" className="btn-primary mx-auto">Return to Home</Link>
      </div>
    </div>
  );
}
