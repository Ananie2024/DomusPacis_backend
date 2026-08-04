/** @type {import('next').NextConfig} */
const nextConfig = {
  images: {
    remotePatterns: [
      { protocol: 'http', hostname: 'localhost' },
      { protocol: 'https', hostname: 'domuspacis.rw' },
      { protocol: 'https', hostname: 'images.unsplash.com' },
      { protocol: 'https', hostname: 'domuspacisbackend-production.up.railway.app' },
      { protocol: 'https', hostname: 'Ananie2024-domuspacis.hf.space' },
    ],
  },

  // Proxy /api/v1/* requests to the backend so the browser never makes
  // a cross-origin call — this eliminates CORS "Network Error" issues.
  async rewrites() {
    const backendUrl = process.env.NEXT_PUBLIC_API_URL || 'https://Ananie2024-domuspacis.hf.space/api/v1';
    return [
      {
        source: '/api/v1/:path*',
        destination: `${backendUrl}/:path*`,
      },
    ];
  },
};

module.exports = nextConfig;
