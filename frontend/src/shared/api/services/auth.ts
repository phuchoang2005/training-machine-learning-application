import { apiClient } from "../axios-client";
import type { CurrentUser } from "../types";

export const authService = {
  /**
   * Returns the authenticated user for the current bearer token.
   * In dev-auth mode the token is the user's email (see axios-client.ts interceptor).
   * Called on app startup to rehydrate session state from the backend.
   */
  getMe: () => apiClient.get<CurrentUser>("/auth/me").then((r) => r.data),

  /** Invalidates the server-side session. Frontend clears sessionStorage separately. */
  logout: () => apiClient.post("/auth/logout"),
};
