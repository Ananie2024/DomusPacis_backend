'use client';
import { useState, useEffect } from 'react';
import Link from 'next/link';
import { Wifi, Wind, Coffee, Star, Users, Loader2 } from 'lucide-react';
import { serviceAssetsApi, ServiceAssetResponse } from '@/lib/api/serviceAssetsApi';
import { AssetType } from '@/lib/types';

const AMENITY_ICONS: Record<string, React.ElementType> = {
  'Free Wi-Fi': Wifi,
  'Air Conditioning': Wind,
  default: Coffee,
};

function getRandomGradient() {
  const gradients = [
    'from-stone-700 to-stone-900',
    'from-burgundy-900 to-stone-900',
    'from-stone-800 to-stone-950',
    'from-gold-800 to-stone-950',
    'from-amber-900 to-stone-900',
    'from-teal-900 to-stone-950',
  ];
  return gradients[Math.floor(Math.random() * gradients.length)];
}

export default function RoomsPage() {
  const [rooms, setRooms] = useState<ServiceAssetResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchAssets = async () => {
      try {
        const page = await serviceAssetsApi.listAll({ page: 0, size: 50 });
        setRooms(page.content.filter(a => a.assetType === AssetType.ROOM && a.isAvailable));
      } catch {
        setRooms([]);
      } finally {
        setLoading(false);
      }
    };
    fetchAssets();
  }, []);

  return (
    <div className="pt-20">
      {/* Hero */}
      <section className="py-20 bg-stone-950 text-center relative overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-b from-stone-900 to-stone-950" />
        <div className="relative max-w-3xl mx-auto px-4">
          <p className="text-gold-500 text-sm uppercase tracking-widest mb-3">Accommodation</p>
          <h1 className="font-display text-5xl text-white mb-5">Rooms & Suites</h1>
          <div className="h-px bg-gradient-to-r from-transparent via-gold-500/50 to-transparent max-w-xs mx-auto mb-5" />
          <p className="text-stone-400 text-lg leading-relaxed">
            Rest in comfort and peace. Our rooms are designed to refresh body and spirit.
          </p>
        </div>
      </section>

      {/* Rooms grid */}
      <section className="py-16 bg-ivory-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          {loading ? (
            <div className="flex items-center justify-center py-20">
              <Loader2 className="animate-spin text-gold-500" size={32} />
            </div>
          ) : rooms.length === 0 ? (
            <div className="text-center py-20 text-stone-500">
              <p>No rooms currently available. Please check back later or contact us directly.</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
              {rooms.map((room, index) => {
                const AmenityIcon = AMENITY_ICONS[room.name.includes('Standard') ? 'Free Wi-Fi' : 'default'] || Coffee;
                const gradient = getRandomGradient();
                return (
                  <div key={room.id} className="card overflow-hidden hover:shadow-card-hover transition-shadow duration-300 relative">
                    {index === 0 && (
                      <div className="absolute top-4 right-4 z-10 flex items-center gap-1 px-3 py-1 rounded-full bg-gold-500 text-white text-xs font-medium">
                        <Star size={10} className="fill-white" /> Most Popular
                      </div>
                    )}

                    <div className={`h-48 bg-gradient-to-br ${gradient} flex items-end p-6`}>
                      <div>
                        <div className="text-gold-300 text-xs uppercase tracking-widest mb-1">
                          {room.capacity > 1 ? `${room.capacity} guests` : '1 guest'}
                        </div>
                        <h3 className="font-display text-2xl text-white">{room.name}</h3>
                      </div>
                    </div>

                    <div className="p-6">
                      <div className="flex items-start justify-between mb-3">
                        <div className="flex items-center gap-1.5 text-stone-600 text-sm">
                          <Users size={14} /> {room.capacity} {room.capacity === 1 ? 'guest' : 'guests'}
                        </div>
                        <div className="text-right">
                          <div className="font-display text-2xl text-stone-900">
                            {room.pricePerUnit.toLocaleString()} RWF
                          </div>
                          <div className="text-stone-400 text-xs">
                            per {room.pricingUnit.toLowerCase().replace('_', ' ')}
                          </div>
                        </div>
                      </div>

                      <p className="text-stone-600 text-sm leading-relaxed mb-4">{room.description}</p>

                      <div className="flex flex-wrap gap-2 mb-5">
                        <span className="inline-flex items-center gap-1 px-2.5 py-1 bg-stone-100 text-stone-600 rounded-full text-xs">
                          <AmenityIcon size={10} />
                          {room.capacity > 1 ? 'Double Bed' : 'Single Bed'}
                        </span>
                        <span className="inline-flex items-center gap-1 px-2.5 py-1 bg-stone-100 text-stone-600 rounded-full text-xs">
                          <Wifi size={10} />
                          Free Wi-Fi
                        </span>
                        <span className="inline-flex items-center gap-1 px-2.5 py-1 bg-stone-100 text-stone-600 rounded-full text-xs">
                          <Wind size={10} />
                          Air Conditioning
                        </span>
                      </div>

                      <Link href={`/booking?asset=${room.id}&type=ROOM`} className="btn-primary w-full justify-center">
                        Book This Room
                      </Link>
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