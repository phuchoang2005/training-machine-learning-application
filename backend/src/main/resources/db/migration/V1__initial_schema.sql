CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TYPE user_role AS ENUM ('USER', 'ADMIN');
CREATE TYPE user_status AS ENUM ('ACTIVE', 'DISABLED');
CREATE TYPE source_type AS ENUM ('GITHUB', 'ZIP');
CREATE TYPE job_status AS ENUM ('CREATED', 'QUEUED', 'RUNNING', 'SUCCESS', 'FAILED', 'CANCELLED', 'RETRYING');
CREATE TYPE queue_status AS ENUM ('WAITING', 'DISPATCHED', 'CANCELLED');
CREATE TYPE stream_type AS ENUM ('STDOUT', 'STDERR');
CREATE TYPE artifact_type AS ENUM ('MODEL', 'CHECKPOINT', 'METRIC', 'OTHER');
CREATE TYPE notification_channel AS ENUM ('IN_APP', 'EMAIL');
CREATE TYPE notification_status AS ENUM ('PENDING', 'SENT', 'FAILED', 'READ');

CREATE TABLE users (
  user_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  email text NOT NULL UNIQUE,
  full_name text NOT NULL,
  role user_role NOT NULL,
  status user_status NOT NULL DEFAULT 'ACTIVE',
  created_at timestamptz NOT NULL DEFAULT now(),
  last_login_at timestamptz
);

CREATE TABLE projects (
  project_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_user_id uuid NOT NULL REFERENCES users(user_id),
  project_name text NOT NULL,
  description text,
  source_type source_type NOT NULL,
  repository_url text,
  local_source_path text,
  training_entrypoint text NOT NULL DEFAULT 'main.py',
  status text NOT NULL DEFAULT 'ACTIVE',
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_projects_owner ON projects(owner_user_id);
CREATE INDEX idx_projects_source_type ON projects(source_type);

CREATE TABLE project_configs (
  config_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  project_id uuid NOT NULL REFERENCES projects(project_id) ON DELETE CASCADE,
  config_name text NOT NULL,
  config_path text NOT NULL,
  yaml_content text NOT NULL,
  default_config boolean NOT NULL DEFAULT false,
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT uq_project_configs_path UNIQUE (project_id, config_path)
);

CREATE TABLE config_snapshots (
  snapshot_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  project_id uuid NOT NULL REFERENCES projects(project_id) ON DELETE CASCADE,
  config_id uuid REFERENCES project_configs(config_id),
  yaml_content text NOT NULL,
  content_hash text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_config_snapshots_project_created ON config_snapshots(project_id, created_at DESC);

CREATE TABLE training_jobs (
  job_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  project_id uuid NOT NULL REFERENCES projects(project_id) ON DELETE CASCADE,
  triggered_by_user_id uuid NOT NULL REFERENCES users(user_id),
  config_snapshot_id uuid NOT NULL REFERENCES config_snapshots(snapshot_id),
  retry_of_job_id uuid REFERENCES training_jobs(job_id),
  status job_status NOT NULL,
  retry_attempt integer NOT NULL DEFAULT 0,
  queue_position integer,
  queued_at timestamptz,
  started_at timestamptz,
  ended_at timestamptz,
  container_id text,
  log_path text,
  artifact_base_path text,
  failure_reason text,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_jobs_project_created ON training_jobs(project_id, created_at DESC);
CREATE INDEX idx_jobs_status ON training_jobs(status);
CREATE INDEX idx_jobs_running ON training_jobs(status, started_at) WHERE status = 'RUNNING';

CREATE TABLE job_queue_entries (
  queue_entry_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  job_id uuid NOT NULL REFERENCES training_jobs(job_id) ON DELETE CASCADE,
  queue_status queue_status NOT NULL,
  enqueued_at timestamptz NOT NULL DEFAULT now(),
  dispatched_at timestamptz
);
CREATE INDEX idx_queue_waiting ON job_queue_entries(queue_status, enqueued_at);
CREATE UNIQUE INDEX uq_queue_active_job ON job_queue_entries(job_id)
  WHERE queue_status IN ('WAITING', 'DISPATCHED');

CREATE TABLE job_log_events (
  log_event_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  job_id uuid NOT NULL REFERENCES training_jobs(job_id) ON DELETE CASCADE,
  stream_type stream_type NOT NULL,
  message text NOT NULL,
  emitted_at timestamptz NOT NULL DEFAULT now(),
  sequence_no integer NOT NULL
);
CREATE INDEX idx_log_events_job_seq ON job_log_events(job_id, sequence_no);

CREATE TABLE job_progress_events (
  progress_event_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  job_id uuid NOT NULL REFERENCES training_jobs(job_id) ON DELETE CASCADE,
  progress_value integer NOT NULL CHECK (progress_value BETWEEN 0 AND 100),
  epoch integer,
  total_epoch integer,
  raw_payload text,
  emitted_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_progress_job_time ON job_progress_events(job_id, emitted_at DESC);

CREATE TABLE artifacts (
  artifact_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  project_id uuid NOT NULL REFERENCES projects(project_id) ON DELETE CASCADE,
  job_id uuid NOT NULL REFERENCES training_jobs(job_id) ON DELETE CASCADE,
  artifact_name text NOT NULL,
  artifact_type artifact_type NOT NULL,
  file_path text NOT NULL,
  file_size_bytes bigint NOT NULL CHECK (file_size_bytes >= 0),
  checksum text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_artifacts_job ON artifacts(job_id);
CREATE INDEX idx_artifacts_project_created ON artifacts(project_id, created_at DESC);

CREATE TABLE notifications (
  notification_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES users(user_id),
  job_id uuid REFERENCES training_jobs(job_id) ON DELETE CASCADE,
  type text NOT NULL,
  channel notification_channel NOT NULL,
  status notification_status NOT NULL,
  message text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  sent_at timestamptz
);
CREATE INDEX idx_notifications_user_status ON notifications(user_id, status, created_at DESC);

CREATE TABLE audit_logs (
  audit_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  actor_user_id uuid REFERENCES users(user_id),
  project_id uuid REFERENCES projects(project_id) ON DELETE SET NULL,
  job_id uuid REFERENCES training_jobs(job_id) ON DELETE SET NULL,
  action text NOT NULL,
  resource_type text NOT NULL,
  resource_id text,
  metadata_json text NOT NULL DEFAULT '{}',
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_project_time ON audit_logs(project_id, created_at DESC);
CREATE INDEX idx_audit_actor_time ON audit_logs(actor_user_id, created_at DESC);

INSERT INTO users (user_id, email, full_name, role, status)
VALUES
  ('00000000-0000-0000-0000-000000000101', 'user@example.com', 'Development User', 'USER', 'ACTIVE'),
  ('00000000-0000-0000-0000-000000000201', 'admin@example.com', 'Development Admin', 'ADMIN', 'ACTIVE')
ON CONFLICT (email) DO NOTHING;
