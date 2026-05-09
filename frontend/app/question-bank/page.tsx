"use client";

import { useState } from "react";
import { motion } from "framer-motion";
import {
  ClipboardList,
  Search,
  Bookmark,
} from "lucide-react";
import { PageHero } from "@/components/new-ui/PageHero";
import { EmptyState } from "@/components/ui/EmptyState";

const directions = ["全部", "Java", "前端", "Go", "算法", "数据库", "系统设计"];
const difficulties = ["全部", "简单", "中等", "困难"];
const types = ["全部", "单选题", "多选题", "判断题", "简答题"];

const mockQuestions = [
  {
    id: 1,
    question: "Java 中 HashMap 的底层数据结构是什么？",
    direction: "Java",
    difficulty: "中等",
    type: "单选题",
    bookmarked: false,
  },
  {
    id: 2,
    question: "Redis 缓存穿透、击穿、雪崩的区别和解决方案？",
    direction: "数据库",
    difficulty: "困难",
    type: "简答题",
    bookmarked: true,
  },
  {
    id: 3,
    question: "CSS 中 flex 布局的 justify-content 有哪些取值？",
    direction: "前端",
    difficulty: "简单",
    type: "多选题",
    bookmarked: false,
  },
  {
    id: 4,
    question: "Go 语言中的 goroutine 和线程有什么区别？",
    direction: "Go",
    difficulty: "中等",
    type: "简答题",
    bookmarked: false,
  },
  {
    id: 5,
    question: "快速排序的时间复杂度是多少？",
    direction: "算法",
    difficulty: "简单",
    type: "单选题",
    bookmarked: true,
  },
];

const difficultyColor: Record<string, string> = {
  简单: "var(--green)",
  中等: "var(--yellow)",
  困难: "var(--red)",
};

export default function QuestionBankPage() {
  const [direction, setDirection] = useState("全部");
  const [difficulty, setDifficulty] = useState("全部");
  const [type, setType] = useState("全部");
  const [search, setSearch] = useState("");
  const [questions, setQuestions] = useState(mockQuestions);

  const filtered = questions.filter((q) => {
    const matchDir = direction === "全部" || q.direction === direction;
    const matchDiff = difficulty === "全部" || q.difficulty === difficulty;
    const matchType = type === "全部" || q.type === type;
    const matchSearch = q.question.toLowerCase().includes(search.toLowerCase());
    return matchDir && matchDiff && matchType && matchSearch;
  });

  const toggleBookmark = (id: number) => {
    setQuestions((prev) =>
      prev.map((q) => (q.id === id ? { ...q, bookmarked: !q.bookmarked } : q))
    );
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
          <p className="field-label">难度</p>
          <div className="chip-row">
            {difficulties.map((d) => (
              <button
                key={d}
                type="button"
                className={`chip ${difficulty === d ? "active" : ""}`}
                onClick={() => setDifficulty(d)}
              >
                {d}
              </button>
            ))}
          </div>
        </div>

        <div className="field-group">
          <p className="field-label">题型</p>
          <div className="chip-row">
            {types.map((t) => (
              <button
                key={t}
                type="button"
                className={`chip ${type === t ? "active" : ""}`}
                onClick={() => setType(t)}
              >
                {t}
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
      {filtered.length === 0 ? (
        <EmptyState
          icon={ClipboardList}
          title="暂无题目"
          description="尝试调整筛选条件或搜索关键词"
        />
      ) : (
        <div className="list-grid">
          {filtered.map((q, i) => (
            <motion.div
              key={q.id}
              className="list-card"
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.05, duration: 0.3 }}
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span
                    className="badge"
                    style={{
                      color: difficultyColor[q.difficulty],
                      background: `${difficultyColor[q.difficulty]}15`,
                    }}
                  >
                    {q.difficulty}
                  </span>
                  <span className="badge" style={{ color: "var(--blue)", background: "var(--blue-soft)" }}>
                    {q.type}
                  </span>
                  <span className="text-xs text-[var(--muted)]">{q.direction}</span>
                </div>
                <button
                  type="button"
                  className="btn btn-ghost icon-btn"
                  onClick={() => toggleBookmark(q.id)}
                >
                  <Bookmark
                    size={14}
                    className={q.bookmarked ? "fill-[var(--blue)] text-[var(--blue)]" : ""}
                  />
                </button>
              </div>
              <h3>{q.question}</h3>
            </motion.div>
          ))}
        </div>
      )}
    </div>
  );
}
