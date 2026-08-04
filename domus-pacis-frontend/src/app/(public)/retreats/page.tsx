'use client';
import { useState, useEffect } from 'react';
import Link from 'next/link';
import { Loader2 } from 'lucide-react';
import { serviceAssetsApi, ServiceAssetResponse } from '@/lib/api/serviceAssetsApi';
import { AssetType } from '@/lib/types';

export default function RetreatsPage() {
  const [retreats, setRetreats] = useState<ServiceAssetResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchAssets = async () => {
      try {
        const page = await serviceAssetsApi.listAll({ page: 0, size: 50 });
        setRetreats(page.content.filter(a => a.assetType === AssetType.RETREAT_CENTER && a.isAvailable));
      } catch {
        setRetreats([]);
      } finally {
        setLoading(false);
      }
    };
    fetchAssets();
  }, []);

  return (
    <div className="pt-20">
      <section className="py-20 bg-stone-950 text-center relative">
        <div className="absolute inset-0 bg-gradient-to-b from-sky-950/30 to-stone-950" />
        <div className="relative max-w-3xl mx-auto px-4">
          <p className="text-gold-500 text-sm uppercase tracking-widest mb-3">Spiritual Renewal</p>
          <h1 className="font-display text-5xl text-white mb-5">Retreat Centres</h1>
          <div className="h-px bg-gradient-to-r from-transparent via-gold-500/50 to-transparent max-w-xs mx-auto mb-5" />
          <p className="text-stone-400 text-lg">Quiet, prayerful spaces for personal and group spiritual renewal.</p>
        </div>
      </section>

      <section className="py-16 bg-ivory-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          {loading ? (
            <div className="flex items-center justify-center py-20">
              <Loader2 className="animate-spin text-gold-500" size={32} />
            </div>
          ) : retreats.length === 0 ? (
            <div className="text-center py-20 text-stone-500">
              <p>No retreat centres currently available. Please contact us directly for inquiries.</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
              {retreats.map((r) => (
                <div key={r.id} className="card-hover">
                  <div className="h-48 bg-gradient-to-br from-sky-900 to-stone-900 rounded-xl mb-5 flex items-center justify-center">
                    <div className="text-center text-white">
                      <div className="text-5xl mb-2">🕊️</div>
                      <div className="text-gold-300 text-xs uppercase tracking-widest">
                        Up to {r.capacity} retreatants
                      </div>
                    </div>
                  </div>
                  <h3 className="font-display text-xl text-stone-900 mb-2">{r.name}</h3>
                  <p className="text-stone-600 text-sm leading-relaxed mb-4">{r.description}</p>
                  <div className="flex items-center justify-between mb-5">
                    <div>
                      <div className="font-display text-lg text-stone-900">
                        {r.pricePerUnit.toLocaleString()} RWF
                      </div>
                      <div className="text-stone-400 text-xs">
                        per {r.pricingUnit.toLowerCase().replace('_', ' ')}
                      </div>
                    </div>
                  </div>
                  <Link
                    href={`/booking?asset=${r.id}&type=RETREAT_CENTER`}
                    className="btn-primary w-full justify-center"
                  >
                    Reserve Now
                  </Link>
                </div>
              ))}
            </div>
          )}
        </div>
      </section>
    </div>
  );
}