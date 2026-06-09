import axios from "axios";

export type ApiError = {
  code: string;
  message: string;
  correlationId?: string;
  details?: Array<{ field: string; reason: string }>;
};

export const apiClient = axios.create({
  baseURL: "/api/v1",
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
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
