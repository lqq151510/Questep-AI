import { BrainCircuit, Settings2, ShieldCheck } from "lucide-react";

import { Button } from "@/components/ui/button";
import { navItems } from "@/lib/dashboard-data";

export function Sidebar() {
  return (
    <aside className="sidebar" aria-label="主导航">
      <div className="brand">
        <div className="brand-mark">
          <BrainCircuit size={22} />
        </div>
        <div>
          <strong>InterviewLab</strong>
          <span>后端面试训练</span>
        </div>
      </div>

      <nav className="nav-list">
        {navItems.map((item) => {
          const Icon = item.icon;
          return (
            <button className={item.active ? "nav-item active" : "nav-item"} key={item.label} type="button">
              <Icon size={18} />
              <span>{item.label}</span>
            </button>
          );
        })}
      </nav>

      <div className="sidebar-footer">
        <div className="health-card">
          <ShieldCheck size={18} />
          <div>
            <strong>LLM Gateway</strong>
            <span>路由与审计可用</span>
          </div>
        </div>
        <Button variant="ghost">
          <Settings2 size={17} />
          系统设置
        </Button>
      </div>
    </aside>
  );
}
