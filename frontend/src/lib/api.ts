import { clearTokens, getAccessToken, getRefreshToken, saveTokens, type TokenResponse } from "./auth";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

type ApiErrorBody = {
  code?: string;
  message?: string;
};

type RequestOptions = {
  auth?: boolean;
  retryOnUnauthorized?: boolean;
};

export type RegisterRequest = {
  tenantName: string;
  adminName: string;
  email: string;
  password: string;
};

export type TenantResponse = {
  id: string;
  name: string;
  settingsJson: string | null;
};

export type UpdateTenantRequest = {
  name: string;
  settingsJson: string;
};

export type TenantUserRole = "TENANT_ADMIN" | "TENANT_MEMBER";

export type UserResponse = {
  userId: string;
  tenantId: string;
  displayName: string;
  email: string;
  role: TenantUserRole;
};

export type CreateUserRequest = {
  displayName: string;
  email: string;
  password: string;
  role: TenantUserRole;
};

export type DocumentStatus = "PROCESSING" | "READY" | "FAILED";

export type DocumentResponse = {
  id: string;
  filename: string;
  contentType: string;
  status: DocumentStatus;
  chunkCount: number | null;
  errorMessage: string | null;
  createdAt: string;
  updatedAt: string;
};

export type ChatAskRequest = {
  question: string;
  topK?: number;
};

export type ChatSourceResponse = {
  document: string;
  chunkId: string;
};

export type ChatEvidenceResponse = {
  document: string;
  chunkId: string;
  sectionTitle: string;
  snippet: string;
  relevanceScore: number;
};

export type ChatAskResponse = {
  answer: string;
  reasoningSummary: string;
  confidence: number;
  sources: ChatSourceResponse[];
  evidence: ChatEvidenceResponse[];
  answerMode: string;
  ticketCreated: boolean;
};

function asString(value: unknown, fallback = ""): string {
  return typeof value === "string" ? value : fallback;
}

function asNumber(value: unknown, fallback = 0): number {
  return typeof value === "number" && Number.isFinite(value) ? value : fallback;
}

function normalizeChatSources(value: unknown): ChatSourceResponse[] {
  if (!Array.isArray(value)) {
    return [];
  }

  return value.map((item) => {
    const source = item as Partial<ChatSourceResponse> | null;
    return {
      document: asString(source?.document, "Unknown document"),
      chunkId: asString(source?.chunkId, "chunk-unknown"),
    };
  });
}

function normalizeChatEvidence(value: unknown): ChatEvidenceResponse[] {
  if (!Array.isArray(value)) {
    return [];
  }

  return value.map((item) => {
    const evidence = item as Partial<ChatEvidenceResponse> | null;
    return {
      document: asString(evidence?.document, "Unknown document"),
      chunkId: asString(evidence?.chunkId, "chunk-unknown"),
      sectionTitle: asString(evidence?.sectionTitle, "General"),
      snippet: asString(evidence?.snippet),
      relevanceScore: asNumber(evidence?.relevanceScore),
    };
  });
}

function normalizeChatAskResponse(value: unknown): ChatAskResponse {
  const response = (value ?? {}) as Partial<ChatAskResponse>;
  return {
    answer: asString(response.answer, "No answer was returned."),
    reasoningSummary: asString(
      response.reasoningSummary,
      "The assistant did not return a reasoning summary for this response.",
    ),
    confidence: asNumber(response.confidence),
    sources: normalizeChatSources(response.sources),
    evidence: normalizeChatEvidence(response.evidence),
    answerMode: asString(response.answerMode, "extractive-grounded"),
    ticketCreated: response.ticketCreated === true,
  };
}

export type TicketStatus = "OPEN" | "IN_PROGRESS" | "RESOLVED";

export type TicketOrigin = "CHAT_LOW_CONFIDENCE" | "MANUAL";

export type TicketResponse = {
  id: string;
  tenantId: string;
  createdByUserId: string;
  createdByEmail: string;
  origin: TicketOrigin;
  status: TicketStatus;
  question: string;
  answer: string | null;
  confidence: number | null;
  sourceCount: number;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
};

async function parseErrorMessage(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as ApiErrorBody;
    return body.message ?? `Request failed with status ${response.status}`;
  } catch {
    return `Request failed with status ${response.status}`;
  }
}

async function refreshAccessToken(): Promise<boolean> {
  const refreshToken = getRefreshToken();
  if (!refreshToken) {
    return false;
  }

  const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ refreshToken }),
  });

  if (!response.ok) {
    clearTokens();
    return false;
  }

  const tokens = (await response.json()) as TokenResponse;
  saveTokens(tokens);
  return true;
}

async function performRequest(
  path: string,
  init: RequestInit = {},
  options: RequestOptions = {},
): Promise<Response> {
  const { auth = false, retryOnUnauthorized = true } = options;
  const headers = new Headers(init.headers ?? {});

  if (init.body && !(init.body instanceof FormData) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  if (auth) {
    const accessToken = getAccessToken();
    if (accessToken) {
      headers.set("Authorization", `Bearer ${accessToken}`);
    }
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers,
  });

  if (response.status === 401 && auth && retryOnUnauthorized) {
    const refreshed = await refreshAccessToken();
    if (refreshed) {
      return performRequest(path, init, { ...options, retryOnUnauthorized: false });
    }
  }

  return response;
}

async function requestJson<T>(
  path: string,
  init: RequestInit = {},
  options: RequestOptions = {},
): Promise<T> {
  const response = await performRequest(path, init, options);

  if (!response.ok) {
    throw new Error(await parseErrorMessage(response));
  }

  return (await response.json()) as T;
}

async function requestVoid(
  path: string,
  init: RequestInit = {},
  options: RequestOptions = {},
): Promise<void> {
  const response = await performRequest(path, init, options);

  if (!response.ok) {
    throw new Error(await parseErrorMessage(response));
  }
}

export async function register(payload: RegisterRequest): Promise<TokenResponse> {
  return requestJson<TokenResponse>("/auth/register", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function login(email: string, password: string): Promise<TokenResponse> {
  return requestJson<TokenResponse>("/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });
}

export async function getMyTenant(): Promise<TenantResponse> {
  return requestJson<TenantResponse>("/tenants/me", { method: "GET" }, { auth: true });
}

export async function updateMyTenant(payload: UpdateTenantRequest): Promise<TenantResponse> {
  return requestJson<TenantResponse>("/tenants/me", {
    method: "PUT",
    body: JSON.stringify(payload),
  }, { auth: true });
}

export async function listUsers(): Promise<UserResponse[]> {
  return requestJson<UserResponse[]>("/users", { method: "GET" }, { auth: true });
}

export async function createUser(payload: CreateUserRequest): Promise<UserResponse> {
  return requestJson<UserResponse>("/users", {
    method: "POST",
    body: JSON.stringify(payload),
  }, { auth: true });
}

export async function uploadDocument(file: File): Promise<DocumentResponse> {
  const formData = new FormData();
  formData.append("file", file);

  return requestJson<DocumentResponse>("/documents", {
    method: "POST",
    body: formData,
  }, { auth: true });
}

export async function listDocuments(): Promise<DocumentResponse[]> {
  return requestJson<DocumentResponse[]>("/documents", { method: "GET" }, { auth: true });
}

export async function getDocument(documentId: string): Promise<DocumentResponse> {
  return requestJson<DocumentResponse>(`/documents/${documentId}`, { method: "GET" }, { auth: true });
}

export async function deleteDocument(documentId: string): Promise<void> {
  return requestVoid(`/documents/${documentId}`, { method: "DELETE" }, { auth: true });
}

export async function askChat(payload: ChatAskRequest): Promise<ChatAskResponse> {
  const response = await requestJson<unknown>("/chat/ask", {
    method: "POST",
    body: JSON.stringify(payload),
  }, { auth: true });
  return normalizeChatAskResponse(response);
}

export async function listTickets(): Promise<TicketResponse[]> {
  return requestJson<TicketResponse[]>("/tickets", { method: "GET" }, { auth: true });
}

export async function updateTicketStatus(ticketId: string, status: TicketStatus): Promise<TicketResponse> {
  return requestJson<TicketResponse>(`/tickets/${ticketId}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status }),
  }, { auth: true });
}
