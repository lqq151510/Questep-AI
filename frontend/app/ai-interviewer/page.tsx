"use client";

import { useEffect, useRef, useState } from "react";
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
  buildInterviewWebSocketUrl,
  createOrGetSession,
  resumeSession,
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
  const wsRef = useRef<WebSocket | null>(null);
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
  const [wsConnected, setWsConnected] = useState(false);

  useEffect(() => {
    createOrGetSession(position, difficulty + 1).then(setActiveSession).catch(() => {});
  }, [position, difficulty]);

  const resumeLastSession = async () => {
    if (!activeSession) return;
    try {
      await resumeSession(activeSession.id);
      setPosition(activeSession.position);
      setDifficulty(activeSession.difficulty - 1);
      await startInterview();
    } catch {
      // fall through to normal start
    }
  };

  const closeInterviewSocket = () => {
    const ws = wsRef.current;
    if (ws) {
      ws.close();
      wsRef.current = null;
    }
    setWsConnected(false);
  };

  const connectInterviewSocket = () => {
    closeInterviewSocket();
    const wsUrl = buildInterviewWebSocketUrl();
    if (!wsUrl) {
      setError("无法建立实时连接，请先登录后重试。");
      return;
    }

    const ws = new WebSocket(wsUrl);
    wsRef.current = ws;

    ws.onopen = () => {
      setWsConnected(true);
      setError("");
    };

    ws.onclose = () => {
      setWsConnected(false);
    };

    ws.onerror = () => {
      setWsConnected(false);
      setError("实时连接失败，请检查后端 WebSocket 服务。");
    };

    ws.onmessage = (event) => {
      try {
        const payload = JSON.parse(event.data) as {
          type?: string;
          message?: string;
          data?: { reply?: string; token?: string; code?: string; fullReply?: string };
        };

        if (payload.type === "chat_token") {
          const token = payload.data?.token || "";
          const now = new Date().toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" });
          setMessages((prev) => {
            const copy = [...prev];
            const last = copy[copy.length - 1];
            if (last && last.role === "ai") {
              copy[copy.length - 1] = { ...last, content: last.content + token };
            } else {
              copy.push({ role: "ai", content: token, time: now });
            }
            return copy;
          });
          if (!sending) setSending(true);
          return;
        }

        if (payload.type === "chat_done") {
          setSending(false);
          return;
        }

        if (payload.type === "chat_reply") {
          const replyText = payload.data?.reply || "请继续回答，或者告诉我你想换一个话题。";
          setMessages((prev) => [
            ...prev,
            {
              role: "ai",
              content: replyText,
              time: new Date().toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" }),
            },
          ]);
          setSending(false);
          return;
        }

        if (payload.type === "chat_error" || payload.type === "error") {
          const errText = payload.message || "对话失败";
          setError(errText);
          setSending(false);
        }
      } catch {
        setError("实时消息解析失败");
        setSending(false);
      }
    };
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
      connectInterviewSocket();
    } catch (e) {
      setError(toErrorMessage(e, "创建面试会话失败"));
    } finally {
      setStarting(false);
    }
  };

  const sendMessage = async () => {
    if (!input.trim() || sending) return;
    const ws = wsRef.current;
    if (!ws || ws.readyState !== WebSocket.OPEN) {
      setError("实时连接未建立，请重新开始面试。");
      return;
    }

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
      ws.send(
        JSON.stringify({
          message: input,
          position,
          difficulty: difficulty + 1,
          context: messages.slice(-8).map((m) => ({
            role: m.role === "ai" ? "assistant" : "user",
            content: m.content,
          })),
        })
      );
    } catch (e) {
      const errMsg = toErrorMessage(e, "对话失败");
      setError(errMsg);
      setSending(false);
      setMessages((prev) => [
        ...prev,
        {
          role: "ai",
          content: `抱歉，出了点问题：${errMsg}`,
          time: new Date().toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" }),
        },
      ]);
    }
  };

  const finishInterview = () => {
    closeInterviewSocket();
    setFinished(true);
  };

  useEffect(() => {
    return () => {
      closeInterviewSocket();
    };
  }, []);

  useEffect(() => {
    if (finished) {
      closeInterviewSocket();
    }
  }, [finished]);

  const resetInterview = () => {
    closeInterviewSocket();
    setStarted(false);
    setFinished(false);
    setSending(false);
    setMessages([]);
  };

  const chatRounds = messages.filter((m) => m.role === "user").length;

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

          {error && <p className="text-sm text-[var(--red)] mb-4">{error}</p>}

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
              共 {chatRounds} 轮对话
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
            <button type="button" className="btn btn-accent" onClick={resetInterview}>
              <RotateCcw size={14} />
              再来一次
            </button>
            <button type="button" className="btn btn-ghost" onClick={resetInterview}>
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
        description={`${difficulties[difficulty].label} · 已进行 ${chatRounds} 轮对话`}
      />

      <div className="panel chat-panel">
        <div className="chat-head">
          <div>
            <p className="chat-title flex items-center gap-2">
              <Bot size={16} className="text-[var(--blue)]" />
              AI 面试官
            </p>
            <p className="chat-subtitle">
              {position} · {difficulties[difficulty].label} · {wsConnected ? "实时已连接" : "实时连接中断"}
            </p>
          </div>
          <div className="flex items-center gap-3">
            <p className="chat-timer">
              <Clock size={14} />
              {chatRounds} 轮
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
            disabled={!input.trim() || sending || !wsConnected}
          >
            {sending ? <Loader2 size={16} className="animate-spin" /> : <Send size={16} />}
          </button>
        </div>
      </div>
    </div>
  );

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
