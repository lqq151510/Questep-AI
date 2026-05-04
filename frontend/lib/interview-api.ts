import { fetchWithRetry } from "@/lib/fetch-with-retry";

const TOKEN_KEY = "interview_token";

type ApiResponse<T> = {
  success?: boolean;
  message?: string;
  data?: T;
};

export type BackendMaterial = {
  id: number;
  userId?: number;
  name: string;
  fileType: string;
  sourceType?: string;
  storageUrl?: string;
  contentHash?: string | null;
  parseStatus?: string;
  parseErrorMsg?: string | null;
  analysisText?: string | null;
  parsedAt?: string | null;
  createdAt?: string;
  updatedAt?: string;
};

export type BackendQuestion = {
  id: number;
  materialId?: number | null;
  creatorUserId?: number | null;
  questionType?: string;
  stemText: string;
  referenceAnswer?: string | null;
  analysisText?: string | null;
  difficulty?: number | null;
  sourceType?: string;
  modelName?: string;
  createdAt?: string;
  updatedAt?: string;
};

export type BackendAsyncTask = {
  id?: number;
  taskNo?: string;
  taskType?: string;
  bizId?: number;
  status?: string;
  progress?: number;
  errorMsg?: string | null;
  createdBy?: number;
  startedAt?: string | null;
  finishedAt?: string | null;
  createdAt?: string;
  updatedAt?: string;
};

export type UploadMaterialResult = {
  material?: BackendMaterial;
  task?: BackendAsyncTask;
};

export type GenerateQuizPayload = {
  materialIds: number[];
  questionType: "choice" | "short" | "code" | "interview";
  difficulty: number;
  count: number;
  interviewMode: boolean;
};

export type GenerateQuizResult = {
  traceId?: string;
  modelBrief?: string;
  questions?: BackendQuestion[];
};

function authHeaders(): HeadersInit {
  if (typeof window === "undefined") {
    return {};
  }
  const token = window.localStorage.getItem(TOKEN_KEY);
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function unwrap<T>(response: Response, fallbackMessage: string): Promise<T> {
  let payload: ApiResponse<T> | null = null;
  try {
    payload = (await response.json()) as ApiResponse<T>;
  } catch {
    // ignore malformed JSON and use fallback below
  }

  if (response.ok && payload?.success && payload.data !== undefined) {
    return payload.data;
  }

  const rawMessage = payload?.message?.trim();
  if (rawMessage) {
    throw new Error(rawMessage);
  }
  if (response.status === 401 || response.status === 403) {
    throw new Error("未登录或登录已过期，请先登录后再试。");
  }
  throw new Error(fallbackMessage);
}

export async function listMaterials(): Promise<BackendMaterial[]> {
  const response = await fetchWithRetry("/api/v1/materials", {
    cache: "no-store",
    headers: authHeaders()
  });
  return unwrap<BackendMaterial[]>(response, "获取资料列表失败");
}

export async function uploadMaterial(file: File): Promise<UploadMaterialResult> {
  const formData = new FormData();
  formData.append("file", file);

  const response = await fetchWithRetry("/api/v1/materials/upload", {
    method: "POST",
    headers: authHeaders(),
    body: formData
  });
  return unwrap<UploadMaterialResult>(response, "上传资料失败");
}

export async function listQuestions(limit = 30): Promise<BackendQuestion[]> {
  const response = await fetchWithRetry(`/api/v1/quizzes/questions?limit=${encodeURIComponent(String(limit))}`, {
    cache: "no-store",
    headers: authHeaders()
  });
  return unwrap<BackendQuestion[]>(response, "获取题库失败");
}

export async function generateQuiz(payload: GenerateQuizPayload): Promise<GenerateQuizResult> {
  const response = await fetchWithRetry("/api/v1/quizzes/generate", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...authHeaders()
    },
    body: JSON.stringify(payload)
  });
  return unwrap<GenerateQuizResult>(response, "组卷失败");
}

export async function getAsyncTask(taskNo: string): Promise<BackendAsyncTask> {
  const response = await fetchWithRetry(`/api/v1/async-tasks/${encodeURIComponent(taskNo)}`, {
    cache: "no-store",
    headers: authHeaders()
  });
  return unwrap<BackendAsyncTask>(response, "获取任务状态失败");
}

export function toErrorMessage(error: unknown, fallback = "请求失败，请稍后重试"): string {
  if (error instanceof Error && error.message.trim()) {
    return error.message;
  }
  return fallback;
}
