"use client";

import Link from "next/link";
import { motion } from "framer-motion";
import {
  Sparkles,
  BrainCircuit,
  MessageSquare,
  Database,
  ClipboardList,
  AlertTriangle,
  Lightbulb,
  ArrowRight,
} from "lucide-react";
import { PageHero } from "@/components/new-ui/PageHero";
import { FeatureCard } from "@/components/new-ui/cards";
import { AnimatedCounter } from "@/components/new-ui/cards";
import { HeroCommandDeck } from "@/components/new-ui/HeroCommandDeck";
import { LockKeyhole, Settings2, Users } from "lucide-react";

const features = [
  {
    icon: BrainCircuit,
    title: "AI 智能测试",
    description: "根据你的技术方向自动生成高质量面试题，实时评估答题表现。",
    tag: "智能出题",
    href: "/ai-test",
  },
  {
    icon: MessageSquare,
    title: "AI 面试官",
    description: "模拟真实面试场景，AI 面试官与你实时对话，提供多维度评分。",
    tag: "模拟面试",
    href: "/ai-interviewer",
  },
  {
    icon: Database,
    title: "知识库管理",
    description: "上传学习资料，AI 自动解析生成结构化知识，随时检索复习。",
    tag: "资料解析",
    href: "/knowledge-base",
  },
  {
    icon: ClipboardList,
    title: "题库练习",
    description: "海量面试真题，支持按方向、难度筛选，针对性提升薄弱环节。",
    tag: "真题练习",
    href: "/question-bank",
  },
  {
    icon: AlertTriangle,
    title: "错题本",
    description: "自动记录错题，智能分析薄弱知识点，科学安排复习计划。",
    tag: "智能复习",
    href: "/wrong-answers",
  },
  {
    icon: Lightbulb,
    title: "面试技巧",
    description: "精选面试攻略和技巧文章，助你掌握面试中的加分细节。",
    tag: "经验分享",
    href: "/interview-tips",
  },
];

const stats = [
  { label: "面试题库", value: 5000, suffix: "+" },
  { label: "覆盖方向", value: 12, suffix: "个" },
  { label: "AI 评分维度", value: 6, suffix: "维" },
  { label: "用户满意度", value: 98, suffix: "%" },
];

const entryCards = [
  {
    icon: LockKeyhole,
    title: "登录 / 注册",
    desc: "进入个人训练空间，保存题单、错题与模型配置。",
    href: "/login",
    accent: "var(--blue)",
  },
  {
    icon: Settings2,
    title: "自定义模型",
    desc: "填入 Base URL、API Key 和模型名，切换你的专属模型。",
    href: "/settings",
    accent: "var(--cyan)",
  },
  {
    icon: Users,
    title: "题量控制",
    desc: "按 5 / 10 / 20 / 30 题自由调整，适配不同训练节奏。",
    href: "/question-bank",
    accent: "var(--green)",
  },
];

export default function LandingPage() {
  return (
    <div>
      <section className="landing-hero">
        <div className="landing-hero-copy">
          <PageHero
            kicker="AI Interview Studio"
            title="AI 驱动的面试训练控制舱"
            description="把题库、知识库、模拟面试和个人训练画像集中到一个实时工作台，让 AI 像面试教练一样持续校准你的能力曲线。"
            actions={
              <>
                <Link href="/home" className="btn btn-accent energy-button">
                  <Sparkles size={16} />
                  开始训练
                  <ArrowRight size={14} />
                </Link>
                <Link href="/profile" className="btn btn-ghost">
                  个人中心
                </Link>
              </>
            }
          />
        </div>
        <HeroCommandDeck />
      </section>

      <section className="hero-split">
        <motion.div
          className="panel hero-panel"
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2, duration: 0.45 }}
        >
            <div className="panel-header compact">
              <div>
                <h2>现在开始</h2>
                <p>先登录，再配置模型和题量。</p>
              </div>
            </div>
          <div className="hero-entry-grid">
            {entryCards.map((entry) => {
              const Icon = entry.icon;
              return (
                <Link href={entry.href} key={entry.title} className="hero-entry-card">
                  <span className="hero-entry-icon" style={{ color: entry.accent, background: `${entry.accent}12` }}>
                    <Icon size={18} />
                  </span>
                  <div>
                    <h3>{entry.title}</h3>
                    <p>{entry.desc}</p>
                  </div>
                </Link>
              );
            })}
          </div>
        </motion.div>

        <motion.div
          className="panel hero-side-panel"
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.28, duration: 0.45 }}
        >
          <div className="panel-header compact">
            <div>
              <h2>训练节奏</h2>
              <p>把功能区按优先级排清楚。</p>
            </div>
          </div>
          <div className="hero-side-stack">
            <div>
              <span className="hero-side-label">首要</span>
              <strong>登录 / 注册</strong>
              <p>先进入个人空间，再保存模型和题单。</p>
            </div>
            <div>
              <span className="hero-side-label">次要</span>
              <strong>模型配置</strong>
              <p>支持 OpenAI、DeepSeek、本地模型的统一切换。</p>
            </div>
            <div>
              <span className="hero-side-label">训练</span>
              <strong>题量与题型</strong>
              <p>按学习状态快速选择题目数量与模式。</p>
            </div>
          </div>
        </motion.div>
      </section>

      {/* Stats */}
      <motion.section
        className="metric-grid"
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.3, duration: 0.5 }}
      >
        {stats.map((stat, i) => (
          <motion.div
            key={stat.label}
            className="metric-card"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.35 + i * 0.08, duration: 0.4 }}
          >
            <p className="metric-label">{stat.label}</p>
            <p className="metric-value">
              <AnimatedCounter target={stat.value} suffix={stat.suffix} />
            </p>
          </motion.div>
        ))}
      </motion.section>

      <div className="gradient-divider" />

      {/* Features */}
      <section className="section-block">
        <div className="section-head">
          <h2>核心功能</h2>
          <p>六大模块，全方位覆盖面试准备需求</p>
        </div>
        <div className="feature-grid">
          {features.map((f, i) => (
            <FeatureCard key={f.title} {...f} index={i} />
          ))}
        </div>
      </section>

      <div className="gradient-divider" />

      {/* CTA */}
      <motion.section
        className="panel text-center"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.6, duration: 0.5 }}
      >
        <div className="relative mx-auto mb-6 flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-[var(--blue-soft)] to-[var(--cyan-soft)] text-[var(--blue)]">
          <Sparkles size={28} strokeWidth={1.5} />
        </div>
        <h2 className="text-xl font-bold text-[var(--ink)]">准备好提升面试能力了吗？</h2>
        <p className="mx-auto mt-3 max-w-md text-sm text-[var(--muted)]">
          立即开始 AI 面试训练，获取个性化学习建议和实时反馈
        </p>
        <div className="mt-6 flex justify-center gap-3">
          <Link href="/home" className="btn btn-accent">
            进入工作台
            <ArrowRight size={14} />
          </Link>
          <Link href="/ai-interviewer" className="btn btn-ghost">
            模拟面试
          </Link>
        </div>
      </motion.section>
    </div>
  );
}
