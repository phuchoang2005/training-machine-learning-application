import { useEffect } from "react";
import { Banner } from "../../shared/components/Feedback";
import { Page, PageHeader } from "../../shared/components/Page";
import { formatDate } from "../../shared/format/formatters";
import { actions } from "../../store/store";
import { useAppDispatch, useAppSelector } from "../../store/hooks";

export function AdminUsersPage() {
  const dispatch = useAppDispatch();
  const users = useAppSelector((state) => state.admin.users);
  const loading = useAppSelector((state) => state.admin.loading);
  const error = useAppSelector((state) => state.admin.error);

  useEffect(() => { dispatch(actions.fetchUsers()); }, [dispatch]);

  const toggleStatus = async (userId: string, current: "ACTIVE" | "DISABLED") => {
    await dispatch(actions.setUserStatusAsync({ userId, status: current === "ACTIVE" ? "DISABLED" : "ACTIVE" }));
  };

  return (
    <Page>
      <PageHeader title="Admin Users" subtitle="Role and activation management with visible status boundaries." />
      {error && <Banner tone="danger">{error}</Banner>}
      <section className="panel">
        {loading && users.length === 0 ? (
          <p>Loading users…</p>
        ) : (
          <div className="data-table">
            <div className="table-head">
              <span>User</span><span>Role</span><span>Status</span><span>Last login</span><span>Action</span>
            </div>
            {users.map((user) => (
              <div className="table-row" key={user.userId}>
                <div><strong>{user.fullName}</strong><small>{user.email}</small></div>
                <span>{user.role}</span>
                <span className={`badge ${user.status === "ACTIVE" ? "success" : "neutral"}`}>{user.status}</span>
                <span>{formatDate(user.lastLoginAt)}</span>
                <button className="button secondary" onClick={() => toggleStatus(user.userId, user.status)}>
                  {user.status === "ACTIVE" ? "Disable" : "Enable"}
                </button>
              </div>
            ))}
          </div>
        )}
      </section>
    </Page>
  );
}
