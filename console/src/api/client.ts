const TOKEN_KEY = "heimdall_token";

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token);
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const token = getToken();
  const res = await fetch(path, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(init?.headers ?? {})
    }
  });
  if (!res.ok) {
    throw new Error(`${init?.method ?? "GET"} ${path} failed: ${res.status}`);
  }
  return res.json() as Promise<T>;
}

export const api = {
  devLogin: (email: string) =>
    request<{ token: string }>("/auth/dev-login", { method: "POST", body: JSON.stringify({ email }) }),

  listPrs: (repoId: string) =>
    request<PrOverview[]>(`/api/v1/repositories/${repoId}/pull-requests`),

  prDetail: (prId: string) =>
    request<PrDetailResponse>(`/api/v1/pull-requests/${prId}`),

  riskEvolution: (prId: string) =>
    request<RiskPoint[]>(`/api/v1/pull-requests/${prId}/risk-evolution`)
};

export interface PrOverview {
  pull_request_id: string;
  repository_id: string;
  number: number;
  title: string;
  latest_run_id: string;
  head_sha: string;
  run_state: string;
  verdict: "MERGE" | "REVIEW" | "BLOCK" | null;
  score: number | null;
  band: string | null;
  finding_count: number;
}

export interface Finding {
  id: string;
  claim_class: string;
  title: string;
  claim: string;
  severity: "critical" | "high" | "medium" | "low" | "info";
  confidence: number;
  verification_status: "VERIFIED" | "UNVERIFIED" | "REJECTED" | "FALSE_POSITIVE" | null;
  verification_reason: string | null;
}

export interface AuditEntry {
  id: number;
  event_type: string;
  actor_type: string;
  created_at: string;
  payload: unknown;
}

export interface PrDetailResponse {
  overview: PrOverview;
  findings: Finding[];
  audit: AuditEntry[];
}

export interface RiskPoint {
  head_sha: string;
  created_at: string;
  score: number;
  band: string;
}
