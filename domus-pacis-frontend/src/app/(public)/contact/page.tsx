'use client';
import { useState } from 'react';
import { Mail, Phone, MapPin, Clock, Send, Loader2 } from 'lucide-react';
import toast from 'react-hot-toast';

export default function ContactPage() {
  const [form, setForm] = useState({ name: '', email: '', subject: '', message: '' });
  const [sending, setSending] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.name || !form.email || !form.message) {
      toast.error('Please fill in all required fields.');
      return;
    }
    setSending(true);
    // Simulate sending — in production, POST to a backend contact endpoint
    await new Promise(r => setTimeout(r, 1000));
    toast.success('Thank you! We will get back to you shortly.');
    setForm({ name: '', email: '', subject: '', message: '' });
    setSending(false);
  };

  return (
    <div className="pt-20">
      <section className="py-20 bg-stone-950 text-center relative">
        <div className="absolute inset-0 bg-gradient-to-b from-stone-900 to-stone-950" />
        <div className="relative max-w-3xl mx-auto px-4">
          <p className="text-gold-500 text-sm uppercase tracking-widest mb-3">Get in Touch</p>
          <h1 className="font-display text-5xl text-white mb-5">Contact Us</h1>
          <div className="h-px bg-gradient-to-r from-transparent via-gold-500/50 to-transparent max-w-xs mx-auto mb-5" />
          <p className="text-stone-400 text-lg">We would love to hear from you. Reach out with any questions or reservation requests.</p>
        </div>
      </section>

      <section className="py-16 bg-ivory-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 grid lg:grid-cols-2 gap-12">
          {/* Contact details */}
          <div>
            <h2 className="font-display text-3xl text-stone-900 mb-6">Our Information</h2>
            <div className="space-y-5">
              {[
                { icon: MapPin, label: 'Address', value: 'Domus Pacis, Kigali, Rwanda' },
                { icon: Phone, label: 'Phone', value: '+250 788 000 000' },
                { icon: Mail, label: 'Email', value: 'info@domuspacis.rw' },
                { icon: Clock, label: 'Office Hours', value: 'Mon–Sat: 8:00 AM – 6:00 PM' },
              ].map(({ icon: Icon, label, value }) => (
                <div key={label} className="flex items-start gap-4">
                  <div className="w-10 h-10 rounded-lg bg-gold-100 text-gold-600 flex items-center justify-center flex-shrink-0">
                    <Icon size={18} />
                  </div>
                  <div>
                    <div className="text-sm text-stone-400">{label}</div>
                    <div className="font-medium text-stone-800">{value}</div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Contact form */}
          <div>
            <h2 className="font-display text-3xl text-stone-900 mb-6">Send a Message</h2>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="label">Full Name *</label>
                  <input value={form.name} onChange={e => setForm(p => ({ ...p, name: e.target.value }))} className="input" placeholder="Your name" />
                </div>
                <div>
                  <label className="label">Email *</label>
                  <input type="email" value={form.email} onChange={e => setForm(p => ({ ...p, email: e.target.value }))} className="input" placeholder="your@email.com" />
                </div>
              </div>
              <div>
                <label className="label">Subject</label>
                <input value={form.subject} onChange={e => setForm(p => ({ ...p, subject: e.target.value }))} className="input" placeholder="How can we help?" />
              </div>
              <div>
                <label className="label">Message *</label>
                <textarea rows={5} value={form.message} onChange={e => setForm(p => ({ ...p, message: e.target.value }))} className="input resize-none" placeholder="Tell us more about your inquiry…" />
              </div>
              <button type="submit" disabled={sending} className="btn-primary w-full justify-center">
                {sending ? <><Loader2 size={15} className="animate-spin" /> Sending…</> : <><Send size={15} /> Send Message</>}
              </button>
            </form>
          </div>
        </div>
      </section>
    </div>
  );
}