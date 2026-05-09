"use client";

import { useState } from "react";
import { motion } from "framer-motion";
import {
  BrainCircuit,
  CheckCircle,
  XCircle,
  RotateCcw,
  ArrowRight,
  Trophy,
  Target,
  TrendingUp,
} from "lucide-react";
import { PageHero } from "@/components/new-ui/PageHero";

const directions = [
  "Java",
  "前端",
  "Go",
  "算法",
  "数据库",
  "系统设计",
  "网络",
  "操作系统",
];

const questions = [
  {
    id: 1,
    type: "单选题",
    question: "Java 中 HashMap 的底层数据结构是什么？",
    options: ["数组", "链表", "数组 + 链表", "红黑树"],
    correct: 2,
  },
  {
    id: 2,
    type: "单选题",
    question: "以下哪个不是 Java 的访问修饰符？",
    options: ["public", "private", "protected", "internal"],
    correct: 3,
  },
  {
    id: 3,
    type: "单选题",
    question: "Spring Boot 的自动配置原理主要基于哪个注解？",
    options: ["@Component", "@Autowired", "@EnableAutoConfiguration", "@Configuration"],
    correct: 2,
  },
];

export default function AITestPage() {
  const [direction, setDirection] = useState("Java");
  const [difficulty, setDifficulty] = useState(3);
  const [started, setStarted] = useState(false);
  const [currentQ, setCurrentQ] = useState(0);
  const [selected, setSelected] = useState<number | null>(null);
  const [answers, setAnswers] = useState<number[]>([]);
  const [finished, setFinished] = useState(false);

  const startTest = () => {
    setStarted(true);
    setCurrentQ(0);
    setSelected(null);
    setAnswers([]);
    setFinished(false);
  };

  const selectOption = (idx: number) => {
    if (selected !== null) return;
    setSelected(idx);
    const newAnswers = [...answers, idx];
    setAnswers(newAnswers);

    setTimeout(() => {
      if (currentQ < questions.length - 1) {
        setCurrentQ((prev) => prev + 1);
        setSelected(null);
      } else {
        setFinished(true);
      }
    }, 800);
  };

  const correctCount = answers.filter((a, i) => a === questions[i].correct).length;
  const progress = ((currentQ + (selected !== null ? 1 : 0)) / questions.length) * 100;

  if (!started) {
    return (
      <div>
        <PageHero
          kicker="AI 测试"
          title="智能技术测试"
          description="选择技术方向和难度，AI 将为你生成个性化面试题目。"
        />

        <div className="panel">
          <div className="field-group">
            <p className="field-label">技术方向</p>
            <div className="chip-row">
              {directions.map((d) => (
                <button
                  key={d}
                  type="button"
                  className={`chip ${direction === d ? "active" : ""}`}
                  onClick={() => setDirection(d)}
                >
                  {d}
                </button>
              ))}
            </div>
          </div>

          <div className="field-group">
            <p className="field-label">难度等级</p>
            <div className="slider-row">
              <input
                type="range"
                min={1}
                max={5}
                value={difficulty}
                onChange={(e) => setDifficulty(Number(e.target.value))}
              />
              <span className="slider-value">{difficulty}</span>
            </div>
          </div>

          <button type="button" className="btn btn-accent wide" onClick={startTest}>
            <BrainCircuit size={16} />
            开始测试
            <ArrowRight size={14} />
          </button>
        </div>
      </div>
    );
  }

  if (finished) {
    return (
      <div>
        <PageHero
          kicker="测试完成"
          title="测试结果"
          description={`${direction} 方向 · 难度 ${difficulty} · 共 ${questions.length} 题`}
        />

        <motion.div
          className="panel text-center"
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.4 }}
        >
          <div className="relative mx-auto mb-6 flex h-20 w-20 items-center justify-center rounded-full bg-gradient-to-br from-[var(--green-soft)] to-[var(--blue-soft)]">
            <Trophy size={32} className="text-[var(--green)]" />
          </div>
          <h2 className="text-2xl font-bold text-[var(--ink)]">
            <AnimatedCounter target={Math.round((correctCount / questions.length) * 100)} suffix="%" />
          </h2>
          <p className="mt-2 text-sm text-[var(--muted)]">
            答对 {correctCount} / {questions.length} 题
          </p>

          <div className="mt-6 grid grid-cols-3 gap-4">
            <div className="rounded-xl bg-[var(--surface-soft)] p-4">
              <Target size={18} className="mx-auto text-[var(--blue)]" />
              <p className="mt-2 text-lg font-bold">{correctCount}</p>
              <p className="text-xs text-[var(--muted)]">正确</p>
            </div>
            <div className="rounded-xl bg-[var(--surface-soft)] p-4">
              <XCircle size={18} className="mx-auto text-[var(--red)]" />
              <p className="mt-2 text-lg font-bold">{questions.length - correctCount}</p>
              <p className="text-xs text-[var(--muted)]">错误</p>
            </div>
            <div className="rounded-xl bg-[var(--surface-soft)] p-4">
              <TrendingUp size={18} className="mx-auto text-[var(--cyan)]" />
              <p className="mt-2 text-lg font-bold">{difficulty}</p>
              <p className="text-xs text-[var(--muted)]">难度</p>
            </div>
          </div>

          <div className="mt-6 flex justify-center gap-3">
            <button type="button" className="btn btn-accent" onClick={startTest}>
              <RotateCcw size={14} />
              重新测试
            </button>
            <button
              type="button"
              className="btn btn-ghost"
              onClick={() => {
                setStarted(false);
                setFinished(false);
              }}
            >
              返回配置
            </button>
          </div>
        </motion.div>
      </div>
    );
  }

  const q = questions[currentQ];

  return (
    <div>
      <PageHero
        kicker={`第 ${currentQ + 1} / ${questions.length} 题`}
        title={direction + " 测试"}
        description={`难度 ${difficulty} · ${q.type}`}
      />

      {/* Progress */}
      <div className="progress-track">
        <div className="progress-fill" style={{ width: `${progress}%` }} />
      </div>

      {/* Question */}
      <motion.div
        key={q.id}
        className="panel"
        initial={{ opacity: 0, x: 20 }}
        animate={{ opacity: 1, x: 0 }}
        transition={{ duration: 0.3 }}
      >
        <p className="question-type">{q.type}</p>
        <h3 className="mt-2 text-lg font-semibold">{q.question}</h3>

        <div className="option-list">
          {q.options.map((opt, idx) => {
            const isSelected = selected === idx;
            const isCorrect = idx === q.correct;
            const showResult = selected !== null;

            let borderColor = "var(--border)";
            let bgColor = "var(--surface)";
            if (showResult) {
              if (isCorrect) {
                borderColor = "var(--green)";
                bgColor = "var(--green-soft)";
              } else if (isSelected) {
                borderColor = "var(--red)";
                bgColor = "var(--red-soft)";
              }
            }

            return (
              <motion.button
                key={idx}
                type="button"
                className="option-card"
                onClick={() => selectOption(idx)}
                disabled={selected !== null}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: idx * 0.06, duration: 0.3 }}
                style={{
                  borderColor,
                  background: bgColor,
                }}
              >
                <span
                  className="option-index"
                  style={
                    showResult && isCorrect
                      ? { background: "var(--green)", color: "white" }
                      : showResult && isSelected
                        ? { background: "var(--red)", color: "white" }
                        : undefined
                  }
                >
                  {showResult && isCorrect ? (
                    <CheckCircle size={14} />
                  ) : showResult && isSelected ? (
                    <XCircle size={14} />
                  ) : (
                    String.fromCharCode(65 + idx)
                  )}
                </span>
                <span>{opt}</span>
              </motion.button>
            );
          })}
        </div>
      </motion.div>
    </div>
  );
}

function AnimatedCounter({ target, suffix }: { target: number; suffix?: string }) {
  const [count, setCount] = useState(0);
  const [hasAnimated, setHasAnimated] = useState(false);

  if (!hasAnimated) {
    setHasAnimated(true);
    const startTime = performance.now();
    const duration = 1200;
    const animate = (currentTime: number) => {
      const elapsed = currentTime - startTime;
      const progress = Math.min(elapsed / duration, 1);
      const eased = 1 - Math.pow(1 - progress, 3);
      setCount(Math.round(eased * target));
      if (progress < 1) {
        requestAnimationFrame(animate);
      }
    };
    requestAnimationFrame(animate);
  }

  return (
    <span>
      {count}
      {suffix}
    </span>
  );
}
