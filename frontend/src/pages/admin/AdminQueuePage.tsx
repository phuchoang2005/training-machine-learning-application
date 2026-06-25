import { useEffect } from "react";
import { StatusBadge } from "../../shared/components/Badges";
import { Metric, MetricGrid } from "../../shared/components/Feedback";
import { Page, PageHeader } from "../../shared/components/Page";
import { formatDate } from "../../shared/format/formatters";
import { actions } from "../../store/store";
import { useAppDispatch, useAppSelector } from "../../store/hooks";

export function AdminQueuePage() {
  const dispatch = useAppDispatch();
  const queue = useAppSelector((state) => state.admin.queue);

  useEffect(() => { dispatch(actions.fetchQueue()); }, [dispatch]);

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
          <div className="table-head">
            <span>Job</span><span>Project</span><span>Status</span><span>Position</span><span>Enqueued</span>
          </div>
          {queue.items.map((item) => (
            <div className="table-row" key={item.jobId}>
              <span>{item.jobId}</span>
              <span>{item.projectName}</span>
              <StatusBadge status={item.status} />
              <span>{item.queuePosition ?? "-"}</span>
              <span>{formatDate(item.enqueuedAt)}</span>
            </div>
          ))}
          {queue.items.length === 0 && <div className="table-row"><span style={{ gridColumn: "1 / -1" }}>Queue is empty.</span></div>}
        </div>
      </section>
    </Page>
  );
}
