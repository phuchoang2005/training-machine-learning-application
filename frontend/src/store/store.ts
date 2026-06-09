import { configureStore, createSlice, nanoid, type PayloadAction } from "@reduxjs/toolkit";
import type {
  Artifact,
  AuditLog,
  CurrentUser,
  JobDetail,
  JobStatus,
  LogEvent,
  Notification,
  ProjectConfigContent,
  ProjectDetail,
  QueueSnapshot,
} from "../shared/api/types";
import { mockState } from "./mock-data";

export type LoginAccount = {
  email: string;
  password: string;
  fullName: string;
  role: CurrentUser["role"];
};

const sampleAccounts: LoginAccount[] = [
  { email: "user@example.com", password: "password", fullName: "Development User", role: "USER" },
  { email: "admin@example.com", password: "password", fullName: "Development Admin", role: "ADMIN" },
];

const storedEmail = sessionStorage.getItem("ai-training-session-email");
const storedAccount = sampleAccounts.find((account) => account.email === storedEmail);
const toCurrentUser = (account: LoginAccount): CurrentUser => ({
  userId: account.email === "admin@example.com" ? "u-admin" : account.email === "user@example.com" ? "u-100" : nanoid(),
  email: account.email,
  fullName: account.fullName,
  role: account.role,
  status: "ACTIVE",
  lastLoginAt: new Date().toISOString(),
});

const authSlice = createSlice({
  name: "auth",
  initialState: {
    currentUser: storedAccount ? toCurrentUser(storedAccount) : null as CurrentUser | null,
    accounts: sampleAccounts,
    status: "idle" as "idle" | "authenticated" | "failed",
    error: undefined as string | undefined,
  },
  reducers: {
    login(state, action: PayloadAction<{ email: string; password: string }>) {
      const account = state.accounts.find((item) => item.email.toLowerCase() === action.payload.email.toLowerCase());
      if (!account || account.password !== action.payload.password) {
        state.status = "failed";
        state.error = "Email or password does not match a development account.";
        return;
      }
      state.currentUser = toCurrentUser(account);
      state.status = "authenticated";
      state.error = undefined;
      sessionStorage.setItem("ai-training-session-email", account.email);
    },
    registerAccount(state, action: PayloadAction<LoginAccount>) {
      const exists = state.accounts.some((item) => item.email.toLowerCase() === action.payload.email.toLowerCase());
      if (exists) {
        state.status = "failed";
        state.error = "An account with this email already exists.";
        return;
      }
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
});

const projectSlice = createSlice({
  name: "projects",
  initialState: {
    items: mockState.projects,
    configsByProjectId: mockState.configsByProjectId,
    activeStatusFilter: "ALL" as JobStatus | "ALL",
  },
  reducers: {
    registerProject: {
      reducer(state, action: PayloadAction<ProjectDetail>) {
        state.items.unshift(action.payload);
        state.configsByProjectId[action.payload.projectId] = {
          configId: nanoid(),
          configPath: "training.yaml",
          contentHash: "draft",
          yamlContent: mockState.defaultYaml,
        };
      },
      prepare(project: Omit<ProjectDetail, "projectId" | "owner" | "createdAt" | "updatedAt" | "latestJobStatus">) {
        const now = new Date().toISOString();
        return {
          payload: {
            ...project,
            projectId: nanoid(),
            owner: mockState.currentUser,
            createdAt: now,
            updatedAt: now,
            latestJobStatus: "CREATED" as const,
          },
        };
      },
    },
    saveConfig(state, action: PayloadAction<{ projectId: string; config: ProjectConfigContent }>) {
      state.configsByProjectId[action.payload.projectId] = action.payload.config;
    },
    setProjectStatus(state, action: PayloadAction<{ projectId: string; status: JobStatus; owner: string }>) {
      const project = state.items.find((item) => item.projectId === action.payload.projectId);
      if (project) {
        project.latestJobStatus = action.payload.status;
        project.lastTrainingOwner = action.payload.owner;
        project.lastTrainingTime = new Date().toISOString();
      }
    },
    setStatusFilter(state, action: PayloadAction<JobStatus | "ALL">) {
      state.activeStatusFilter = action.payload;
    },
  },
});

const jobSlice = createSlice({
  name: "jobs",
  initialState: {
    items: mockState.jobs,
    logsByJobId: mockState.logsByJobId,
    artifactsByJobId: mockState.artifactsByJobId,
    queue: mockState.queue,
    connection: "CONNECTED" as "CONNECTED" | "RECONNECTING" | "FALLBACK POLLING" | "DISCONNECTED",
  },
  reducers: {
    startJob: {
      reducer(state, action: PayloadAction<JobDetail>) {
        state.items.unshift(action.payload);
        state.logsByJobId[action.payload.jobId] = [
          {
            logEventId: nanoid(),
            sequenceNo: 1,
            streamType: "STDOUT",
            message: "Training request accepted. Immutable configuration snapshot created.",
            emittedAt: action.payload.createdAt,
          },
        ];
      },
      prepare(project: ProjectDetail) {
        const now = new Date().toISOString();
        return {
          payload: {
            jobId: nanoid(),
            projectId: project.projectId,
            projectName: project.projectName,
            triggeredBy: mockState.currentUser,
            status: "QUEUED" as const,
            queuePosition: 2,
            progress: { available: false },
            retryAttempt: 0,
            createdAt: now,
            queuedAt: now,
          },
        };
      },
    },
    cancelJob(state, action: PayloadAction<{ jobId: string; reason: string }>) {
      const job = state.items.find((item) => item.jobId === action.payload.jobId);
      if (job) {
        job.status = "CANCELLED";
        job.endedAt = new Date().toISOString();
        job.failureReason = action.payload.reason || "Cancelled by user request";
      }
    },
    retryJob(state, action: PayloadAction<string>) {
      const original = state.items.find((item) => item.jobId === action.payload);
      if (!original) return;
      const now = new Date().toISOString();
      state.items.unshift({
        ...original,
        jobId: nanoid(),
        status: "QUEUED",
        queuePosition: 1,
        retryOfJobId: original.jobId,
        retryAttempt: original.retryAttempt + 1,
        createdAt: now,
        queuedAt: now,
        startedAt: undefined,
        endedAt: undefined,
        failureReason: undefined,
        progress: { available: false },
      });
    },
    appendLog(state, action: PayloadAction<{ jobId: string; event: LogEvent }>) {
      const lines = state.logsByJobId[action.payload.jobId] ?? [];
      if (!lines.some((line) => line.logEventId === action.payload.event.logEventId)) {
        state.logsByJobId[action.payload.jobId] = [...lines, action.payload.event].slice(-500);
      }
    },
    setConnection(state, action: PayloadAction<typeof state.connection>) {
      state.connection = action.payload;
    },
  },
});

const notificationSlice = createSlice({
  name: "notifications",
  initialState: { items: mockState.notifications },
  reducers: {
    markRead(state, action: PayloadAction<string>) {
      const notification = state.items.find((item) => item.notificationId === action.payload);
      if (notification) notification.status = "READ";
    },
  },
});

const adminSlice = createSlice({
  name: "admin",
  initialState: { users: mockState.users, audit: mockState.audit, queue: mockState.queue as QueueSnapshot },
  reducers: {
    setUserStatus(state, action: PayloadAction<{ userId: string; status: "ACTIVE" | "DISABLED" }>) {
      const user = state.users.find((item) => item.userId === action.payload.userId);
      if (user) user.status = action.payload.status;
    },
  },
});

const themeSlice = createSlice({
  name: "theme",
  initialState: { mode: document.documentElement.classList.contains("dark") ? "dark" : "light" },
  reducers: {
    setTheme(state, action: PayloadAction<"light" | "dark">) {
      state.mode = action.payload;
      document.documentElement.classList.toggle("dark", action.payload === "dark");
    },
  },
});

export const store = configureStore({
  reducer: {
    auth: authSlice.reducer,
    projects: projectSlice.reducer,
    jobs: jobSlice.reducer,
    notifications: notificationSlice.reducer,
    admin: adminSlice.reducer,
    theme: themeSlice.reducer,
  },
});

export const actions = {
  ...authSlice.actions,
  ...projectSlice.actions,
  ...jobSlice.actions,
  ...notificationSlice.actions,
  ...adminSlice.actions,
  ...themeSlice.actions,
};

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
export type { Artifact, AuditLog, ProjectConfigContent };
