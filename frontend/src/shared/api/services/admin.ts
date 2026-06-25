import { apiClient } from "../axios-client";
import type { CurrentUser, AuditLog } from "../types";

/** Cursor-based page envelope used by admin list endpoints. */
type ApiPage<T> = { data: T[]; page: { limit: number; nextCursor?: string | null; hasMore: boolean } };

export const adminService = {
  /**
   * Lists all platform users.
   * ADMIN role required — the backend returns HTTP 403 for USER callers.
   */
  listUsers: (params?: { limit?: number }) =>
    apiClient.get<ApiPage<CurrentUser>>("/admin/users", { params }).then((r) => r.data),

  /**
   * Toggles a user's activation status.
   * Disabled users are immediately blocked from the API; their in-flight jobs are unaffected.
   */
  setUserStatus: (userId: string, status: "ACTIVE" | "DISABLED") =>
    apiClient.patch<{ userId: string; status: string }>(`/admin/users/${userId}/status`, { status }).then((r) => r.data),

  /**
   * Returns an immutable audit trail in reverse-chronological order.
   * Entries are never deleted — this endpoint is append-only on the backend.
   */
  listAuditLogs: (params?: { limit?: number }) =>
    apiClient.get<ApiPage<AuditLog>>("/audit-logs", { params }).then((r) => r.data),
};
