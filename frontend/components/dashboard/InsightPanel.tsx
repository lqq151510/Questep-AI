import { AlertTriangle } from "lucide-react";

import { Progress } from "@/components/ui/progress";
import { knowledge, weakPoints } from "@/lib/dashboard-data";

export function InsightPanel() {
  return (
    <div className="panel insight-panel">
      <div className="panel-header compact">
        <div>
          <h2>短板</h2>
          <p>按知识点聚合本周表现。</p>
        </div>
      </div>

      <div className="knowledge-bars">
        {knowledge.map((item) => (
          <div className="knowledge-row" key={item.label}>
            <div>
              <span>{item.label}</span>
              <strong>{item.value}%</strong>
            </div>
            <Progress tone={item.tone} value={item.value} />
          </div>
        ))}
      </div>

      <div className="weak-list">
        {weakPoints.map((item) => (
          <div className="weak-item" key={item}>
            <AlertTriangle size={15} />
            <span>{item}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
