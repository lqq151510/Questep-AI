"use client";

import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import {
  MessageSquare,
  Send,
  Clock,
  User,
  Bot,
  RotateCcw,
  Star,
  Award,
  ArrowRight,
  Loader2,
  History,
} from "lucide-react";
import { PageHero } from "@/components/new-ui/PageHero";
import {
  createOrGetSession,
  resumeSession,
  sendChatMessage,
  type ChatMessagePayload,
  type InterviewSession,
  toErrorMessage,
} from "@/lib/interview-api";

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

type InterviewMessage = {
  role: "ai" | "user";
  content: string;
  time: string;
};

export default function AIInterviewerPage() {
  const [position, setPosition] = useState("Java 后端");
  const [difficulty, setDifficulty] = useState(1);
  const [started, setStarted] = useState(false);
  const [messages, setMessages] = useState<InterviewMessage[]>([]);
  const [input, setInput] = useState("");
  const [finished, setFinished] = useState(false);
  const [activeSession, setActiveSession] = useState<InterviewSession | null>(null);
  const [sending, setSending] = useState(false);
  const [starting, setStarting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    createOrGetSession(position, difficulty + 1).then(setActiveSession).catch(() => {});
  }, [position, difficulty]);

  const resumeLastSession = async () => {
    if (!activeSession) return;
    try {
      await resumeSession(activeSession.id);
      setPosition(activeSession.position);
      setDifficulty(activeSession.difficulty - 1);
      startInterview();
    } catch {
      // fall through to normal start
    }
  };

  const startInterview = async () => {
    setStarting(true);
    setError("");
    try {
      const session = await createOrGetSession(position, difficulty + 1);
      setActiveSession(session);

      const greeting: InterviewMessage = {
        role: "ai",
        content: `你好！我是你的 AI 面试官。今天我们将进行一场 ${position} 的模拟面试（${difficulties[difficulty].label}）。请先做一下自我介绍吧。`,
        time: new Date().toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" }),
      };
      setMessages([greeting]);
      setStarted(true);
      setFinished(false);
    } catch (e) {
      setError(toErrorMessage(e, "创建面试会话失败"));
    } finally {
      setStarting(false);
    }
  };

  const sendMessage = async () => {
    if (!input.trim() || sending) return;
    const newMsg: InterviewMessage = {
      role: "user",
      content: input,
      time: new Date().toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" }),
    };
    setMessages((prev) => [...prev, newMsg]);
    setInput("");
    setSending(true);
    setError("");

    try {
      const context: ChatMessagePayload[] = [
        {
          role: "system",
          content: `你是一位专业的 ${position} 面试官，面试难度为${difficulties[difficulty].label}。请根据候选人的回答进行追问，深入考察技术深度和项目经验。每次回复都要提出下一个问题，保持面试节奏。`,
        },
        ...messages.slice(-8).map((m) => ({
          role: (m.role === "ai" ? "assistant" : "user") as string,
          content: m.content,
        })),
      ];

      const result = await sendChatMessage(input, context);
      const aiReply: InterviewMessage = {
        role: "ai",
        content: result.reply || "请继续回答，或者告诉我你想换一个话题。",
        time: new Date().toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" }),
      };
      setMessages((prev) => [...prev, aiReply]);
    } catch (e) {
      const errMsg = toErrorMessage(e, "对话失败");
      setError(errMsg);
      setMessages((prev) => [
        ...prev,
        {
          role: "ai",
          content: `抱歉，出了点问题：${errMsg}`,
          time: new Date().toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" }),
        },
      ]);
    } finally {
      setSending(false);
    }
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
          description="选择岗位和难度，AI 面试官将根据你的回答实时追问，模拟真实面试场景。"
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

          {error && (
            <p className="text-sm text-[var(--red)] mb-4">{error}</p>
          )}

          {activeSession && (
            <button type="button" className="btn btn-ghost wide" onClick={resumeLastSession}>
              <History size={16} />
              恢复上次会话 ({activeSession.position} · L{activeSession.difficulty})
            </button>
          )}
          <button
            type="button"
            className="btn btn-accent wide"
            onClick={startInterview}
            disabled={starting}
          >
            {starting ? <Loader2 size={16} className="animate-spin" /> : <MessageSquare size={16} />}
            {starting ? "创建会话中..." : "开始面试"}
            {!starting && <ArrowRight size={14} />}
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
            <h2 className="text-3xl font-bold text-[var(--ink)]">
              {messages.length > 2 ? "面试已完成" : "面试已结束"}
            </h2>
            <p className="mt-1 text-sm text-[var(--muted)]">
              共 {messages.filter((m) => m.role === "user").length} 轮对话
            </p>
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
                回答时注意结构化表达，先总后分
              </li>
              <li className="flex items-start gap-2">
                <span className="mt-1 h-1.5 w-1.5 rounded-full bg-[var(--blue)]" />
                对核心概念的理解可以更深入
              </li>
            </ul>
          </div>

          <div className="mt-6 flex justify-center gap-3">
            <button type="button" className="btn btn-accent" onClick={() => { setStarted(false); setFinished(false); }}>
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
        description={`${difficulties[difficulty].label} · 已进行 ${messages.filter((m) => m.role === "user").length} 轮对话`}
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
              {messages.filter((m) => m.role === "user").length} 轮
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
          {sending && (
            <div className="chat-bubble ai">
              <div className="bubble-head">
                <Bot size={14} />
                AI 面试官
              </div>
              <div className="flex items-center gap-2">
                <Loader2 size={14} className="animate-spin" />
                <span className="text-sm text-[var(--muted)]">思考追问中...</span>
              </div>
            </div>
          )}
        </div>

        <div className="chat-input-row">
          <input
            type="text"
            className="chat-input"
            placeholder="输入你的回答..."
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && sendMessage()}
            disabled={sending}
          />
          <button
            type="button"
            className="btn btn-accent icon-btn"
            onClick={sendMessage}
            disabled={!input.trim() || sending}
          >
            {sending ? <Loader2 size={16} className="animate-spin" /> : <Send size={16} />}
          </button>
        </div>
      </div>
    </div>
  );
}
