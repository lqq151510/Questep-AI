"use client";

import { ChangeEvent, useMemo, useRef } from "react";
import { AlertTriangle, CircleCheckBig, FileText, ListFilter, RefreshCcw, UploadCloud } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { statusText } from "@/lib/dashboard-format";
import { useDashboardStore } from "@/stores/useDashboardStore";
import type { MaterialFilter } from "@/types/dashboard";

const filterOptions: Array<[MaterialFilter, string]> = [
  ["all", "全部"],
  ["ready", "已解析"],
  ["parsing", "解析中"],
  ["failed", "失败"]
];

export function MaterialPanel() {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const materials = useDashboardStore((state) => state.materials);
  const selectedMaterialIds = useDashboardStore((state) => state.selectedMaterialIds);
  const materialFilter = useDashboardStore((state) => state.materialFilter);
  const apiState = useDashboardStore((state) => state.apiState);
  const setMaterialFilter = useDashboardStore((state) => state.setMaterialFilter);
  const toggleMaterial = useDashboardStore((state) => state.toggleMaterial);
  const refreshMaterials = useDashboardStore((state) => state.refreshMaterials);
  const uploadMaterial = useDashboardStore((state) => state.uploadMaterial);

  const filteredMaterials = useMemo(() => {
    if (materialFilter === "all") return materials;
    return materials.filter((item) => item.status === materialFilter);
  }, [materialFilter, materials]);

  function handleUpload(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (file) void uploadMaterial(file);
    event.target.value = "";
  }

  return (
    <div className="panel material-panel">
      <div className="panel-header">
        <div>
          <h2>资料处理</h2>
          <p>解析队列与题单来源。</p>
        </div>
        <div className="header-actions">
          <Button aria-busy={apiState === "syncing"} disabled={apiState === "syncing"} onClick={() => void refreshMaterials()}>
            <RefreshCcw size={16} />
            {apiState === "syncing" ? "同步中" : "刷新"}
          </Button>
          <Button onClick={() => fileInputRef.current?.click()} variant="primary">
            <UploadCloud size={17} />
            导入
          </Button>
          <input ref={fileInputRef} className="visually-hidden" onChange={handleUpload} type="file" />
        </div>
      </div>

      <div className="filter-row" aria-label="资料筛选">
        {filterOptions.map(([value, label]) => (
          <Button
            key={value}
            onClick={() => setMaterialFilter(value)}
            selected={materialFilter === value}
            variant="filter"
          >
            <ListFilter size={14} />
            {label}
          </Button>
        ))}
        {apiState === "offline" && (
          <span className="sync-state offline">
            <AlertTriangle size={14} />
            后端未连接，已保留本地数据
          </span>
        )}
        {apiState === "online" && (
          <span className="sync-state online">
            <CircleCheckBig size={14} />
            已同步
          </span>
        )}
      </div>

      <div className="material-list">
        {filteredMaterials.map((item) => (
          <article className="material-row" key={item.id}>
            <label className="check-cell">
              <input
                checked={selectedMaterialIds.includes(item.id)}
                onChange={() => toggleMaterial(item.id)}
                type="checkbox"
              />
            </label>
            <div className="file-token">
              <FileText size={18} />
            </div>
            <div className="material-main">
              <div className="row-title">
                <strong>{item.name}</strong>
                <span>{item.type}</span>
              </div>
              <Progress label={`${item.name} 解析进度`} value={item.progress} />
            </div>
            <div className="material-meta">
              <span className={`status-dot ${item.status}`}>{statusText(item.status)}</span>
              <span>{item.chunks} chunks</span>
              <span>{item.score}%</span>
              <span>{item.updatedAt}</span>
            </div>
          </article>
        ))}
      </div>
    </div>
  );
}
