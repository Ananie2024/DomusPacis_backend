'use client';
import { useState, useEffect, useCallback } from 'react';
import { useForm } from 'react-hook-form';
import { bookingSchema, BookingFormData } from '@/lib/validation/schemas';
import { bookingApi } from '@/lib/api/bookingApi';
import { AssetType, ServiceAsset } from '@/lib/types';
import { ChevronRight, ChevronLeft, CheckCircle, Calendar, User, Building2, ClipboardCheck, AlertTriangle } from 'lucide-react';
import toast from 'react-hot-toast';
import { useSearchParams } from 'next/navigation';

import { serviceAssetsApi } from '@/lib/api/serviceAssetsApi';

const TYPE_META: Record<string, { label: string; icon: string; desc: string }> = {
  ROOM:            { label: 'Room',            icon: '🛏️', desc: 'Overnight accommodation' },
  CONFERENCE_HALL: { label: 'Conference Hall', icon: '🏛️', desc: 'Meetings & events' },
  WEDDING_GARDEN:  { label: 'Wedding Garden',  icon: '🌸', desc: 'Wedding ceremonies' },
  RETREAT_CENTER:  { label: 'Retreat Centre',  icon: '🕊️', desc: 'Spiritual retreats' },
};

const STEPS = [
  { id: 1, label: 'Service',  icon: Building2 },
  { id: 2, label: 'Dates',    icon: Calendar  },
  { id: 3, label: 'Details',  icon: User      },
  { id: 4, label: 'Review',   icon: ClipboardCheck },
];

export default function BookingPage() {
  const searchParams = useSearchParams();
  const assetFromQuery = searchParams.get('asset');
  const typeFromQuery = searchParams.get('type');

  const [step,        setStep]        = useState(1);
  const [assetType,   setAssetType]   = useState<AssetType | null>(null);
  const [selectedAssetId, setSelectedAssetId] = useState<string | null>(null);
  const [submitted,   setSubmitted]   = useState(false);
  const [loading,     setLoading]     = useState(false);
  const [availTypes,  setAvailTypes]  = useState<string[]>([]);

  useEffect(() => {
    serviceAssetsApi.listAll({ page: 0, size: 100 }).then(p => {
      const types = new Set(p.content.filter(a => a.isAvailable).map(a => a.assetType));
      setAvailTypes(Array.from(types));
    }).catch(() => setAvailTypes(Object.keys(TYPE_META)));
  }, []);

  const [assetsOfType, setAssetsOfType] = useState<ServiceAsset[]>([]);

  const ASSET_TYPES = Object.entries(TYPE_META)
    .filter(([key]) => availTypes.length === 0 || availTypes.includes(key))
    .map(([key, meta]) => ({ value: key, ...meta }));

  // N.B. shouldUnregister: false is critical — it keeps field values in form state
  // when inputs unmount between wizard steps.
  const { register, watch, setValue, getValues, setError, clearErrors, formState: { errors } } = useForm<BookingFormData>({
    defaultValues: { numberOfGuests: 1, serviceAssetId: assetFromQuery || '' },
    shouldUnregister: false,
  });

  // When asset type is selected, fetch available assets of that type
  useEffect(() => {
    if (assetType) {
      serviceAssetsApi.listAll({ page: 0, size: 100 }).then(page => {
        const filtered = page.content.filter(a => a.assetType === assetType && a.isAvailable);
        setAssetsOfType(filtered);
        // Auto-select first available asset
        if (filtered.length > 0) {
          setSelectedAssetId(filtered[0].id);
          setValue('serviceAssetId', filtered[0].id);
        }
      });
    } else {
      setAssetsOfType([]);
      setSelectedAssetId(null);
    }
  }, [assetType, setValue]);

  useEffect(() => {
    if (assetFromQuery && typeFromQuery) {
      setAssetType(typeFromQuery as AssetType);
      setSelectedAssetId(assetFromQuery);
      setValue('serviceAssetId', assetFromQuery);
      // Auto-advance to step 2 when service is pre-selected via query params
      setStep(2);
    }
  }, [assetFromQuery, typeFromQuery, setValue]);

  const formValues = watch();

  const submitBooking = useCallback(async (data: BookingFormData) => {
    setLoading(true);
    try {
      await bookingApi.createBooking({
        serviceAssetId: data.serviceAssetId,
        checkInDate: data.checkIn,
        checkOutDate: data.checkOut,
        numberOfGuests: data.numberOfGuests,
        specialRequests: data.notes ?? undefined,
        firstName: data.firstName,
        lastName: data.lastName,
        email: data.email,
        phone: data.phone,
      });
      setSubmitted(true);
    } catch (err: any) {
      const message = err?.response?.data?.message || err?.message || 'Booking could not be submitted. Please try again or contact us directly.';
      toast.error(message);
    } finally {
      setLoading(false);
    }
  }, []);

  const handleSubmitForm = async (e: React.FormEvent) => {
    e.preventDefault();
    clearErrors();
    const data = getValues();
    const result = bookingSchema.safeParse(data);
    if (result.success) {
      submitBooking(result.data);
    } else {
      for (const issue of result.error.issues) {
        const field = issue.path[0] as keyof BookingFormData;
        setError(field, { message: issue.message });
      }
      toast.error(result.error.issues[0]?.message || 'Please fix the errors.', { duration: 4000 });
    }
  };

  if (submitted) {
    return (
      <div className="pt-20 min-h-screen bg-ivory-50 flex items-center justify-center px-4">
        <div className="card max-w-lg w-full text-center py-16">
          <CheckCircle size={56} className="text-green-500 mx-auto mb-5" />
          <h2 className="font-display text-3xl text-stone-900 mb-3">Booking Submitted!</h2>
          <p className="text-stone-600 leading-relaxed mb-6">
            Thank you for choosing Domus Pacis. Your booking request has been received and our team will confirm your reservation via email within 24 hours.
          </p>
          <a href="/home" className="btn-primary mx-auto">Return to Home</a>
        </div>
      </div>
    );
  }

  return (
    <div className="pt-20 min-h-screen bg-ivory-50">
      {/* Page header */}
      <section className="py-16 bg-stone-950 text-center">
        <div className="max-w-2xl mx-auto px-4">
          <p className="text-gold-500 text-sm uppercase tracking-widest mb-3">Reservations</p>
          <h1 className="font-display text-4xl text-white mb-3">Make a Booking</h1>
          <p className="text-stone-400">Complete the form below and we&lsquo;ll confirm your reservation shortly.</p>
        </div>
      </section>

      <div className="max-w-3xl mx-auto px-4 sm:px-6 py-12">
        {/* Step indicator */}
        <div className="flex items-center justify-center mb-10">
          {STEPS.map((s, i) => {
            const Icon = s.icon;
            const active = step === s.id;
            const done   = step > s.id;
            return (
              <div key={s.id} className="flex items-center">
                <div className={`flex items-center gap-2 px-4 py-2 rounded-full text-sm font-medium transition-all ${
                  active ? 'bg-gold-600 text-white shadow-gold' :
                  done   ? 'bg-green-100 text-green-700' :
                           'bg-stone-100 text-stone-400'
                }`}>
                  <Icon size={15} />
                  <span className="hidden sm:inline">{s.label}</span>
                </div>
                {i < STEPS.length - 1 && (
                  <div className={`w-8 h-px mx-1 ${done ? 'bg-green-400' : 'bg-stone-200'}`} />
                )}
              </div>
            );
          })}
        </div>

        <form onSubmit={handleSubmitForm}>
          {/* ── Step 1 — Service type ── */}
          {step === 1 && (
            <div className="card animate-fade-in">
              <h2 className="font-display text-2xl text-stone-900 mb-6">Choose a Service</h2>
              <div className="grid grid-cols-2 gap-4 mb-8">
                {ASSET_TYPES.map((t) => (
                  <button
                    key={t.value}
                    type="button"
                    onClick={() => {
                      setAssetType(t.value as AssetType);
                    }}
                    className={`p-5 rounded-xl border-2 text-left transition-all duration-200 ${
                      assetType === t.value
                        ? 'border-gold-500 bg-gold-50 shadow-gold'
                        : 'border-stone-200 hover:border-gold-300 hover:bg-ivory-100'
                    }`}
                  >
                    <div className="text-3xl mb-2">{t.icon}</div>
                    <div className="font-medium text-stone-900 text-sm">{t.label}</div>
                    <div className="text-stone-500 text-xs mt-0.5">{t.desc}</div>
                  </button>
                ))}
              </div>

              {/* Asset selector dropdown */}
              {assetType && assetsOfType.length > 0 && (
                <div className="mb-6">
                  <label className="label">Select {assetType.replace('_', ' ')}</label>
                  <select
                    value={selectedAssetId || ''}
                    onChange={(e) => {
                      setSelectedAssetId(e.target.value);
                      setValue('serviceAssetId', e.target.value);
                    }}
                    className="input"
                  >
                    {assetsOfType.map(asset => (
                      <option key={asset.id} value={asset.id}>
                        {asset.name} - {asset.description || 'No description'}
                      </option>
                    ))}
                  </select>
                  {errors.serviceAssetId && <p className="form-error">{errors.serviceAssetId.message}</p>}
                </div>
              )}

              {!assetType && <p className="text-stone-500 text-sm mb-4">Please select a service type above</p>}
              <button type="button" disabled={!assetType} onClick={() => setStep(2)} className="btn-primary w-full justify-center">
                Continue <ChevronRight size={16} />
              </button>
            </div>
          )}

          {/* ── Step 2 — Dates ── */}
          {step === 2 && (
            <div className="card animate-fade-in">
              <h2 className="font-display text-2xl text-stone-900 mb-6">Select Dates</h2>
              <div className="grid grid-cols-2 gap-4 mb-4">
                <div>
                  <label className="label">Check-in Date</label>
                  <input type="date" {...register('checkIn')} className="input" />
                  {errors.checkIn && <p className="form-error">{errors.checkIn.message}</p>}
                </div>
                <div>
                  <label className="label">Check-out / End Date</label>
                  <input type="date" {...register('checkOut')} className="input" />
                  {errors.checkOut && <p className="form-error">{errors.checkOut.message}</p>}
                </div>
              </div>
              <div className="mb-6">
                <label className="label">Number of Guests</label>
                <input type="number" min={1} max={500} {...register('numberOfGuests', { valueAsNumber: true })} className="input w-32" />
                {errors.numberOfGuests && <p className="form-error">{errors.numberOfGuests.message}</p>}
              </div>
              <div className="mb-6">
                <label className="label">Special Requests (optional)</label>
                <textarea rows={3} {...register('notes')} className="input resize-none" placeholder="Any special requirements or notes..." />
              </div>
              <div className="flex flex-col sm:flex-row gap-3">
                <button type="button" onClick={() => setStep(1)} className="btn-secondary w-full sm:flex-1 justify-center">
                  <ChevronLeft size={16} /> Back
                </button>
                <button type="button" onClick={() => setStep(3)} className="btn-primary w-full sm:flex-1 justify-center">
                  Continue <ChevronRight size={16} />
                </button>
              </div>
            </div>
          )}

          {/* ── Step 3 — Guest details ── */}
          {step === 3 && (
            <div className="card animate-fade-in">
              <h2 className="font-display text-2xl text-stone-900 mb-6">Your Details</h2>
              <div className="grid grid-cols-2 gap-4 mb-4">
                <div>
                  <label className="label">First Name</label>
                  <input {...register('firstName')} className="input" placeholder="John" />
                  {errors.firstName && <p className="form-error">{errors.firstName.message}</p>}
                </div>
                <div>
                  <label className="label">Last Name</label>
                  <input {...register('lastName')} className="input" placeholder="Doe" />
                  {errors.lastName && <p className="form-error">{errors.lastName.message}</p>}
                </div>
              </div>
              <div className="mb-4">
                <label className="label">Email Address</label>
                <input type="email" {...register('email')} className="input" placeholder="john@example.com" />
                {errors.email && <p className="form-error">{errors.email.message}</p>}
              </div>
              <div className="mb-6">
                <label className="label">Phone Number</label>
                <input type="tel" {...register('phone')} className="input" placeholder="+250 78 000 0000" />
                {errors.phone && <p className="form-error">{errors.phone.message}</p>}
              </div>
              <div className="flex flex-col sm:flex-row gap-3">
                <button type="button" onClick={() => setStep(2)} className="btn-secondary w-full sm:flex-1 justify-center">
                  <ChevronLeft size={16} /> Back
                </button>
                <button type="button" onClick={() => setStep(4)} className="btn-primary w-full sm:flex-1 justify-center">
                  Review <ChevronRight size={16} />
                </button>
              </div>
            </div>
          )}

          {/* ── Step 4 — Review ── */}
          {step === 4 && (
            <div className="card animate-fade-in">
              <h2 className="font-display text-2xl text-stone-900 mb-6">Review Your Booking</h2>

              {/* Validation errors summary */}
              {Object.keys(errors).length > 0 && (
                <div className="bg-red-50 border border-red-200 rounded-xl p-4 mb-6 flex items-start gap-3">
                  <AlertTriangle size={20} className="text-red-500 shrink-0 mt-0.5" />
                  <div>
                    <p className="font-medium text-red-800 text-sm mb-1">Please fix the following before submitting:</p>
                    <ul className="list-disc list-inside text-red-700 text-xs space-y-0.5">
                      {Object.entries(errors).map(([field, err]) => (
                        <li key={field}>{String(err?.message || 'Invalid value')}</li>
                      ))}
                    </ul>
                  </div>
                </div>
              )}

              <div className="bg-ivory-100 rounded-xl p-5 space-y-3 mb-6 text-sm">
                <div className="flex justify-between">
                  <span className="text-stone-500">Service Type</span>
                  <span className="font-medium text-stone-800">{ASSET_TYPES.find(t => t.value === assetType)?.label}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-stone-500">Check-in</span>
                  <span className="font-medium text-stone-800">{formValues.checkIn || '—'}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-stone-500">Check-out</span>
                  <span className="font-medium text-stone-800">{formValues.checkOut || '—'}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-stone-500">Guests</span>
                  <span className="font-medium text-stone-800">{formValues.numberOfGuests}</span>
                </div>
                <div className="border-t border-stone-200 pt-3 flex justify-between">
                  <span className="text-stone-500">Name</span>
                  <span className="font-medium text-stone-800">{formValues.firstName} {formValues.lastName}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-stone-500">Email</span>
                  <span className="font-medium text-stone-800">{formValues.email}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-stone-500">Phone</span>
                  <span className="font-medium text-stone-800">{formValues.phone}</span>
                </div>
              </div>
              <p className="text-xs text-stone-400 mb-5">
                By submitting this booking, you agree to our Terms of Use and Privacy Policy. Our team will contact you within 24 hours to confirm availability and provide a quote.
              </p>
              <div className="flex flex-col sm:flex-row gap-3">
                <button type="button" onClick={() => setStep(3)} className="btn-secondary w-full sm:flex-1 justify-center">
                  <ChevronLeft size={16} /> Back
                </button>
                <button type="submit" disabled={loading} className="btn-primary w-full sm:flex-1 justify-center">
                  {loading ? 'Submitting…' : 'Submit Booking'}
                </button>
              </div>
            </div>
          )}
        </form>
      </div>
    </div>
  );
}