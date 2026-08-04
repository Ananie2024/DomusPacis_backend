'use client';
import { useState, useEffect } from 'react';
import Link from 'next/link';
import { Loader2, ArrowRight, Users, Bed, Building2, Trees, Church } from 'lucide-react';
import { serviceAssetsApi, ServiceAssetResponse } from '@/lib/api/serviceAssetsApi';

const CATEGORY_META: Record<string, { label: string; icon: React.ElementType; color: string; href: string }> = {
  ROOM:           { label: 'Accommodation',    icon: Bed,       color: 'from-amber-900 to-stone-900', href: '/rooms' },
  CONFERENCE_HALL: { label: 'Conference Halls', icon: Building2, color: 'from-sky-900 to-stone-900',  href: '/conference' },
  WEDDING_GARDEN: { label: 'Wedding Gardens',  icon: Trees,     color: 'from-rose-900 to-stone-900',  href: '/weddings' },
  RETREAT_CENTER: { label: 'Retreat Centres',  icon: Church,    color: 'from-violet-900 to-stone-900', href: '/retreats' },
};

export default function ServicesPage() {
  const [assets, setAssets] = useState<ServiceAssetResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    serviceAssetsApi.listAll({ page: 0, size: 100 })
      .then(p => setAssets(p.content.filter(a => a.isAvailable)))
      .catch(() => setAssets([]))
      .finally(() => setLoading(false));
  }, []);

  const grouped = assets.reduce<Record<string, ServiceAssetResponse[]>>((acc, a) => {
    (acc[a.assetType] ??= []).push(a);
    return acc;
  }, {});

  return (
    <div className="pt-20">
      <section className="py-20 bg-stone-950 text-center relative">
        <div className="absolute inset-0 bg-gradient-to-b from-stone-900 to-stone-950" />
        <div className="relative max-w-3xl mx-auto px-4">
          <p className="text-gold-500 text-sm uppercase tracking-widest mb-3">Our Offerings</p>
          <h1 className="font-display text-5xl text-white mb-5">Services</h1>
          <div className="h-px bg-gradient-to-r from-transparent via-gold-500/50 to-transparent max-w-xs mx-auto mb-5" />
          <p className="text-stone-400 text-lg">Discover everything Domus Pacis has to offer — from peaceful stays to grand celebrations.</p>
        </div>
      </section>

      <section className="py-16 bg-ivory-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          {loading ? (
            <div className="flex items-center justify-center py-20"><Loader2 className="animate-spin text-gold-500" size={32} /></div>
          ) : Object.keys(grouped).length === 0 ? (
            <div className="text-center py-20 text-stone-500"><p>No services currently available. Please check back later.</p></div>
          ) : (
            <div className="space-y-16">
              {Object.entries(grouped).map(([type, items]) => {
                const meta = CATEGORY_META[type];
                const Icon = meta?.icon ?? Users;
                return (
                  <div key={type}>
                    <div className="flex items-center justify-between mb-6">
                      <div className="flex items-center gap-3">
                        <div className={`w-10 h-10 rounded-lg bg-gradient-to-br ${meta?.color ?? 'from-stone-700 to-stone-900'} flex items-center justify-center text-white`}>
                          <Icon size={18} />
                        </div>
                        <div>
                          <h2 className="font-display text-2xl text-stone-900">{meta?.label ?? type}</h2>
                          <p className="text-stone-500 text-sm">{items.length} {items.length === 1 ? 'option' : 'options'} available</p>
                        </div>
                      </div>
                      {meta && <Link href={meta.href} className="text-sm text-gold-600 hover:text-gold-700 font-medium flex items-center gap-1">View All <ArrowRight size={13} /></Link>}
                    </div>
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                      {items.map(item => (
                        <Link key={item.id} href={`/booking?asset=${item.id}&type=${type}`} className="card-hover group">
                          <h3 className="font-display text-lg text-stone-900 group-hover:text-gold-700 transition-colors mb-1">{item.name}</h3>
                          <p className="text-stone-500 text-sm mb-3 line-clamp-2">{item.description}</p>
                          <div className="flex items-center justify-between">
                            <span className="text-sm font-medium text-stone-800">{item.pricePerUnit.toLocaleString()} RWF</span>
                            <span className="text-xs text-stone-400">per {item.pricingUnit.toLowerCase().replace('_', ' ')}</span>
                          </div>
                        </Link>
                      ))}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </section>
    </div>
  );
}