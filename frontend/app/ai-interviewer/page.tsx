"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { Bot, Clock3, FileText, RefreshCw, Send, User } from "lucide-react";
import { PageHero } from "@/components/new-ui/PageHero";
import { useToast } from "@/components/new-ui/ToastProvider";
import { listQuestions, toErrorMessage, type BackendQuestion } from "@/lib/interview-api";

type ChatMessage = {
  id: string;
  role: "ai" | "user";
  text: string;
  time: string;
};

function formatTime(date: Date) {
  return date.toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" });
}

function formatSeconds(totalSeconds: number) {
  const minutes = String(Math.floor(totalSeconds / 60)).padStart(2, "0");
  const seconds = String(totalSeconds % 60).padStart(2, "0");
  return `${minutes}:${seconds}`;
}

function inferRoleKeywords(role: string): string[] {
  const lower = role.toLowerCase();
  if (lower.includes("全栈")) return ["spring", "mysql", "redis", "系统设计"];
  if (lower.includes("系统设计")) return ["系统设计", "高并发", "可用性", "架构"];
  return ["java", "spring", "并发", "事务"];
}

function pickQuestionPool(questions: BackendQuestion[], role: string): BackendQuestion[] {
  const keywords = inferRoleKeywords(role);
  const selected = questions.filter((item) =>
    keywords.some((keyword) => item.stemText.toLowerCase().includes(keyword.toLowerCase()))
  );
  return selected.length > 0 ? selected : questions;
}

export default function AiInterviewerPage() {
  const { showToast } = useToast();
  const [started, setStarted] = useState(false);
  const [ended, setEnded] = useState(false);
  const [jobRole, setJobRole] = useState("Java 后端工程师");
  const [difficulty, setDifficulty] = useState("中级");
  const [input, setInput] = useState("");
  const [seconds, setSeconds] = useState(0);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [questionBank, setQuestionBank] = useState<BackendQuestion[]>([]);
  const [questionCursor, setQuestionCursor] = useState(0);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!started || ended) {
      return;
    }
    const timer = window.setInterval(() => setSeconds((prev) => prev + 1), 1000);
    return () => window.clearInterval(timer);
  }, [started, ended]);

  const aiScore = useMemo(() => {
    const baseline = 70 + Math.min(22, Math.floor(messages.length * 1.6));
    return Math.min(98, baseline);
  }, [messages.length]);

  const questionPool = useMemo(() => pickQuestionPool(questionBank, jobRole), [questionBank, jobRole]);

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      const data = await listQuestions(60);
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

  const startInterview = () => {
    if (questionPool.length === 0) {
      showToast("暂无面试题，请先在知识库生成题目");
      return;
    }
    const opener = questionPool[0];
    setMessages([
      {
        id: crypto.randomUUID(),
        role: "ai",
        text: `你好，这是一场「${jobRole} · ${difficulty}」模拟面试。第一题：${opener.stemText}`,
        time: formatTime(new Date())
      }
    ]);
    setQuestionCursor(1);
    setSeconds(0);
    setStarted(true);
    setEnded(false);
    showToast("模拟面试已开始");
  };

  const sendMessage = () => {
    const text = input.trim();
    if (!text || questionPool.length === 0) {
      return;
    }
    const userMessage: ChatMessage = {
      id: crypto.randomUUID(),
      role: "user",
      text,
      time: formatTime(new Date())
    };
    setMessages((prev) => [...prev, userMessage]);
    setInput("");

    window.setTimeout(() => {
      const nextQuestion = questionPool[questionCursor % questionPool.length];
      const followUp: ChatMessage = {
        id: crypto.randomUUID(),
        role: "ai",
        text: `继续追问：${nextQuestion.stemText}\n请补充你的取舍依据与性能影响。`,
        time: formatTime(new Date())
      };
      setQuestionCursor((prev) => prev + 1);
      setMessages((prev) => [...prev, followUp]);
    }, 500);
  };

  const endInterview = () => {
    if (!started) {
      return;
    }
    setEnded(true);
    showToast("已生成面试反馈报告");
  };

  return (
    <div className="container">
      <PageHero
        kicker="Mock Interview"
        title="AI 面试官"
        description="基于后端真实题库追问关键细节并输出结构化反馈。"
      />

      {!started ? (
        <section className="panel form-panel">
          <div className="row-actions">
            <button type="button" className="btn" onClick={() => void refresh()} disabled={loading}>
              <RefreshCw size={14} />
              {loading ? "刷新中" : `已加载 ${questionPool.length} 题`}
            </button>
          </div>

          <h2>面试配置</h2>
          <div className="field-group">
            <label htmlFor="role" className="field-label">
              目标岗位
            </label>
            <select
              id="role"
              className="input-select"
              value={jobRole}
              onChange={(event) => setJobRole(event.target.value)}
            >
              <option>Java 后端工程师</option>
              <option>全栈开发工程师</option>
              <option>高级系统设计工程师</option>
            </select>
          </div>

          <div className="field-group">
            <label htmlFor="level" className="field-label">
              难度级别
            </label>
            <select
              id="level"
              className="input-select"
              value={difficulty}
              onChange={(event) => setDifficulty(event.target.value)}
            >
              <option>初级</option>
              <option>中级</option>
              <option>高级</option>
            </select>
          </div>

          <button type="button" className="btn btn-accent wide" onClick={startInterview}>
            开始面试
          </button>
        </section>
      ) : (
        <section className="panel chat-panel">
          <div className="chat-head">
            <div>
              <p className="chat-title">
                {jobRole} · {difficulty}
              </p>
              <p className="chat-subtitle">已运行 {formatSeconds(seconds)}</p>
            </div>
            <p className="chat-timer">
              <Clock3 size={14} />
              {formatSeconds(seconds)}
            </p>
          </div>

          <div className="chat-log">
            {messages.map((message) => (
              <article key={message.id} className={message.role === "ai" ? "chat-bubble ai" : "chat-bubble user"}>
                <div className="bubble-head">
                  <span>{message.role === "ai" ? <Bot size={14} /> : <User size={14} />}</span>
                  <span>{message.role === "ai" ? "AI 面试官" : "你"}</span>
                  <span>{message.time}</span>
                </div>
                <p>{message.text}</p>
              </article>
            ))}
          </div>

          {!ended ? (
            <>
              <div className="chat-input-row">
                <input
                  type="text"
                  className="chat-input"
                  placeholder="输入你的回答…"
                  value={input}
                  onChange={(event) => setInput(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === "Enter") {
                      sendMessage();
                    }
                  }}
                />
                <button type="button" className="btn btn-accent icon-btn" onClick={sendMessage}>
                  <Send size={14} />
                </button>
              </div>

              <div className="row-actions">
                <button type="button" className="btn" onClick={endInterview}>
                  结束面试并生成报告
                </button>
              </div>
            </>
          ) : (
            <article className="result-panel">
              <p className="result-badge">
                <FileText size={16} /> 报告已生成
              </p>
              <h3>综合得分 {aiScore}</h3>
              <p>优势：回答覆盖面较好。建议：进一步量化结果，并补充容量、成本与降级策略。</p>
              <div className="row-actions">
                <button type="button" className="btn btn-accent" onClick={startInterview}>
                  再来一轮
                </button>
                <button type="button" className="btn" onClick={() => showToast("已加入错题强化计划")}>
                  加入强化计划
                </button>
              </div>
            </article>
          )}
        </section>
      )}
    </div>
  );
}
