import type { Metadata } from "next";
import { Inter, JetBrains_Mono } from "next/font/google";
import "./globals.css";
import AppChrome from "@/components/new-ui/AppChrome";
import { ToastProvider } from "@/components/new-ui/ToastProvider";
import { ThemeProvider } from "@/components/theme/ThemeProvider";
import { ErrorBoundary } from "@/components/ui/error-boundary";

const inter = Inter({
  subsets: ["latin"],
  variable: "--font-inter",
  display: "swap",
});

const jetbrainsMono = JetBrains_Mono({
  subsets: ["latin"],
  variable: "--font-jetbrains",
  display: "swap",
});

export const metadata: Metadata = {
  title: "AI Interview Studio",
  description: "AI 驱动的面试训练平台",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="zh-CN" className={`${inter.variable} ${jetbrainsMono.variable}`} suppressHydrationWarning>
      <body>
        <ThemeProvider>
          <div className="app-root">
            {/* Aurora background */}
            <div className="aurora-bg" aria-hidden="true" />
            
            {/* Particle layer */}
            <div className="particle-layer" aria-hidden="true">
              {Array.from({ length: 24 }).map((_, i) => (
                <div
                  key={i}
                  className="particle-dot"
                  style={{
                    left: `${(i / 24) * 100}%`,
                    animationDuration: `${12 + (i % 8) * 2}s`,
                    animationDelay: `${i * 0.5}s`,
                    width: `${2 + (i % 3)}px`,
                    height: `${2 + (i % 3)}px`,
                  }}
                />
              ))}
            </div>

            <ToastProvider>
              <AppChrome />
              <ErrorBoundary>
                <main className="container page-fade">{children}</main>
              </ErrorBoundary>
            </ToastProvider>
          </div>
        </ThemeProvider>
      </body>
    </html>
  );
}
