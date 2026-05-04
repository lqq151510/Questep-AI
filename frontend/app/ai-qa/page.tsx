"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { Bot, MessageSquareText, RefreshCw, Send, User } from "lucide-react";
import { PageHero } from "@/components/new-ui/PageHero";
import { useToast } from "@/components/new-ui/ToastProvider";
import { listQuestions, toErrorMessage, type BackendQuestion } from "@/lib/interview-api";

type Message = {
  id: string;
  role: "user" | "ai";
  text: string;
};

function buildAnswer(question: BackendQuestion): string {
  const parts = [question.referenceAnswer?.trim(), question.analysisText?.trim()].filter(
    (item): item is string => Boolean(item && item.length > 0)
  );
  if (parts.length === 0) {
    return "建议按定义-原理-场景-边界四段式回答，并结合你的项目经历补充取舍。";
  }
  return parts.join("\n");
}

function matchQuestion(prompt: string, questions: BackendQuestion[]): BackendQuestion | null {
  const lower = prompt.toLowerCase();
  for (const question of questions) {
    const stem = question.stemText.toLowerCase();
    if (stem.includes(lower) || lower.includes(stem)) {
      return question;
    }
  }
  const keywords = lower.split(/[\s，。；、,.!?？]+/).filter((token) => token.length >= 2);
  let best: { item: BackendQuestion; score: number } | null = null;
  for (const question of questions) {
    const stem = question.stemText.toLowerCase();
    const score = keywords.reduce((sum, token) => (stem.includes(token) ? sum + 1 : sum), 0);
    if (!best || score > best.score) {
      best = { item: question, score };
    }
  }
  if (!best || best.score === 0) {
    return null;
  }
  return best.item;
}

export default function AiQaPage() {
  const { showToast } = useToast();
  const [messages, setMessages] = useState<Message[]>([
    {
      id: crypto.randomUUID(),
      role: "ai",
      text: "你好，我是你的 AI 面试辅导助手。我会优先基于你题库里的真实题目给出回答。"
    }
  ]);
  const [input, setInput] = useState("");
  const [questionBank, setQuestionBank] = useState<BackendQuestion[]>([]);
  const [loading, setLoading] = useState(false);

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      const data = await listQuestions(80);
      setQuestionBank(data);
    } catch (error) {
      showToast(toErrorMessage(error, "加载题库失败"));
    } finally {
      setLoading(false);
    }
  }, [showToast]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const presets = useMemo(() => {
    const fromApi = questionBank.slice(0, 5).map((item) => item.stemText);
    if (fromApi.length > 0) {
      return fromApi;
    }
    return [
      "volatile 和 synchronized 的区别",
      "HashMap 为什么线程不安全",
      "MySQL 索引失效场景",
      "Redis 持久化策略怎么选",
      "Spring 事务失效排查路径"
    ];
  }, [questionBank]);

  const sendMessage = (text: string) => {
    const clean = text.trim();
    if (!clean) {
      return;
    }
    const userMsg: Message = { id: crypto.randomUUID(), role: "user", text: clean };
    setMessages((prev) => [...prev, userMsg]);
    setInput("");

    window.setTimeout(() => {
      const matched = matchQuestion(clean, questionBank);
      const aiText = matched
        ? buildAnswer(matched)
        : "题库里暂未命中完全匹配的问题。建议你换个关键词，或先在知识库生成更多题目。";
      setMessages((prev) => [
        ...prev,
        { id: crypto.randomUUID(), role: "ai", text: aiText }
      ]);
    }, 420);
  };

  return (
    <div className="container">
      <PageHero
        kicker="Live Q&A"
        title="AI 问答助手"
        description="优先基于后端题库里的真实题干、参考答案和解析进行问答。"
      />

      <section className="panel qa-panel">
        <div className="row-actions">
          <button type="button" className="btn" onClick={() => void refresh()} disabled={loading}>
            <RefreshCw size={14} />
            {loading ? "刷新中" : `已加载 ${questionBank.length} 题`}
          </button>
        </div>

        <div className="chip-row">
          {presets.map((preset) => (
            <button key={preset} type="button" className="chip" onClick={() => sendMessage(preset)}>
              {preset}
            </button>
          ))}
        </div>

        <div className="chat-log">
          {messages.map((message) => (
            <article key={message.id} className={message.role === "ai" ? "chat-bubble ai" : "chat-bubble user"}>
              <div className="bubble-head">
                <span>{message.role === "ai" ? <Bot size={14} /> : <User size={14} />}</span>
                <span>{message.role === "ai" ? "AI 助手" : "你"}</span>
              </div>
              <p>{message.text}</p>
            </article>
          ))}
        </div>

        <div className="chat-input-row">
          <input
            type="text"
            className="chat-input"
            placeholder="输入你想追问的问题…"
            value={input}
            onChange={(event) => setInput(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === "Enter") {
                sendMessage(input);
              }
            }}
          />
          <button
            type="button"
            className="btn btn-accent icon-btn"
            onClick={() => {
              if (!input.trim()) {
                showToast("请先输入问题");
                return;
              }
              sendMessage(input);
            }}
          >
            <Send size={14} />
          </button>
        </div>

        <div className="qa-hint">
          <MessageSquareText size={14} />
          建议追问时补充岗位场景，例如「高并发订单系统里如何落地？」
        </div>
      </section>
    </div>
  );
}
