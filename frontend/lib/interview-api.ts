import { fetchWithRetry } from "@/lib/fetch-with-retry";

const ACCESS_TOKEN_KEY = "interview_token";
const REFRESH_TOKEN_KEY = "interview_refresh_token";

type ApiResponse<T> = {
  success?: boolean;
  message?: string;
  data?: T;
};

export type AuthTokenPayload = {
  token: string;
  refreshToken?: string | null;
  tokenType?: string;
  expiresInSeconds?: number;
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

let refreshPromise: Promise<boolean> | null = null;

function readAccessToken(): string | null {
  if (typeof window === "undefined") {
    return null;
  }
  return window.localStorage.getItem(ACCESS_TOKEN_KEY);
}

function readRefreshToken(): string | null {
  if (typeof window === "undefined") {
    return null;
  }
  return window.localStorage.getItem(REFRESH_TOKEN_KEY);
}

export function saveAuthTokens(payload: AuthTokenPayload): void {
  if (typeof window === "undefined") {
    return;
  }
  const accessToken = payload.token?.trim();
  if (accessToken) {
    window.localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
  }
  if (payload.refreshToken !== undefined) {
    const refreshToken = payload.refreshToken?.trim();
    if (refreshToken) {
      window.localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
    } else {
      window.localStorage.removeItem(REFRESH_TOKEN_KEY);
    }
  }
}

export function clearAuthTokens(): void {
  if (typeof window === "undefined") {
    return;
  }
  window.localStorage.removeItem(ACCESS_TOKEN_KEY);
  window.localStorage.removeItem(REFRESH_TOKEN_KEY);
}

function withAuthorization(options: RequestInit = {}): RequestInit {
  const token = readAccessToken();
  const headers = new Headers(options.headers ?? {});
  if (token && !headers.has("Authorization")) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  return { ...options, headers };
}

async function parseApiResponse<T>(response: Response): Promise<ApiResponse<T> | null> {
  try {
    return (await response.json()) as ApiResponse<T>;
  } catch {
    return null;
  }
}

async function refreshAccessToken(): Promise<boolean> {
  const refreshToken = readRefreshToken();
  if (!refreshToken) {
    return false;
  }
  if (refreshPromise) {
    return refreshPromise;
  }
  refreshPromise = (async () => {
    try {
      const response = await fetchWithRetry("/api/v1/auth/refresh", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken })
      });
      const payload = await parseApiResponse<AuthTokenPayload>(response);
      if (!response.ok || !payload?.success || !payload.data?.token) {
        clearAuthTokens();
        return false;
      }
      saveAuthTokens(payload.data);
      return true;
    } catch {
      clearAuthTokens();
      return false;
    } finally {
      refreshPromise = null;
    }
  })();
  return refreshPromise;
}

export async function fetchWithAuth(url: string, options: RequestInit = {}): Promise<Response> {
  const response = await fetchWithRetry(url, withAuthorization(options));
  if (response.status !== 401) {
    return response;
  }
  const refreshed = await refreshAccessToken();
  if (!refreshed) {
    return response;
  }
  return fetchWithRetry(url, withAuthorization(options));
}

async function unwrap<T>(response: Response, fallbackMessage: string): Promise<T> {
  const payload = await parseApiResponse<T>(response);

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
  const response = await fetchWithAuth("/api/v1/materials", { cache: "no-store" });
  return unwrap<BackendMaterial[]>(response, "获取资料列表失败");
}

export async function uploadMaterial(file: File): Promise<UploadMaterialResult> {
  const formData = new FormData();
  formData.append("file", file);

  const response = await fetchWithAuth("/api/v1/materials/upload", {
    method: "POST",
    body: formData
  });
  return unwrap<UploadMaterialResult>(response, "上传资料失败");
}

export async function listQuestions(limit = 30): Promise<BackendQuestion[]> {
  const response = await fetchWithAuth(`/api/v1/quizzes/questions?limit=${encodeURIComponent(String(limit))}`, { cache: "no-store" });
  return unwrap<BackendQuestion[]>(response, "获取题库失败");
}

export async function generateQuiz(payload: GenerateQuizPayload): Promise<GenerateQuizResult> {
  const response = await fetchWithAuth("/api/v1/quizzes/generate", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
  return unwrap<GenerateQuizResult>(response, "组卷失败");
}

export async function getAsyncTask(taskNo: string): Promise<BackendAsyncTask> {
  const response = await fetchWithAuth(`/api/v1/async-tasks/${encodeURIComponent(taskNo)}`, { cache: "no-store" });
  return unwrap<BackendAsyncTask>(response, "获取任务状态失败");
}

export function toErrorMessage(error: unknown, fallback = "请求失败，请稍后重试"): string {
  if (error instanceof Error && error.message.trim()) {
    return error.message;
  }
  return fallback;
}
