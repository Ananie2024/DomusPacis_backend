'use client';
import { useState } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { loginSchema, LoginFormData } from '@/lib/validation/schemas';
import { authApi } from '@/lib/api/authApi';
import { useAuthStore } from '@/stores/authStore';
import { useRouter } from 'next/navigation';
import { Eye, EyeOff, LogIn } from 'lucide-react';
import toast from 'react-hot-toast';
import { UserRole } from '@/lib/types';

export default function LoginPage() {
  const [showPwd, setShowPwd] = useState(false);
  const { setAuth } = useAuthStore();
  const router = useRouter();

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
  });

const onSubmit = async (data: LoginFormData) => {
  try {
    const res = await authApi.login(data);
    setAuth(
      {
        id:        res.userId,
        email:     res.email,
        firstName: res.firstName,
        lastName:  res.lastName,
        role:      res.role as UserRole,
        createdAt: new Date().toISOString(),
      },
      res.accessToken,
      res.refreshToken
    );
    toast.success(`Welcome back, ${res.firstName}!`);
    router.push('/dashboard');
  } catch (err) {
    console.error('Login error:', err);
    toast.error('Invalid email or password.');
  }
};

  return (
    <div className="min-h-screen bg-stone-950 flex items-center justify-center px-4">
      {/* Background */}
      <div className="absolute inset-0 bg-gradient-to-br from-burgundy-950/60 via-stone-950 to-stone-900" />
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 text-[30rem] text-white/[0.015] font-serif select-none pointer-events-none">✝</div>

      <div className="relative w-full max-w-sm">
        {/* Logo */}
         <div className="mb-8">
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
           <p className="text-stone-500 text-sm mt-2">Admin Portal</p>
         </div>

        <div className="bg-white/[0.06] backdrop-blur-sm border border-white/10 rounded-2xl p-8">
          <h2 className="font-display text-xl text-white mb-6">Sign in to your account</h2>

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div>
              <label className="block text-stone-400 text-sm mb-1.5">Email</label>
              <input
                type="email"
                {...register('email')}
                className="w-full px-4 py-2.5 bg-white/5 text-white text-sm border border-white/10 rounded-lg placeholder:text-stone-600 focus:outline-none focus:ring-2 focus:ring-gold-400 focus:border-gold-400 transition-all"
                placeholder="Name@domuspacis.rw"
                suppressHydrationWarning
              />
              {errors.email && <p className="text-red-400 text-xs mt-1">{errors.email.message}</p>}
            </div>

            <div>
              <label className="block text-stone-400 text-sm mb-1.5">Password</label>
              <div className="relative">
                <input
                  type={showPwd ? 'text' : 'password'}
                  {...register('password')}
                  className="w-full px-4 py-2.5 bg-white/5 text-white text-sm border border-white/10 rounded-lg placeholder:text-stone-600 focus:outline-none focus:ring-2 focus:ring-gold-400 focus:border-gold-400 transition-all pr-10"
                  placeholder="your password"
                  suppressHydrationWarning
                />
                <button
                  type="button"
                  onClick={() => setShowPwd(!showPwd)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-stone-500 hover:text-stone-300"
                >
                  {showPwd ? <EyeOff size={15} /> : <Eye size={15} />}
                </button>
              </div>
              {errors.password && <p className="text-red-400 text-xs mt-1">{errors.password.message}</p>}
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full flex items-center justify-center gap-2 py-3 bg-gold-600 hover:bg-gold-700 text-white font-medium text-sm rounded-lg transition-all duration-200 mt-2 shadow-gold disabled:opacity-60"
            >
              {isSubmitting ? (
                <span className="animate-spin w-4 h-4 border-2 border-white/30 border-t-white rounded-full" />
              ) : (
                <LogIn size={15} />
              )}
              {isSubmitting ? 'Signing in…' : 'Sign In'}
            </button>
          </form>

          <p className="text-center text-stone-600 text-xs mt-6">
            Restricted access — Domus Pacis staff only.
          </p>
        </div>
      </div>
    </div>
  );
}
