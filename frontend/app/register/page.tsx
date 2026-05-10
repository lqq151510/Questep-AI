"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Eye, EyeOff, RefreshCw, ShieldCheck, UserPlus } from "lucide-react";
import { PageHero } from "@/components/new-ui/PageHero";
import { useDashboardStore } from "@/stores/useDashboardStore";

export default function RegisterPage() {
  const router = useRouter();
  const { register, fetchCaptcha, isLoggedIn, authState } = useDashboardStore();

  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPwd, setShowPwd] = useState(false);
  const [captchaId, setCaptchaId] = useState("");
  const [captchaCode, setCaptchaCode] = useState("");
  const [captchaDisplay, setCaptchaDisplay] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (isLoggedIn) { router.replace("/home"); return; }
    const init = async () => {
      const result = await fetchCaptcha();
      if (result) {
        setCaptchaId(result.captchaId);
        setCaptchaDisplay(result.captchaCode);
      }
    };
    init();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isLoggedIn, router]);

  const loadCaptcha = async () => {
    const result = await fetchCaptcha();
    if (result) {
      setCaptchaId(result.captchaId);
      setCaptchaDisplay(result.captchaCode);
      setCaptchaCode("");
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    if (!username.trim()) { setError("请输入用户名"); return; }
    if (username.trim().length < 3) { setError("用户名至少 3 个字符"); return; }
    if (!email.trim()) { setError("请输入邮箱"); return; }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) { setError("邮箱格式不正确"); return; }
    if (!password) { setError("请输入密码"); return; }
    if (password.length < 8) { setError("密码至少 8 位"); return; }
    if (!captchaCode.trim()) { setError("请输入验证码"); return; }

    setBusy(true);
    const err = await register({
      username: username.trim(),
      email: email.trim(),
      password,
      captchaId,
      captchaCode: captchaCode.trim()
    });
    setBusy(false);
    if (err) {
      setError(err);
      if (err.includes("验证码")) loadCaptcha();
    } else {
      router.replace("/home");
    }
  };

  return (
    <div>
      <PageHero
        kicker="认证"
        title="用户注册"
        description="创建账号以保存你的训练数据和个性化设置。"
      />

      <div className="auth-form-wrap">
        <form className="auth-form" onSubmit={handleSubmit}>
          {error && <div className="auth-error">{error}</div>}

          <div className="auth-field">
            <label className="auth-label" htmlFor="reg-username">用户名</label>
            <input
              id="reg-username"
              className="auth-input"
              type="text"
              placeholder="3~64 个字符"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoComplete="username"
              autoFocus
            />
          </div>

          <div className="auth-field">
            <label className="auth-label" htmlFor="reg-email">邮箱</label>
            <input
              id="reg-email"
              className="auth-input"
              type="email"
              placeholder="your@email.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              autoComplete="email"
            />
          </div>

          <div className="auth-field">
            <label className="auth-label" htmlFor="reg-password">密码</label>
            <div className="auth-input-wrap">
              <input
                id="reg-password"
                className="auth-input"
                type={showPwd ? "text" : "password"}
                placeholder="至少 8 位"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="new-password"
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

          <div className="auth-field">
            <label className="auth-label" htmlFor="reg-captcha">验证码</label>
            <div className="auth-captcha-row">
              <div className="auth-captcha-code">
                <ShieldCheck size={16} />
                <span className="captcha-digits">{captchaDisplay || "------"}</span>
              </div>
              <button type="button" className="btn btn-ghost" onClick={loadCaptcha}>
                <RefreshCw size={15} />
                换一张
              </button>
            </div>
            <input
              id="reg-captcha"
              className="auth-input"
              type="text"
              placeholder="请输入上方验证码"
              value={captchaCode}
              onChange={(e) => setCaptchaCode(e.target.value)}
              autoComplete="off"
              style={{ marginTop: 10 }}
            />
          </div>

          <button
            className="btn btn-accent wide"
            type="submit"
            disabled={busy || authState === "syncing"}
          >
            <UserPlus size={17} />
            {busy ? "注册中..." : "注册"}
          </button>

          <p className="auth-switch">
            已有账号？<Link href="/login">立即登录</Link>
          </p>
        </form>
      </div>
    </div>
  );
}
