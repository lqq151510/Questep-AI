"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Eye, EyeOff, LogIn } from "lucide-react";
import { PageHero } from "@/components/new-ui/PageHero";
import { useDashboardStore } from "@/stores/useDashboardStore";

export default function LoginPage() {
  const router = useRouter();
  const { login, isLoggedIn, authState } = useDashboardStore();

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [showPwd, setShowPwd] = useState(false);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (isLoggedIn) router.replace("/home");
  }, [isLoggedIn, router]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    if (!username.trim()) { setError("请输入用户名"); return; }
    if (!password) { setError("请输入密码"); return; }

    setBusy(true);
    const err = await login({ username: username.trim(), password });
    setBusy(false);
    if (err) {
      setError(err);
    } else {
      router.replace("/home");
    }
  };

  return (
    <div>
      <PageHero
        kicker="认证"
        title="用户登录"
        description="登录以保存个人题单、训练记录和错题数据。"
      />

      <div className="auth-form-wrap">
        <form className="auth-form" onSubmit={handleSubmit}>
          {error && <div className="auth-error">{error}</div>}

          <div className="auth-field">
            <label className="auth-label" htmlFor="username">用户名</label>
            <input
              id="username"
              className="auth-input"
              type="text"
              placeholder="请输入用户名"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoComplete="username"
              autoFocus
            />
          </div>

          <div className="auth-field">
            <label className="auth-label" htmlFor="password">密码</label>
            <div className="auth-input-wrap">
              <input
                id="password"
                className="auth-input"
                type={showPwd ? "text" : "password"}
                placeholder="请输入密码"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="current-password"
              />
              <button
                type="button"
                className="auth-eye"
                onClick={() => setShowPwd(!showPwd)}
                aria-label={showPwd ? "隐藏密码" : "显示密码"}
              >
                {showPwd ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
          </div>

          <button
            className="btn btn-accent wide"
            type="submit"
            disabled={busy || authState === "syncing"}
          >
            <LogIn size={17} />
            {busy ? "登录中..." : "登录"}
          </button>

          <p className="auth-switch">
            还没有账号？<Link href="/register">立即注册</Link>
          </p>
        </form>
      </div>
    </div>
  );
}
