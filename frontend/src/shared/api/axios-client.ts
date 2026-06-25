import axios from "axios";

/**
 * Normalised API error shape.
 * All HTTP and network failures are mapped to this type by the response interceptor
 * so callers never need to inspect raw Axios error structures.
 */
export type ApiError = {
  code: string;
  message: string;
  /** Correlation ID for cross-referencing backend logs; present on 4xx/5xx responses. */
  correlationId?: string;
  /** Field-level validation failures returned by the backend on HTTP 400. */
  details?: Array<{ field: string; reason: string }>;
};

/**
 * Configured Axios instance for all REST calls to `/api/v1`.
 *
 * Request interceptor — adds `Authorization: Bearer <email>` from sessionStorage.
 * In dev-auth mode the email IS the bearer token; the backend resolves it to a seeded account.
 * When real OIDC is enabled this interceptor is replaced with a proper token exchange — no
 * call-site changes required.
 *
 * Response interceptor — normalises every failure to `ApiError` so `createAsyncThunk`
 * callers receive a consistent rejection payload.
 */
export const apiClient = axios.create({
  baseURL: "/api/v1",
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});

apiClient.interceptors.request.use((config) => {
  const token = sessionStorage.getItem("ai-training-session-email");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const payload = error.response?.data?.error;
    const normalized: ApiError = {
      code: payload?.code ?? `HTTP_${error.response?.status ?? "NETWORK"}`,
      message: payload?.message ?? error.message ?? "Unexpected platform error",
      correlationId: payload?.correlationId ?? error.response?.headers?.["x-correlation-id"],
      details: payload?.details,
    };
    return Promise.reject(normalized);
  },
);
