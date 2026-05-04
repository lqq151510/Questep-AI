import type { Metadata } from "next";
import "./globals.css";
import { AppChrome } from "@/components/new-ui/AppChrome";
import { ToastProvider } from "@/components/new-ui/ToastProvider";

export const metadata: Metadata = {
  title: "AI Interview Studio",
  description: "AI interview preparation studio with testing, mock interviews, Q&A, and dynamic learning pages."
};

export default function RootLayout({
  children
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="zh-CN">
      <body>
        <ToastProvider>
          <AppChrome>{children}</AppChrome>
        </ToastProvider>
      </body>
    </html>
  );
}
