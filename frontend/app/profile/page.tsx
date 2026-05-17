"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  BrainCircuit,
  Database,
  Gauge,
  LogOut,
  Settings2,
  ShieldCheck,
  Sparkles,
  Target,
  UserRound,
} from "lucide-react";
import { motion } from "framer-motion";

import { PageHero } from "@/components/new-ui/PageHero";
import {
  fetchDashboardMetrics,
  getAuthSessionRemainingMs,
  getLlmSettings,
  type DashboardMetrics,
  type LlmSettingsView,
} from "@/lib/interview-api";
import { useDashboardStore } from "@/stores/useDashboardStore";

const emptyMetrics: DashboardMetrics = {
  completedTests: 0,
  mockInterviews: 0,
  masteredPoints: 0,
  pendingWrongReviews: 0,
  parsedMaterials: 0,
  recentInterviewQuestions: 0,
  activeInterviewSessions: 0,
};

function maskBaseUrl(value?: string) {
  if (!value) return "未配置";
  try {
    const url = new URL(value);
    return `${url.origin.replace(/^https?:\/\//, "")}${url.pathname === "/" ? "" : url.pathname}`;
  } catch {
    return value.length > 24 ? `${value.slice(0, 24)}...` : value;
  }
}

function formatRemaining(ms: number | null) {
  if (ms === null) return "未登录";
  if (ms <= 0) return "已过期";
  const minutes = Math.ceil(ms / 60000);
  if (minutes < 60) return `${minutes} 分钟`;
  return `${Math.ceil(minutes / 60)} 小时`;
}

export default function ProfilePage() {
  const router = useRouter();
  const { user, isLoggedIn, logout, difficulty, count } = useDashboardStore();
  const [mounted, setMounted] = useState(false);
  const [metrics, setMetrics] = useState<DashboardMetrics>(emptyMetrics);
  const [settings, setSettings] = useState<LlmSettingsView | null>(null);
  const [remainingMs, setRemainingMs] = useState<number | null>(getAuthSessionRemainingMs());
  const [syncLabel, setSyncLabel] = useState("同步中");

  useEffect(() => {
    setMounted(true);
  }, []);

  useEffect(() => {
    if (mounted && !isLoggedIn) {
      router.replace("/login");
    }
  }, [isLoggedIn, mounted, router]);

  useEffect(() => {
    if (!isLoggedIn) return;
    let cancelled = false;
    void Promise.allSettled([fetchDashboardMetrics(), getLlmSettings()]).then(([metricResult, settingResult]) => {
      if (cancelled) return;
      if (metricResult.status === "fulfilled") {
        setMetrics(metricResult.value);
      }
      if (settingResult.status === "fulfilled") {
        setSettings(settingResult.value);
      }
      setSyncLabel(metricResult.status === "fulfilled" || settingResult.status === "fulfilled" ? "已同步" : "离线数据");
    });
    return () => {
      cancelled = true;
    };
  }, [isLoggedIn]);

  useEffect(() => {
    const timer = window.setInterval(() => {
      setRemainingMs(getAuthSessionRemainingMs());
    }, 30_000);
    return () => window.clearInterval(timer);
  }, []);

  const totalActivity = useMemo(
    () => metrics.completedTests + metrics.mockInterviews + metrics.parsedMaterials + metrics.pendingWrongReviews,
    [metrics]
  );

  if (!mounted || !isLoggedIn || !user) return null;

  return (
    <div>
      <PageHero
        kicker="Profile"
        title="个人中心"
        description="查看你的训练画像、模型配置和学习偏好，把每次练习沉淀成可追踪的能力档案。"
        actions={
          <>
            <Link href="/settings" className="btn btn-accent energy-button">
              <Settings2 size={16} />
              模型配置
            </Link>
            <button
              type="button"
              className="btn btn-ghost"
              onClick={() => {
                logout();
                router.replace("/login");
              }}
            >
              <LogOut size={16} />
              退出登录
            </button>
          </>
        }
      />

      <section className="profile-shell">
        <motion.div
          className="profile-identity panel"
          initial={{ opacity: 0, y: 18 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.45 }}
        >
          <div className="profile-avatar">
            <UserRound size={34} />
          </div>
          <div>
            <span className="profile-eyebrow">AI 学习档案</span>
            <h2>{user.username}</h2>
            <p>当前状态：{syncLabel} · 会话剩余 {formatRemaining(remainingMs)}</p>
          </div>
          <div className="profile-orbit" aria-hidden="true" />
        </motion.div>

        <motion.div
          className="profile-radar panel"
          initial={{ opacity: 0, y: 18 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.08, duration: 0.45 }}
        >
          <div className="profile-radar-core">
            <Sparkles size={24} />
            <strong>{Math.min(99, 48 + totalActivity * 3)}</strong>
            <span>成长指数</span>
          </div>
        </motion.div>
      </section>

      <section className="profile-stat-grid">
        {[
          { icon: Gauge, label: "完成测试", value: metrics.completedTests, hint: "累计生成/练习题目" },
          { icon: BrainCircuit, label: "模拟面试", value: metrics.mockInterviews, hint: "近期面试训练" },
          { icon: Database, label: "知识资料", value: metrics.parsedMaterials, hint: "已解析资料" },
          { icon: Target, label: "错题待复习", value: metrics.pendingWrongReviews, hint: "需要回看巩固" },
        ].map((item, index) => {
          const Icon = item.icon;
          return (
            <motion.div
              className="profile-stat-card"
              key={item.label}
              initial={{ opacity: 0, y: 14 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.16 + index * 0.05, duration: 0.35 }}
            >
              <Icon size={18} />
              <span>{item.label}</span>
              <strong>{item.value}</strong>
              <p>{item.hint}</p>
            </motion.div>
          );
        })}
      </section>

      <section className="profile-info-grid">
        <div className="panel profile-info-card">
          <div className="panel-header compact">
            <div>
              <h2>AI 配置摘要</h2>
              <p>当前模型与服务地址只做脱敏展示。</p>
            </div>
          </div>
          <dl className="profile-dl">
            <div>
              <dt>厂商</dt>
              <dd>{settings?.providerName || "未同步"}</dd>
            </div>
            <div>
              <dt>模型</dt>
              <dd>{settings?.modelName || "未配置"}</dd>
            </div>
            <div>
              <dt>Base URL</dt>
              <dd>{maskBaseUrl(settings?.baseUrl)}</dd>
            </div>
          </dl>
        </div>

        <div className="panel profile-info-card">
          <div className="panel-header compact">
            <div>
              <h2>学习偏好</h2>
              <p>沿用你当前题库训练设置。</p>
            </div>
          </div>
          <dl className="profile-dl">
            <div>
              <dt>默认方向</dt>
              <dd>Java + AI</dd>
            </div>
            <div>
              <dt>默认题量</dt>
              <dd>{count} 题</dd>
            </div>
            <div>
              <dt>难度等级</dt>
              <dd>{difficulty} / 5</dd>
            </div>
          </dl>
        </div>

        <div className="panel profile-info-card">
          <div className="panel-header compact">
            <div>
              <h2>安全状态</h2>
              <p>登录态由本地会话过期时间保护。</p>
            </div>
          </div>
          <div className="profile-security">
            <ShieldCheck size={22} />
            <div>
              <strong>会话保护已启用</strong>
              <span>剩余 {formatRemaining(remainingMs)}</span>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
