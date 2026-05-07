"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { PlusCircle, RefreshCw, Search } from "lucide-react";
import { PageHero } from "@/components/new-ui/PageHero";
import { useToast } from "@/components/new-ui/ToastProvider";
import { listQuestions, toErrorMessage, type BackendQuestion } from "@/lib/interview-api";

type QuestionItem = {
  id: string;
  title: string;
  category: "Java" | "Spring" | "MySQL" | "Redis" | "系统设计" | "通用";
  difficulty: "初级" | "中级" | "高级";
  type: "单选" | "多选" | "简答" | "编程" | "面试";
};

const categories = ["全部", "Java", "Spring", "MySQL", "Redis", "系统设计", "通用"] as const;
const levels = ["全部", "初级", "中级", "高级"] as const;
const types = ["全部", "单选", "多选", "简答", "编程", "面试"] as const;

function inferCategory(stem: string): QuestionItem["category"] {
  const lower = stem.toLowerCase();
  if (lower.includes("spring")) return "Spring";
  if (lower.includes("mysql") || lower.includes("索引") || lower.includes("事务")) return "MySQL";
  if (lower.includes("redis")) return "Redis";
  if (lower.includes("系统设计") || lower.includes("高并发")) return "系统设计";
  if (lower.includes("java") || lower.includes("jvm") || lower.includes("线程")) return "Java";
  return "通用";
}

function normalizeDifficulty(value?: number | null): QuestionItem["difficulty"] {
  const raw = value ?? 3;
  if (raw <= 2) return "初级";
  if (raw <= 3) return "中级";
  return "高级";
}

function normalizeType(value?: string): QuestionItem["type"] {
  const upper = String(value ?? "").toUpperCase();
  if (upper.includes("MULTIPLE")) return "多选";
  if (upper.includes("SINGLE")) return "单选";
  if (upper.includes("CODING") || upper.includes("CODE")) return "编程";
  if (upper.includes("INTERVIEW")) return "面试";
  return "简答";
}

function toQuestionItem(question: BackendQuestion): QuestionItem {
  return {
    id: String(question.id),
    title: question.stemText,
    category: inferCategory(question.stemText),
    difficulty: normalizeDifficulty(question.difficulty),
    type: normalizeType(question.questionType)
  };
}

export default function QuestionBankPage() {
  const { showToast } = useToast();
  const [items, setItems] = useState<QuestionItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [category, setCategory] = useState<(typeof categories)[number]>("全部");
  const [level, setLevel] = useState<(typeof levels)[number]>("全部");
  const [type, setType] = useState<(typeof types)[number]>("全部");
  const [keyword, setKeyword] = useState("");

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      const questions = await listQuestions(100);
      setItems(questions.map(toQuestionItem));
    } catch (error) {
      showToast(toErrorMessage(error, "加载题库失败"));
    } finally {
      setLoading(false);
    }
  }, [showToast]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const filtered = useMemo(() => {
    const text = keyword.trim().toLowerCase();
    return items.filter((item) => {
      const categoryPass = category === "全部" || item.category === category;
      const levelPass = level === "全部" || item.difficulty === level;
      const typePass = type === "全部" || item.type === type;
      const textPass = !text || item.title.toLowerCase().includes(text);
      return categoryPass && levelPass && typePass && textPass;
    });
  }, [category, items, keyword, level, type]);

  return (
    <div className="container">
      <PageHero
        kicker="Question Bank"
        title="题库中心"
        description="按方向、难度和题型筛选题目，快速加入练习卷。"
      />

      <section className="panel">
        <div className="filter-grid">
          <select className="input-select" value={category} onChange={(event) => setCategory(event.target.value as (typeof categories)[number])}>
            {categories.map((item) => (
              <option key={item}>{item}</option>
            ))}
          </select>
          <select className="input-select" value={level} onChange={(event) => setLevel(event.target.value as (typeof levels)[number])}>
            {levels.map((item) => (
              <option key={item}>{item}</option>
            ))}
          </select>
          <select className="input-select" value={type} onChange={(event) => setType(event.target.value as (typeof types)[number])}>
            {types.map((item) => (
              <option key={item}>{item}</option>
            ))}
          </select>
          <label className="search-input-wrap">
            <Search size={14} />
            <input
              type="text"
              placeholder="搜索题目…"
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
            />
          </label>
        </div>

        <div className="row-actions">
          <button type="button" className="btn" onClick={() => void refresh()} disabled={loading}>
            <RefreshCw size={14} />
            {loading ? "加载中" : "刷新题库"}
          </button>
        </div>

        <div className="list-grid">
          {filtered.map((item) => (
            <article key={item.id} className="list-card">
              <p className="list-meta">
                {item.category} · {item.difficulty} · {item.type}
              </p>
              <h3>{item.title}</h3>
              <button type="button" className="btn btn-accent" onClick={() => showToast("已加入练习队列")}>
                <PlusCircle size={14} />
                加入练习
              </button>
            </article>
          ))}
          {filtered.length === 0 && (
            <article className="list-card">
              <h3>暂无题目</h3>
              <p className="list-meta">请先在知识库上传资料并生成题目。</p>
            </article>
          )}
        </div>
      </section>
    </div>
  );
}
