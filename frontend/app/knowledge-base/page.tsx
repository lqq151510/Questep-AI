"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { FilePlus2, FileText, RefreshCw, Search } from "lucide-react";
import { PageHero } from "@/components/new-ui/PageHero";
import { useToast } from "@/components/new-ui/ToastProvider";
import {
  generateQuiz,
  getAsyncTask,
  listMaterials,
  toErrorMessage,
  uploadMaterial,
  type BackendMaterial
} from "@/lib/interview-api";

type MaterialStatus = "done" | "parsing" | "failed";

type MaterialView = {
  id: number;
  name: string;
  type: string;
  date: string;
  status: MaterialStatus;
  tags: string[];
  error: string | null;
};

function normalizeStatus(value?: string): MaterialStatus {
  const lower = String(value ?? "").toLowerCase();
  if (lower.includes("fail") || lower.includes("error")) {
    return "failed";
  }
  if (lower.includes("pending") || lower.includes("process") || lower.includes("running")) {
    return "parsing";
  }
  return "done";
}

function inferTags(material: BackendMaterial): string[] {
  const name = material.name.toLowerCase();
  const tags = new Set<string>();
  if (name.includes("java")) tags.add("Java");
  if (name.includes("spring")) tags.add("Spring");
  if (name.includes("mysql")) tags.add("MySQL");
  if (name.includes("redis")) tags.add("Redis");
  if (name.includes("system") || name.includes("设计")) tags.add("系统设计");
  if (tags.size === 0) {
    tags.add(material.fileType.toUpperCase());
  }
  return Array.from(tags);
}

function formatDate(value?: string): string {
  if (!value) return "未知时间";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "未知时间";
  return date.toLocaleDateString("zh-CN");
}

function toMaterialView(item: BackendMaterial): MaterialView {
  return {
    id: item.id,
    name: item.name,
    type: item.fileType?.toUpperCase() || "FILE",
    date: formatDate(item.updatedAt ?? item.createdAt),
    status: normalizeStatus(item.parseStatus),
    tags: inferTags(item),
    error: item.parseErrorMsg ?? null
  };
}

export default function KnowledgeBasePage() {
  const { showToast } = useToast();
  const [keyword, setKeyword] = useState("");
  const [materials, setMaterials] = useState<MaterialView[]>([]);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      const result = await listMaterials();
      setMaterials(result.map(toMaterialView));
    } catch (error) {
      showToast(toErrorMessage(error, "加载资料失败"));
    } finally {
      setLoading(false);
    }
  }, [showToast]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const refreshAfterTaskSettles = useCallback(async (taskNo: string) => {
    for (let attempt = 0; attempt < 10; attempt++) {
      await delay(2500);
      const task = await getAsyncTask(taskNo);
      const status = String(task.status ?? "").toUpperCase();
      if (status === "SUCCESS" || status === "FAILED") {
        showToast(status === "SUCCESS" ? "资料解析完成" : task.errorMsg ?? "资料解析失败");
        await refresh();
        return;
      }
    }
  }, [refresh, showToast]);

  const visibleMaterials = useMemo(() => {
    const query = keyword.trim().toLowerCase();
    if (!query) return materials;
    return materials.filter((item) => {
      const text = `${item.name} ${item.type} ${item.tags.join(" ")}`.toLowerCase();
      return text.includes(query);
    });
  }, [keyword, materials]);

  const stats = useMemo(
    () => ({
      total: materials.length,
      done: materials.filter((item) => item.status === "done").length,
      tags: new Set(materials.flatMap((item) => item.tags)).size
    }),
    [materials]
  );

  const handleFileChoose = () => {
    fileInputRef.current?.click();
  };

  const handleFileChange = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    setUploading(true);
    try {
      const result = await uploadMaterial(file);
      showToast(result.task?.taskNo ? `上传成功，任务已入队：${result.task.taskNo}` : "上传成功，任务已入队");
      await refresh();
      if (result.task?.taskNo) {
        void refreshAfterTaskSettles(result.task.taskNo);
      }
    } catch (error) {
      showToast(toErrorMessage(error, "上传失败"));
    } finally {
      setUploading(false);
      event.target.value = "";
    }
  };

  const handleGenerate = async (materialId: number) => {
    try {
      const result = await generateQuiz({
        materialIds: [materialId],
        questionType: "short",
        difficulty: 3,
        count: 3,
        interviewMode: true
      });
      const size = result.questions?.length ?? 0;
      showToast(`组卷完成，生成 ${size} 道题`);
    } catch (error) {
      showToast(toErrorMessage(error, "组卷失败"));
    }
  };

  return (
    <div className="container">
      <PageHero
        kicker="Knowledge Hub"
        title="个人知识库"
        description="上传资料并自动解析成知识点，快速联动生成题目与复习任务。"
      />

      <section className="metric-grid compact">
        <article className="metric-card">
          <p className="metric-label">资料总数</p>
          <p className="metric-value">{stats.total}</p>
        </article>
        <article className="metric-card">
          <p className="metric-label">已解析</p>
          <p className="metric-value">{stats.done}</p>
        </article>
        <article className="metric-card">
          <p className="metric-label">知识标签</p>
          <p className="metric-value">{stats.tags}</p>
        </article>
      </section>

      <section className="panel">
        <input
          ref={fileInputRef}
          type="file"
          hidden
          onChange={(event) => void handleFileChange(event)}
          accept=".txt,.md,.csv,.json"
        />
        <button type="button" className="upload-area" onClick={handleFileChoose} disabled={uploading}>
          <FilePlus2 size={20} />
          <span>{uploading ? "上传中..." : "点击上传学习资料"}</span>
          <small>支持 TXT / Markdown / CSV / JSON，最大 10MB</small>
        </button>

        <div className="search-row">
          <h2>我的资料</h2>
          <div className="row-actions">
            <button type="button" className="btn" onClick={() => void refresh()} disabled={loading}>
              <RefreshCw size={14} />
              {loading ? "刷新中" : "刷新"}
            </button>
            <label className="search-input-wrap">
              <Search size={14} />
              <input
                type="text"
                placeholder="搜索资料或标签…"
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
              />
            </label>
          </div>
        </div>

        <div className="material-grid">
          {visibleMaterials.map((material) => (
            <article key={material.id} className="material-card">
              <div className="material-head">
                <FileText size={18} />
                <span
                  className={
                    material.status === "done"
                      ? "badge success"
                      : "badge warning"
                  }
                >
                  {material.status === "done" ? "已解析" : material.status === "failed" ? "失败" : "解析中"}
                </span>
              </div>
              <h3>{material.name}</h3>
              <p className="meta-text">
                {material.date} · {material.type}
              </p>
              <div className="tag-row">
                {material.tags.map((tag) => (
                  <span key={`${material.id}-${tag}`} className="tag">
                    {tag}
                  </span>
                ))}
              </div>
              {material.error && <p className="meta-text">失败原因：{material.error}</p>}
              <div className="row-actions">
                <button type="button" className="btn btn-accent" onClick={() => void handleGenerate(material.id)}>
                  生成题目
                </button>
              </div>
            </article>
          ))}
          {visibleMaterials.length === 0 && (
            <article className="material-card">
              <h3>暂无可展示资料</h3>
              <p className="meta-text">请先上传资料，或检查当前登录令牌是否有效。</p>
            </article>
          )}
        </div>
      </section>
    </div>
  );
}

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => window.setTimeout(resolve, ms));
}
