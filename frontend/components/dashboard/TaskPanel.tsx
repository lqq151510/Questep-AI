"use client";

import { ChevronRight, X } from "lucide-react";

import { Progress } from "@/components/ui/progress";
import { taskStatusText } from "@/lib/dashboard-format";
import { useDashboardStore } from "@/stores/useDashboardStore";

export function TaskPanel() {
  const tasks = useDashboardStore((state) => state.tasks);

  return (
    <div className="panel task-panel">
      <div className="panel-header compact">
        <div>
          <h2>队列</h2>
          <p>解析、向量化与报告。</p>
        </div>
      </div>
      <div className="task-list">
        {tasks.map((task) => (
          <article className="task-row" key={task.id}>
            <div className={`task-icon ${task.status}`}>
              {task.status === "failed" ? <X size={16} /> : <ChevronRight size={16} />}
            </div>
            <div className="task-body">
              <div className="row-title">
                <strong>{task.title}</strong>
                <span>{taskStatusText(task.status)}</span>
              </div>
              <p>{task.materialName}</p>
              <Progress size="small" value={task.progress} />
              <div className="task-meta">
                <span>{task.traceId}</span>
                <span>{task.duration}</span>
              </div>
            </div>
          </article>
        ))}
      </div>
    </div>
  );
}
