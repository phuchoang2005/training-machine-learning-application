import { useEffect } from "react";
import { Banner } from "../../shared/components/Feedback";
import { Page, PageHeader } from "../../shared/components/Page";
import { formatDate } from "../../shared/format/formatters";
import { actions } from "../../store/store";
import { useAppDispatch, useAppSelector } from "../../store/hooks";

export function AdminAuditPage() {
  const dispatch = useAppDispatch();
  const audit = useAppSelector((state) => state.admin.audit);
  const loading = useAppSelector((state) => state.admin.loading);
  const error = useAppSelector((state) => state.admin.error);

  useEffect(() => { dispatch(actions.fetchAuditLogs()); }, [dispatch]);

  return (
    <Page>
      <PageHeader title="Admin Audit" subtitle="Correlation-ready trace of privileged actions and training operations." />
      {error && <Banner tone="danger">{error}</Banner>}
      <section className="panel">
        {loading && audit.length === 0 ? (
          <p>Loading audit logs…</p>
        ) : (
          <div className="data-table">
            <div className="table-head">
              <span>Time</span><span>Actor</span><span>Action</span><span>Resource</span>
            </div>
            {audit.map((row) => (
              <div className="table-row" key={row.auditId}>
                <span>{formatDate(row.createdAt)}</span>
                <span>{row.actor.email}</span>
                <span>{row.action}</span>
                <span>{row.resourceType}:{row.resourceId}</span>
              </div>
            ))}
            {audit.length === 0 && !loading && <div className="table-row"><span style={{ gridColumn: "1 / -1" }}>No audit events found.</span></div>}
          </div>
        )}
      </section>
    </Page>
  );
}
