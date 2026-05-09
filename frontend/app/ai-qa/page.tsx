"use client";

import { useState } from "react";
import { motion } from "framer-motion";
import {
  Send,
  Bot,
  User,
  Lightbulb,
} from "lucide-react";
import { PageHero } from "@/components/new-ui/PageHero";

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

const mockMessages: Message[] = [
  {
    role: "ai",
    content: "你好！我是你的 AI 面试助手。有任何技术问题都可以问我，我会为你提供详细的解答和面试建议。",
  },
];

export default function AIQAPage() {
  const [messages, setMessages] = useState<Message[]>(mockMessages);
  const [input, setInput] = useState("");

  const sendMessage = (text: string) => {
    if (!text.trim()) return;
    const userMsg = { role: "user" as const, content: text };
    setMessages((prev) => [...prev, userMsg]);
    setInput("");

    setTimeout(() => {
      const aiReply = {
        role: "ai" as const,
        content: `这是一个很好的问题！关于「${text}」，我来为你详细解答：\n\n1. **核心概念**：首先需要理解其基本原理...\n2. **实际应用**：在实际项目中，通常会...\n3. **面试要点**：回答这个问题时，建议从以下几个方面展开...\n\n希望这个解答对你有帮助！如果还有疑问，可以继续提问。`,
      };
      setMessages((prev) => [...prev, aiReply]);
    }, 1500);
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
        </div>

        {/* Quick Questions */}
        {messages.length <= 2 && (
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
          />
          <button
            type="button"
            className="btn btn-accent icon-btn"
            onClick={() => sendMessage(input)}
            disabled={!input.trim()}
          >
            <Send size={16} />
          </button>
        </div>
      </div>
    </div>
  );
}
