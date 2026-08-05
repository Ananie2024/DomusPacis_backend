/** @type {import('next').NextConfig} */
const nextConfig = {
  images: {
    remotePatterns: [
      { protocol: 'http', hostname: 'localhost' },
      { protocol: 'https', hostname: 'domuspacis.rw' },
      { protocol: 'https', hostname: 'images.unsplash.com' },
      { protocol: 'https', hostname: 'Ananie2024-domuspacis.hf.space' },
    ],
  },

  // Proxy /api/v1/* requests to the backend so the browser never makes
  // a cross-origin call — this eliminates CORS "Network Error" issues.
  async rewrites() {
    // NOTE: NEXT_PUBLIC_API_URL must be the base URL WITHOUT /api/v1.
    // e.g. https://Ananie2024-domuspacis.hf.space
    const backendUrl = process.env.NEXT_PUBLIC_API_URL || 'https://Ananie2024-domuspacis.hf.space';
    return [
      {
        source: '/api/v1/:path*',
        destination: `${backendUrl}/:path*`,
      },
    ];
  },
};

module.exports = nextConfig;
