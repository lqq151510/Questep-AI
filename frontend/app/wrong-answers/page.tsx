"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { Check, RefreshCw, Search, Target } from "lucide-react";
import { PageHero } from "@/components/new-ui/PageHero";
import { useToast } from "@/components/new-ui/ToastProvider";
import { listQuestions, toErrorMessage, type BackendQuestion } from "@/lib/interview-api";

type WrongItem = {
  id: string;
  title: string;
  category: string;
  level: "高频" | "常规";
  mastered: boolean;
};

const WRONG_BOOK_KEY = "wrong_question_records";

function inferCategory(stem: string): string {
  const lower = stem.toLowerCase();
  if (lower.includes("spring")) return "Spring";
  if (lower.includes("mysql") || lower.includes("索引")) return "MySQL";
  if (lower.includes("redis")) return "Redis";
  if (lower.includes("系统设计") || lower.includes("高并发")) return "系统设计";
  if (lower.includes("java") || lower.includes("线程") || lower.includes("jvm")) return "Java";
  return "通用";
}

function normalizeLevel(difficulty?: number | null): "高频" | "常规" {
  return (difficulty ?? 3) >= 4 ? "高频" : "常规";
}

function toWrongItem(question: BackendQuestion): WrongItem {
  return {
    id: `q-${question.id}`,
    title: question.stemText,
    category: inferCategory(question.stemText),
    level: normalizeLevel(question.difficulty),
    mastered: false
  };
}

function readLocalWrongItems(): WrongItem[] {
  if (typeof window === "undefined") return [];
  const raw = window.localStorage.getItem(WRONG_BOOK_KEY);
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw) as WrongItem[];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function persist(items: WrongItem[]) {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(WRONG_BOOK_KEY, JSON.stringify(items));
}

export default function WrongAnswersPage() {
  const { showToast } = useToast();
  const [category, setCategory] = useState("全部");
  const [keyword, setKeyword] = useState("");
  const [items, setItems] = useState<WrongItem[]>([]);
  const [loading, setLoading] = useState(false);

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      const localItems = readLocalWrongItems();
      const remoteQuestions = await listQuestions(30);
      const remoteItems = remoteQuestions.map(toWrongItem);
      const merged = new Map<string, WrongItem>();
      for (const item of remoteItems) {
        merged.set(item.id, item);
      }
      for (const item of localItems) {
        merged.set(item.id, item);
      }
      const result = Array.from(merged.values());
      setItems(result);
      persist(result);
    } catch (error) {
      showToast(toErrorMessage(error, "加载错题失败"));
      const localItems = readLocalWrongItems();
      setItems(localItems);
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
      const textPass = !text || item.title.toLowerCase().includes(text);
      return categoryPass && textPass;
    });
  }, [category, items, keyword]);

  const toggleMastered = (id: string) => {
    setItems((prev) => {
      const next = prev.map((item) => (item.id === id ? { ...item, mastered: !item.mastered } : item));
      persist(next);
      return next;
    });
  };

  return (
    <div className="container">
      <PageHero
        kicker="Wrong Answer Book"
        title="错题本"
        description="聚焦高频错误题，持续复习直到掌握。"
      />

      <section className="panel">
        <div className="filter-grid two">
          <select className="input-select" value={category} onChange={(event) => setCategory(event.target.value)}>
            <option>全部</option>
            <option>Java</option>
            <option>Spring</option>
            <option>MySQL</option>
            <option>Redis</option>
            <option>系统设计</option>
            <option>通用</option>
          </select>
          <label className="search-input-wrap">
            <Search size={14} />
            <input
              type="text"
              placeholder="搜索错题…"
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
            />
          </label>
        </div>

        <div className="row-actions">
          <button type="button" className="btn" onClick={() => void refresh()} disabled={loading}>
            <RefreshCw size={14} />
            {loading ? "刷新中" : "刷新错题"}
          </button>
        </div>

        <div className="list-grid">
          {filtered.map((item) => (
            <article key={item.id} className="list-card">
              <p className="list-meta">
                {item.category} · {item.level}
              </p>
              <h3>{item.title}</h3>
              <div className="row-actions">
                <button type="button" className="btn btn-accent" onClick={() => showToast("已加入复习队列")}>
                  <Target size={14} />
                  去复习
                </button>
                <button
                  type="button"
                  className="btn"
                  onClick={() => {
                    toggleMastered(item.id);
                    showToast(item.mastered ? "已取消掌握标记" : "已标记为掌握");
                  }}
                >
                  <Check size={14} />
                  {item.mastered ? "取消掌握" : "标记掌握"}
                </button>
              </div>
            </article>
          ))}
          {filtered.length === 0 && (
            <article className="list-card">
              <h3>暂无错题</h3>
              <p className="list-meta">请先完成一轮测试，错题会自动汇总到这里。</p>
            </article>
          )}
        </div>
      </section>
    </div>
  );
}
