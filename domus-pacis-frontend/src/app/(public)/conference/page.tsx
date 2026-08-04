'use client';
import { useState, useEffect } from 'react';
import Link from 'next/link';
import { Monitor, Mic, Wifi, Users, Coffee, Loader2 } from 'lucide-react';
import { serviceAssetsApi, ServiceAssetResponse } from '@/lib/api/serviceAssetsApi';
import { AssetType } from '@/lib/types';

function getRandomGradient() {
  const gradients = [
    'from-stone-700 to-stone-900',
    'from-burgundy-900 to-stone-900',
    'from-stone-800 to-stone-950',
    'from-gold-800 to-stone-950',
    'from-teal-900 to-stone-950',
    'from-sky-900 to-stone-950',
  ];
  return gradients[Math.floor(Math.random() * gradients.length)];
}

export default function ConferencePage() {
  const [halls, setHalls] = useState<ServiceAssetResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchAssets = async () => {
      try {
        const page = await serviceAssetsApi.listAll({ page: 0, size: 50 });
        setHalls(page.content.filter(a => a.assetType === AssetType.CONFERENCE_HALL && a.isAvailable));
      } catch {
        setHalls([]);
      } finally {
        setLoading(false);
      }
    };
    fetchAssets();
  }, []);

  return (
    <div className="pt-20">
      <section className="py-20 bg-stone-950 text-center relative">
        <div className="absolute inset-0 bg-gradient-to-b from-stone-900 to-stone-950" />
        <div className="relative max-w-3xl mx-auto px-4">
          <p className="text-gold-500 text-sm uppercase tracking-widest mb-3">Meetings & Events</p>
          <h1 className="font-display text-5xl text-white mb-5">Conference Halls</h1>
          <div className="h-px bg-gradient-to-r from-transparent via-gold-500/50 to-transparent max-w-xs mx-auto mb-5" />
          <p className="text-stone-400 text-lg">Professional, fully equipped spaces for every event size and format.</p>
        </div>
      </section>

      <section className="py-16 bg-ivory-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          {loading ? (
            <div className="flex items-center justify-center py-20">
              <Loader2 className="animate-spin text-gold-500" size={32} />
            </div>
          ) : halls.length === 0 ? (
            <div className="text-center py-20 text-stone-500">
              <p>No conference halls currently available. Please contact us directly for inquiries.</p>
            </div>
          ) : (
            <div className="space-y-8">
              {halls.map((hall) => {
                const gradient = getRandomGradient();
                return (
                  <div key={hall.id} className="card overflow-hidden">
                    <div className="flex flex-col lg:flex-row">
                      <div className={`lg:w-1/3 min-h-52 bg-gradient-to-br ${gradient} flex items-center justify-center p-10`}>
                        <div className="text-center text-white">
                          <div className="font-display text-5xl font-bold text-gold-400 mb-2">{hall.capacity}</div>
                          <div className="text-stone-300 text-sm">delegates</div>
                          <div className="mt-3 text-xs text-stone-400 uppercase tracking-widest">
                            {hall.pricingUnit.replace('_', ' ')}
                          </div>
                        </div>
                      </div>
                      <div className="lg:w-2/3 p-8">
                        <div className="flex items-start justify-between mb-4">
                          <div>
                            <h3 className="font-display text-2xl text-stone-900">{hall.name}</h3>
                            <div className="flex items-center gap-1.5 text-stone-500 text-sm mt-1">
                              <Users size={13} /> Up to {hall.capacity} delegates
                            </div>
                          </div>
                          <div className="text-right">
                            <div className="font-display text-xl text-stone-900">
                              {hall.pricePerUnit.toLocaleString()} RWF
                            </div>
                            <div className="text-stone-400 text-xs">
                              per {hall.pricingUnit.toLowerCase().replace('_', ' ')}
                            </div>
                          </div>
                        </div>
                        <p className="text-stone-600 text-sm leading-relaxed mb-5">{hall.description}</p>
                        <div className="grid grid-cols-2 sm:grid-cols-3 gap-2 mb-6">
                          {[
                            'Projector & screen',
                            'PA system',
                            'Podium',
                            'Air conditioning',
                            'Wi-Fi',
                            'Catering service',
                          ].map((f) => (
                            <div key={f} className="flex items-center gap-2 text-xs text-stone-600 bg-stone-50 rounded-lg px-3 py-2">
                              {f.includes('Wi-Fi') ? <Wifi size={12} className="text-gold-500" /> :
                               f.includes('Projector') || f.includes('screen') ? <Monitor size={12} className="text-gold-500" /> :
                               f.includes('PA') || f.includes('Mic') ? <Mic size={12} className="text-gold-500" /> :
                               f.includes('Catering') || f.includes('Coffee') ? <Coffee size={12} className="text-gold-500" /> :
                               <Users size={12} className="text-gold-500" />}
                              {f}
                            </div>
                          ))}
                        </div>
                        <Link
                          href={`/booking?asset=${hall.id}&type=CONFERENCE_HALL`}
                          className="btn-primary"
                        >
                          Book This Hall
                        </Link>
                      </div>
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