import type { JobStreamClientOptions, JobStreamEvent, JobStreamState } from "./job-stream-types";
export type { JobStreamClientOptions, JobStreamEvent, JobStreamState };

/**
 * Opens a WebSocket connection to the job event stream and auto-reconnects on failure.
 *
 * State transitions:
 *   connecting → connected (on open)
 *   connected  → unavailable (on error/abnormal close) → reconnecting → ...
 *   connected  → unauthorized (close code 1008) — permanent, no retry
 *   *          → closed (caller invoked `.close()`)
 *
 * Pass `options.lastEventId` on reconnect to resume from the last processed event
 * and avoid replaying duplicate events.
 *
 * @returns An object with a `close()` method to terminate the connection cleanly.
 */
export function createJobStreamClient(options: JobStreamClientOptions) {
  let socket: WebSocket | undefined;
  let closedByCaller = false;
  let reconnectTimer: number | undefined;
  const reconnectDelayMs = options.reconnectDelayMs ?? 5_000;

  const emitState = (state: JobStreamState) => options.onStateChange?.(state);

  const connect = () => {
    emitState(socket ? "reconnecting" : "connecting");
    socket = new WebSocket(buildUrl(options));
    socket.addEventListener("open", () => emitState("connected"));
    socket.addEventListener("message", (message) => {
      options.onEvent(JSON.parse(message.data) as JobStreamEvent);
    });
    socket.addEventListener("close", (event) => {
      if (closedByCaller) {
        emitState("closed");
        return;
      }
      if (event.code === 1008) {
        emitState("unauthorized");
        return;
      }
      emitState("unavailable");
      reconnectTimer = window.setTimeout(connect, reconnectDelayMs);
    });
    socket.addEventListener("error", () => emitState("unavailable"));
  };

  connect();

  return {
    /** Cleanly closes the connection and suppresses auto-reconnect. */
    close() {
      closedByCaller = true;
      if (reconnectTimer) window.clearTimeout(reconnectTimer);
      socket?.close();
    },
  };
}

function buildUrl(options: JobStreamClientOptions) {
  const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
  const params = new URLSearchParams();
  if (options.token) params.set("token", options.token);
  if (options.lastEventId) params.set("lastEventId", options.lastEventId);
  const query = params.toString();
  return `${protocol}//${window.location.host}/api/v1/ws/jobs/${options.jobId}${query ? `?${query}` : ""}`;
}
