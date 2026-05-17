"use client";

import { motion } from "framer-motion";
import { Activity, BrainCircuit, Database, Gauge, MessageSquare, Radar, Sparkles, Zap } from "lucide-react";

const orbitItems = [
  { label: "题库", value: "5k+", icon: Database },
  { label: "评分", value: "6维", icon: Gauge },
  { label: "问答", value: "RAG", icon: MessageSquare },
  { label: "面试", value: "AI", icon: BrainCircuit },
];

const streams = ["Java 并发", "Spring AI", "RAG 检索", "系统设计", "JVM 调优"];

export function HeroCommandDeck() {
  return (
    <motion.aside
      className="command-deck"
      initial={{ opacity: 0, x: 28, scale: 0.98 }}
      animate={{ opacity: 1, x: 0, scale: 1 }}
      transition={{ duration: 0.65, ease: [0.22, 1, 0.36, 1] }}
      aria-label="AI 面试训练动态控制舱"
    >
      <div className="deck-grid" aria-hidden="true" />
      <div className="deck-scan" aria-hidden="true" />

      <div className="deck-header">
        <span className="deck-status">
          <Activity size={15} />
          AI Coach Online
        </span>
        <span className="deck-chip">
          <Sparkles size={14} />
          Live
        </span>
      </div>

      <div className="deck-core">
        <motion.div
          className="radar-ring outer"
          animate={{ rotate: 360 }}
          transition={{ duration: 18, repeat: Infinity, ease: "linear" }}
        />
        <motion.div
          className="radar-ring middle"
          animate={{ rotate: -360 }}
          transition={{ duration: 14, repeat: Infinity, ease: "linear" }}
        />
        <div className="radar-pulse" />
        <div className="radar-center">
          <Radar size={28} />
          <strong>82</strong>
          <span>ready</span>
        </div>
        {orbitItems.map((item, index) => {
          const Icon = item.icon;
          return (
            <motion.div
              className={`orbit-card orbit-card-${index + 1}`}
              key={item.label}
              initial={{ opacity: 0, scale: 0.8 }}
              animate={{ opacity: [0.82, 1, 0.82], scale: 1, y: [0, -6, 0] }}
              whileHover={{ scale: 1.05 }}
              transition={{ delay: 0.35 + index * 0.08, duration: 0.35 }}
            >
              <Icon size={15} />
              <span>{item.label}</span>
              <strong>{item.value}</strong>
            </motion.div>
          );
        })}
      </div>

      <div className="deck-stream">
        {streams.map((item, index) => (
          <motion.div
            className="stream-item"
            key={item}
            animate={{ x: ["0%", "-18%", "0%"], opacity: [0.62, 1, 0.62] }}
            transition={{ duration: 4 + index * 0.35, repeat: Infinity, ease: "easeInOut" }}
          >
            <Zap size={13} />
            <span>{item}</span>
          </motion.div>
        ))}
      </div>
    </motion.aside>
  );
}
