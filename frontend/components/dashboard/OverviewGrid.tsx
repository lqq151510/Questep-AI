import { FileText, MessageSquareText, Sparkles, TimerReset } from "lucide-react";

import { StatTile } from "@/components/dashboard/StatTile";

type OverviewGridProps = {
  readyCount: number;
  materialCount: number;
  runningCount: number;
  avgScore: number;
};

export function OverviewGrid({ readyCount, materialCount, runningCount, avgScore }: OverviewGridProps) {
  return (
    <section className="overview-grid" aria-label="数据概览">
      <StatTile icon={FileText} label="资料" value={`${readyCount}/${materialCount}`} note="可出题" />
      <StatTile icon={TimerReset} label="队列" value={`${runningCount}`} note="运行中" />
      <StatTile icon={Sparkles} label="质量" value={`${avgScore}%`} note="题目均分" />
      <StatTile icon={MessageSquareText} label="模拟" value="12" note="本周" />
    </section>
  );
}
