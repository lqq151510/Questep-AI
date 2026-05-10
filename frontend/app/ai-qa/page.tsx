"use client";

import { useState } from "react";
import { motion } from "framer-motion";
import {
  Send,
  Bot,
  User,
  Lightbulb,
  Loader2,
} from "lucide-react";
import { PageHero } from "@/components/new-ui/PageHero";
import {
  sendChatMessage,
  type ChatMessagePayload,
  toErrorMessage,
} from "@/lib/interview-api";

const quickQuestions = [
  "Java 中 volatile 关键字的作用是什么？",
  "Redis 缓存穿透、击穿、雪崩的区别和解决方案？",
  "Spring Boot 的自动配置原理是什么？",
  "如何设计一个高并发的秒杀系统？",
  "MySQL 索引优化有哪些技巧？",
];

type Message = {
  role: "ai" | "user";
  content: string;
};

export default function AIQAPage() {
  const [messages, setMessages] = useState<Message[]>([
    {
      role: "ai",
      content: "你好！我是你的 AI 面试助手。有任何技术问题都可以问我，我会为你提供详细的解答和面试建议。",
    },
  ]);
  const [input, setInput] = useState("");
  const [sending, setSending] = useState(false);

  const sendMessage = async (text: string) => {
    if (!text.trim() || sending) return;
    const userMsg = { role: "user" as const, content: text };
    setMessages((prev) => [...prev, userMsg]);
    setInput("");
    setSending(true);

    try {
      const context: ChatMessagePayload[] = messages.slice(-6).map((m) => ({
        role: m.role === "ai" ? "assistant" : "user",
        content: m.content,
      }));

      const result = await sendChatMessage(text, context);
      const aiReply = {
        role: "ai" as const,
        content: result.reply || "抱歉，AI 暂时无法回答，请稍后重试。",
      };
      setMessages((prev) => [...prev, aiReply]);
    } catch (e) {
      const errMsg = toErrorMessage(e, "对话失败，请稍后重试");
      setMessages((prev) => [
        ...prev,
        { role: "ai", content: `抱歉，出了点问题：${errMsg}` },
      ]);
    } finally {
      setSending(false);
    }
  };

  return (
    <div>
      <PageHero
        kicker="AI 问答"
        title="智能问答"
        description="有任何技术问题都可以向 AI 提问，获取详细的解答和面试建议。"
      />

      <div className="panel chat-panel">
        <div className="chat-head">
          <div>
            <p className="chat-title flex items-center gap-2">
              <Bot size={16} className="text-[var(--blue)]" />
              AI 面试助手
            </p>
            <p className="chat-subtitle">随时解答你的技术疑问</p>
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
                {msg.role === "ai" ? "AI 助手" : "你"}
              </div>
              <p className="whitespace-pre-wrap">{msg.content}</p>
            </motion.div>
          ))}
          {sending && (
            <div className="chat-bubble ai">
              <div className="bubble-head">
                <Bot size={14} />
                AI 助手
              </div>
              <div className="flex items-center gap-2">
                <Loader2 size={14} className="animate-spin" />
                <span className="text-sm text-[var(--muted)]">思考中...</span>
              </div>
            </div>
          )}
        </div>

        {messages.length <= 2 && !sending && (
          <div className="mb-4">
            <p className="mb-2 flex items-center gap-1.5 text-xs font-medium text-[var(--muted)]">
              <Lightbulb size={12} />
              快捷问题
            </p>
            <div className="flex flex-wrap gap-2">
              {quickQuestions.map((q) => (
                <button
                  key={q}
                  type="button"
                  className="tag hover:border-[var(--blue)] hover:text-[var(--blue)]"
                  onClick={() => sendMessage(q)}
                  disabled={sending}
                >
                  {q}
                </button>
              ))}
            </div>
          </div>
        )}

        <div className="chat-input-row">
          <input
            type="text"
            className="chat-input"
            placeholder="输入你的问题..."
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && sendMessage(input)}
            disabled={sending}
          />
          <button
            type="button"
            className="btn btn-accent icon-btn"
            onClick={() => sendMessage(input)}
            disabled={!input.trim() || sending}
          >
            {sending ? <Loader2 size={16} className="animate-spin" /> : <Send size={16} />}
          </button>
        </div>
      </div>
    </div>
  );
}
