"use client";

import { useState } from "react";
import { motion } from "framer-motion";
import {
  MessageSquare,
  Send,
  Clock,
  User,
  Bot,
  RotateCcw,
  Star,
  Target,
  TrendingUp,
  Award,
  ArrowRight,
} from "lucide-react";
import { PageHero } from "@/components/new-ui/PageHero";

const positions = [
  "Java 后端",
  "前端开发",
  "Go 后端",
  "算法工程师",
  "全栈开发",
  "数据工程师",
];

const difficulties = [
  { label: "初级", desc: "1-3 年经验" },
  { label: "中级", desc: "3-5 年经验" },
  { label: "高级", desc: "5 年以上" },
];

const mockMessages = [
  {
    role: "ai" as const,
    content: "你好！我是你的 AI 面试官。今天我们将进行一场 Java 后端开发的模拟面试。请先做一下自我介绍吧。",
    time: "14:30",
  },
  {
    role: "user" as const,
    content: "你好，我叫张三，有 3 年 Java 开发经验，熟悉 Spring Boot、MySQL、Redis 等技术栈。",
    time: "14:31",
  },
  {
    role: "ai" as const,
    content: "很好。那我们先从技术基础开始。请说说 Java 中 HashMap 的底层实现原理，以及它在 JDK 1.8 中做了哪些优化？",
    time: "14:32",
  },
];

const scores = [
  { label: "技术能力", score: 85, icon: Target },
  { label: "表达能力", score: 78, icon: MessageSquare },
  { label: "逻辑思维", score: 82, icon: TrendingUp },
  { label: "项目经验", score: 80, icon: Award },
];

export default function AIInterviewerPage() {
  const [position, setPosition] = useState("Java 后端");
  const [difficulty, setDifficulty] = useState(1);
  const [started, setStarted] = useState(false);
  const [messages, setMessages] = useState(mockMessages);
  const [input, setInput] = useState("");
  const [finished, setFinished] = useState(false);

  const startInterview = () => {
    setStarted(true);
    setMessages(mockMessages);
    setFinished(false);
  };

  const sendMessage = () => {
    if (!input.trim()) return;
    const newMsg = { role: "user" as const, content: input, time: new Date().toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" }) };
    setMessages((prev) => [...prev, newMsg]);
    setInput("");

    setTimeout(() => {
      const aiReply = {
        role: "ai" as const,
        content: "回答得不错！那我们再深入一点，你能说说 Redis 的持久化机制吗？RDB 和 AOF 各有什么优缺点？",
        time: new Date().toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" }),
      };
      setMessages((prev) => [...prev, aiReply]);
    }, 1200);
  };

  const finishInterview = () => {
    setFinished(true);
  };

  if (!started) {
    return (
      <div>
        <PageHero
          kicker="AI 面试官"
          title="模拟面试"
          description="选择岗位和难度，与 AI 面试官进行真实场景模拟。"
        />

        <div className="panel">
          <div className="field-group">
            <p className="field-label">面试岗位</p>
            <div className="chip-row">
              {positions.map((p) => (
                <button
                  key={p}
                  type="button"
                  className={`chip ${position === p ? "active" : ""}`}
                  onClick={() => setPosition(p)}
                >
                  {p}
                </button>
              ))}
            </div>
          </div>

          <div className="field-group">
            <p className="field-label">难度等级</p>
            <div className="grid grid-cols-3 gap-3">
              {difficulties.map((d, i) => (
                <button
                  key={d.label}
                  type="button"
                  className={`rounded-xl border p-4 text-left transition-all ${
                    difficulty === i
                      ? "border-[var(--blue)] bg-[var(--blue-soft)] shadow-glow"
                      : "border-[var(--border)] bg-[var(--surface-soft)] hover:border-[var(--border-strong)]"
                  }`}
                  onClick={() => setDifficulty(i)}
                >
                  <p className="font-semibold text-sm">{d.label}</p>
                  <p className="mt-1 text-xs text-[var(--muted)]">{d.desc}</p>
                </button>
              ))}
            </div>
          </div>

          <button type="button" className="btn btn-accent wide" onClick={startInterview}>
            <MessageSquare size={16} />
            开始面试
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
          kicker="面试完成"
          title="面试报告"
          description={`${position} · ${difficulties[difficulty].label}`}
        />

        <motion.div
          className="panel"
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.4 }}
        >
          <div className="text-center">
            <div className="relative mx-auto mb-4 flex h-20 w-20 items-center justify-center rounded-full bg-gradient-to-br from-[var(--blue-soft)] to-[var(--cyan-soft)]">
              <Star size={32} className="text-[var(--blue)]" />
            </div>
            <h2 className="text-3xl font-bold text-[var(--ink)]">81.25</h2>
            <p className="mt-1 text-sm text-[var(--muted)]">综合评分</p>
          </div>

          <div className="mt-6 grid grid-cols-2 gap-4">
            {scores.map((s) => {
              const Icon = s.icon;
              return (
                <div key={s.label} className="rounded-xl bg-[var(--surface-soft)] p-4">
                  <div className="flex items-center gap-2">
                    <Icon size={16} className="text-[var(--blue)]" />
                    <span className="text-sm font-medium">{s.label}</span>
                  </div>
                  <div className="mt-3 h-2 overflow-hidden rounded-full bg-[var(--border)]">
                    <motion.div
                      className="h-full rounded-full bg-gradient-to-r from-[var(--blue)] to-[var(--cyan)]"
                      initial={{ width: 0 }}
                      animate={{ width: `${s.score}%` }}
                      transition={{ duration: 1, delay: 0.3 }}
                    />
                  </div>
                  <p className="mt-2 text-right text-sm font-bold">{s.score}</p>
                </div>
              );
            })}
          </div>

          <div className="mt-6 rounded-xl bg-[var(--surface-soft)] p-4">
            <h3 className="font-semibold text-sm">改进建议</h3>
            <ul className="mt-3 space-y-2 text-sm text-[var(--muted)]">
              <li className="flex items-start gap-2">
                <span className="mt-1 h-1.5 w-1.5 rounded-full bg-[var(--blue)]" />
                建议在回答技术问题时增加实际项目案例
              </li>
              <li className="flex items-start gap-2">
                <span className="mt-1 h-1.5 w-1.5 rounded-full bg-[var(--blue)]" />
                对 Redis 持久化机制的理解可以更深入
              </li>
              <li className="flex items-start gap-2">
                <span className="mt-1 h-1.5 w-1.5 rounded-full bg-[var(--blue)]" />
                表达清晰，但语速可以适当放慢
              </li>
            </ul>
          </div>

          <div className="mt-6 flex justify-center gap-3">
            <button type="button" className="btn btn-accent" onClick={startInterview}>
              <RotateCcw size={14} />
              再来一次
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

  return (
    <div>
      <PageHero
        kicker="模拟面试中"
        title={position + " 面试"}
        description={`${difficulties[difficulty].label} · 已进行 ${messages.length} 轮对话`}
      />

      <div className="panel chat-panel">
        <div className="chat-head">
          <div>
            <p className="chat-title flex items-center gap-2">
              <Bot size={16} className="text-[var(--blue)]" />
              AI 面试官
            </p>
            <p className="chat-subtitle">{position} · {difficulties[difficulty].label}</p>
          </div>
          <div className="flex items-center gap-3">
            <p className="chat-timer">
              <Clock size={14} />
              15:00
            </p>
            <button type="button" className="btn btn-ghost icon-btn" onClick={finishInterview} title="结束面试">
              <Award size={16} />
            </button>
          </div>
        </div>

        <div className="chat-log">
          {messages.map((msg, i) => (
            <motion.div
              key={i}
              className={`chat-bubble ${msg.role}`}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.3 }}
            >
              <div className="bubble-head">
                {msg.role === "ai" ? <Bot size={14} /> : <User size={14} />}
                {msg.role === "ai" ? "AI 面试官" : "你"}
                <span className="text-[var(--subtle)]">{msg.time}</span>
              </div>
              <p>{msg.content}</p>
            </motion.div>
          ))}
        </div>

        <div className="chat-input-row">
          <input
            type="text"
            className="chat-input"
            placeholder="输入你的回答..."
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && sendMessage()}
          />
          <button
            type="button"
            className="btn btn-accent icon-btn"
            onClick={sendMessage}
            disabled={!input.trim()}
          >
            <Send size={16} />
          </button>
        </div>
      </div>
    </div>
  );
}
