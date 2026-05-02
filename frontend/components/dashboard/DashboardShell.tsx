"use client";

import { useEffect, useMemo } from "react";

import { InsightPanel } from "@/components/dashboard/InsightPanel";
import { MaterialPanel } from "@/components/dashboard/MaterialPanel";
import { OverviewGrid } from "@/components/dashboard/OverviewGrid";
import { QuestionComposer } from "@/components/dashboard/QuestionComposer";
import { TaskPanel } from "@/components/dashboard/TaskPanel";
import { ParticleField } from "@/components/layout/ParticleField";
import { Sidebar } from "@/components/layout/Sidebar";
import { TopBar } from "@/components/layout/TopBar";
import { useDashboardStore } from "@/stores/useDashboardStore";

export function DashboardShell() {
  const materials = useDashboardStore((state) => state.materials);
  const tasks = useDashboardStore((state) => state.tasks);
  const selectedMaterialIds = useDashboardStore((state) => state.selectedMaterialIds);
  const tickProgress = useDashboardStore((state) => state.tickProgress);

  useEffect(() => {
    const timer = window.setInterval(tickProgress, 1600);
    return () => window.clearInterval(timer);
  }, [tickProgress]);

  const metrics = useMemo(() => {
    const readyCount = materials.filter((item) => item.status === "ready").length;
    const runningCount = tasks.filter((item) => item.status === "running").length;
    const avgScore =
      materials.length > 0 ? Math.round(materials.reduce((sum, item) => sum + item.score, 0) / materials.length) : 0;

    return { readyCount, runningCount, avgScore };
  }, [materials, tasks]);

  return (
    <main className="app-shell">
      <ParticleField />
      <Sidebar />

      <section className="workspace">
        <TopBar runningCount={metrics.runningCount} selectedCount={selectedMaterialIds.length} />
        <OverviewGrid
          avgScore={metrics.avgScore}
          materialCount={materials.length}
          readyCount={metrics.readyCount}
          runningCount={metrics.runningCount}
        />

        <section className="content-grid">
          <section className="primary-column">
            <MaterialPanel />
            <QuestionComposer />
          </section>

          <aside className="right-column">
            <TaskPanel />
            <InsightPanel />
          </aside>
        </section>
      </section>
    </main>
  );
}
