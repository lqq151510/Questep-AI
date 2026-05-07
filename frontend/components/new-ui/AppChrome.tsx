"use client";

import type { ReactNode } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { Menu, X } from "lucide-react";
import { NAV_ITEMS } from "@/components/new-ui/nav-config";

type AppChromeProps = {
  children: ReactNode;
};

function isActive(pathname: string, href: string) {
  if (href === "/") {
    return pathname === "/";
  }
  return pathname === href || pathname.startsWith(`${href}/`);
}

export function AppChrome({ children }: AppChromeProps) {
  const pathname = usePathname();
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    setMenuOpen(false);
  }, [pathname]);

  const particles = useMemo(
    () =>
      Array.from({ length: 22 }, (_, index) => ({
        id: index,
        style: {
          left: `${(index * 17) % 100}%`,
          animationDelay: `${-index * 0.7}s`,
          animationDuration: `${11 + (index % 6) * 1.3}s`
        }
      })),
    []
  );

  return (
    <div className="app-root">
      <div className="glow-bg" />
      <div className="particle-layer" aria-hidden>
        {particles.map((particle) => (
          <span key={particle.id} className="particle-dot" style={particle.style} />
        ))}
      </div>

      <header className="top-nav">
        <div className="top-nav-inner">
          <Link href="/" className="brand-logo">
            <span className="brand-mark">AI</span>
            <span className="brand-text">Interview Studio</span>
          </Link>

          <nav className="desktop-nav" aria-label="主导航">
            {NAV_ITEMS.map((item) => (
              <Link
                key={item.href}
                href={item.href}
                className={isActive(pathname, item.href) ? "nav-link active" : "nav-link"}
              >
                {item.label}
              </Link>
            ))}
          </nav>

          <button
            type="button"
            className="menu-toggle"
            onClick={() => setMenuOpen((prev) => !prev)}
            aria-label="切换导航菜单"
            aria-expanded={menuOpen}
          >
            {menuOpen ? <X size={18} /> : <Menu size={18} />}
          </button>
        </div>
      </header>

      <nav className={menuOpen ? "mobile-nav active" : "mobile-nav"} aria-label="移动端导航">
        {NAV_ITEMS.map((item) => (
          <Link
            key={item.href}
            href={item.href}
            className={isActive(pathname, item.href) ? "mobile-link active" : "mobile-link"}
          >
            {item.label}
          </Link>
        ))}
      </nav>

      <main className="page-fade">{children}</main>
    </div>
  );
}
