"use client";

import { useState } from "react";
import { motion } from "framer-motion";
import {
  AlertTriangle,
  RotateCcw,
  CheckCircle,
  TrendingUp,
  BookOpen,
} from "lucide-react";
import { PageHero } from "@/components/new-ui/PageHero";
import { EmptyState } from "@/components/ui/EmptyState";

const mockWrongAnswers = [
  {
    id: 1,
    question: "Java 中 volatile 关键字的作用是什么？",
    direction: "Java",
    yourAnswer: "保证原子性",
    correctAnswer: "保证可见性和禁止指令重排序",
    mastered: false,
    reviewCount: 2,
  },
  {
    id: 2,
    question: "Redis 持久化机制有哪些？",
    direction: "数据库",
    yourAnswer: "RDB",
    correctAnswer: "RDB 和 AOF",
    mastered: true,
    reviewCount: 5,
  },
  {
    id: 3,
    question: "TCP 三次握手的过程是什么？",
    direction: "网络",
    yourAnswer: "SYN -> ACK -> SYN+ACK",
    correctAnswer: "SYN -> SYN+ACK -> ACK",
    mastered: false,
    reviewCount: 1,
  },
];

export default function WrongAnswersPage() {
  const [wrongAnswers, setWrongAnswers] = useState(mockWrongAnswers);

  const toggleMastered = (id: number) => {
    setWrongAnswers((prev) =>
      prev.map((w) => (w.id === id ? { ...w, mastered: !w.mastered } : w))
    );
  };

  const unmastered = wrongAnswers.filter((w) => !w.mastered);
  const mastered = wrongAnswers.filter((w) => w.mastered);
  const masteryRate = Math.round((mastered.length / wrongAnswers.length) * 100) || 0;

  return (
    <div>
      <PageHero
        kicker="错题本"
        title="错题复习"
        description="自动记录错题，智能分析薄弱知识点，科学安排复习计划。"
      />

      {/* Stats */}
      <div className="metric-grid compact">
        <motion.div
          className="metric-card"
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4 }}
        >
          <p className="metric-label">总错题数</p>
          <p className="metric-value">{wrongAnswers.length}</p>
        </motion.div>
        <motion.div
          className="metric-card"
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.08, duration: 0.4 }}
        >
          <p className="metric-label">待复习</p>
          <p className="metric-value">{unmastered.length}</p>
        </motion.div>
        <motion.div
          className="metric-card"
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.16, duration: 0.4 }}
        >
          <p className="metric-label">掌握率</p>
          <p className="metric-value">{masteryRate}%</p>
        </motion.div>
      </div>

      {/* Mastery Progress */}
      <div className="panel mt-5">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <TrendingUp size={16} className="text-[var(--blue)]" />
            <span className="text-sm font-medium">掌握进度</span>
          </div>
          <span className="text-sm font-bold text-[var(--blue)]">{masteryRate}%</span>
        </div>
        <div className="mt-3 h-2.5 overflow-hidden rounded-full bg-[var(--border)]">
          <motion.div
            className="h-full rounded-full bg-gradient-to-r from-[var(--blue)] to-[var(--cyan)]"
            initial={{ width: 0 }}
            animate={{ width: `${masteryRate}%` }}
            transition={{ duration: 1, delay: 0.3 }}
          />
        </div>
      </div>

      {/* Unmastered */}
      <section className="section-block mt-5">
        <div className="section-head compact">
          <h2 className="flex items-center gap-2">
            <AlertTriangle size={16} className="text-[var(--red)]" />
            待复习 ({unmastered.length})
          </h2>
        </div>

        {unmastered.length === 0 ? (
          <EmptyState
            icon={CheckCircle}
            title="太棒了！"
            description="所有错题都已掌握，继续保持"
          />
        ) : (
          <div className="list-grid">
            {unmastered.map((w, i) => (
              <motion.div
                key={w.id}
                className="list-card"
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: i * 0.06, duration: 0.3 }}
              >
                <div className="flex items-center gap-2">
                  <span className="badge danger">{w.direction}</span>
                  <span className="text-xs text-[var(--subtle)]">
                    已复习 {w.reviewCount} 次
                  </span>
                </div>
                <h3 className="mt-2">{w.question}</h3>
                <div className="mt-3 space-y-2 text-sm">
                  <p className="text-[var(--red)]">
                    你的答案：{w.yourAnswer}
                  </p>
                  <p className="text-[var(--green)]">
                    正确答案：{w.correctAnswer}
                  </p>
                </div>
                <div className="row-actions">
                  <button
                    type="button"
                    className="btn btn-accent"
                    onClick={() => toggleMastered(w.id)}
                  >
                    <CheckCircle size={14} />
                    标记掌握
                  </button>
                  <button type="button" className="btn btn-ghost">
                    <BookOpen size={14} />
                    查看解析
                  </button>
                </div>
              </motion.div>
            ))}
          </div>
        )}
      </section>

      {/* Mastered */}
      {mastered.length > 0 && (
        <section className="section-block">
          <div className="section-head compact">
            <h2 className="flex items-center gap-2">
              <CheckCircle size={16} className="text-[var(--green)]" />
              已掌握 ({mastered.length})
            </h2>
          </div>
          <div className="list-grid">
            {mastered.map((w, i) => (
              <motion.div
                key={w.id}
                className="list-card opacity-70"
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 0.7, y: 0 }}
                transition={{ delay: i * 0.06, duration: 0.3 }}
              >
                <div className="flex items-center gap-2">
                  <span className="badge success">{w.direction}</span>
                  <span className="text-xs text-[var(--subtle)]">
                    复习 {w.reviewCount} 次
                  </span>
                </div>
                <h3 className="mt-2">{w.question}</h3>
                <div className="row-actions">
                  <button
                    type="button"
                    className="btn btn-ghost"
                    onClick={() => toggleMastered(w.id)}
                  >
                    <RotateCcw size={14} />
                    重新复习
                  </button>
                </div>
              </motion.div>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
