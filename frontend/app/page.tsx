import Link from "next/link";
import { ArrowRight, Bot, BrainCircuit, BookOpenText, Sparkles, Target, Trophy } from "lucide-react";
import { AnimatedCounter } from "@/components/new-ui/AnimatedCounter";
import { FeatureCard, MetricCard } from "@/components/new-ui/cards";
import { PageHero } from "@/components/new-ui/PageHero";

const features = [
  {
    href: "/ai-test",
    icon: <Target size={20} />,
    title: "AI 自适应测试",
    description: "根据岗位、难度和题型实时组卷，自动追踪答题进度与薄弱点。",
    tag: "Practice Engine"
  },
  {
    href: "/ai-interviewer",
    icon: <Bot size={20} />,
    title: "AI 模拟面试官",
    description: "支持真实面试节奏的追问、反问和结构化报告，强化表达与应答能力。",
    tag: "Mock Interview"
  },
  {
    href: "/ai-qa",
    icon: <BrainCircuit size={20} />,
    title: "即时 AI 问答",
    description: "围绕常见八股与系统设计问题，给出可落地、可延展的讲解路径。",
    tag: "Knowledge Coach"
  },
  {
    href: "/knowledge-base",
    icon: <BookOpenText size={20} />,
    title: "个人知识库",
    description: "上传资料后自动解析成知识点，打通题库、错题本与复习计划。",
    tag: "Data Hub"
  }
] as const;

export default function LandingPage() {
  return (
    <div className="container">
      <PageHero
        kicker="AI Interview Studio"
        title="用 AI 重塑你的面试准备路径"
        description="从资料沉淀、专项测试到模拟面试与错题追踪，形成可闭环、可量化、可持续迭代的备考系统。"
        actions={
          <>
            <Link href="/ai-test" className="btn btn-accent">
              开始测试
              <ArrowRight size={16} />
            </Link>
            <Link href="/home" className="btn btn-ghost">
              查看系统总览
            </Link>
          </>
        }
      />

      <section className="metric-grid">
        <MetricCard
          label="累计题目生成"
          value={<AnimatedCounter value={12840} suffix="+" />}
          hint="多题型覆盖 Java / Spring / MySQL / 算法"
        />
        <MetricCard
          label="模拟面试时长"
          value={<AnimatedCounter value={468} suffix="h" />}
          hint="会话记录可回看，支持复盘与二次训练"
        />
        <MetricCard
          label="平均正确率提升"
          value={<AnimatedCounter value={37} suffix="%" />}
          hint="根据错题分布给出下一轮训练建议"
        />
        <MetricCard
          label="知识点沉淀"
          value={<AnimatedCounter value={1420} suffix=" 条" />}
          hint="从资料中自动抽取关键概念与题源"
        />
      </section>

      <div className="gradient-divider" />

      <section className="section-block">
        <div className="section-head">
          <h2>核心能力组件</h2>
          <p>页面已切换到新版视觉与交互体系，并支持动态效果与组件化扩展。</p>
        </div>
        <div className="feature-grid">
          {features.map((item) => (
            <FeatureCard key={item.title} {...item} />
          ))}
        </div>
      </section>

      <section className="panel spotlight">
        <div>
          <h3>训练建议引擎</h3>
          <p>根据最近 7 天测试结果与面试报告，自动推荐下一阶段训练组合。</p>
        </div>
        <div className="spotlight-stats">
          <span>
            <Sparkles size={16} />
            动态学习路径
          </span>
          <span>
            <Trophy size={16} />
            目标岗位命中率
          </span>
        </div>
      </section>
    </div>
  );
}
