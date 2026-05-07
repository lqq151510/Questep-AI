import type { NextConfig } from "next";

const backendUrl = process.env.BACKEND_API_URL ?? "http://127.0.0.1:8080";

const nextConfig: NextConfig = {
  devIndicators: false,
  async rewrites() {
    return [
      {
        source: "/api/v1/:path*",
        destination: `${backendUrl}/api/v1/:path*`
      },
      {
        source: "/api/health",
        destination: `${backendUrl}/api/v1/health`
      }
    ];
  }
};

export default nextConfig;
