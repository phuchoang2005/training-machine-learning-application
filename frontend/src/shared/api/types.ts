export type UserRole = "USER" | "ADMIN";
export type UserStatus = "ACTIVE" | "DISABLED";
export type SourceType = "GITHUB" | "ZIP";
export type JobStatus = "CREATED" | "QUEUED" | "RUNNING" | "RETRYING" | "SUCCESS" | "FAILED" | "CANCELLED";
export type StreamType = "STDOUT" | "STDERR";
export type ArtifactType = "MODEL" | "METRICS" | "LOG" | "OTHER";
export type NotificationStatus = "UNREAD" | "READ" | "DELIVERY_FAILED";

export type CurrentUser = {
  userId: string;
  email: string;
  fullName: string;
  role: UserRole;
  status: UserStatus;
  lastLoginAt: string;
};

export type UserSummary = Pick<CurrentUser, "userId" | "email" | "fullName">;

export type ProjectSummary = {
  projectId: string;
  projectName: string;
  description: string;
  sourceType: SourceType;
  latestJobStatus: JobStatus;
  lastTrainingTime?: string;
  lastTrainingOwner?: string;
};

export type ProjectDetail = ProjectSummary & {
  repositoryUrl?: string;
  trainingEntrypoint: string;
  owner: UserSummary;
  createdAt: string;
  updatedAt: string;
};

export type ProjectConfigContent = {
  configId: string;
  configPath: string;
  yamlContent: string;
  contentHash: string;
};

export type ProgressResponse = {
  available: boolean;
  value?: number;
  epoch?: number;
  totalEpoch?: number;
  updatedAt?: string;
};

export type JobDetail = {
  jobId: string;
  projectId: string;
  projectName: string;
  triggeredBy: UserSummary;
  status: JobStatus;
  queuePosition?: number;
  progress: ProgressResponse;
  retryOfJobId?: string;
  retryAttempt: number;
  createdAt: string;
  queuedAt?: string;
  startedAt?: string;
  endedAt?: string;
  failureReason?: string;
};

export type LogEvent = {
  logEventId: string;
  sequenceNo: number;
  streamType: StreamType;
  message: string;
  emittedAt: string;
};

export type Artifact = {
  artifactId: string;
  artifactName: string;
  artifactType: ArtifactType;
  fileSizeBytes: number;
  checksum: string;
  createdAt: string;
};

export type Notification = {
  notificationId: string;
  jobId: string;
  type: string;
  channel: string;
  status: NotificationStatus;
  message: string;
  createdAt: string;
};

export type QueueItem = {
  jobId: string;
  projectName: string;
  status: JobStatus;
  queuePosition?: number;
  enqueuedAt: string;
};

export type QueueSnapshot = {
  runningCount: number;
  runningLimit: number;
  queuedCount: number;
  items: QueueItem[];
};

export type AuditLog = {
  auditId: string;
  actor: UserSummary;
  projectId?: string;
  jobId?: string;
  action: string;
  resourceType: string;
  resourceId: string;
  createdAt: string;
};
