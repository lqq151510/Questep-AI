"use client";

import { useState, useRef } from "react";
import { motion } from "framer-motion";
import {
  BookOpen,
  Upload,
  FileText,
  Trash2,
  Loader2,
  CheckCircle,
  Search,
} from "lucide-react";
import { PageHero } from "@/components/new-ui/PageHero";
import { EmptyState } from "@/components/ui/EmptyState";

const mockMaterials = [
  {
    id: 1,
    name: "Java 并发编程实战.pdf",
    type: "PDF",
    size: "2.4 MB",
    status: "已解析" as const,
    date: "2024-01-15",
  },
  {
    id: 2,
    name: "Redis 设计与实现.md",
    type: "Markdown",
    size: "156 KB",
    status: "已解析" as const,
    date: "2024-01-14",
  },
  {
    id: 3,
    name: "Spring Boot 源码分析.txt",
    type: "Text",
    size: "89 KB",
    status: "解析中" as const,
    date: "2024-01-13",
  },
];

export default function KnowledgeBasePage() {
  const [materials, setMaterials] = useState(mockMaterials);
  const [search, setSearch] = useState("");
  const [dragOver, setDragOver] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const filtered = materials.filter((m) =>
    m.name.toLowerCase().includes(search.toLowerCase())
  );

  const handleUpload = () => {
    fileInputRef.current?.click();
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files?.length) return;
    const file = files[0];
    const newMaterial = {
      id: Date.now(),
      name: file.name,
      type: file.name.split(".").pop()?.toUpperCase() || "File",
      size: (file.size / 1024).toFixed(0) + " KB",
      status: "解析中" as const,
      date: new Date().toISOString().split("T")[0],
    };
    setMaterials((prev) => [newMaterial, ...prev]);

    setTimeout(() => {
      setMaterials((prev) =>
        prev.map((m) => (m.id === newMaterial.id ? { ...m, status: "已解析" as const } : m))
      );
    }, 3000);
  };

  const deleteMaterial = (id: number) => {
    setMaterials((prev) => prev.filter((m) => m.id !== id));
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

      {/* Materials Grid */}
      {filtered.length === 0 ? (
        <EmptyState
          icon={BookOpen}
          title="暂无资料"
          description="上传你的学习资料，AI 将自动解析并生成知识库"
          action={{ label: "上传资料", onClick: handleUpload }}
        />
      ) : (
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
                  onClick={() => deleteMaterial(m.id)}
                >
                  <Trash2 size={14} />
                </button>
              </div>
              <h3 className="truncate">{m.name}</h3>
              <p className="meta-text">
                {m.type} · {m.size} · {m.date}
              </p>
              <div className="tag-row">
                <span
                  className={`badge ${
                    m.status === "已解析" ? "success" : "warning"
                  }`}
                >
                  {m.status === "已解析" ? (
                    <CheckCircle size={12} />
                  ) : (
                    <Loader2 size={12} className="animate-spin" />
                  )}
                  {m.status}
                </span>
              </div>
            </motion.div>
          ))}
        </div>
      )}
    </div>
  );
}
