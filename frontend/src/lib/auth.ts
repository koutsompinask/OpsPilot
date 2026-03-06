export type TokenResponse = {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  tokenType: string;
};

export type AuthClaims = {
  sub?: string;
  tenant_id?: string;
  role?: "TENANT_ADMIN" | "TENANT_MEMBER";
  email?: string;
  exp?: number;
};

const ACCESS_TOKEN_KEY = "opspilot.access_token";
const REFRESH_TOKEN_KEY = "opspilot.refresh_token";

export function saveTokens(tokens: TokenResponse) {
  localStorage.setItem(ACCESS_TOKEN_KEY, tokens.accessToken);
  localStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken);
}

export function clearTokens() {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
}

export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY);
}

function decodePayload(token: string): AuthClaims | null {
  const parts = token.split(".");
  if (parts.length < 2) {
    return null;
  }

  try {
    const normalized = parts[1].replace(/-/g, "+").replace(/_/g, "/");
    const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, "=");
    return JSON.parse(window.atob(padded)) as AuthClaims;
  } catch {
    return null;
  }
}

export function getAuthClaims(): AuthClaims | null {
  const token = getAccessToken();
  if (!token) {
    return null;
  }
  return decodePayload(token);
}

export function isAccessTokenValid(): boolean {
  const claims = getAuthClaims();
  if (!claims?.exp) {
    return Boolean(getAccessToken());
  }

  const nowInSeconds = Math.floor(Date.now() / 1000);
  return claims.exp > nowInSeconds;
}
