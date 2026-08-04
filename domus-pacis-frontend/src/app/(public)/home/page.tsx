import type { Metadata } from 'next';
import Link from 'next/link';
import { ArrowRight, Star, Users, Award, MapPin } from 'lucide-react';

export const metadata: Metadata = {
  title: 'Welcome — Domus Pacis',
  description: 'A sanctuary of hospitality and spiritual renewal in the heart of Kigali.',
};

// ── Icon Mapping ─────────────────────────────────────────────
const ICONS = {
  Users,
  Award,
  Star,
  MapPin,
};

// ── Services (UNCHANGED) ─────────────────────────────────────
const SERVICES = [
  {
    icon: '🛏️',
    title: 'Accommodation',
    description: 'Peaceful, well-appointed rooms with modern amenities and serene surroundings.',
    href: '/rooms',
    color: 'from-yellow-50 to-yellow-100',
  },
  {
    icon: '🏛️',
    title: 'Conference Halls',
    description: 'Fully equipped halls for professional meetings, seminars, and corporate events.',
    href: '/conference',
    color: 'from-blue-50 to-indigo-50',
  },
  {
    icon: '🌸',
    title: 'Wedding Gardens',
    description: 'Beautifully landscaped outdoor spaces to celebrate your most important day.',
    href: '/weddings',
    color: 'from-rose-50 to-pink-50',
  },
  {
    icon: '🕊️',
    title: 'Retreat Centre',
    description: 'Dedicated space for spiritual retreats, prayer, and personal reflection.',
    href: '/retreats',
    color: 'from-violet-50 to-purple-50',
  },
];

// ── Fetch Homepage Data ─────────────────────────────────────
export const dynamic = 'force-dynamic';
async function getHomepageData() {
  try {
    const res = await fetch(
      `${process.env.NEXT_PUBLIC_API_URL}/public/home/homepage-analytics`,
      { next: { revalidate: 60 } }
    );

   if (!res.ok) {
     const text = await res.text();
     console.error('API ERROR:', text);
     throw new Error(`Failed: ${res.status}`);
   }

    return res.json();
  } catch (error) {
    console.error('Homepage fetch failed:', error);

    // Fallback (keeps UI alive)
    return {
      stats: [
        { label: 'Years of Service', value: '—', icon: 'Award' },
        { label: 'Guests Welcomed', value: '—', icon: 'Users' },
        { label: 'Events Hosted', value: '—', icon: 'Star' },
        { label: 'Rooms Available', value: '—', icon: 'MapPin' },
      ],
      testimonials: [],
    };
  }
}

// ── Page Component ──────────────────────────────────────────
export default async function HomePage() {
  const data = await getHomepageData();

  const stats = data.stats || [];
  const testimonials = data.testimonials || [];

  return (
    <>
      {/* ── Hero ──────────────────────────────────────────── */}
      <section className="relative min-h-screen flex items-center justify-center overflow-hidden bg-sacred-gradient">
        <div className="absolute inset-0 hero-overlay" />
        <div className="absolute inset-0 grain opacity-40" />

        <div className="relative z-10 max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 text-center pt-28 pb-20">
          <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-yellow-100 text-yellow-700 text-xs font-medium uppercase tracking-widest mb-8">
            <span className="w-1.5 h-1.5 rounded-full bg-yellow-500 animate-pulse" />
            DOMUS PACIS · Archdiocese of Kigali
          </div>

          <h1 className="font-display text-5xl sm:text-6xl lg:text-7xl text-stone-900 leading-tight mb-6">
            A HOME OF PEACE
            <span className="block text-transparent bg-clip-text bg-gradient-to-r from-yellow-500 via-blue-600 to-violet-600 italic">
              Hospitality · Events · Retreats
            </span>
          </h1>

          <p className="text-stone-600 text-lg sm:text-xl leading-relaxed max-w-2xl mx-auto mb-10">
            A sanctuary in Kigali where hospitality, serenity, and spiritual renewal meet — crafted for rest, celebration, and reflection.
          </p>

          <div className="flex flex-wrap items-center justify-center gap-4">
            <Link href="/booking" className="btn-primary px-8 py-3.5 text-base glow-gold">
              Book Your Stay <ArrowRight size={16} />
            </Link>
            <Link href="/services" className="btn-secondary px-8 py-3.5 text-base">
              Explore Services
            </Link>
          </div>

          {/* ── Dynamic Stats ───────────────────────────── */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-6 mt-16 pt-10 border-t border-yellow-200">
            {stats.map((stat: any) => {
              const Icon = ICONS[stat.icon as keyof typeof ICONS] || Users;

              return (
                <div key={stat.label} className="flex flex-col items-center gap-1">
                  <Icon size={18} className="text-yellow-600 mb-1" />
                  <span className="font-display text-3xl font-semibold text-stone-900">
                    {stat.value}
                  </span>
                  <span className="text-stone-500 text-xs uppercase tracking-wide">
                    {stat.label}
                  </span>
                </div>
              );
            })}
          </div>
        </div>
      </section>

      {/* ── Services (UNCHANGED) ─────────────────────────── */}
      <section id="services" className="py-24 bg-sacred-gradient">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-14">
            <p className="text-yellow-600 text-sm uppercase tracking-widest mb-3">What We Offer</p>
            <h2 className="section-title text-4xl mb-4">Exceptional Services</h2>
            <div className="gold-divider max-w-xs mx-auto mb-5" />
            <p className="section-subtitle mx-auto text-center">
              From peaceful stays to grand celebrations, Domus Pacis offers refined spaces shaped by care and dignity.
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {SERVICES.map((svc) => (
              <Link key={svc.href} href={svc.href} className={`group card-hover bg-gradient-to-br ${svc.color} border border-white/60`}>
                <div className="text-4xl mb-4">{svc.icon}</div>
                <h3 className="font-display text-xl text-stone-900 mb-2 group-hover:text-yellow-700 transition-colors">
                  {svc.title}
                </h3>
                <p className="text-sm text-stone-600 mb-4">{svc.description}</p>
                <div className="flex items-center gap-1.5 text-yellow-600 text-sm font-medium group-hover:gap-2.5 transition-all">
                  Learn more <ArrowRight size={14} />
                </div>
              </Link>
            ))}
          </div>
        </div>
      </section>

      {/* ── About (UNCHANGED) ───────────────────────────── */}
      <section id="about" className="py-24 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 grid lg:grid-cols-2 gap-16 items-center">
          <div>
            <p className="text-yellow-600 text-sm uppercase tracking-widest mb-3">Our Mission</p>
            <h2 className="section-title text-4xl mb-5">
              Faith, Service & <span className="text-yellow-600 italic">Hospitality</span>
            </h2>
            <div className="gold-divider max-w-[120px] mb-6" />
            <p className="text-stone-600 mb-5">
              Domus Pacis — “House of Peace” — welcomes travelers, pilgrims, and families seeking rest and meaning.
            </p>
            <p className="text-stone-600 mb-8">
              Every guest is received not as a client, but as a brother or sister.
            </p>
            <Link href="/services" className="btn-primary">
              Discover Our Story <ArrowRight size={15} />
            </Link>
          </div>

          <div className="relative">
            <div className="aspect-[4/3] rounded-3xl bg-sacred-gradient shadow-xl flex items-center justify-center">
              <div className="text-center">
                <div className="text-8xl mb-4 text-yellow-500">✝</div>
                <p className="font-display text-2xl italic text-violet-700">“Pax Vobiscum”</p>
                <p className="text-stone-500 text-sm mt-2">Peace be with you</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ── Testimonials (Dynamic) ───────────────────────── */}
      <section id="testimonials" className="py-24 bg-gradient-to-br from-blue-50 via-white to-violet-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center mb-14">
          <p className="text-yellow-600 text-sm uppercase tracking-widest mb-3">Testimonials</p>
          <h2 className="section-title text-4xl mb-4">What Our Guests Say</h2>
        </div>

        <div className="grid md:grid-cols-3 gap-8 max-w-7xl mx-auto px-4">
          {testimonials.map((t: any, i: number) => (
            <div key={i} className="card-hover">
              <div className="flex gap-0.5 mb-5">
                {Array.from({ length: 5 }).map((_, j) => (
                  <Star key={j} size={14} className="fill-yellow-500 text-yellow-500" />
                ))}
              </div>
              <p className="text-stone-600 italic mb-6">“{t.quote}”</p>
              <div className="flex items-center gap-3 border-t pt-5">
                <div className="w-9 h-9 rounded-full bg-yellow-500 text-white flex items-center justify-center">
                  {t.name?.charAt(0) || '?'}
                </div>
                <div>
                  <div className="text-stone-900">{t.name}</div>
                  <div className="text-stone-500 text-xs">{t.role}</div>
                </div>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* ── CTA (UNCHANGED) ─────────────────────────────── */}
      <section className="py-20 bg-sacred-gradient text-center">
        <h2 className="font-display text-4xl text-stone-900 mb-4">
          Ready to Experience Domus Pacis?
        </h2>
        <p className="text-stone-600 mb-8">
          Step into a place designed for peace, dignity, and meaningful moments.
        </p>

        <div className="flex justify-center gap-4">
          <Link href="/booking" className="btn-primary px-8 py-3.5 glow-gold">
            Make a Reservation <ArrowRight size={16} />
          </Link>
          <Link href="/contact" className="btn-secondary px-8 py-3.5">
            Contact Us
          </Link>
        </div>
      </section>
    </>
  );
}