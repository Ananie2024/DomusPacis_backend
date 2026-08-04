'use client';

import Image from 'next/image';
import Link from 'next/link';
import { useState, useEffect } from 'react';
import { Menu, X, Phone } from 'lucide-react';
import { cn } from '@/lib/utils';

const NAV = [
  { label: 'Services',    href: '/services'    },
  { label: 'Rooms',       href: '/rooms'        },
  { label: 'Conferences', href: '/conference'   },
  { label: 'Weddings',    href: '/weddings'     },
  { label: 'Retreats',    href: '/retreats'     },
  { label: 'Contact',     href: '/contact'      },
];

export function PublicHeader() {
  const [open, setOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 30);
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  return (
    <header
      className={cn(
        'fixed top-0 inset-x-0 z-50 transition-all duration-300',
        scrolled
          ? 'bg-white/80 backdrop-blur-md shadow-md py-4 border-b border-yellow-300/40'
          : 'bg-transparent py-6'
      )}
    >
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between">

        {/* Logo + Home */}
        <div className="flex items-center gap-6">
          <Link href="/" className="flex items-center group">
            <Image
              src="/images/domus-pacis-logo.png"
              alt="Domus Pacis Logo"
              width={240}
              height={90}
              className="h-14 sm:h-16 md:h-20 w-auto object-contain transition-transform duration-300 group-hover:scale-[1.03]"
              priority
            />
          </Link>

          <Link
            href="/"
            className="hidden md:inline text-sm font-medium text-stone-600 hover:text-blue-700 transition-colors"
          >
            Home
          </Link>
        </div>

        {/* Desktop nav */}
        <nav className="hidden md:flex items-center gap-1">
          {NAV.map((n) => (
            <Link
              key={n.href}
              href={n.href}
              className="px-4 py-2 text-stone-700 hover:text-blue-700 text-sm font-medium rounded-lg hover:bg-white/60 transition-all duration-200"
            >
              {n.label}
            </Link>
          ))}
        </nav>

        {/* CTA */}
        <div className="hidden md:flex items-center gap-4">
          <a
            href="tel:+250781234567"
            className="flex items-center gap-2 text-blue-700 hover:text-blue-900 text-sm font-medium transition-colors"
          >
            <Phone size={15} />
            <span>+250 78 123 4567</span>
          </a>

          <Link
            href="/booking"
            className="bg-gradient-to-r from-yellow-400 to-yellow-500 text-white font-semibold text-sm px-6 py-2.5 rounded-xl shadow hover:shadow-md hover:scale-[1.02] transition-all"
          >
            Book Now
          </Link>
        </div>

        {/* Mobile toggle */}
        <button
          className="md:hidden text-stone-700 p-2 rounded-lg hover:bg-stone-200"
          onClick={() => setOpen(!open)}
          aria-label="Toggle menu"
        >
          {open ? <X size={22} /> : <Menu size={22} />}
        </button>
      </div>

      {/* Mobile drawer */}
      {open && (
        <div className="md:hidden bg-white border-t border-yellow-200 px-4 pb-6 pt-4 flex flex-col gap-1 shadow-lg">
          <Link
            href="/"
            onClick={() => setOpen(false)}
            className="px-4 py-3 text-stone-700 hover:text-blue-700 text-sm font-medium rounded-xl hover:bg-yellow-50"
          >
            Home
          </Link>

          {NAV.map((n) => (
            <Link
              key={n.href}
              href={n.href}
              onClick={() => setOpen(false)}
              className="px-4 py-3 text-stone-700 hover:text-blue-700 text-sm font-medium rounded-xl hover:bg-yellow-50"
            >
              {n.label}
            </Link>
          ))}

          <Link
            href="/booking"
            onClick={() => setOpen(false)}
            className="mt-3 bg-yellow-400 text-white text-center py-3 rounded-xl font-semibold"
          >
            Book Now
          </Link>
        </div>
      )}
    </header>
  );
}