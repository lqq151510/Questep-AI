"use client";

import { useEffect, useMemo, useState } from "react";
import { Activity, Database, Gauge, GitBranch, Layers, RefreshCw } from "lucide-react";
import { MetricCard } from "@/components/new-ui/cards";
import { PageHero } from "@/components/new-ui/PageHero";
import { listMaterials, listQuestions, toErrorMessage } from "@/lib/interview-api";

const steps = [
  "上传资料并自动解析",
  "AI 生成专项题目",
  "模拟面试追问训练",
  "错题归档与二次强化",
  "周维度学习报告"
] as const;

const stack = [
  { label: "Frontend", value: "Next.js + React 19", icon: <Layers size={16} /> },
  { label: "Backend", value: "Spring Boot 3.3", icon: <Activity size={16} /> },
  { label: "Storage", value: "MySQL + 本地文件", icon: <Database size={16} /> },
  { label: "Pipeline", value: "任务追踪 + 异步记录", icon: <GitBranch size={16} /> }
] as const;

const WRONG_BOOK_KEY = "wrong_question_records";

function readWrongCount(): number {
  if (typeof window === "undefined") return 0;
  const raw = window.localStorage.getItem(WRONG_BOOK_KEY);
  if (!raw) return 0;
  try {
    const parsed = JSON.parse(raw) as Array<{ mastered?: boolean }>;
    return parsed.filter((item) => !item.mastered).length;
  } catch {
    return 0;
  }
}

export default function HomeOverviewPage() {
  const [activeStep, setActiveStep] = useState(0);
  const [materialCount, setMaterialCount] = useState(0);
  const [runningTasks, setRunningTasks] = useState(0);
  const [questionCount, setQuestionCount] = useState(0);
  const [wrongCount, setWrongCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [errorHint, setErrorHint] = useState("");

  useEffect(() => {
    const timer = window.setInterval(() => {
      setActiveStep((prev) => (prev + 1) % steps.length);
    }, 2600);
    return () => window.clearInterval(timer);
  }, []);

  const refresh = async () => {
    setLoading(true);
    setErrorHint("");
    try {
      const [materials, questions] = await Promise.all([listMaterials(), listQuestions(100)]);
      setMaterialCount(materials.length);
      setRunningTasks(
        materials.filter((item) => {
          const status = String(item.parseStatus ?? "").toUpperCase();
          return status === "PENDING" || status === "PROCESSING";
        }).length
      );
      setQuestionCount(questions.length);
      setWrongCount(readWrongCount());
    } catch (error) {
      setErrorHint(toErrorMessage(error, "加载总览数据失败"));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void refresh();
  }, []);

  const completionRate = useMemo(() => {
    if (questionCount === 0) return 0;
    const done = Math.max(questionCount - wrongCount, 0);
    return Math.min(100, Math.round((done / questionCount) * 100));
  }, [questionCount, wrongCount]);

  return (
    <div className="container">
      <PageHero
        kicker="System Overview"
        title="面试训练全链路总览"
        description="把资料、题目、面试、错题和复盘串成统一训练流，组件化页面支持持续演进。"
      />

      <div className="row-actions">
        <button type="button" className="btn" onClick={() => void refresh()} disabled={loading}>
          <RefreshCw size={14} />
          {loading ? "刷新中" : "刷新总览"}
        </button>
      </div>
      {errorHint && <p className="meta-text">{errorHint}</p>}

      <section className="metric-grid">
        <MetricCard label="资料总数" value={String(materialCount)} hint="来源：/api/v1/materials" />
        <MetricCard label="进行中任务" value={String(runningTasks)} hint="按资料解析状态推断" />
        <MetricCard label="题库规模" value={String(questionCount)} hint="来源：/api/v1/quizzes/questions" />
        <MetricCard label="阶段完成率" value={`${completionRate}%`} hint="按题库总量与错题本估算" />
      </section>

      <section className="panel section-stack">
        <div className="section-head compact">
          <h2>技术组件栈</h2>
          <p>新页面已按模块拆分，可独立扩展功能与动画效果。</p>
        </div>
        <div className="stack-grid">
          {stack.map((item) => (
            <article key={item.label} className="stack-item">
              <span className="stack-icon">{item.icon}</span>
              <p className="stack-label">{item.label}</p>
              <p className="stack-value">{item.value}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="panel timeline-panel">
        <div className="section-head compact">
          <h2>训练流程</h2>
          <p>自动轮播当前步骤，也可手动点击切换。</p>
        </div>
        <div className="timeline">
          {steps.map((step, index) => (
            <button
              key={step}
              type="button"
              className={index === activeStep ? "timeline-step active" : "timeline-step"}
              onClick={() => setActiveStep(index)}
            >
              <span className="timeline-num">{String(index + 1).padStart(2, "0")}</span>
              <span>{step}</span>
              <Gauge size={14} />
            </button>
          ))}
        </div>
      </section>
    </div>
  );
}
