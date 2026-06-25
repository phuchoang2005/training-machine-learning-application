import { createAsyncThunk, createSlice, type PayloadAction } from "@reduxjs/toolkit";
import type { CurrentUser } from "../../shared/api/types";
import { authService } from "../../shared/api/services/auth";
import { sampleAccounts, storedAccount, toCurrentUser, type LoginAccount } from "../session";

/**
 * Rehydrates the authenticated user from the backend on app startup.
 * On success updates `currentUser` with the authoritative backend record.
 * On failure (backend unreachable) silently keeps the dev-mock account already in state
 * so the app stays usable during local development without the backend running.
 */
export const fetchCurrentUser = createAsyncThunk("auth/me", () => authService.getMe());

const account = storedAccount();

export const authSlice = createSlice({
  name: "auth",
  initialState: {
    currentUser: account ? toCurrentUser(account) : null as CurrentUser | null,
    accounts: sampleAccounts,
    status: "idle" as "idle" | "loading" | "authenticated" | "failed",
    error: undefined as string | undefined,
  },
  reducers: {
    login(state, action: PayloadAction<{ email: string; password: string }>) {
      const match = state.accounts.find((item) => item.email.toLowerCase() === action.payload.email.toLowerCase());
      if (!match || match.password !== action.payload.password) return fail(state, "Email or password does not match a development account.");
      state.currentUser = toCurrentUser(match);
      state.status = "authenticated";
      state.error = undefined;
      sessionStorage.setItem("ai-training-session-email", match.email);
    },
    registerAccount(state, action: PayloadAction<LoginAccount>) {
      const exists = state.accounts.some((item) => item.email.toLowerCase() === action.payload.email.toLowerCase());
      if (exists) return fail(state, "An account with this email already exists.");
      state.accounts.push(action.payload);
      state.currentUser = toCurrentUser(action.payload);
      state.status = "authenticated";
      state.error = undefined;
      sessionStorage.setItem("ai-training-session-email", action.payload.email);
    },
    logout(state) {
      state.currentUser = null;
      state.status = "idle";
      state.error = undefined;
      sessionStorage.removeItem("ai-training-session-email");
    },
    setRole(state, action: PayloadAction<CurrentUser["role"]>) {
      if (state.currentUser) state.currentUser.role = action.payload;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchCurrentUser.pending, (state) => { state.status = "loading"; })
      .addCase(fetchCurrentUser.fulfilled, (state, action) => {
        state.currentUser = action.payload;
        state.status = "authenticated";
        state.error = undefined;
      })
      .addCase(fetchCurrentUser.rejected, (state) => {
        // Backend unreachable — keep the dev-mock account already in state
        if (state.currentUser) state.status = "authenticated";
        else state.status = "idle";
      });
  },
});

function fail(state: { status: string; error?: string }, message: string) {
  state.status = "failed";
  state.error = message;
}
