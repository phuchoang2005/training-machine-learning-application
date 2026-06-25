package com.example.aitraining.realtime;

import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Observer Pattern</b> — WebSocket event bus for live training-job streaming.
 *
 * <p>This handler is the <em>subject</em>; connected WebSocket sessions are the observers.
 * Multiple browser tabs can subscribe to the same job simultaneously (each creates a separate
 * session keyed by {@code session.getId()}).
 *
 * <h2>Session lifecycle</h2>
 * <ol>
 *   <li>On upgrade, {@link com.example.aitraining.config.WebSocketConfig.DevelopmentBearerHandshakeInterceptor}
 *       validates the token and stores the {@code jobId} in the session attributes.</li>
 *   <li>{@link #afterConnectionEstablished} registers the session and sends an initial
 *       {@code CONNECTED} event.</li>
 *   <li>{@link #publish} fans out events to all sessions subscribed to the given job.</li>
 *   <li>{@link #afterConnectionClosed} removes the session; the per-job map is removed when
 *       its last session disconnects.</li>
 * </ol>
 *
 * <h2>Event types published</h2>
 * <ul>
 *   <li>{@code CONNECTED} — sent once on handshake completion.</li>
 *   <li>{@code STATUS_CHANGE} — when the job transitions to a new lifecycle state.</li>
 *   <li>{@code LOG} — a single stdout or stderr line from the training container.</li>
 *   <li>{@code PROGRESS} — a parsed progress snapshot (percentage, epoch, totalEpoch).</li>
 * </ul>
 */
@Component
public class JobStreamWebSocketHandler extends TextWebSocketHandler {
  private final ObjectMapper mapper;
  private final Map<UUID, Map<String, WebSocketSession>> sessionsByJob = new ConcurrentHashMap<>();

  public JobStreamWebSocketHandler(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws IOException {
    UUID jobId = (UUID) session.getAttributes().get("jobId");
    sessionsByJob.computeIfAbsent(jobId, ignored -> new ConcurrentHashMap<>()).put(session.getId(), session);
    send(session, new JobStreamEnvelope("CONNECTED", jobId, Map.of("connected", true), Instant.now()));
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    UUID jobId = (UUID) session.getAttributes().get("jobId");
    Map<String, WebSocketSession> sessions = sessionsByJob.get(jobId);
    if (sessions == null) {
      return;
    }
    sessions.remove(session.getId());
    if (sessions.isEmpty()) {
      sessionsByJob.remove(jobId);
    }
  }

  /**
   * Fans out an event to all WebSocket sessions currently subscribed to {@code jobId}.
   * Sessions that fail to send (closed concurrently) are silently skipped; the next
   * {@link #afterConnectionClosed} callback removes them.
   *
   * @param jobId   the job whose subscribers receive the event
   * @param type    event type string (e.g. {@code "LOG"}, {@code "STATUS_CHANGE"})
   * @param payload arbitrary serializable payload; will be JSON-encoded
   */
  public void publish(UUID jobId, String type, Object payload) {
    Map<String, WebSocketSession> sessions = sessionsByJob.get(jobId);
    if (sessions == null || sessions.isEmpty()) {
      return;
    }
    JobStreamEnvelope envelope = new JobStreamEnvelope(type, jobId, payload, Instant.now());
    sessions.values().forEach(session -> {
      try {
        send(session, envelope);
      } catch (IOException ignored) {
        // The next close callback removes the stale session.
      }
    });
  }

  private void send(WebSocketSession session, JobStreamEnvelope envelope) throws IOException {
    if (session.isOpen()) {
      session.sendMessage(new TextMessage(mapper.writeValueAsString(envelope)));
    }
  }

  /**
   * Standard envelope for all WebSocket messages sent by this handler.
   *
   * @param type       event type (e.g. {@code "LOG"}, {@code "STATUS_CHANGE"}, {@code "PROGRESS"})
   * @param jobId      the job this event belongs to
   * @param payload    event-specific data; structure varies by type
   * @param occurredAt server-side timestamp of the event
   */
  public record JobStreamEnvelope(String type, UUID jobId, Object payload, Instant occurredAt) {
  }
}
