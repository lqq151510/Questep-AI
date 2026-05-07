import {
  BarChart3,
  BookOpenCheck,
  ClipboardList,
  Database,
  Gauge,
  Mic2
} from "lucide-react";

import type { KnowledgeItem, MaterialItem, NavItem, TaskItem } from "@/types/dashboard";

export const seedMaterials: MaterialItem[] = [
  {
    id: "mat-java",
    name: "Java 并发面经.md",
    type: "MD",
    status: "ready",
    progress: 100,
    chunks: 128,
    score: 94,
    updatedAt: "09:42"
  },
  {
    id: "mat-spring",
    name: "Spring Boot 项目复盘.md",
    type: "MD",
    status: "ready",
    progress: 100,
    chunks: 76,
    score: 88,
    updatedAt: "10:18"
  },
  {
    id: "mat-mysql",
    name: "MySQL 索引与事务.csv",
    type: "CSV",
    status: "parsing",
    progress: 64,
    chunks: 43,
    score: 71,
    updatedAt: "11:05"
  }
];

export const seedTasks: TaskItem[] = [
  {
    id: "task-9842",
    title: "资料切分与向量化",
    materialName: "MySQL 索引与事务.csv",
    status: "running",
    progress: 64,
    traceId: "trc-7a91c",
    duration: "01:46"
  },
  {
    id: "task-9731",
    title: "Java 并发题库生成",
    materialName: "Java 并发面经.md",
    status: "done",
    progress: 100,
    traceId: "trc-29bd4",
    duration: "00:58"
  },
  {
    id: "task-9608",
    title: "薄弱点分析报告",
    materialName: "Spring Boot 项目复盘.md",
    status: "queued",
    progress: 12,
    traceId: "trc-41fda",
    duration: "排队中"
  }
];

export const navItems: NavItem[] = [
  { label: "任务台", icon: Gauge, href: "/home", active: true },
  { label: "资料库", icon: Database, href: "/knowledge-base", active: false },
  { label: "刷题", icon: ClipboardList, href: "/question-bank", active: false },
  { label: "面试", icon: Mic2, href: "/ai-interviewer", active: false },
  { label: "错题本", icon: BookOpenCheck, href: "/wrong-answers", active: false },
  { label: "诊断", icon: BarChart3, href: "/ai-qa", active: false }
];

export const knowledge: KnowledgeItem[] = [
  { label: "并发模型", value: 92, tone: "teal" },
  { label: "事务隔离", value: 68, tone: "amber" },
  { label: "Spring AOP", value: 84, tone: "indigo" },
  { label: "缓存一致性", value: 61, tone: "coral" }
];

export const weakPoints = [
  "索引回表与覆盖索引的边界场景",
  "线程池拒绝策略的业务补偿",
  "事务传播行为在异步任务中的影响"
];

export const initialDraftQuestions = [
  "讲清楚 volatile 与 synchronized 的可见性差异，并给出项目里的使用边界。",
  "线程池参数如何根据 I/O 密集型任务调整？需要说明拒绝策略。",
  "解释 Spring 事务失效的三个常见原因，并给出排查路径。"
];
