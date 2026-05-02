import { create } from "zustand";

import { initialDraftQuestions, seedMaterials, seedTasks } from "@/lib/dashboard-data";
import { createLocalMaterial, mapRemoteMaterial, modeText, nowLabel } from "@/lib/dashboard-format";
import type {
  ApiResponse,
  AsyncTaskRecord,
  GeneratedQuizResult,
  MaterialFilter,
  MaterialItem,
  QuestionMode,
  RemoteMaterial,
  RemoteState,
  TaskItem,
  TaskStatus,
  UploadMaterialResult
} from "@/types/dashboard";

type DashboardState = {
  materials: MaterialItem[];
  tasks: TaskItem[];
  selectedMaterialIds: string[];
  materialFilter: MaterialFilter;
  questionMode: QuestionMode;
  difficulty: number;
  interviewMode: boolean;
  apiState: RemoteState;
  quizState: RemoteState;
  draftQuestions: string[];
  setMaterialFilter: (filter: MaterialFilter) => void;
  setQuestionMode: (mode: QuestionMode) => void;
  setDifficulty: (difficulty: number) => void;
  setInterviewMode: (enabled: boolean) => void;
  tickProgress: () => void;
  toggleMaterial: (id: string) => void;
  uploadMaterial: (file: File) => Promise<void>;
  refreshMaterials: () => Promise<void>;
  generateQuestions: () => Promise<void>;
};

export const useDashboardStore = create<DashboardState>((set, get) => ({
  materials: seedMaterials,
  tasks: seedTasks,
  selectedMaterialIds: ["mat-java", "mat-spring"],
  materialFilter: "all",
  questionMode: "choice",
  difficulty: 3,
  interviewMode: true,
  apiState: "idle",
  quizState: "idle",
  draftQuestions: initialDraftQuestions,
  setMaterialFilter: (filter) => set({ materialFilter: filter }),
  setQuestionMode: (mode) => set({ questionMode: mode }),
  setDifficulty: (difficulty) => set({ difficulty }),
  setInterviewMode: (enabled) => set({ interviewMode: enabled }),
  tickProgress: () =>
    set((state) => ({
      materials: state.materials.map((item) => {
        if (item.status !== "parsing") return item;
        const nextProgress = Math.min(item.progress + 4, 100);
        return {
          ...item,
          progress: nextProgress,
          status: nextProgress >= 100 ? "ready" : "parsing",
          score: nextProgress >= 100 ? Math.max(item.score, 86) : item.score,
          updatedAt: nextProgress >= 100 ? nowLabel() : item.updatedAt
        };
      }),
      tasks: state.tasks.map((item) => {
        if (item.status !== "running") return item;
        const nextProgress = Math.min(item.progress + 5, 100);
        return {
          ...item,
          progress: nextProgress,
          status: nextProgress >= 100 ? "done" : "running",
          duration: nextProgress >= 100 ? "02:14" : item.duration
        };
      })
    })),
  toggleMaterial: (id) =>
    set((state) => ({
      selectedMaterialIds: state.selectedMaterialIds.includes(id)
        ? state.selectedMaterialIds.filter((item) => item !== id)
        : [...state.selectedMaterialIds, id]
    })),
  uploadMaterial: async (file) => {
    const optimisticId = `mat-${Date.now()}`;
    const optimisticMaterial = createLocalMaterial(file, optimisticId);
    const optimisticTask = createUploadTask(file.name);

    set((state) => ({
      apiState: "syncing",
      materials: [optimisticMaterial, ...state.materials],
      tasks: [optimisticTask, ...state.tasks],
      selectedMaterialIds: [optimisticId, ...state.selectedMaterialIds]
    }));

    try {
      const formData = new FormData();
      formData.append("file", file);
      const response = await fetch("/api/materials/upload", {
        method: "POST",
        headers: authHeaders(),
        body: formData
      });
      const payload = (await response.json()) as ApiResponse<UploadMaterialResult>;
      if (!response.ok || !payload.success || !payload.data?.material) throw new Error(payload.message);

      const remoteMaterial = mapRemoteMaterial(payload.data.material, 0);
      const remoteTask = mapUploadTask(payload.data.task, remoteMaterial.name, optimisticTask);

      set((state) => ({
        apiState: "online",
        materials: state.materials.map((item) => (item.id === optimisticId ? remoteMaterial : item)),
        tasks: state.tasks.map((item) => (item.id === optimisticTask.id ? remoteTask : item)),
        selectedMaterialIds: state.selectedMaterialIds.map((id) => (id === optimisticId ? remoteMaterial.id : id))
      }));
    } catch {
      set({ apiState: "offline" });
    }
  },
  refreshMaterials: async () => {
    set({ apiState: "syncing" });
    try {
      const response = await fetch("/api/materials", { cache: "no-store", headers: authHeaders() });
      const payload = (await response.json()) as ApiResponse<RemoteMaterial[]>;
      if (!response.ok || !payload.success || !Array.isArray(payload.data)) throw new Error("invalid response");

      const remoteMaterials = payload.data.map(mapRemoteMaterial);

      if (remoteMaterials.length > 0) {
        set((state) => {
          const existing = new Set(remoteMaterials.map((item) => item.id));
          return {
            materials: [...remoteMaterials, ...state.materials.filter((item) => !existing.has(item.id))],
            selectedMaterialIds: Array.from(new Set([...remoteMaterials.map((item) => item.id), ...state.selectedMaterialIds])),
            apiState: "online"
          };
        });
      } else {
        set({ apiState: "online" });
      }
    } catch {
      set({ apiState: "offline" });
    }
  },
  generateQuestions: async () => {
    const { selectedMaterialIds, questionMode, difficulty, interviewMode } = get();
    const remoteMaterialIds = selectedMaterialIds
      .map((id) => Number(id))
      .filter((id) => Number.isInteger(id) && id > 0);

    if (remoteMaterialIds.length === 0) {
      set({ draftQuestions: createLocalDraftQuestions(), quizState: "offline" });
      return;
    }

    set({ quizState: "syncing" });
    try {
      const response = await fetch("/api/quizzes/generate", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...authHeaders()
        },
        body: JSON.stringify({
          materialIds: remoteMaterialIds,
          questionType: questionMode,
          difficulty,
          count: 3,
          interviewMode
        })
      });
      const payload = (await response.json()) as ApiResponse<GeneratedQuizResult>;
      if (!response.ok || !payload.success || !payload.data?.questions?.length) {
        throw new Error(payload.message || "quiz generation failed");
      }
      set({ draftQuestions: payload.data.questions.map((question) => question.stemText), quizState: "online" });
    } catch {
      set({ draftQuestions: createLocalDraftQuestions(), quizState: "offline" });
    }
  }
}));

function authHeaders(): HeadersInit {
  if (typeof window === "undefined") return {};
  const token = window.localStorage.getItem("interview_token");
  return token ? { Authorization: `Bearer ${token}` } : {};
}

function createUploadTask(fileName: string): TaskItem {
  return {
    id: `task-${String(Date.now()).slice(-5)}`,
    title: "资料解析任务",
    materialName: fileName,
    status: "running",
    progress: 8,
    traceId: `trc-${Math.random().toString(16).slice(2, 7)}`,
    duration: "00:03"
  };
}

function mapUploadTask(task: AsyncTaskRecord | undefined, materialName: string, fallback: TaskItem): TaskItem {
  if (!task) return fallback;
  return {
    id: task.taskNo ?? fallback.id,
    title: task.taskType === "MATERIAL_PARSE" ? "资料解析任务" : task.taskType ?? fallback.title,
    materialName,
    status: normalizeTaskStatus(task.status),
    progress: task.progress ?? fallback.progress,
    traceId: task.taskNo ?? fallback.traceId,
    duration: task.status === "DONE" ? "已完成" : "已入队"
  };
}

function normalizeTaskStatus(status?: string): TaskStatus {
  const value = String(status ?? "").toLowerCase();
  if (value.includes("done") || value.includes("success")) return "done";
  if (value.includes("fail") || value.includes("error")) return "failed";
  if (value.includes("pending") || value.includes("queue")) return "queued";
  return "running";
}

function createLocalDraftQuestions() {
  const { materials, selectedMaterialIds, questionMode, difficulty, interviewMode } = useDashboardStore.getState();
  const selectedMaterials = materials.filter((item) => selectedMaterialIds.includes(item.id));
  const source = selectedMaterials.map((item) => item.name.replace(/\.[^.]+$/, "")).join("、") || "已选资料";
  const depth = ["基础", "进阶", "项目化", "高压追问", "专家复盘"][difficulty - 1];
  const interviewSuffix = interviewMode ? "要求继续追问候选人的取舍依据。" : "要求输出参考答案要点。";

  return [
    `${source}：设计一道${depth}${modeText(questionMode)}，题干必须落到项目边界。`,
    "设计一道跨模块问题：资料解析、异步任务、AI 网关之间如何保证可追踪性？",
    `补一道反例题：模型输出质量不稳定时，系统如何降级？${interviewSuffix}`
  ];
}
