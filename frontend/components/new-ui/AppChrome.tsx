"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { LogOut, Menu, User, X } from "lucide-react";
import { ThemeToggle } from "@/components/theme/ThemeToggle";
import { useDashboardStore } from "@/stores/useDashboardStore";

const navLinks = [
  { href: "/home", label: "总览" },
  { href: "/ai-test", label: "AI测试" },
  { href: "/ai-interviewer", label: "AI面试官" },
  { href: "/ai-qa", label: "AI问答" },
  { href: "/knowledge-base", label: "知识库" },
  { href: "/question-bank", label: "题库" },
  { href: "/wrong-answers", label: "错题本" },
  { href: "/interview-tips", label: "面试技巧" },
];

export default function AppChrome() {
  const pathname = usePathname();
  const router = useRouter();
  const { user, isLoggedIn, logout } = useDashboardStore();
  const [menuOpen, setMenuOpen] = useState(false);

  // Close mobile menu on route change
  useEffect(() => {
    setMenuOpen(false);
  }, [pathname]);

  // Close mobile menu on Escape key
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.key === "Escape") setMenuOpen(false);
    };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, []);

  return (
    <>
      <nav className="top-nav">
        <div className="top-nav-inner">
          <Link href="/" className="brand-logo">
            <span className="brand-mark">AI</span>
            <span>面试训练</span>
          </Link>

          <div className="desktop-nav">
            {navLinks.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                className={`nav-link ${pathname === link.href ? "active" : ""}`}
              >
                {link.label}
              </Link>
            ))}
          </div>

          <div className="chrome-actions">
            {isLoggedIn && user ? (
              <>
                <span className="chrome-user-badge">
                  <User size={14} />
                  {user.username}
                </span>
                <button
                  type="button"
                  className="chrome-auth-link"
                  onClick={() => { logout(); router.push("/login"); }}
                >
                  <LogOut size={14} />
                  退出
                </button>
              </>
            ) : (
              <Link href="/login" className="chrome-auth-link">
                登录 / 注册
              </Link>
            )}
            <Link href="/home" className="chrome-auth-link subtle">
              模型配置
            </Link>
            <ThemeToggle />
            <button
              type="button"
              className="menu-toggle"
              onClick={() => setMenuOpen((prev) => !prev)}
              aria-label={menuOpen ? "关闭菜单" : "打开菜单"}
              aria-expanded={menuOpen}
            >
              {menuOpen ? <X size={18} /> : <Menu size={18} />}
            </button>
          </div>
        </div>
      </nav>

      {/* Mobile dropdown */}
      <div className={`mobile-nav ${menuOpen ? "active" : ""}`}>
        {navLinks.map((link) => (
          <Link
            key={link.href}
            href={link.href}
            className={`mobile-link ${pathname === link.href ? "active" : ""}`}
          >
            {link.label}
          </Link>
        ))}
      </div>
    </>
  );
}
