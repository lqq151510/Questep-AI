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
  optionsJson?: Record<string, string> | null;
  referenceAnswer?: string | null;
  analysisText?: string | null;
  difficulty?: number | null;
  sourceType?: string;
  modelName?: string;
  createdAt?: string;
  updatedAt?: string;
};

export type LoginPayload = {
  username: string;
  password: string;
};

export type RegisterPayload = LoginPayload & {
  email: string;
  captchaId: string;
  captchaCode: string;
};

export type CaptchaResult = {
  captchaId: string;
  captchaCode: string;
};

export type BackendAsyncTask = {
  id?: number;
  taskNo?: string;
  taskType?: string;
  bizType?: string;
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
  fallbackUsed?: boolean;
  invalidCount?: number;
  warnings?: string[];
};

export type DashboardMetrics = {
  completedTests: number;
  mockInterviews: number;
  masteredPoints: number;
  pendingWrongReviews: number;
  parsedMaterials: number;
  recentInterviewQuestions: number;
  activeInterviewSessions: number;
};

let refreshPromise: Promise<boolean> | null = null;

function readAccessToken(): string | null {
  if (typeof window === "undefined") {
    return null;
  }
  return window.localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function getAccessToken(): string | null {
  return readAccessToken();
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

export function buildInterviewWebSocketUrl(): string {
  if (typeof window === "undefined") {
    return "";
  }

  const explicitWs = process.env.NEXT_PUBLIC_BACKEND_WS_URL?.trim();
  const explicitApi = process.env.NEXT_PUBLIC_BACKEND_API_URL?.trim();
  let base = explicitWs;

  if (!base) {
    const fallbackHttp = explicitApi || "http://127.0.0.1:8080";
    base = fallbackHttp.replace(/^http/i, "ws");
  }

  const normalizedBase = base.replace(/\/$/, "");
  const token = readAccessToken();
  const tokenParam = token ? `?token=${encodeURIComponent(token)}` : "";
  return `${normalizedBase}/ws/interview${tokenParam}`;
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

  if (response.status === 408) {
    throw new Error("请求超时，请检查网络连接后重试");
  }
  if (response.status === 429) {
    throw new Error("请求过于频繁，请稍后重试");
  }
  if (response.status >= 500) {
    throw new Error("服务暂时不可用，请稍后重试");
  }
  if (response.status === 401 || response.status === 403) {
    throw new Error("未登录或登录已过期，请先登录后再试。");
  }

  const rawMessage = payload?.message?.trim();
  if (rawMessage) {
    throw new Error(rawMessage);
  }
  throw new Error(fallbackMessage);
}

export async function login(payload: LoginPayload): Promise<AuthTokenPayload> {
  const response = await fetchWithRetry("/api/v1/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
  return unwrap<AuthTokenPayload>(response, "登录失败");
}

export async function register(payload: RegisterPayload): Promise<AuthTokenPayload> {
  const response = await fetchWithRetry("/api/v1/auth/register", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
  return unwrap<AuthTokenPayload>(response, "注册失败");
}

export async function fetchCaptcha(): Promise<CaptchaResult> {
  const response = await fetchWithRetry("/api/v1/auth/captcha", { cache: "no-store" });
  return unwrap<CaptchaResult>(response, "获取验证码失败");
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

export async function listQuestions(page = 0, pageSize = 20): Promise<BackendQuestion[]> {
  const response = await fetchWithAuth(
    `/api/v1/quizzes/questions?page=${encodeURIComponent(String(page))}&pageSize=${encodeURIComponent(String(pageSize))}`,
    { cache: "no-store" }
  );
  return unwrap<BackendQuestion[]>(response, "获取题库失败");
}

function isParseSuccess(status?: string): boolean {
  const normalized = String(status ?? "").toUpperCase();
  return normalized === "SUCCESS" || normalized === "DONE" || normalized === "READY";
}

function isMasteredStatus(status?: string): boolean {
  const normalized = String(status ?? "").toUpperCase();
  return normalized === "MASTERED" || normalized === "DONE" || normalized === "COMPLETE";
}

export async function fetchDashboardMetrics(): Promise<DashboardMetrics> {
  const [questionsResult, materialsResult, wrongBooksResult, activeSessionResult] = await Promise.allSettled([
    listQuestions(0, 50),
    listMaterials(),
    listWrongBooks(),
    getActiveSession()
  ]);

  const questions = questionsResult.status === "fulfilled" ? questionsResult.value : [];
  const materials = materialsResult.status === "fulfilled" ? materialsResult.value : [];
  const wrongBooks = wrongBooksResult.status === "fulfilled" ? wrongBooksResult.value : [];
  const activeSession = activeSessionResult.status === "fulfilled" ? activeSessionResult.value : null;

  const recentInterviewQuestions = questions.filter((question) =>
    String(question.questionType ?? "").toLowerCase().includes("interview")
  ).length;
  const parsedMaterials = materials.filter((material) => isParseSuccess(material.parseStatus)).length;
  const masteredPoints = wrongBooks.filter((item) => isMasteredStatus(item.masteryStatus)).length;
  const pendingWrongReviews = Math.max(wrongBooks.length - masteredPoints, 0);
  const activeInterviewSessions = activeSession ? 1 : 0;

  return {
    completedTests: questions.length,
    mockInterviews: recentInterviewQuestions + activeInterviewSessions,
    masteredPoints,
    pendingWrongReviews,
    parsedMaterials,
    recentInterviewQuestions,
    activeInterviewSessions
  };
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

export type WrongBookItem = {
  id: number;
  questionId: number;
  question: string;
  questionType?: string;
  referenceAnswer?: string | null;
  analysisText?: string | null;
  difficulty?: number | null;
  masteryStatus: string;
  wrongCount: number;
  reviewCount: number;
  notes?: string | null;
};

export type AddWrongBookPayload = {
  questionId: number;
};

export type UpdateMasteryPayload = {
  masteryStatus: string;
};

export async function listWrongBooks(masteryStatus?: string): Promise<WrongBookItem[]> {
  const url = masteryStatus
    ? `/api/v1/wrong-books?masteryStatus=${encodeURIComponent(masteryStatus)}`
    : "/api/v1/wrong-books";
  const response = await fetchWithAuth(url, { cache: "no-store" });
  return unwrap<WrongBookItem[]>(response, "获取错题本失败");
}

export async function addWrongBook(payload: AddWrongBookPayload): Promise<WrongBookItem> {
  const response = await fetchWithAuth("/api/v1/wrong-books", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
  return unwrap<WrongBookItem>(response, "添加错题失败");
}

export async function updateMasteryStatus(id: number, payload: UpdateMasteryPayload): Promise<WrongBookItem> {
  const response = await fetchWithAuth(`/api/v1/wrong-books/${id}/mastery`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
  return unwrap<WrongBookItem>(response, "更新掌握状态失败");
}

export async function retryParseMaterial(materialId: number): Promise<UploadMaterialResult> {
  const response = await fetchWithAuth(`/api/v1/materials/${materialId}/retry-parse`, {
    method: "POST"
  });
  return unwrap<UploadMaterialResult>(response, "重试解析失败");
}

export async function deleteMaterial(materialId: number): Promise<void> {
  const response = await fetchWithAuth(`/api/v1/materials/${materialId}`, {
    method: "DELETE"
  });
  await unwrap<void>(response, "删除资料失败");
}

export async function deleteWrongBook(id: number): Promise<void> {
  const response = await fetchWithAuth(`/api/v1/wrong-books/${id}`, {
    method: "DELETE"
  });
  return unwrap<void>(response, "删除错题失败");
}

export type InterviewSession = {
  id: number;
  userId: number;
  position: string;
  difficulty: number;
  status: string;
  contextSnapshot?: string | null;
  createdAt?: string;
  updatedAt?: string;
};

export async function createOrGetSession(position: string, difficulty: number): Promise<InterviewSession> {
  const response = await fetchWithAuth("/api/v1/interviews/sessions", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ position, difficulty })
  });
  return unwrap<InterviewSession>(response, "创建会话失败");
}

export async function getActiveSession(): Promise<InterviewSession | null> {
  const response = await fetchWithAuth("/api/v1/interviews/sessions/active", { cache: "no-store" });
  if (response.status === 404) return null;
  return unwrap<InterviewSession>(response, "获取会话失败");
}

export async function resumeSession(sessionId: number): Promise<InterviewSession> {
  const response = await fetchWithAuth(`/api/v1/interviews/${sessionId}/resume`, {
    method: "POST"
  });
  return unwrap<InterviewSession>(response, "恢复会话失败");
}

export type ChatMessagePayload = {
  role: string;
  content: string;
};

export type ChatResponse = {
  reply: string;
};

export async function sendChatMessage(message: string, context?: ChatMessagePayload[]): Promise<ChatResponse> {
  const response = await fetchWithAuth("/api/v1/chat", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ message, context })
  });
  return unwrap<ChatResponse>(response, "对话失败");
}

export type LlmSettingsView = {
  providerName: string;
  modelName: string;
  baseUrl: string;
  hasApiKey: boolean;
  enabled: boolean;
  source: string;
};

export type UpdateLlmSettingsPayload = {
  providerName: string;
  modelName: string;
  baseUrl?: string;
  apiKey?: string;
  enabled?: boolean;
};

export async function getLlmSettings(): Promise<LlmSettingsView> {
  const response = await fetchWithAuth("/api/v1/llm/settings", { cache: "no-store" });
  return unwrap<LlmSettingsView>(response, "获取模型设置失败");
}

export async function updateLlmSettings(payload: UpdateLlmSettingsPayload): Promise<LlmSettingsView> {
  const response = await fetchWithAuth("/api/v1/llm/settings", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
  return unwrap<LlmSettingsView>(response, "保存模型设置失败");
}

export function toErrorMessage(error: unknown, fallback = "请求失败，请稍后重试"): string {
  if (error instanceof Error && error.message.trim()) {
    return error.message;
  }
  return fallback;
}
