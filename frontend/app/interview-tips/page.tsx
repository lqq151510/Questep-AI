"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { Lightbulb, RefreshCw, Sparkles } from "lucide-react";
import { PageHero } from "@/components/new-ui/PageHero";
import { useToast } from "@/components/new-ui/ToastProvider";
import { listMaterials, listQuestions, toErrorMessage } from "@/lib/interview-api";

type Tip = {
  id: string;
  category: "技术准备" | "表达技巧" | "场景应对" | "心态调节";
  title: string;
  content: string;
};

const categories = ["全部", "技术准备", "表达技巧", "场景应对", "心态调节"] as const;

function buildTips(materialCount: number, parsedCount: number, questionCount: number): Tip[] {
  return [
    {
      id: "t1",
      category: "技术准备",
      title: "先补齐资料覆盖，再追求题量",
      content: `当前资料 ${materialCount} 份、已解析 ${parsedCount} 份。建议优先把核心项目文档解析完成后再扩题。`
    },
    {
      id: "t2",
      category: "表达技巧",
      title: "把题库答案改造成 STAR 表达",
      content: `你当前题库已有 ${questionCount} 道题。每题用「背景-行动-结果」补充一个真实项目案例，回答会更有说服力。`
    },
    {
      id: "t3",
      category: "场景应对",
      title: "追问时优先谈取舍和边界",
      content: "当被追问时，先给结论，再说明为什么不用其他方案，最后补一条风险与降级路径。"
    },
    {
      id: "t4",
      category: "心态调节",
      title: "建立 5 分钟热身清单",
      content: "面试前快速过一遍：最近项目亮点、两条高频故障复盘、一个性能优化案例。"
    }
  ];
}

export default function InterviewTipsPage() {
  const { showToast } = useToast();
  const [activeCategory, setActiveCategory] = useState<(typeof categories)[number]>("全部");
  const [tips, setTips] = useState<Tip[]>([]);
  const [loading, setLoading] = useState(false);

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      const [materials, questions] = await Promise.all([listMaterials(), listQuestions(100)]);
      const parsedCount = materials.filter((item) => String(item.parseStatus ?? "").toUpperCase() === "SUCCESS").length;
      setTips(buildTips(materials.length, parsedCount, questions.length));
    } catch (error) {
      showToast(toErrorMessage(error, "加载建议失败"));
      setTips(buildTips(0, 0, 0));
    } finally {
      setLoading(false);
    }
  }, [showToast]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const visibleTips = useMemo(() => {
    if (activeCategory === "全部") {
      return tips;
    }
    return tips.filter((tip) => tip.category === activeCategory);
  }, [activeCategory, tips]);

  return (
    <div className="container">
      <PageHero
        kicker="Interview Tips"
        title="面试技巧库"
        description="基于你当前资料与题库的真实状态，动态生成可执行建议。"
      />

      <section className="panel">
        <div className="row-actions">
          <button type="button" className="btn" onClick={() => void refresh()} disabled={loading}>
            <RefreshCw size={14} />
            {loading ? "刷新中" : "刷新建议"}
          </button>
        </div>

        <div className="chip-row">
          {categories.map((category) => (
            <button
              key={category}
              type="button"
              className={activeCategory === category ? "chip active" : "chip"}
              onClick={() => {
                setActiveCategory(category);
                showToast(`已切换到「${category}」`);
              }}
            >
              {category}
            </button>
          ))}
        </div>

        <div className="tips-grid">
          {visibleTips.map((tip) => (
            <article key={tip.id} className="tip-card">
              <p className="list-meta">
                <Sparkles size={14} /> {tip.category}
              </p>
              <h3>{tip.title}</h3>
              <p>{tip.content}</p>
              <button type="button" className="btn" onClick={() => showToast("已加入今日练习计划")}>
                <Lightbulb size={14} />
                加入今日计划
              </button>
            </article>
          ))}
          {visibleTips.length === 0 && (
            <article className="tip-card">
              <h3>暂无建议</h3>
              <p>请先上传资料并生成题目，系统会自动给出针对性建议。</p>
            </article>
          )}
        </div>
      </section>
    </div>
  );
}
