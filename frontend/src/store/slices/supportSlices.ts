import { createAsyncThunk, createSlice, type PayloadAction } from "@reduxjs/toolkit";
import type { AuditLog, CurrentUser, QueueSnapshot } from "../../shared/api/types";
import { adminService } from "../../shared/api/services/admin";
import { mockState } from "../mock-data";

/** Loads all platform users into `state.admin.users`. ADMIN role required. */
export const fetchUsers = createAsyncThunk("admin/fetchUsers", () =>
  adminService.listUsers({ limit: 200 }).then((r) => r.data),
);

/**
 * Toggles a user's activation status.
 * The optimistic reducer update runs immediately on fulfillment;
 * the backend enforces the restriction on the next API call from that user.
 */
export const setUserStatusAsync = createAsyncThunk(
  "admin/setUserStatus",
  async ({ userId, status }: { userId: string; status: "ACTIVE" | "DISABLED" }) => {
    await adminService.setUserStatus(userId, status);
    return { userId, status };
  },
);

/** Loads the audit trail into `state.admin.audit`. Entries are immutable and append-only. */
export const fetchAuditLogs = createAsyncThunk("admin/fetchAuditLogs", () =>
  adminService.listAuditLogs({ limit: 200 }).then((r) => r.data),
);

export const notificationSlice = createSlice({
  name: "notifications",
  initialState: { items: mockState.notifications },
  reducers: {
    markRead(state, action: PayloadAction<string>) {
      const notification = state.items.find((item) => item.notificationId === action.payload);
      if (notification) notification.status = "READ";
    },
  },
});

export const adminSlice = createSlice({
  name: "admin",
  initialState: {
    users: [] as CurrentUser[],
    audit: [] as AuditLog[],
    queue: { runningCount: 0, runningLimit: 1, queuedCount: 0, items: [] } as QueueSnapshot,
    loading: false,
    error: undefined as string | undefined,
  },
  reducers: {
    setUserStatus(state, action: PayloadAction<{ userId: string; status: "ACTIVE" | "DISABLED" }>) {
      const user = state.users.find((item) => item.userId === action.payload.userId);
      if (user) user.status = action.payload.status;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchUsers.pending, (state) => { state.loading = true; state.error = undefined; })
      .addCase(fetchUsers.fulfilled, (state, action) => { state.loading = false; state.users = action.payload; })
      .addCase(fetchUsers.rejected, (state, action) => { state.loading = false; state.error = action.error.message; })

      .addCase(setUserStatusAsync.fulfilled, (state, action) => {
        const user = state.users.find((u) => u.userId === action.payload.userId);
        if (user) user.status = action.payload.status;
      })

      .addCase(fetchAuditLogs.pending, (state) => { state.loading = true; })
      .addCase(fetchAuditLogs.fulfilled, (state, action) => { state.loading = false; state.audit = action.payload; })
      .addCase(fetchAuditLogs.rejected, (state, action) => { state.loading = false; state.error = action.error.message; });
  },
});

export const themeSlice = createSlice({
  name: "theme",
  initialState: { mode: document.documentElement.classList.contains("dark") ? "dark" : "light" },
  reducers: {
    setTheme(state, action: PayloadAction<"light" | "dark">) {
      state.mode = action.payload;
      document.documentElement.classList.toggle("dark", action.payload === "dark");
    },
  },
});
