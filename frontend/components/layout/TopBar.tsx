import { Bell, PanelLeft, Search } from "lucide-react";

import { Button } from "@/components/ui/button";

type TopBarProps = {
  selectedCount: number;
  runningCount: number;
};

export function TopBar({ selectedCount, runningCount }: TopBarProps) {
  return (
    <header className="topbar">
      <div className="topbar-title">
        <Button aria-label="折叠侧边栏" variant="icon">
          <PanelLeft size={19} />
        </Button>
        <div>
          <h1>面试任务台</h1>
          <p>
            {selectedCount} 份资料已选，{runningCount} 个任务在跑。
          </p>
        </div>
      </div>
      <div className="topbar-actions">
        <label className="search-box">
          <Search size={17} />
          <input aria-label="搜索资料和任务" placeholder="搜索资料、任务、知识点" />
        </label>
        <Button aria-label="通知" variant="icon">
          <Bell size={18} />
        </Button>
        <div className="user-chip">demo_user</div>
      </div>
    </header>
  );
}
