import type {
  MaterialItem,
  MaterialStatus,
  QuestionMode,
  RemoteMaterial,
  TaskStatus
} from "@/types/dashboard";

export function statusText(status: MaterialStatus) {
  return {
    ready: "已解析",
    parsing: "解析中",
    failed: "失败"
  }[status];
}

export function taskStatusText(status: TaskStatus) {
  return {
    done: "完成",
    running: "运行中",
    queued: "等待",
    failed: "失败"
  }[status];
}

export function modeText(mode: QuestionMode) {
  return {
    choice: "选择题",
    short: "简答题",
    code: "编程题"
  }[mode];
}

export function nowLabel() {
  return new Intl.DateTimeFormat("zh-CN", {
    hour: "2-digit",
    minute: "2-digit"
  }).format(new Date());
}

export function materialTypeFromName(name: string) {
  return name.includes(".") ? name.split(".").pop()?.toUpperCase() ?? "FILE" : "FILE";
}

export function createLocalMaterial(file: File, id = `mat-${Date.now()}`): MaterialItem {
  return {
    id,
    name: file.name,
    type: materialTypeFromName(file.name),
    status: "parsing",
    progress: 8,
    chunks: Math.max(16, Math.round(file.size / 16000)),
    score: 62,
    updatedAt: nowLabel()
  };
}

export function mapRemoteMaterial(item: RemoteMaterial, index: number): MaterialItem {
  const status = normalizeMaterialStatus(item.parseStatus);

  return {
    id: String(item.id ?? `remote-${index}`),
    name: String(item.name ?? "未命名资料"),
    type: String(item.fileType ?? materialTypeFromName(String(item.name ?? ""))).toUpperCase(),
    status,
    progress: status === "ready" ? 100 : status === "failed" ? 100 : 64,
    chunks: 32 + index * 9,
    score: status === "failed" ? 48 : 82 + (index % 3) * 4,
    updatedAt: formatRemoteTime(item.updatedAt)
  };
}

function normalizeMaterialStatus(status?: string): MaterialStatus {
  const value = String(status ?? "").toLowerCase();
  if (value.includes("fail") || value.includes("error")) return "failed";
  if (value.includes("pending") || value.includes("parsing") || value.includes("running")) return "parsing";
  return "ready";
}

function formatRemoteTime(value?: string) {
  if (!value) return nowLabel();
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return nowLabel();
  return new Intl.DateTimeFormat("zh-CN", {
    hour: "2-digit",
    minute: "2-digit"
  }).format(date);
}
