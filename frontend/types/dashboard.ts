import type { LucideIcon } from "lucide-react";

export type MaterialStatus = "ready" | "parsing" | "failed";
export type TaskStatus = "done" | "running" | "queued" | "failed";
export type QuestionMode = "choice" | "short" | "code";
export type MaterialFilter = "all" | MaterialStatus;
export type RemoteState = "idle" | "syncing" | "online" | "offline";

export type MaterialItem = {
  id: string;
  name: string;
  type: string;
  status: MaterialStatus;
  progress: number;
  chunks: number;
  score: number;
  updatedAt: string;
};

export type TaskItem = {
  id: string;
  title: string;
  materialName: string;
  status: TaskStatus;
  progress: number;
  traceId: string;
  duration: string;
};

export type KnowledgeItem = {
  label: string;
  value: number;
  tone: "teal" | "amber" | "indigo" | "coral";
};

export type NavItem = {
  label: string;
  icon: LucideIcon;
  href: string;
  active: boolean;
};

export type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
};

export type RemoteMaterial = {
  id?: number | string;
  name?: string;
  fileType?: string;
  parseStatus?: string;
  updatedAt?: string;
};

export type AsyncTaskRecord = {
  taskNo?: string;
  taskType?: string;
  status?: string;
  progress?: number;
};

export type UploadMaterialResult = {
  material?: RemoteMaterial;
  task?: AsyncTaskRecord;
};

export type GeneratedQuestion = {
  id: number;
  stemText: string;
};

export type GeneratedQuizResult = {
  traceId: string;
  modelBrief: string;
  questions: GeneratedQuestion[];
};
