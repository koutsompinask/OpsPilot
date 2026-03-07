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
