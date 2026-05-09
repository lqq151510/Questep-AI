"use client";

import { useState } from "react";
import { motion } from "framer-motion";
import {
  Lightbulb,
  ChevronDown,
  ChevronUp,
  Share2,
  Bookmark,
} from "lucide-react";
import { PageHero } from "@/components/new-ui/PageHero";

const categories = [
  { id: "all", label: "全部", icon: Lightbulb },
  { id: "java", label: "Java", icon: Lightbulb },
  { id: "frontend", label: "前端", icon: Lightbulb },
  { id: "algorithm", label: "算法", icon: Lightbulb },
  { id: "behavior", label: "行为面试", icon: Lightbulb },
  { id: "resume", label: "简历技巧", icon: Lightbulb },
];

const tips = [
  {
    id: 1,
    category: "java",
    title: "Java 面试必考点：HashMap 源码解析",
    content:
      "HashMap 是 Java 面试中的高频考点。JDK 1.8 之前使用数组+链表，1.8 之后引入了红黑树优化。当链表长度超过 8 且数组长度超过 64 时，链表会转换为红黑树。\n\n关键参数：\n- 默认初始容量：16\n- 默认负载因子：0.75\n- 扩容阈值：容量 × 负载因子\n\n面试建议：不仅要说出数据结构，还要能解释为什么这样设计，以及线程安全问题。",
    read: false,
    bookmarked: false,
  },
  {
    id: 2,
    category: "behavior",
    title: "行为面试：STAR 法则的正确使用",
    content:
      "STAR 法则是回答行为面试问题的黄金框架：\n\nS - Situation（情境）：描述当时的情境\nT - Task（任务）：你需要完成的任务\nA - Action（行动）：你采取了哪些行动\nR - Result（结果）：最终取得了什么结果\n\n常见题目：\n- 描述一次你解决冲突的经历\n- 举例说明你如何带领团队完成目标\n- 谈谈你失败的一次经历以及学到的教训",
    read: true,
    bookmarked: true,
  },
  {
    id: 3,
    category: "algorithm",
    title: "算法面试：如何优雅地处理边界条件",
    content:
      "算法面试中，边界条件的处理往往是区分优秀和普通候选人的关键。\n\n常见边界：\n- 空输入 / null\n- 单元素数组\n- 极大/极小值\n- 重复元素\n\n建议：在写代码前先列出所有可能的边界情况，与面试官确认后再开始编码。",
    read: false,
    bookmarked: false,
  },
  {
    id: 4,
    category: "resume",
    title: "技术简历：如何让 HR 一眼看中你",
    content:
      "技术简历的核心原则：\n\n1. 量化成果：使用数字说话\n   - 优化后 QPS 提升 300%\n   - 将响应时间从 2s 降至 200ms\n\n2. 突出技术栈：明确列出使用的技术\n\n3. 项目描述采用 PAR 结构：\n   - Problem（问题）\n   - Action（行动）\n   - Result（结果）\n\n4. 控制篇幅：1-2 页为佳，重点突出最近 2-3 个项目",
    read: false,
    bookmarked: false,
  },
];

export default function InterviewTipsPage() {
  const [activeCategory, setActiveCategory] = useState("all");
  const [expanded, setExpanded] = useState<number | null>(null);
  const [tipsList, setTipsList] = useState(tips);

  const filtered =
    activeCategory === "all"
      ? tipsList
      : tipsList.filter((t) => t.category === activeCategory);

  const toggleExpand = (id: number) => {
    setExpanded((prev) => (prev === id ? null : id));
    setTipsList((prev) =>
      prev.map((t) => (t.id === id ? { ...t, read: true } : t))
    );
  };

  const toggleBookmark = (id: number) => {
    setTipsList((prev) =>
      prev.map((t) => (t.id === id ? { ...t, bookmarked: !t.bookmarked } : t))
    );
  };

  return (
    <div>
      <PageHero
        kicker="面试技巧"
        title="经验分享"
        description="精选面试攻略和技巧文章，助你掌握面试中的加分细节。"
      />

      {/* Categories */}
      <div className="flex gap-2 overflow-x-auto pb-2">
        {categories.map((cat) => {
          const Icon = cat.icon;
          return (
            <button
              key={cat.id}
              type="button"
              className={`chip flex-shrink-0 ${activeCategory === cat.id ? "active" : ""}`}
              onClick={() => setActiveCategory(cat.id)}
            >
              <Icon size={14} />
              {cat.label}
            </button>
          );
        })}
      </div>

      {/* Tips List */}
      <div className="tips-grid mt-5">
        {filtered.map((tip, i) => {
          const isExpanded = expanded === tip.id;
          return (
            <motion.div
              key={tip.id}
              className="tip-card"
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.06, duration: 0.3 }}
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  {!tip.read && (
                    <span className="h-2 w-2 rounded-full bg-[var(--blue)]" />
                  )}
                  <span className="badge" style={{ color: "var(--blue)", background: "var(--blue-soft)" }}>
                    {categories.find((c) => c.id === tip.category)?.label}
                  </span>
                </div>
                <div className="flex items-center gap-1">
                  <button
                    type="button"
                    className="btn btn-ghost icon-btn"
                    onClick={() => toggleBookmark(tip.id)}
                  >
                    <Bookmark
                      size={14}
                      className={tip.bookmarked ? "fill-[var(--blue)] text-[var(--blue)]" : ""}
                    />
                  </button>
                  <button type="button" className="btn btn-ghost icon-btn">
                    <Share2 size={14} />
                  </button>
                </div>
              </div>

              <h3>{tip.title}</h3>

              <div
                className={`overflow-hidden transition-all ${isExpanded ? "max-h-[500px]" : "max-h-0"}`}
              >
                <p className="whitespace-pre-wrap">{tip.content}</p>
              </div>

              <button
                type="button"
                className="mt-3 flex items-center gap-1 text-sm font-medium text-[var(--blue)]"
                onClick={() => toggleExpand(tip.id)}
              >
                {isExpanded ? (
                  <>
                    <ChevronUp size={14} />
                    收起
                  </>
                ) : (
                  <>
                    <ChevronDown size={14} />
                    展开阅读
                  </>
                )}
              </button>
            </motion.div>
          );
        })}
      </div>
    </div>
  );
}
