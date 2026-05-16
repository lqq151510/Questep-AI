import { create } from "zustand";

import { initialDraftQuestions } from "@/lib/dashboard-data";
import { createLocalMaterial, mapRemoteMaterial, modeText, nowLabel } from "@/lib/dashboard-format";
import {
  clearAuthTokens,
  clearAuthTokensAndNotify,
  deleteMaterial as requestDeleteMaterial,
  fetchCaptcha as requestCaptcha,
  generateQuiz as requestGenerateQuiz,
  getAsyncTask,
  listMaterials as requestListMaterials,
  login as requestLogin,
  register as requestRegister,
  refreshAuthSession,
  saveAuthTokens,
  toErrorMessage,
  retryParseMaterial as requestRetryParse,
  uploadMaterial as requestUploadMaterial,
  type BackendAsyncTask,
  type CaptchaResult,
  type LoginPayload,
  type RegisterPayload
} from "@/lib/interview-api";
import type {
  MaterialFilter,
  MaterialItem,
  QuestionMode,
  RemoteState,
  TaskItem,
  TaskStatus
} from "@/types/dashboard";

type DashboardState = {
  // auth
  user: { username: string } | null;
  isLoggedIn: boolean;
  authState: RemoteState;
  login: (payload: LoginPayload) => Promise<string | null>;
  register: (payload: RegisterPayload) => Promise<string | null>;
  fetchCaptcha: () => Promise<CaptchaResult | null>;
  logout: () => void;

  // quiz
  materials: MaterialItem[];
  tasks: TaskItem[];
  selectedMaterialIds: string[];
  materialFilter: MaterialFilter;
  questionMode: QuestionMode;
  difficulty: number;
  count: number;
  interviewMode: boolean;
  apiState: RemoteState;
  quizState: RemoteState;
  quizWarnings: string[];
  draftQuestions: string[];
  setMaterialFilter: (filter: MaterialFilter) => void;
  setQuestionMode: (mode: QuestionMode) => void;
  setDifficulty: (difficulty: number) => void;
  setCount: (count: number) => void;
  setInterviewMode: (enabled: boolean) => void;
  tickProgress: () => void;
  toggleMaterial: (id: string) => void;
  uploadMaterial: (file: File) => Promise<void>;
  refreshMaterials: () => Promise<void>;
  deleteMaterial: (materialId: string) => Promise<void>;
  generateQuestions: () => Promise<void>;
  retryParseMaterial: (materialId: number) => Promise<void>;
};

type DashboardSetter = (updater: (state: DashboardState) => Partial<DashboardState>) => void;

const AUTH_EXPIRED_EVENT = "interview-auth-expired";

function readStoredUser(): { username: string } | null {
  if (typeof window === "undefined") return null;
  const token = localStorage.getItem("interview_token");
  const username = localStorage.getItem("interview_username");
  const sessionExpiresAtRaw = localStorage.getItem("interview_session_expires_at");
  const sessionExpiresAt = sessionExpiresAtRaw ? Number(sessionExpiresAtRaw) : NaN;

  if (token && username && Number.isFinite(sessionExpiresAt) && sessionExpiresAt > Date.now()) return { username };

  if (token || username || sessionExpiresAtRaw) {
    clearAuthTokensAndNotify();
    localStorage.removeItem("interview_username");
  }
  return null;
}

export const useDashboardStore = create<DashboardState>((set, get) => ({
  // auth
  user: readStoredUser(),
  isLoggedIn: readStoredUser() !== null,
  authState: readStoredUser() !== null ? "online" : "idle",
  login: async (payload) => {
    set({ authState: "syncing" });
    try {
      const result = await requestLogin(payload);
      saveAuthTokens(result);
      refreshAuthSession();
      const username = payload.username;
      set({ user: { username }, isLoggedIn: true, authState: "online" });
      localStorage.setItem("interview_username", username);
      return null;
    } catch (error) {
      set({ authState: "offline" });
      return toErrorMessage(error, "登录失败");
    }
  },
  register: async (payload) => {
    set({ authState: "syncing" });
    try {
      const result = await requestRegister(payload);
      saveAuthTokens(result);
      refreshAuthSession();
      const username = payload.username;
      set({ user: { username }, isLoggedIn: true, authState: "online" });
      localStorage.setItem("interview_username", username);
      return null;
    } catch (error) {
      set({ authState: "offline" });
      return toErrorMessage(error, "注册失败");
    }
  },
  fetchCaptcha: async () => {
    try {
      return await requestCaptcha();
    } catch {
      return null;
    }
  },
  logout: () => {
    clearAuthTokens();
    localStorage.removeItem("interview_username");
    set({ user: null, isLoggedIn: false, authState: "idle" });
  },

  // quiz
  materials: [],
  tasks: [],
  selectedMaterialIds: [],
  materialFilter: "all",
  questionMode: "choice",
  difficulty: 3,
  count: 10,
  interviewMode: true,
  apiState: "idle",
  quizState: "idle",
  quizWarnings: [],
  draftQuestions: initialDraftQuestions,
  setMaterialFilter: (filter) => set({ materialFilter: filter }),
  setQuestionMode: (mode) => set({ questionMode: mode }),
  setDifficulty: (difficulty) => set({ difficulty }),
  setCount: (count) => set({ count }),
  setInterviewMode: (enabled) => set({ interviewMode: enabled }),
  tickProgress: () => {
    void syncProgressFromBackend(set, get);
  },
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
      const result = await requestUploadMaterial(file);
      if (!result.material) {
        throw new Error("上传响应缺少资料信息");
      }

      const remoteMaterial = mapRemoteMaterial(result.material, 0);
      const remoteTask = mapUploadTask(result.task, remoteMaterial.name, optimisticTask);

      set((state) => ({
        apiState: "online",
        materials: state.materials.map((item) => (item.id === optimisticId ? remoteMaterial : item)),
        tasks: state.tasks.map((item) => (item.id === optimisticTask.id ? remoteTask : item)),
        selectedMaterialIds: state.selectedMaterialIds.map((id) => (id === optimisticId ? remoteMaterial.id : id))
      }));

      if (result.task?.taskNo) {
        void pollTaskStatus(result.task.taskNo, remoteMaterial.id, remoteMaterial.name, set);
      }
    } catch (error) {
      const detail = toErrorMessage(error, "上传失败，已保留本地记录");
      set((state) => ({
        apiState: "offline",
        materials: state.materials.map((item) =>
          item.id === optimisticId ? { ...item, status: "failed", progress: 100, detail } : item
        ),
        tasks: state.tasks.map((item) =>
          item.id === optimisticTask.id ? { ...item, status: "failed", progress: 100, duration: "失败", detail } : item
        )
      }));
    }
  },
  refreshMaterials: async () => {
    set({ apiState: "syncing" });
    try {
      const remoteMaterials = (await requestListMaterials()).map(mapRemoteMaterial);

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
  deleteMaterial: async (materialId) => {
    const numericId = Number(materialId);
    if (!Number.isInteger(numericId) || numericId <= 0) {
      set((state) => ({
        materials: state.materials.filter((item) => item.id !== materialId),
        selectedMaterialIds: state.selectedMaterialIds.filter((id) => id !== materialId)
      }));
      return;
    }
    try {
      await requestDeleteMaterial(numericId);
      set((state) => ({
        materials: state.materials.filter((item) => item.id !== materialId),
        selectedMaterialIds: state.selectedMaterialIds.filter((id) => id !== materialId),
        apiState: "online"
      }));
    } catch (error) {
      const detail = toErrorMessage(error, "删除资料失败");
      set((state) => ({
        apiState: "offline",
        materials: state.materials.map((item) =>
          item.id === materialId ? { ...item, detail } : item
        )
      }));
    }
  },
  generateQuestions: async () => {
    const { selectedMaterialIds, questionMode, difficulty, count, interviewMode } = get();
    const remoteMaterialIds = selectedMaterialIds
      .map((id) => Number(id))
      .filter((id) => Number.isInteger(id) && id > 0);

    if (remoteMaterialIds.length === 0) {
      set({ draftQuestions: createLocalDraftQuestions(), quizState: "offline" });
      return;
    }

    set({ quizState: "syncing", quizWarnings: [] });
    try {
      const result = await requestGenerateQuiz({
        materialIds: remoteMaterialIds,
        questionType: questionMode,
        difficulty,
        count,
        interviewMode
      });
      if (!result.questions?.length) {
        throw new Error("quiz generation returned no questions");
      }
      const warnings = result.warnings ?? [];
      if (result.fallbackUsed && warnings.length === 0) {
        warnings.push("题目生成部分使用了降级策略");
      }
      set({
        draftQuestions: (result.questions ?? []).map((question) => question.stemText),
        quizState: "online",
        quizWarnings: warnings
      });
    } catch {
      set({ draftQuestions: createLocalDraftQuestions(), quizState: "offline", quizWarnings: [] });
    }
  },
  retryParseMaterial: async (materialId) => {
    set({ apiState: "syncing" });
    try {
      const result = await requestRetryParse(materialId);
      const material = get().materials.find((m) => m.id === String(materialId));
      const materialName = material?.name ?? "未知资料";
      const remoteTask = mapUploadTask(result.task, materialName);

      set((state) => ({
        apiState: "online",
        tasks: [remoteTask, ...state.tasks],
        materials: state.materials.map((item) =>
          item.id === String(materialId)
            ? { ...item, status: "parsing" as const, progress: 10, detail: "重新解析已入队" }
            : item
        )
      }));

      if (result.task?.taskNo) {
        void pollTaskStatus(result.task.taskNo, String(materialId), materialName, set);
      }
    } catch (error) {
      const detail = toErrorMessage(error, "重试解析失败");
      set((state) => ({
        apiState: "offline",
        materials: state.materials.map((item) =>
          item.id === String(materialId) ? { ...item, detail } : item
        )
      }));
    }
  }
}));

if (typeof window !== "undefined") {
  window.addEventListener(AUTH_EXPIRED_EVENT, () => {
    useDashboardStore.setState({ user: null, isLoggedIn: false, authState: "idle" });
  });

  window.addEventListener("focus", () => {
    refreshAuthSession();
  });

  window.addEventListener("visibilitychange", () => {
    if (!document.hidden) {
      refreshAuthSession();
    }
  });
}

function createUploadTask(fileName: string): TaskItem {
  return {
    id: `task-${String(Date.now()).slice(-5)}`,
    title: "资料解析任务",
    materialName: fileName,
    status: "running",
    progress: 8,
    traceId: `trc-${Math.random().toString(16).slice(2, 7)}`,
    duration: "等待后端确认",
    detail: "上传请求已发出"
  };
}

function mapUploadTask(task: BackendAsyncTask | undefined, materialName: string, fallback?: TaskItem): TaskItem {
  if (!task) return fallback ?? createUploadTask(materialName);
  const status = normalizeTaskStatus(task.status);
  return {
    id: task.taskNo ?? fallback?.id ?? `task-${String(Date.now()).slice(-5)}`,
    title: task.taskType === "MATERIAL_PARSE" ? "资料解析任务" : task.taskType ?? fallback?.title ?? "异步任务",
    materialName,
    status,
    progress: task.progress ?? fallback?.progress ?? statusProgress(status),
    traceId: task.taskNo ?? fallback?.traceId ?? "pending-task",
    duration: statusDuration(status),
    detail: task.errorMsg ?? fallback?.detail
  };
}

function normalizeTaskStatus(status?: string): TaskStatus {
  const value = String(status ?? "").toLowerCase();
  if (value.includes("done") || value.includes("success")) return "done";
  if (value.includes("fail") || value.includes("error")) return "failed";
  if (value.includes("pending") || value.includes("queue")) return "queued";
  return "running";
}

function statusProgress(status: TaskStatus) {
  if (status === "done" || status === "failed") return 100;
  if (status === "queued") return 10;
  return 60;
}

function statusDuration(status: TaskStatus) {
  return {
    done: "已完成",
    failed: "失败",
    queued: "已入队",
    running: "处理中"
  }[status];
}

async function syncProgressFromBackend(set: DashboardSetter, get: () => DashboardState) {
  const activeTasks = get().tasks.filter(
    (item) => (item.status === "running" || item.status === "queued") && !item.id.startsWith("task-")
  );
  if (activeTasks.length === 0) {
    return;
  }

  const results = await Promise.allSettled(activeTasks.map((task) => getAsyncTask(task.id)));
  const latestTasks = new Map<string, BackendAsyncTask>();

  results.forEach((result, index) => {
    if (result.status === "fulfilled") {
      latestTasks.set(activeTasks[index].id, result.value);
    }
  });

  if (latestTasks.size === 0) {
    set(() => ({ apiState: "offline" }));
    return;
  }

  set((state) => {
    const materialTaskMap = new Map<string, { task: BackendAsyncTask; status: TaskStatus }>();
    latestTasks.forEach((task) => {
      if (typeof task.bizId === "number" && task.bizId > 0) {
        materialTaskMap.set(String(task.bizId), { task, status: normalizeTaskStatus(task.status) });
      }
    });

    return {
      apiState: "online",
      tasks: state.tasks.map((item) => {
        const latest = latestTasks.get(item.id);
        return latest ? mapUploadTask(latest, item.materialName, item) : item;
      }),
      materials: state.materials.map((item) => {
        const current = materialTaskMap.get(item.id);
        if (!current) return item;
        return applyTaskStateToMaterial(item, current.task, current.status);
      })
    };
  });
}

async function pollTaskStatus(taskNo: string, materialId: string, materialName: string, set: DashboardSetter) {
  for (let attempt = 0; attempt < 12; attempt++) {
    await delay(2500);
    try {
      const task = await getAsyncTask(taskNo);
      const normalizedStatus = normalizeTaskStatus(task.status);
      set((state) => ({
        apiState: "online",
        tasks: state.tasks.map((item) => (item.id === taskNo ? mapUploadTask(task, materialName, item) : item)),
        materials: state.materials.map((item) =>
          item.id === materialId ? applyTaskStateToMaterial(item, task, normalizedStatus) : item
        )
      }));
      if (normalizedStatus === "done" || normalizedStatus === "failed") {
        return;
      }
    } catch (error) {
      const detail = toErrorMessage(error, "任务状态刷新失败");
      set((state) => ({
        apiState: "offline",
        tasks: state.tasks.map((item) => (item.id === taskNo ? { ...item, detail } : item))
      }));
      return;
    }
  }
}

function applyTaskStateToMaterial(item: MaterialItem, task: BackendAsyncTask, status: TaskStatus): MaterialItem {
  if (status === "done") {
    return { ...item, status: "ready", progress: 100, score: Math.max(item.score, 86), updatedAt: nowLabel(), detail: "解析完成" };
  }
  if (status === "failed") {
    return { ...item, status: "failed", progress: 100, updatedAt: nowLabel(), detail: task.errorMsg ?? "解析失败" };
  }
  return {
    ...item,
    status: "parsing",
    progress: Math.max(item.progress, task.progress ?? statusProgress(status)),
    updatedAt: nowLabel(),
    detail: status === "queued" ? "任务已入队" : "后端解析中"
  };
}

function delay(ms: number) {
  return new Promise((resolve) => window.setTimeout(resolve, ms));
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
