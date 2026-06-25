import { useEffect } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import { AdminGuard, NestedJobRedirect, RequireAuth } from "./auth/guards";
import { AppShell } from "./layout/AppShell";
import { actions } from "../store/store";
import { useAppDispatch } from "../store/hooks";
import { AdminAuditPage } from "../pages/admin/AdminAuditPage";
import { AdminQueuePage } from "../pages/admin/AdminQueuePage";
import { AdminUsersPage } from "../pages/admin/AdminUsersPage";
import { ErrorPage } from "../pages/ErrorPage";
import { JobDetailPage } from "../pages/jobs/JobDetailPage";
import { LoginCallbackPage } from "../pages/auth/LoginCallbackPage";
import { LoginPage } from "../pages/auth/LoginPage";
import { NotificationListPage } from "../pages/notifications/NotificationListPage";
import { ProjectDashboardPage } from "../pages/projects/ProjectDashboardPage";
import { ProjectDetailPage } from "../pages/projects/ProjectDetailPage";
import { RegisterAccountPage } from "../pages/auth/RegisterAccountPage";
import { RegisterProjectPage } from "../pages/projects/RegisterProjectPage";

export function App() {
  const dispatch = useAppDispatch();

  useEffect(() => {
    const media = window.matchMedia("(prefers-color-scheme: dark)");
    const syncTheme = () =>
      dispatch(actions.setTheme(media.matches ? "dark" : "light"));
    syncTheme();
    media.addEventListener("change", syncTheme);
    return () => media.removeEventListener("change", syncTheme);
  }, [dispatch]);

  useEffect(() => {
    dispatch(actions.fetchCurrentUser());
  }, [dispatch]);

  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterAccountPage />} />
      <Route path="/login/callback" element={<LoginCallbackPage />} />
      <Route element={<RequireAuth />}>
        <Route element={<AppShell />}>{privateRoutes()}</Route>
      </Route>
      <Route
        path="/403"
        element={
          <ErrorPage
            code="403"
            title="Access restricted"
            message="This route is only available when the backend authorizes the current role or resource relationship."
          />
        }
      />
      <Route
        path="*"
        element={
          <ErrorPage
            code="404"
            title="Route not found"
            message="The requested workspace route does not exist."
          />
        }
      />
    </Routes>
  );
}

function privateRoutes() {
  return (
    <>
      <Route path="/" element={<Navigate to="/projects" replace />} />
      <Route path="/projects" element={<ProjectDashboardPage />} />
      <Route path="/projects/new" element={<RegisterProjectPage />} />
      <Route path="/projects/:projectId" element={<ProjectDetailPage />} />
      <Route
        path="/projects/:projectId/configs/:configId"
        element={<ProjectDetailPage initialTab="config" />}
      />
      <Route
        path="/projects/:projectId/jobs/:jobId"
        element={<NestedJobRedirect />}
      />
      <Route path="/jobs/:jobId" element={<JobDetailPage />} />
      <Route path="/notifications" element={<NotificationListPage />} />
      <Route
        path="/admin/users"
        element={
          <AdminGuard>
            <AdminUsersPage />
          </AdminGuard>
        }
      />
      <Route
        path="/admin/queue"
        element={
          <AdminGuard>
            <AdminQueuePage />
          </AdminGuard>
        }
      />
      <Route
        path="/admin/audit"
        element={
          <AdminGuard>
            <AdminAuditPage />
          </AdminGuard>
        }
      />
    </>
  );
}
