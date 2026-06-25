/** Current connectivity state of the WebSocket client. */
export type JobStreamState =
  | "connecting"
  | "connected"
  | "reconnecting"
  | "closed"
  | "unauthorized"  // server sent close code 1008 — do not retry
  | "unavailable";  // network error or abnormal close — will auto-retry

/**
 * Envelope for all WebSocket messages at `/api/v1/ws/jobs/:jobId`.
 * Handled types: `CONNECTED`, `LOG`, `PROGRESS`, `STATUS_CHANGE`.
 * `eventId` is used with `lastEventId` to resume after a reconnect without replaying events.
 */
export type JobStreamEvent<TPayload = unknown> = {
  /** Opaque cursor; pass as `lastEventId` on reconnect to skip already-seen events. */
  eventId?: string;
  type: string;
  jobId: string;
  payload: TPayload;
  occurredAt?: string;
};

export type JobStreamClientOptions = {
  jobId: string;
  /**
   * Bearer token sent via `?token=` query param because the browser WebSocket API
   * does not support custom headers.
   * In dev-auth mode this is the user's email.
   */
  token?: string;
  /** Resume cursor from a previous connection; server replays events after this ID. */
  lastEventId?: string;
  onEvent: (event: JobStreamEvent) => void;
  onStateChange?: (state: JobStreamState) => void;
  /** Milliseconds before attempting a reconnect after `unavailable`. Defaults to 5000. */
  reconnectDelayMs?: number;
};
