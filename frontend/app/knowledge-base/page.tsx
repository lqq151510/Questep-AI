"use client";

import { useEffect, useState, useRef } from "react";
import { motion } from "framer-motion";
import {
  BookOpen,
  Upload,
  FileText,
  Loader2,
  CheckCircle,
  Search,
  RefreshCw,
  AlertTriangle,
} from "lucide-react";
import { PageHero } from "@/components/new-ui/PageHero";
import { EmptyState } from "@/components/ui/EmptyState";
import { useDashboardStore } from "@/stores/useDashboardStore";
import { Skeleton } from "@/components/ui/Skeleton";

export default function KnowledgeBasePage() {
  const {
    materials,
    apiState,
    uploadMaterial,
    refreshMaterials,
  } = useDashboardStore();

  const [search, setSearch] = useState("");
  const [dragOver, setDragOver] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    refreshMaterials();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const filtered = materials.filter((m) =>
    m.name.toLowerCase().includes(search.toLowerCase())
  );

  const handleUpload = () => {
    fileInputRef.current?.click();
  };

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files?.length) return;
    await uploadMaterial(files[0]);
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  const statusLabel = (status: string) => {
    if (status === "ready") return "已解析";
    if (status === "parsing") return "解析中";
    return "失败";
  };

  return (
    <div>
      <PageHero
        kicker="知识库"
        title="资料管理"
        description="上传学习资料，AI 自动解析并生成结构化知识，随时检索复习。"
      />

      {/* Upload Area */}
      <div
        className={`upload-area ${dragOver ? "border-[var(--blue)] bg-[var(--blue-soft)]" : ""}`}
        onDragOver={(e) => {
          e.preventDefault();
          setDragOver(true);
        }}
        onDragLeave={() => setDragOver(false)}
        onDrop={(e) => {
          e.preventDefault();
          setDragOver(false);
        }}
        onClick={handleUpload}
      >
        <Upload size={28} className="text-[var(--blue)]" />
        <span>点击或拖拽上传资料</span>
        <small>支持 PDF、TXT、Markdown 格式</small>
        <input
          ref={fileInputRef}
          type="file"
          accept=".pdf,.txt,.md"
          className="hidden"
          onChange={handleFileChange}
        />
      </div>

      {/* Search */}
      <div className="search-row">
        <h2>我的资料</h2>
        <div className="search-input-wrap">
          <Search size={14} />
          <input
            type="text"
            placeholder="搜索资料..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
      </div>

      {/* Loading */}
      {apiState === "syncing" && filtered.length === 0 && (
        <div className="material-grid">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="material-card">
              <Skeleton className="h-5 w-5 mb-3" />
              <Skeleton className="h-5 w-3/4 mb-2" />
              <Skeleton className="h-4 w-1/2" />
            </div>
          ))}
        </div>
      )}

      {/* Materials Grid */}
      {apiState !== "syncing" && filtered.length === 0 && (
        <EmptyState
          icon={BookOpen}
          title="暂无资料"
          description="上传你的学习资料，AI 将自动解析并生成知识库"
          action={{ label: "上传资料", onClick: handleUpload }}
        />
      )}

      {filtered.length > 0 && (
        <div className="material-grid">
          {filtered.map((m, i) => (
            <motion.div
              key={m.id}
              className="material-card"
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.06, duration: 0.4 }}
            >
              <div className="material-head">
                <FileText size={20} className="text-[var(--blue)]" />
                <button
                  type="button"
                  className="btn btn-ghost icon-btn"
                  onClick={() => refreshMaterials()}
                  title="刷新"
                >
                  <RefreshCw size={14} />
                </button>
              </div>
              <h3 className="truncate">{m.name}</h3>
              <p className="meta-text">
                {m.type} · {m.updatedAt}
              </p>
              <div className="tag-row">
                <span className={`badge ${m.status === "ready" ? "success" : m.status === "failed" ? "danger" : "warning"}`}>
                  {m.status === "ready" ? (
                    <CheckCircle size={12} />
                  ) : m.status === "failed" ? (
                    <AlertTriangle size={12} />
                  ) : (
                    <Loader2 size={12} className="animate-spin" />
                  )}
                  {statusLabel(m.status)}
                </span>
              </div>
            </motion.div>
          ))}
        </div>
      )}
    </div>
  );
}
