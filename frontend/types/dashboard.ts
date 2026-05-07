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
  detail?: string;
};

export type TaskItem = {
  id: string;
  title: string;
  materialName: string;
  status: TaskStatus;
  progress: number;
  traceId: string;
  duration: string;
  detail?: string;
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

export type RemoteMaterial = {
  id?: number | string;
  name?: string;
  fileType?: string;
  parseStatus?: string;
  parseErrorMsg?: string | null;
  updatedAt?: string;
};
