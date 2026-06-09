import { useEffect, useMemo, useState } from "react";
import { Link, Navigate, Outlet, Route, Routes, useLocation, useNavigate, useParams } from "react-router-dom";
import { motion, useReducedMotion } from "framer-motion";
import {
  Activity,
  AlertTriangle,
  Bell,
  Check,
  Clock,
  Download,
  FileCode2,
  FolderKanban,
  Gauge,
  History,
  LogOut,
  Menu,
  Moon,
  Play,
  Plus,
  RefreshCcw,
  Search,
  Shield,
  Sparkles,
  Square,
  Sun,
  Upload,
  Users,
  X,
} from "lucide-react";
import { actions } from "../store/store";
import { useAppDispatch, useAppSelector } from "../store/hooks";
import type { JobDetail, JobStatus, ProjectDetail, StreamType } from "../shared/api/types";
import { pageVariants, panelVariants } from "../shared/motion/variants";
import type { ReactNode } from "react";

const statusOrder: Array<JobStatus | "ALL"> = ["ALL", "CREATED", "QUEUED", "RUNNING", "SUCCESS", "FAILED", "CANCELLED", "RETRYING"];

export function App() {
  const dispatch = useAppDispatch();

  useEffect(() => {
    const media = window.matchMedia("(prefers-color-scheme: dark)");
    const syncTheme = () => dispatch(actions.setTheme(media.matches ? "dark" : "light"));
    syncTheme();
    media.addEventListener("change", syncTheme);
    return () => media.removeEventListener("change", syncTheme);
  }, [dispatch]);

  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterAccountPage />} />
      <Route path="/login/callback" element={<LoginCallbackPage />} />
      <Route element={<RequireAuth />}>
        <Route element={<AppShell />}>
          <Route path="/" element={<Navigate to="/projects" replace />} />
          <Route path="/projects" element={<ProjectDashboardPage />} />
          <Route path="/projects/new" element={<RegisterProjectPage />} />
          <Route path="/projects/:projectId" element={<ProjectDetailPage />} />
          <Route path="/projects/:projectId/configs/:configId" element={<ProjectDetailPage initialTab="config" />} />
          <Route path="/projects/:projectId/jobs/:jobId" element={<NestedJobRedirect />} />
          <Route path="/jobs/:jobId" element={<JobDetailPage />} />
          <Route path="/notifications" element={<NotificationListPage />} />
          <Route path="/admin/users" element={<AdminGuard><AdminUsersPage /></AdminGuard>} />
          <Route path="/admin/queue" element={<AdminGuard><AdminQueuePage /></AdminGuard>} />
          <Route path="/admin/audit" element={<AdminGuard><AdminAuditPage /></AdminGuard>} />
        </Route>
      </Route>
      <Route path="/403" element={<ErrorPage code="403" title="Access restricted" message="This route is only available when the backend authorizes the current role or resource relationship." />} />
      <Route path="*" element={<ErrorPage code="404" title="Route not found" message="The requested workspace route does not exist." />} />
    </Routes>
  );
}

function RequireAuth() {
  const user = useAppSelector((state) => state.auth.currentUser);
  const location = useLocation();
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  return <Outlet />;
}

function AdminGuard({ children }: { children: ReactNode }) {
  const user = useAppSelector((state) => state.auth.currentUser);
  return user?.role === "ADMIN" ? <>{children}</> : <Navigate to="/403" replace />;
}

function NestedJobRedirect() {
  const { jobId } = useParams();
  return <Navigate to={`/jobs/${jobId}`} replace />;
}

function AppShell() {
  const [drawerOpen, setDrawerOpen] = useState(false);
  const location = useLocation();
  const user = useAppSelector((state) => state.auth.currentUser);
  const unread = useAppSelector((state) => state.notifications.items.filter((item) => item.status !== "READ").length);
  const connection = useAppSelector((state) => state.jobs.connection);
  if (!user) return <Navigate to="/login" replace />;

  useEffect(() => setDrawerOpen(false), [location.pathname]);

  return (
    <div className="app-shell">
      <CelestialBackground />
      <aside className="sidebar">
        <Brand />
        <NavLinks role={user.role} />
        <UserPanel />
      </aside>
      {drawerOpen && (
        <div className="drawer-backdrop" onClick={() => setDrawerOpen(false)}>
          <aside className="drawer" onClick={(event) => event.stopPropagation()}>
            <button className="icon-button drawer-close" aria-label="Close navigation" onClick={() => setDrawerOpen(false)}><X size={18} /></button>
            <Brand />
            <NavLinks role={user.role} />
            <UserPanel />
          </aside>
        </div>
      )}
      <div className="main-frame">
        <header className="topbar">
          <button className="icon-button mobile-menu" aria-label="Open navigation" onClick={() => setDrawerOpen(true)}><Menu size={20} /></button>
          <Breadcrumbs />
          <div className="topbar-actions">
            <ConnectionBadge state={connection} />
            <Link className="icon-button badge-button" to="/notifications" aria-label={`${unread} unread notifications`}>
              <Bell size={18} />
              {unread > 0 && <span>{unread}</span>}
            </Link>
            <ThemeToggle />
          </div>
        </header>
        <main className="page-frame">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

function Brand() {
  return (
    <Link className="brand" to="/projects">
      <span className="brand-mark"><Sparkles size={20} fill="currentColor" /></span>
      <span>Future</span>
    </Link>
  );
}

function NavLinks({ role }: { role: "USER" | "ADMIN" }) {
  const location = useLocation();
  const items = [
    { to: "/projects", label: "Projects", icon: FolderKanban },
    { to: "/notifications", label: "Notifications", icon: Bell },
    ...(role === "ADMIN"
      ? [
          { to: "/admin/queue", label: "Admin Queue", icon: Gauge },
          { to: "/admin/users", label: "Admin Users", icon: Users },
          { to: "/admin/audit", label: "Audit", icon: Shield },
        ]
      : []),
  ];
  return (
    <nav className="nav-links" aria-label="Primary navigation">
      {items.map((item) => {
        const Icon = item.icon;
        const active = location.pathname.startsWith(item.to);
        return (
          <Link key={item.to} className={active ? "nav-link active" : "nav-link"} to={item.to}>
            <Icon size={18} />
            <span>{item.label}</span>
          </Link>
        );
      })}
    </nav>
  );
}

function UserPanel() {
  const dispatch = useAppDispatch();
  const user = useAppSelector((state) => state.auth.currentUser);
  if (!user) return null;
  return (
    <div className="user-panel">
      <div>
        <strong>{user.fullName}</strong>
        <span>{user.email}</span>
      </div>
      <span className="role-badge">{user.role === "ADMIN" ? "Admin" : "User"}</span>
      <button className="button secondary full-width" onClick={() => dispatch(actions.logout())}><LogOut size={16} /> Logout</button>
    </div>
  );
}

function ThemeToggle() {
  const dispatch = useAppDispatch();
  const mode = useAppSelector((state) => state.theme.mode);
  return (
    <button className="icon-button" aria-label="Toggle theme" onClick={() => dispatch(actions.setTheme(mode === "dark" ? "light" : "dark"))}>
      {mode === "dark" ? <Sun size={18} /> : <Moon size={18} />}
    </button>
  );
}

function Breadcrumbs() {
  const location = useLocation();
  const parts = location.pathname.split("/").filter(Boolean);
  const labels = parts.length ? parts : ["projects"];
  return (
    <div className="breadcrumbs" aria-label="Breadcrumb">
      {labels.map((part, index) => (
        <span key={`${part}-${index}`}>
          {index > 0 && <span className="crumb-separator">/</span>}
          {part.split("-").join(" ")}
        </span>
      ))}
    </div>
  );
}

function Page({ children, width = "ops" }: { children: ReactNode; width?: "ops" | "form" }) {
  const reduceMotion = useReducedMotion();
  return (
    <motion.div
      className={`page ${width === "form" ? "form-content" : "ops-content"}`}
      variants={reduceMotion ? undefined : pageVariants}
      initial="initial"
      animate="enter"
      exit="exit"
    >
      {children}
    </motion.div>
  );
}

function ProjectDashboardPage() {
  const dispatch = useAppDispatch();
  const projects = useAppSelector((state) => state.projects.items);
  const filter = useAppSelector((state) => state.projects.activeStatusFilter);
  const [query, setQuery] = useState("");
  const visibleProjects = projects.filter((project) => {
    const queryMatch = `${project.projectName} ${project.description}`.toLowerCase().includes(query.toLowerCase());
    return queryMatch && (filter === "ALL" || project.latestJobStatus === filter);
  });

  return (
    <Page>
      <PageHeader
        title="Project Dashboard"
        subtitle="Authorized projects, latest training state, and direct access to configuration and monitoring workflows."
        action={<Link className="button primary" to="/projects/new"><Plus size={17} /> Register Project</Link>}
      />
      <Toolbar>
        <label className="search-field">
          <Search size={17} />
          <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search projects" />
        </label>
        <div className="segmented" role="tablist" aria-label="Status filter">
          {statusOrder.map((status) => (
            <button key={status} className={filter === status ? "active" : ""} onClick={() => dispatch(actions.setStatusFilter(status))}>{status}</button>
          ))}
        </div>
      </Toolbar>
      {visibleProjects.length === 0 ? (
        <EmptyState icon={<FolderKanban />} title="No authorized projects" message="No projects match the current filters." />
      ) : (
        <div className="data-table project-table">
          <div className="table-head">
            <span>Project</span><span>Status</span><span>Source</span><span>Last training</span><span>Owner</span><span>Action</span>
          </div>
          {visibleProjects.map((project) => (
            <motion.div key={project.projectId} className="table-row" variants={panelVariants} initial="initial" animate="enter">
              <div><strong>{project.projectName}</strong><small>{project.description}</small></div>
              <StatusBadge status={project.latestJobStatus} />
              <span>{project.sourceType}</span>
              <span>{formatDate(project.lastTrainingTime)}</span>
              <span>{project.lastTrainingOwner ?? "Not trained"}</span>
              <Link className="button secondary" to={`/projects/${project.projectId}`}>Open</Link>
            </motion.div>
          ))}
        </div>
      )}
    </Page>
  );
}

function RegisterProjectPage() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const [sourceType, setSourceType] = useState<"GITHUB" | "ZIP">("GITHUB");
  const [projectName, setProjectName] = useState("");
  const [description, setDescription] = useState("");
  const [repositoryUrl, setRepositoryUrl] = useState("");
  const [trainingEntrypoint, setTrainingEntrypoint] = useState("python train.py --config training.yaml");
  const valid = projectName.trim() && trainingEntrypoint.trim() && (sourceType === "ZIP" || repositoryUrl.startsWith("https://"));

  const submit = () => {
    const action = actions.registerProject({ projectName, description, sourceType, repositoryUrl, trainingEntrypoint });
    dispatch(action);
    navigate(`/projects/${action.payload.projectId}`);
  };

  return (
    <Page width="form">
      <PageHeader title="Register Project" subtitle="Create a GitHub or ZIP-backed training project with the source metadata required by the API contract." />
      <section className="panel">
        <div className="segmented large">
          <button className={sourceType === "GITHUB" ? "active" : ""} onClick={() => setSourceType("GITHUB")}><FileCode2 size={16} /> GitHub</button>
          <button className={sourceType === "ZIP" ? "active" : ""} onClick={() => setSourceType("ZIP")}><Upload size={16} /> ZIP Upload</button>
        </div>
        <FormGrid>
          <TextField label="Project name" value={projectName} onChange={setProjectName} />
          <TextField label="Description" value={description} onChange={setDescription} />
          {sourceType === "GITHUB" ? <TextField label="Repository URL" value={repositoryUrl} onChange={setRepositoryUrl} placeholder="https://github.com/company/model" /> : <FileDrop />}
          <TextField label="Training entrypoint" value={trainingEntrypoint} onChange={setTrainingEntrypoint} />
        </FormGrid>
        <div className="form-actions">
          <button className="button primary" disabled={!valid} onClick={submit}><Plus size={17} /> Create Project</button>
        </div>
      </section>
    </Page>
  );
}

function ProjectDetailPage({ initialTab = "overview" }: { initialTab?: "overview" | "config" | "history" }) {
  const { projectId } = useParams();
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const project = useAppSelector((state) => state.projects.items.find((item) => item.projectId === projectId));
  const config = useAppSelector((state) => state.projects.configsByProjectId[projectId ?? ""]);
  const jobs = useAppSelector((state) => state.jobs.items.filter((job) => job.projectId === projectId));
  const [tab, setTab] = useState(initialTab);
  const [yaml, setYaml] = useState(config?.yamlContent ?? "");
  const [validation, setValidation] = useState<"idle" | "valid" | "invalid">("idle");
  const [startOpen, setStartOpen] = useState(false);

  if (!project || !config) return <ErrorPage code="404" title="Project not found" message="No authorized project was returned for this route." />;

  const startTraining = () => {
    const action = actions.startJob(project);
    dispatch(action);
    dispatch(actions.setProjectStatus({ projectId: project.projectId, status: action.payload.status, owner: action.payload.triggeredBy.email }));
    navigate(`/jobs/${action.payload.jobId}`);
  };

  return (
    <Page>
      <PageHeader
        title={project.projectName}
        subtitle={project.description}
        action={<button className="button primary" onClick={() => setStartOpen(true)}><Play size={17} /> Start Training</button>}
      />
      <div className="split-layout">
        <section className="panel">
          <h2>Project Summary</h2>
          <KeyValue label="Source" value={project.sourceType} />
          <KeyValue label="Repository" value={project.repositoryUrl ?? "ZIP upload"} />
          <KeyValue label="Entrypoint" value={project.trainingEntrypoint} />
          <KeyValue label="Owner" value={project.owner.email} />
          <KeyValue label="Latest status" value={<StatusBadge status={project.latestJobStatus} />} />
        </section>
        <section className="panel">
          <Tabs value={tab} onChange={setTab} items={[["overview", "Overview"], ["config", "Configuration"], ["history", "History"]]} />
          {tab === "overview" && <TrainingHistory jobs={jobs.slice(0, 3)} compact />}
          {tab === "config" && (
            <ConfigEditor
              yaml={yaml}
              setYaml={setYaml}
              validation={validation}
              onValidate={() => setValidation(yaml.includes("training:") ? "valid" : "invalid")}
              onSave={() => dispatch(actions.saveConfig({ projectId: project.projectId, config: { ...config, yamlContent: yaml, contentHash: "draft-local" } }))}
            />
          )}
          {tab === "history" && <TrainingHistory jobs={jobs} />}
        </section>
      </div>
      {startOpen && (
        <Dialog title="Start Training With Care" onClose={() => setStartOpen(false)}>
          <p>The platform will submit the current YAML as an immutable configuration snapshot for this training job.</p>
          <pre className="yaml-preview">{yaml}</pre>
          <div className="dialog-actions">
            <button className="button secondary" onClick={() => setStartOpen(false)}>Cancel</button>
            <button className="button primary" onClick={startTraining}><Play size={17} /> Start</button>
          </div>
        </Dialog>
      )}
    </Page>
  );
}

function JobDetailPage() {
  const { jobId } = useParams();
  const dispatch = useAppDispatch();
  const job = useAppSelector((state) => state.jobs.items.find((item) => item.jobId === jobId));
  const logs = useAppSelector((state) => state.jobs.logsByJobId[jobId ?? ""] ?? []);
  const artifacts = useAppSelector((state) => state.jobs.artifactsByJobId[jobId ?? ""] ?? []);
  const connection = useAppSelector((state) => state.jobs.connection);
  const [filter, setFilter] = useState<StreamType | "ALL">("ALL");
  const [query, setQuery] = useState("");
  const [cancelOpen, setCancelOpen] = useState(false);
  const [reason, setReason] = useState("");
  const visibleLogs = logs.filter((line) => (filter === "ALL" || line.streamType === filter) && line.message.toLowerCase().includes(query.toLowerCase()));

  if (!job) return <ErrorPage code="404" title="Job not found" message="No authorized job was returned for this route." />;

  return (
    <Page>
      <PageHeader
        title={`Job ${job.jobId}`}
        subtitle={job.projectName}
        action={<ActionToolbar job={job} onCancel={() => setCancelOpen(true)} onRetry={() => dispatch(actions.retryJob(job.jobId))} />}
      />
      <div className="job-layout">
        <aside className="job-side">
          <section className="panel">
            <h2>Status</h2>
            <StatusBadge status={job.status} large />
            <KeyValue label="Triggered by" value={job.triggeredBy.email} />
            <KeyValue label="Queue position" value={job.queuePosition ?? "None"} />
            <KeyValue label="Started" value={formatDate(job.startedAt)} />
            <KeyValue label="Ended" value={formatDate(job.endedAt)} />
            {job.failureReason && <Banner tone="danger">{job.failureReason}</Banner>}
          </section>
          <section className="panel">
            <h2>Progress</h2>
            {job.progress.available ? (
              <>
                <div className="progress"><span style={{ width: `${job.progress.value ?? 0}%` }} /></div>
                <strong>{job.progress.value}%</strong>
                <p>Epoch {job.progress.epoch} of {job.progress.totalEpoch}</p>
              </>
            ) : (
              <Banner tone="warning">Progress Information Not Available</Banner>
            )}
          </section>
          <section className="panel">
            <h2>Artifacts</h2>
            {artifacts.length === 0 ? <EmptyState title="No artifacts yet" message="Artifacts appear after successful terminal status." compact /> : artifacts.map((artifact) => (
              <div className="artifact-row" key={artifact.artifactId}>
                <div><strong>{artifact.artifactName}</strong><small>{formatBytes(artifact.fileSizeBytes)} · {artifact.artifactType}</small></div>
                <a className="icon-button" href={`/api/v1/artifacts/${artifact.artifactId}/download`} aria-label={`Download ${artifact.artifactName}`}><Download size={17} /></a>
              </div>
            ))}
          </section>
        </aside>
        <section className="panel log-panel">
          <div className="panel-header">
            <h2>Live Logs</h2>
            <ConnectionBadge state={connection} />
          </div>
          <Toolbar>
            <label className="search-field"><Search size={17} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search logs" /></label>
            <div className="segmented">
              {(["ALL", "STDOUT", "STDERR"] as const).map((item) => <button key={item} className={filter === item ? "active" : ""} onClick={() => setFilter(item)}>{item}</button>)}
            </div>
          </Toolbar>
          <div className="log-viewer" role="log" aria-live="polite">
            {visibleLogs.map((line) => (
              <div className="log-line" key={line.logEventId}>
                <span>{line.sequenceNo.toString().padStart(4, "0")}</span>
                <span>{line.streamType}</span>
                <code>{line.message}</code>
              </div>
            ))}
          </div>
        </section>
      </div>
      {cancelOpen && (
        <Dialog title="Cancel Training Job" onClose={() => setCancelOpen(false)} danger>
          <p>Cancellation is destructive for the active run. The backend remains authoritative for final status.</p>
          <label className="field"><span>Reason</span><textarea value={reason} onChange={(event) => setReason(event.target.value)} /></label>
          <div className="dialog-actions">
            <button className="button secondary" onClick={() => setCancelOpen(false)}>Keep Running</button>
            <button className="button danger" onClick={() => { dispatch(actions.cancelJob({ jobId: job.jobId, reason })); setCancelOpen(false); }}><Square size={16} /> Cancel Job</button>
          </div>
        </Dialog>
      )}
    </Page>
  );
}

function NotificationListPage() {
  const dispatch = useAppDispatch();
  const notifications = useAppSelector((state) => state.notifications.items);
  return (
    <Page width="form">
      <PageHeader title="Notifications" subtitle="Job outcomes, delivery failures, and direct links back to monitoring context." />
      <section className="panel list-panel">
        {notifications.map((notification) => (
          <div className="notification-row" key={notification.notificationId}>
            <StatusDot tone={notification.status === "READ" ? "neutral" : notification.status === "DELIVERY_FAILED" ? "danger" : "info"} />
            <div>
              <strong>{notification.type}</strong>
              <p>{notification.message}</p>
              <small>{formatDate(notification.createdAt)} · {notification.channel}</small>
            </div>
            <Link className="button secondary" to={`/jobs/${notification.jobId}`}>Open job</Link>
            {notification.status !== "READ" && <button className="button ghost" onClick={() => dispatch(actions.markRead(notification.notificationId))}>Mark read</button>}
          </div>
        ))}
      </section>
    </Page>
  );
}

function AdminQueuePage() {
  const queue = useAppSelector((state) => state.admin.queue);
  return (
    <Page>
      <PageHeader title="Admin Queue" subtitle="Global capacity metrics and FIFO queue visibility without exposing source or artifact contents." />
      <MetricGrid>
        <Metric label="Running" value={`${queue.runningCount}/${queue.runningLimit}`} />
        <Metric label="Queued" value={queue.queuedCount.toString()} />
        <Metric label="Capacity" value={queue.runningCount >= queue.runningLimit ? "Busy" : "Available"} />
      </MetricGrid>
      <section className="panel">
        <h2>Queue Snapshot</h2>
        <div className="data-table">
          <div className="table-head"><span>Job</span><span>Project</span><span>Status</span><span>Position</span><span>Enqueued</span></div>
          {queue.items.map((item) => (
            <div className="table-row" key={item.jobId}>
              <span>{item.jobId}</span><span>{item.projectName}</span><StatusBadge status={item.status} /><span>{item.queuePosition ?? "-"}</span><span>{formatDate(item.enqueuedAt)}</span>
            </div>
          ))}
        </div>
      </section>
    </Page>
  );
}

function AdminUsersPage() {
  const dispatch = useAppDispatch();
  const users = useAppSelector((state) => state.admin.users);
  return (
    <Page>
      <PageHeader title="Admin Users" subtitle="Role and activation management with visible status boundaries." />
      <section className="panel">
        <div className="data-table">
          <div className="table-head"><span>User</span><span>Role</span><span>Status</span><span>Last login</span><span>Action</span></div>
          {users.map((user) => (
            <div className="table-row" key={user.userId}>
              <div><strong>{user.fullName}</strong><small>{user.email}</small></div>
              <span>{user.role}</span>
              <span className={`badge ${user.status === "ACTIVE" ? "success" : "neutral"}`}>{user.status}</span>
              <span>{formatDate(user.lastLoginAt)}</span>
              <button className="button secondary" onClick={() => dispatch(actions.setUserStatus({ userId: user.userId, status: user.status === "ACTIVE" ? "DISABLED" : "ACTIVE" }))}>
                {user.status === "ACTIVE" ? "Disable" : "Enable"}
              </button>
            </div>
          ))}
        </div>
      </section>
    </Page>
  );
}

function AdminAuditPage() {
  const audit = useAppSelector((state) => state.admin.audit);
  return (
    <Page>
      <PageHeader title="Admin Audit" subtitle="Correlation-ready trace of privileged actions and training operations." />
      <section className="panel">
        <div className="data-table">
          <div className="table-head"><span>Time</span><span>Actor</span><span>Action</span><span>Resource</span></div>
          {audit.map((row) => (
            <div className="table-row" key={row.auditId}>
              <span>{formatDate(row.createdAt)}</span><span>{row.actor.email}</span><span>{row.action}</span><span>{row.resourceType}:{row.resourceId}</span>
            </div>
          ))}
        </div>
      </section>
    </Page>
  );
}

function LoginPage() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const currentUser = useAppSelector((state) => state.auth.currentUser);
  const error = useAppSelector((state) => state.auth.error);
  const accounts = useAppSelector((state) => state.auth.accounts);
  const [email, setEmail] = useState("user@example.com");
  const [password, setPassword] = useState("password");
  const from = (location.state as { from?: string } | null)?.from ?? "/projects";

  useEffect(() => {
    if (currentUser) navigate(from, { replace: true });
  }, [currentUser, from, navigate]);

  const submit = () => dispatch(actions.login({ email, password }));

  return (
    <AuthPage>
      <section className="login-card auth-card">
        <Brand />
        <div>
          <h1>Welcome Home</h1>
          <p>Sign in with a development account while company SSO/OIDC is not configured.</p>
        </div>
        <Banner tone="warning">Development only. Production authentication remains company SSO/OIDC.</Banner>
        <FormGrid>
          <TextField label="Email" value={email} onChange={setEmail} />
          <label className="field"><span>Password</span><input type="password" value={password} onChange={(event) => setPassword(event.target.value)} /></label>
        </FormGrid>
        {error && <Banner tone="danger">{error}</Banner>}
        <button className="button primary full-width" onClick={submit}>Sign In</button>
        <div className="sample-accounts">
          <strong>Sample accounts</strong>
          {accounts.slice(0, 2).map((account) => (
            <button key={account.email} className="sample-account" onClick={() => { setEmail(account.email); setPassword(account.password); }}>
              <span>{account.role}</span>
              <code>{account.email}</code>
              <small>{account.password}</small>
            </button>
          ))}
        </div>
        <Link className="button secondary full-width" to="/register">Register development account</Link>
      </section>
    </AuthPage>
  );
}

function RegisterAccountPage() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const currentUser = useAppSelector((state) => state.auth.currentUser);
  const error = useAppSelector((state) => state.auth.error);
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("password");
  const [role, setRole] = useState<"USER" | "ADMIN">("USER");
  const valid = fullName.trim() && email.includes("@") && password.length >= 6;

  useEffect(() => {
    if (currentUser) navigate("/projects", { replace: true });
  }, [currentUser, navigate]);

  const submit = () => {
    if (!valid) return;
    dispatch(actions.registerAccount({ fullName, email, password, role }));
  };

  return (
    <AuthPage>
      <section className="login-card auth-card">
        <Brand />
        <div>
          <h1>Register Account</h1>
          <p>Create a local development session for onboarding flow validation.</p>
        </div>
        <Banner tone="warning">This registration is frontend-local until a backend account endpoint is introduced.</Banner>
        <FormGrid>
          <TextField label="Full name" value={fullName} onChange={setFullName} />
          <TextField label="Email" value={email} onChange={setEmail} />
          <label className="field"><span>Password</span><input type="password" value={password} onChange={(event) => setPassword(event.target.value)} /></label>
          <div className="segmented large">
            <button className={role === "USER" ? "active" : ""} onClick={() => setRole("USER")}>User</button>
            <button className={role === "ADMIN" ? "active" : ""} onClick={() => setRole("ADMIN")}>Admin</button>
          </div>
        </FormGrid>
        {error && <Banner tone="danger">{error}</Banner>}
        <button className="button primary full-width" disabled={!valid} onClick={submit}>Create Account</button>
        <Link className="button secondary full-width" to="/login">Back to Login</Link>
      </section>
    </AuthPage>
  );
}

function AuthPage({ children }: { children: ReactNode }) {
  return (
    <div className="auth-page">
      <CelestialBackground />
      {children}
    </div>
  );
}

function LoginCallbackPage() {
  return (
    <AuthPage>
      <section className="login-card">
        <Brand />
        <h1>Welcome Home</h1>
        <p>Your authenticated session is ready for project training workflows.</p>
        <Link className="button primary" to="/login">Continue to Login</Link>
      </section>
    </AuthPage>
  );
}

function ErrorPage({ code, title, message }: { code: string; title: string; message: string }) {
  return (
    <Page width="form">
      <section className="error-card">
        <strong>{code}</strong>
        <h1>{title}</h1>
        <p>{message}</p>
        <Link className="button primary" to="/projects">Back to Projects</Link>
      </section>
    </Page>
  );
}

function PageHeader({ title, subtitle, action }: { title: string; subtitle: string; action?: React.ReactNode }) {
  return (
    <header className="page-header">
      <div><h1>{title}</h1><p>{subtitle}</p></div>
      {action && <div className="page-action">{action}</div>}
    </header>
  );
}

function Toolbar({ children }: { children: ReactNode }) {
  return <div className="toolbar">{children}</div>;
}

function FormGrid({ children }: { children: ReactNode }) {
  return <div className="form-grid">{children}</div>;
}

function TextField({ label, value, onChange, placeholder }: { label: string; value: string; onChange: (value: string) => void; placeholder?: string }) {
  return <label className="field"><span>{label}</span><input value={value} onChange={(event) => onChange(event.target.value)} placeholder={placeholder} /></label>;
}

function FileDrop() {
  return <div className="file-drop"><Upload size={22} /><span>Drop a ZIP archive here or choose a file</span><small>.zip source packages are validated by the backend upload endpoint.</small></div>;
}

function Tabs<T extends string>({ value, onChange, items }: { value: T; onChange: (value: T) => void; items: Array<[T, string]> }) {
  return <div className="tabs">{items.map(([id, label]) => <button key={id} className={value === id ? "active" : ""} onClick={() => onChange(id)}>{label}</button>)}</div>;
}

function ConfigEditor({ yaml, setYaml, validation, onValidate, onSave }: { yaml: string; setYaml: (value: string) => void; validation: string; onValidate: () => void; onSave: () => void }) {
  return (
    <div className="config-editor">
      <div className="editor-toolbar">
        <button className="button secondary" onClick={onValidate}><Check size={16} /> Validate</button>
        <button className="button primary" onClick={onSave}>Save Draft</button>
      </div>
      {validation === "valid" && <Banner tone="success">YAML validation passed.</Banner>}
      {validation === "invalid" && <Banner tone="danger">YAML must include a training section.</Banner>}
      <textarea className="code-editor" value={yaml} onChange={(event) => setYaml(event.target.value)} spellCheck={false} />
    </div>
  );
}

function TrainingHistory({ jobs, compact = false }: { jobs: JobDetail[]; compact?: boolean }) {
  if (jobs.length === 0) return <EmptyState title="No training jobs" message="Start training to create the first job history row." compact />;
  return (
    <div className={compact ? "data-table compact" : "data-table"}>
      <div className="table-head"><span>Job</span><span>Status</span><span>Started</span><span>Duration</span><span>Action</span></div>
      {jobs.map((job) => (
        <div className="table-row" key={job.jobId}>
          <span>{job.jobId}</span><StatusBadge status={job.status} /><span>{formatDate(job.startedAt ?? job.createdAt)}</span><span>{duration(job)}</span><Link className="button secondary" to={`/jobs/${job.jobId}`}>Open</Link>
        </div>
      ))}
    </div>
  );
}

function ActionToolbar({ job, onCancel, onRetry }: { job: JobDetail; onCancel: () => void; onRetry: () => void }) {
  const canCancel = job.status === "RUNNING" || job.status === "QUEUED";
  const canRetry = job.status === "FAILED" || job.status === "CANCELLED";
  return (
    <div className="action-toolbar">
      <button className="button danger" disabled={!canCancel} onClick={onCancel}><Square size={16} /> Cancel</button>
      <button className="button secondary" disabled={!canRetry} onClick={onRetry}><RefreshCcw size={16} /> Retry</button>
    </div>
  );
}

function Dialog({ title, children, onClose, danger = false }: { title: string; children: ReactNode; onClose: () => void; danger?: boolean }) {
  return (
    <div className="dialog-backdrop" role="presentation" onMouseDown={onClose}>
      <motion.div className={`dialog ${danger ? "danger-dialog" : ""}`} role="dialog" aria-modal="true" aria-label={title} onMouseDown={(event) => event.stopPropagation()} initial={{ opacity: 0, scale: 0.98 }} animate={{ opacity: 1, scale: 1 }}>
        <div className="dialog-header"><h2>{title}</h2><button className="icon-button" aria-label="Close dialog" onClick={onClose}><X size={18} /></button></div>
        {children}
      </motion.div>
    </div>
  );
}

function StatusBadge({ status, large = false }: { status: JobStatus; large?: boolean }) {
  const Icon = status === "SUCCESS" ? Check : status === "FAILED" ? AlertTriangle : status === "RUNNING" ? Activity : status === "QUEUED" || status === "CREATED" ? Clock : status === "RETRYING" ? RefreshCcw : Square;
  return <span className={`status-badge ${status.toLowerCase()} ${large ? "large" : ""}`}><Icon size={large ? 18 : 14} /> {label(status)}</span>;
}

function ConnectionBadge({ state }: { state: string }) {
  return <span className={`connection ${state.toLowerCase().replace(" ", "-")}`}><StatusDot tone={state === "CONNECTED" ? "success" : state === "DISCONNECTED" ? "danger" : "warning"} />{state}</span>;
}

function StatusDot({ tone }: { tone: "success" | "danger" | "warning" | "info" | "neutral" }) {
  return <span className={`status-dot ${tone}`} aria-hidden="true" />;
}

function Banner({ children, tone }: { children: ReactNode; tone: "success" | "warning" | "danger" }) {
  return <div className={`banner ${tone}`}>{children}</div>;
}

function EmptyState({ icon, title, message, compact = false }: { icon?: React.ReactNode; title: string; message: string; compact?: boolean }) {
  return <section className={`empty-state ${compact ? "compact" : ""}`}>{icon}<h2>{title}</h2><p>{message}</p></section>;
}

function KeyValue({ label, value }: { label: string; value: React.ReactNode }) {
  return <div className="key-value"><span>{label}</span><strong>{value}</strong></div>;
}

function MetricGrid({ children }: { children: ReactNode }) {
  return <div className="metric-grid">{children}</div>;
}

function Metric({ label, value }: { label: string; value: string }) {
  return <section className="metric panel"><span>{label}</span><strong>{value}</strong></section>;
}

function CelestialBackground() {
  return <div className="celestial-bg" aria-hidden="true"><span /><span /><span /></div>;
}

function formatDate(value?: string) {
  if (!value) return "-";
  return new Intl.DateTimeFormat(undefined, { month: "short", day: "2-digit", hour: "2-digit", minute: "2-digit" }).format(new Date(value));
}

function formatBytes(bytes: number) {
  if (bytes > 1_000_000) return `${(bytes / 1_000_000).toFixed(1)} MB`;
  if (bytes > 1_000) return `${(bytes / 1_000).toFixed(1)} KB`;
  return `${bytes} B`;
}

function duration(job: JobDetail) {
  const start = new Date(job.startedAt ?? job.createdAt).getTime();
  const end = job.endedAt ? new Date(job.endedAt).getTime() : Date.now();
  const minutes = Math.max(1, Math.round((end - start) / 60_000));
  return `${minutes}m`;
}

function label(status: JobStatus) {
  return status.charAt(0) + status.slice(1).toLowerCase();
}
