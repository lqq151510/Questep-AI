"use client";

import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import {
  ClipboardList,
  Search,
  Bookmark,
} from "lucide-react";
import { PageHero } from "@/components/new-ui/PageHero";
import { EmptyState } from "@/components/ui/EmptyState";
import { Skeleton } from "@/components/ui/Skeleton";
import { listQuestions, type BackendQuestion, toErrorMessage } from "@/lib/interview-api";

const difficulties = ["全部", "1", "2", "3", "4", "5"];
const difficultyLabels: Record<string, string> = {
  "1": "基础",
  "2": "简单",
  "3": "中等",
  "4": "困难",
  "5": "专家",
};
const difficultyColor: Record<string, string> = {
  "1": "var(--green)",
  "2": "var(--green)",
  "3": "var(--yellow)",
  "4": "var(--red)",
  "5": "var(--red)",
};

export default function QuestionBankPage() {
  const [questions, setQuestions] = useState<BackendQuestion[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [difficulty, setDifficulty] = useState("全部");
  const [bookmarked, setBookmarked] = useState<Set<number>>(new Set());

  const [page, setPage] = useState(0);
  const [pageSize] = useState(20);

  const fetchQuestions = () => {
    setLoading(true);
    setError("");
    listQuestions(page, pageSize)
      .then(setQuestions)
      .catch((e) => setError(toErrorMessage(e, "获取题库失败")))
      .finally(() => setLoading(false));
  };

  useEffect(() => { fetchQuestions(); }, [page]);

  const filtered = questions.filter((q) => {
    const matchDiff = difficulty === "全部" || String(q.difficulty ?? "") === difficulty;
    const matchSearch = q.stemText?.toLowerCase().includes(search.toLowerCase());
    return matchDiff && matchSearch;
  });

  const toggleBookmark = (id: number) => {
    setBookmarked((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  };

  return (
    <div>
      <PageHero
        kicker="题库"
        title="面试题库"
        description="海量面试真题，支持按方向、难度筛选，针对性提升薄弱环节。"
      />

      {/* Filters */}
      <div className="panel">
        <div className="field-group">
          <p className="field-label">难度筛选</p>
          <div className="chip-row">
            {difficulties.map((d) => (
              <button
                key={d}
                type="button"
                className={`chip ${difficulty === d ? "active" : ""}`}
                onClick={() => setDifficulty(d)}
              >
                {d === "全部" ? "全部" : `${d} · ${difficultyLabels[d]}`}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Search & Results */}
      <div className="search-row mt-5">
        <h2>
          题目列表
          <span className="ml-2 text-sm font-normal text-[var(--muted)]">
            共 {filtered.length} 题
          </span>
        </h2>
        <div className="search-input-wrap">
          <Search size={14} />
          <input
            type="text"
            placeholder="搜索题目..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
      </div>

      {/* Questions List */}
      {loading && (
        <div className="list-grid">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="list-card">
              <Skeleton className="h-4 w-1/4 mb-3" />
              <Skeleton className="h-5 w-3/4" />
            </div>
          ))}
        </div>
      )}

      {!loading && error && (
        <EmptyState
          icon={ClipboardList}
          title="加载失败"
          description={error}
          action={{ label: "重试", onClick: fetchQuestions }}
        />
      )}

      {!loading && !error && filtered.length === 0 && (
        <EmptyState
          icon={ClipboardList}
          title="暂无题目"
          description="尝试调整筛选条件或搜索关键词，或先生成题目"
        />
      )}

      {!loading && !error && filtered.length > 0 && (
        <div className="list-grid">
          {filtered.map((q, i) => {
            const diff = String(q.difficulty ?? "");
            const color = difficultyColor[diff] || "var(--muted)";
            return (
              <motion.div
                key={q.id}
                className="list-card"
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: i * 0.05, duration: 0.3 }}
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="badge" style={{ color, background: `${color}15` }}>
                      {difficultyLabels[diff] || `L${diff}`}
                    </span>
                    <span className="badge" style={{ color: "var(--blue)", background: "var(--blue-soft)" }}>
                      {q.questionType || q.sourceType || "通用"}
                    </span>
                    {q.modelName && (
                      <span className="text-xs text-[var(--muted)]">{q.modelName}</span>
                    )}
                  </div>
                  <button
                    type="button"
                    className="btn btn-ghost icon-btn"
                    onClick={() => toggleBookmark(q.id)}
                  >
                    <Bookmark
                      size={14}
                      className={bookmarked.has(q.id) ? "fill-[var(--blue)] text-[var(--blue)]" : ""}
                    />
                  </button>
                </div>
                <h3>{q.stemText}</h3>
              </motion.div>
            );
          })}
        </div>
      )}

      {!loading && !error && (
        <div className="pagination-row">
          <button
            type="button"
            className="btn btn-ghost"
            disabled={page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
          >
            上一页
          </button>
          <span className="text-sm text-[var(--muted)]">第 {page + 1} 页</span>
          <button
            type="button"
            className="btn btn-ghost"
            disabled={filtered.length < pageSize}
            onClick={() => setPage((p) => p + 1)}
          >
            下一页
          </button>
        </div>
      )}
    </div>
  );
}
