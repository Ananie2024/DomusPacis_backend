import Image from 'next/image';
import Link from 'next/link';
import { MapPin, Phone, Mail, Facebook, Twitter, Instagram } from 'lucide-react';

export function PublicFooter() {
  return (
    <footer className="bg-gradient-to-br from-yellow-50 via-white to-blue-50 text-stone-700">

      {/* Divider */}
      <div className="h-px bg-gradient-to-r from-transparent via-yellow-400/60 to-transparent" />

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-12">

          {/* Brand */}
          <div>
            <div className="mb-6">
              <Image
                src="/images/domus-pacis-logo.png"
                alt="Domus Pacis Logo"
                width={260}
                height={100}
                className="h-20 w-auto object-contain"
              />
            </div>

            <div className="w-16 h-[2px] bg-yellow-400 mb-4 rounded-full" />

            <p className="text-sm leading-relaxed text-stone-600 max-w-sm mb-6">
              A sanctuary of hospitality and spiritual renewal under the Catholic Archdiocese of Kigali — offering accommodation, conferences, weddings and retreats.
            </p>

            <div className="flex items-center gap-3">
              {[Facebook, Twitter, Instagram].map((Icon, i) => (
                <a
                  key={i}
                  href="#"
                  className="w-10 h-10 rounded-full bg-white shadow-sm hover:bg-yellow-400 flex items-center justify-center text-stone-600 hover:text-white hover:scale-110 transition-all"
                >
                  <Icon size={16} />
                </a>
              ))}
            </div>
          </div>

          {/* Services */}
          <div>
            <h4 className="font-display text-stone-900 text-base font-semibold mb-5">
              Our Services
            </h4>
            <ul className="space-y-2 text-sm">
              {[
                ['Accommodation', '/rooms'],
                ['Conference Halls', '/conference'],
                ['Wedding Gardens', '/weddings'],
                ['Retreat Centres', '/retreats'],
                ['Book a Service', '/booking'],
              ].map(([label, href]) => (
                <li key={href}>
                  <Link href={href} className="hover:text-blue-700 transition-colors">
                    {label}
                  </Link>
                </li>
              ))}
            </ul>
          </div>

          {/* Quick links */}
          <div>
            <h4 className="font-display text-stone-900 text-base font-semibold mb-5">
              Quick Links
            </h4>
            <ul className="space-y-2 text-sm">
              {[
                ['Home',         '/'],
                ['About Us',     '/#about'],
                ['Services',     '/services'],
                ['Gallery',      '/services#gallery'],
                ['Testimonials', '/#testimonials'],
                ['Contact',      '/contact'],
              ].map(([label, href]) => (
                <li key={href}>
                  <Link href={href} className="hover:text-blue-700 transition-colors">
                    {label}
                  </Link>
                </li>
              ))}
            </ul>
          </div>

          {/* Contact */}
          <div>
            <h4 className="font-display text-stone-900 text-base font-semibold mb-5">
              Contact Us
            </h4>
            <ul className="space-y-4 text-sm">
              <li className="flex items-start gap-3">
                <MapPin size={16} className="text-yellow-500 mt-1 flex-shrink-0" />
                <span>KN 3 Ave, Kigali, Rwanda<br />Catholic Archdiocese</span>
              </li>

              <li className="flex items-center gap-3">
                <Phone size={16} className="text-yellow-500 flex-shrink-0" />
                <a href="tel:+250781234567" className="hover:text-blue-700">
                  +250 78 123 4567
                </a>
              </li>

              <li className="flex items-center gap-3">
                <Mail size={16} className="text-yellow-500 flex-shrink-0" />
                <a href="mailto:info@domuspacis.rw" className="hover:text-blue-700">
                  info@domuspacis.rw
                </a>
              </li>
            </ul>
          </div>

        </div>
      </div>

      {/* Bottom */}
      <div className="border-t border-yellow-200 py-6">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-stone-500">
          <span>
            © {new Date().getFullYear()} Domus Pacis — Catholic Archdiocese of Kigali. All rights reserved.
          </span>

          <div className="flex items-center gap-5">
            <Link href="/privacy" className="hover:text-stone-700 transition-colors">
              Privacy Policy
            </Link>
            <Link href="/terms" className="hover:text-stone-700 transition-colors">
              Terms of Use
            </Link>
          </div>
        </div>
      </div>
    </footer>
  );
}