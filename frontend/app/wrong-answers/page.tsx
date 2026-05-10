"use client";

import { useState, useEffect, useCallback } from "react";
import { motion } from "framer-motion";
import {
  AlertTriangle,
  RotateCcw,
  CheckCircle,
  TrendingUp,
  BookOpen,
  Loader2,
} from "lucide-react";
import { PageHero } from "@/components/new-ui/PageHero";
import { EmptyState } from "@/components/ui/EmptyState";
import {
  listWrongBooks,
  updateMasteryStatus,
  type WrongBookItem,
} from "@/lib/interview-api";
import { toErrorMessage } from "@/lib/interview-api";

type FrontendWrongAnswer = {
  id: number;
  question: string;
  direction: string;
  yourAnswer: string;
  correctAnswer: string;
  mastered: boolean;
  reviewCount: number;
  wrongBookId: number;
  questionId: number;
  analysisText?: string | null;
};

function mapBackendToFrontend(item: WrongBookItem): FrontendWrongAnswer {
  const typeMap: Record<string, string> = {
    SINGLE_CHOICE: "单选",
    MULTIPLE_CHOICE: "多选",
    SHORT_ANSWER: "简答",
    CODING: "编程",
    INTERVIEW: "面试",
    choice: "单选",
    short: "简答",
    code: "编程",
    interview: "面试",
  };
  return {
    id: item.id,
    question: item.question || "无题目内容",
    direction: typeMap[item.questionType || ""] || item.questionType || "其他",
    yourAnswer: "未记录",
    correctAnswer: item.referenceAnswer || "暂无参考答案",
    mastered: item.masteryStatus === "MASTERED",
    reviewCount: item.reviewCount || item.wrongCount || 0,
    wrongBookId: item.id,
    questionId: item.questionId,
    analysisText: item.analysisText,
  };
}

export default function WrongAnswersPage() {
  const [wrongAnswers, setWrongAnswers] = useState<FrontendWrongAnswer[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadWrongBooks = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const items = await listWrongBooks();
      setWrongAnswers(items.map(mapBackendToFrontend));
    } catch (err) {
      setError(toErrorMessage(err, "加载错题本失败"));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadWrongBooks();
  }, [loadWrongBooks]);

  const toggleMastered = async (id: number) => {
    const item = wrongAnswers.find((w) => w.id === id);
    if (!item) return;

    const newStatus = item.mastered ? "UNMASTERED" : "MASTERED";
    const originalAnswers = [...wrongAnswers];

    setWrongAnswers((prev) =>
      prev.map((w) => (w.id === id ? { ...w, mastered: !w.mastered } : w))
    );

    try {
      await updateMasteryStatus(item.wrongBookId, { masteryStatus: newStatus });
    } catch (err) {
      setWrongAnswers(originalAnswers);
      setError(toErrorMessage(err, "更新状态失败"));
    }
  };

  const unmastered = wrongAnswers.filter((w) => !w.mastered);
  const mastered = wrongAnswers.filter((w) => w.mastered);
  const masteryRate =
    wrongAnswers.length > 0
      ? Math.round((mastered.length / wrongAnswers.length) * 100)
      : 0;

  if (loading) {
    return (
      <div className="flex h-96 items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-[var(--blue)]" />
      </div>
    );
  }

  return (
    <div>
      <PageHero
        kicker="错题本"
        title="错题复习"
        description="自动记录错题，智能分析薄弱知识点，科学安排复习计划。"
      />

      {error && (
        <div className="mb-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
          <button
            type="button"
            className="ml-2 underline"
            onClick={() => void loadWrongBooks()}
          >
            重试
          </button>
        </div>
      )}

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
          <span className="text-sm font-bold text-[var(--blue)]">
            {masteryRate}%
          </span>
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
                    onClick={() => void toggleMastered(w.id)}
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
                    onClick={() => void toggleMastered(w.id)}
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
