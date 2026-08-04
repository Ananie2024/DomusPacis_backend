import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

export function middleware(request: NextRequest) {
  const token = request.cookies.get('access_token')?.value;
  const { pathname } = request.nextUrl;

  const isProtected = pathname.startsWith('/dashboard') ||
                      pathname.startsWith('/admin')     ||
                      pathname.match(/^\/(analytics|bookings|customers|finance|inventory|staff|tax|users)/);

  if (isProtected && !token) {
    return NextResponse.redirect(new URL('/login', request.url));
  }

  // Redirect logged-in users away from login page
  if (pathname === '/login' && token) {
    return NextResponse.redirect(new URL('/dashboard', request.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    '/dashboard/:path*',
    '/finance/:path*',
    '/bookings/:path*',
    '/customers/:path*',
    '/tax/:path*',
    '/staff/:path*',
    '/inventory/:path*',
    '/analytics/:path*',
    '/users/:path*',
    '/login',
  ],
};
