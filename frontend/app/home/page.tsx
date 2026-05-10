"use client";

import { useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { motion } from "framer-motion";
import { useDashboardStore } from "@/stores/useDashboardStore";

const MotionLink = motion(Link);
import {
  BrainCircuit,
  MessageSquare,
  BookOpen,
  ClipboardList,
  AlertTriangle,
  Lightbulb,
  Target,
  Zap,
  Award,
  LockKeyhole,
  Settings2,
  ListRestart,
} from "lucide-react";
import { PageHero } from "@/components/new-ui/PageHero";
import { MetricCard } from "@/components/new-ui/cards";

const metrics = [
  { label: "已完成测试", value: 42, hint: "本周 +5", trend: "up" as const, trendValue: "+12%" },
  { label: "模拟面试", value: 18, hint: "平均评分 85", trend: "up" as const, trendValue: "+8%" },
  { label: "掌握知识点", value: 156, hint: "共 200 个", trend: "up" as const, trendValue: "+15" },
  { label: "错题复习", value: 23, hint: "待复习", trend: "neutral" as const, trendValue: "进行中" },
];

const stack = [
  { icon: BrainCircuit, label: "AI 引擎", value: "GPT-4" },
  { icon: Target, label: "题库覆盖", value: "12 方向" },
  { icon: Zap, label: "响应速度", value: "< 1s" },
  { icon: Award, label: "评分维度", value: "6 维度" },
];

const trainingSteps = [
  { num: "01", title: "选择方向", desc: "Java / 前端 / Go / 算法", active: true },
  { num: "02", title: "AI 测试", desc: "智能出题，即时反馈", active: false },
  { num: "03", title: "模拟面试", desc: "真实场景，多维评分", active: false },
  { num: "04", title: "错题复习", desc: "薄弱点，针对性提升", active: false },
];

const quickLinks = [
  { icon: LockKeyhole, title: "登录 / 注册", desc: "进入个人训练空间", href: "/login", color: "var(--blue)" },
  { icon: Settings2, title: "自定义模型", desc: "配置专属推理模型", href: "/home", color: "var(--cyan)" },
  { icon: ListRestart, title: "题量设置", desc: "选择 5 / 10 / 20 / 30 题", href: "/question-bank", color: "var(--green)" },
  { icon: BrainCircuit, title: "AI 测试", desc: "快速开始技术测试", href: "/ai-test", color: "var(--blue)" },
  { icon: MessageSquare, title: "AI 面试官", desc: "模拟真实面试", href: "/ai-interviewer", color: "var(--cyan)" },
  { icon: BookOpen, title: "知识库", desc: "管理学习资料", href: "/knowledge-base", color: "var(--green)" },
  { icon: ClipboardList, title: "题库", desc: "海量真题练习", href: "/question-bank", color: "var(--yellow)" },
  { icon: AlertTriangle, title: "错题本", desc: "查看待复习错题", href: "/wrong-answers", color: "var(--red)" },
  { icon: Lightbulb, title: "面试技巧", desc: "阅读经验文章", href: "/interview-tips", color: "var(--blue)" },
];

export default function HomePage() {
  const router = useRouter();
  const isLoggedIn = useDashboardStore((s) => s.isLoggedIn);

  useEffect(() => {
    if (!isLoggedIn) router.replace("/login");
  }, [isLoggedIn, router]);

  if (!isLoggedIn) return null;

  return (
    <div>
      <PageHero
        kicker="工作台"
        title="学习总览"
        description="查看你的学习进度和训练数据，快速进入各个功能模块。"
      />

      <section className="home-rail">
        <div className="home-rail-main">
          <div className="panel home-focus-panel">
            <div className="panel-header compact">
              <div>
                <h2>登录与模型</h2>
                <p>先把身份和模型层搭好，再进入训练。</p>
              </div>
            </div>
            <div className="focus-list">
              <Link href="/login" className="focus-item">
                <strong>用户登录 / 注册</strong>
                <span>保存个人题单、错题和训练记录</span>
              </Link>
              <Link href="/home" className="focus-item">
                <strong>用户自定义模型</strong>
                <span>填写 Base URL、API Key、模型名</span>
              </Link>
              <Link href="/question-bank" className="focus-item">
                <strong>题量范围</strong>
                <span>按 5 / 10 / 20 / 30 题自由切换</span>
              </Link>
            </div>
          </div>

          <div className="panel home-support-panel">
            <div className="panel-header compact">
              <div>
                <h2>训练重点</h2>
                <p>把可用功能区分成主次节奏。</p>
              </div>
            </div>
            <div className="home-support-stack">
              <div>
                <span>主区</span>
                <strong>AI 测试 / 面试官</strong>
              </div>
              <div>
                <span>辅区</span>
                <strong>知识库 / 错题本</strong>
              </div>
              <div>
                <span>工具区</span>
                <strong>题库 / 技巧 / 模型配置</strong>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Metrics */}
      <section className="metric-grid">
        {metrics.map((m, i) => (
          <MetricCard key={m.label} {...m} index={i} />
        ))}
      </section>

      <div className="gradient-divider" />

      {/* Quick Links */}
      <section className="section-block">
        <div className="section-head compact">
          <h2>快捷入口</h2>
        </div>
        <div className="feature-grid">
          {quickLinks.map((link, i) => {
            const Icon = link.icon;
            return (
              <MotionLink
                key={link.title}
                href={link.href}
                className="feature-card"
                initial={{ opacity: 0, y: 16 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.1 + i * 0.06, duration: 0.4 }}
              >
                <span
                  className="feature-icon"
                  style={{
                    background: `linear-gradient(135deg, ${link.color}15, ${link.color}08)`,
                    color: link.color,
                  }}
                >
                  <Icon size={20} strokeWidth={1.8} />
                </span>
                <h3>{link.title}</h3>
                <p>{link.desc}</p>
              </MotionLink>
            );
          })}
        </div>
      </section>

      <div className="gradient-divider" />

      {/* Training Steps */}
      <section className="section-block">
        <div className="section-head compact">
          <h2>训练流程</h2>
          <p>按照推荐流程，系统提升面试能力</p>
        </div>
        <div className="timeline">
          {trainingSteps.map((step, i) => (
            <motion.div
              key={step.num}
              className={`timeline-step ${step.active ? "active" : ""}`}
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.2 + i * 0.08, duration: 0.4 }}
            >
              <span className="timeline-num">{step.num}</span>
              <div>
                <span className="font-semibold text-sm">{step.title}</span>
                <p className="text-xs text-[var(--muted)] mt-0.5">{step.desc}</p>
              </div>
              {step.active && (
                <span className="badge" style={{ color: "var(--blue)", background: "var(--blue-soft)" }}>
                  进行中
                </span>
              )}
            </motion.div>
          ))}
        </div>
      </section>

      <div className="gradient-divider" />

      {/* Tech Stack */}
      <section className="section-block">
        <div className="section-head compact">
          <h2>技术栈</h2>
        </div>
        <div className="stack-grid">
          {stack.map((item, i) => {
            const Icon = item.icon;
            return (
              <motion.div
                key={item.label}
                className="stack-item"
                initial={{ opacity: 0, y: 12 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.3 + i * 0.06, duration: 0.4 }}
              >
                <span className="stack-icon">
                  <Icon size={20} strokeWidth={1.5} />
                </span>
                <p className="stack-label">{item.label}</p>
                <p className="stack-value">{item.value}</p>
              </motion.div>
            );
          })}
        </div>
      </section>
    </div>
  );
}
