"use client";

import { useState, useEffect } from "react";
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
  Loader2,
} from "lucide-react";
import { PageHero } from "@/components/new-ui/PageHero";
import {
  generateQuiz,
  listMaterials,
  type BackendMaterial,
  toErrorMessage,
} from "@/lib/interview-api";

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

const directionToQuestionType: Record<string, string> = {
  "Java": "choice",
  "前端": "choice",
  "Go": "choice",
  "算法": "coding",
  "数据库": "short",
  "系统设计": "interview",
  "网络": "choice",
  "操作系统": "short",
};

type QuizQuestion = {
  id: number;
  type: string;
  question: string;
  options: string[];
  correct: number;
  referenceAnswer?: string;
};

export default function AITestPage() {
  const [direction, setDirection] = useState("Java");
  const [difficulty, setDifficulty] = useState(3);
  const [started, setStarted] = useState(false);
  const [currentQ, setCurrentQ] = useState(0);
  const [selected, setSelected] = useState<number | null>(null);
  const [answers, setAnswers] = useState<number[]>([]);
  const [finished, setFinished] = useState(false);
  const [questions, setQuestions] = useState<QuizQuestion[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [materials, setMaterials] = useState<BackendMaterial[]>([]);

  useEffect(() => {
    listMaterials().then((mats) => {
      const ready = mats.filter((m) => m.parseStatus === "SUCCESS");
      setMaterials(ready);
    }).catch(() => {});
  }, []);

  const startTest = async () => {
    if (materials.length === 0) {
      setError("请先上传并等待资料解析完成后再开始测试");
      return;
    }
    setLoading(true);
    setError("");
    try {
      const result = await generateQuiz({
        materialIds: materials.slice(0, 3).map((m) => m.id),
        questionType: (directionToQuestionType[direction] || "choice") as "choice" | "short" | "code" | "interview",
        difficulty,
        count: 5,
        interviewMode: false,
      });

      const quizQuestions: QuizQuestion[] = (result.questions || []).map((q, idx) => {
        let options: string[] = [];
        const correct = 0;
        if (q.optionsJson && typeof q.optionsJson === "object") {
          const opts = q.optionsJson as Record<string, string>;
          options = Object.values(opts);
        } else if (q.referenceAnswer) {
          options = [q.referenceAnswer, "选项 B", "选项 C", "选项 D"];
        } else {
          options = ["A", "B", "C", "D"];
        }

        return {
          id: q.id || idx,
          type: q.questionType || "SINGLE_CHOICE",
          question: q.stemText,
          options,
          correct,
          referenceAnswer: q.referenceAnswer || undefined,
        };
      });

      if (quizQuestions.length === 0) {
        setError("AI 未能生成题目，请稍后重试");
        return;
      }

      setQuestions(quizQuestions);
      setStarted(true);
      setCurrentQ(0);
      setSelected(null);
      setAnswers([]);
      setFinished(false);
    } catch (e) {
      setError(toErrorMessage(e, "生成题目失败，请稍后重试"));
    } finally {
      setLoading(false);
    }
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

  const correctCount = answers.filter((a, i) => a === questions[i]?.correct).length;
  const progress = questions.length > 0
    ? ((currentQ + (selected !== null ? 1 : 0)) / questions.length) * 100
    : 0;

  if (!started) {
    return (
      <div>
        <PageHero
          kicker="AI 测试"
          title="智能技术测试"
          description="选择技术方向和难度，AI 将根据你的知识库生成个性化测试题。"
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

          {materials.length === 0 && (
            <p className="text-sm text-[var(--muted)] mb-4">
              请先在知识库上传资料并等待解析完成，AI 将基于你的资料生成题目。
            </p>
          )}

          {error && (
            <p className="text-sm text-[var(--red)] mb-4">{error}</p>
          )}

          <button
            type="button"
            className="btn btn-accent wide"
            onClick={startTest}
            disabled={loading}
          >
            {loading ? (
              <Loader2 size={16} className="animate-spin" />
            ) : (
              <BrainCircuit size={16} />
            )}
            {loading ? "AI 出题中..." : "开始测试"}
            {!loading && <ArrowRight size={14} />}
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
            <button type="button" className="btn btn-accent" onClick={() => { setStarted(false); setFinished(false); }}>
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
        description={`难度 ${difficulty} · ${q?.type || ""}`}
      />

      <div className="progress-track">
        <div className="progress-fill" style={{ width: `${progress}%` }} />
      </div>

      <motion.div
        key={q?.id}
        className="panel"
        initial={{ opacity: 0, x: 20 }}
        animate={{ opacity: 1, x: 0 }}
        transition={{ duration: 0.3 }}
      >
        <p className="question-type">{q?.type}</p>
        <h3 className="mt-2 text-lg font-semibold">{q?.question}</h3>

        <div className="option-list">
          {q?.options.map((opt, idx) => {
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
