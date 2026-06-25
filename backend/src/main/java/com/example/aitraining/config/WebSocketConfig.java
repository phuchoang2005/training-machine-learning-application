package com.example.aitraining.config;

import com.example.aitraining.repo.UserRepository;
import com.example.aitraining.realtime.JobStreamWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.UUID;

/**
 * Registers the WebSocket endpoint and applies the bearer-token handshake interceptor.
 *
 * <p>{@link JobStreamWebSocketHandler} is mounted at {@code /ws/jobs/{jobId}} (under the
 * {@code /api/v1} context path, so the full URL is {@code /api/v1/ws/jobs/{jobId}}).
 * CORS is open ({@code setAllowedOrigins("*")}) for development convenience.
 *
 * <p>The inner {@link DevelopmentBearerHandshakeInterceptor} validates the bearer token
 * during the WebSocket upgrade handshake, before the session is established, so unauthenticated
 * clients receive HTTP 401 and the socket is never opened.  The token may be sent in:
 * <ul>
 *   <li>the {@code Authorization: Bearer <token>} header, or</li>
 *   <li>the {@code ?token=<token>} query parameter (for browser clients that cannot set
 *       headers on WebSocket connections).</li>
 * </ul>
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
  private final JobStreamWebSocketHandler jobStreamHandler;
  private final UserRepository users;

  public WebSocketConfig(JobStreamWebSocketHandler jobStreamHandler, UserRepository users) {
    this.jobStreamHandler = jobStreamHandler;
    this.users = users;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(jobStreamHandler, "/ws/jobs/{jobId}")
        .addInterceptors(new DevelopmentBearerHandshakeInterceptor(users))
        .setAllowedOrigins("*");
  }

  /**
   * Validates the bearer token during the WebSocket upgrade handshake.
   * On success, stores the {@code jobId} (parsed from the URL path) in the session attributes
   * so {@link JobStreamWebSocketHandler} can look up the correct subscriber set.
   */
  static final class DevelopmentBearerHandshakeInterceptor implements HandshakeInterceptor {
    private final UserRepository users;

    DevelopmentBearerHandshakeInterceptor(UserRepository users) {
      this.users = users;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
        WebSocketHandler wsHandler, Map<String, Object> attributes) {
      String token = bearerToken(request);
      if (token == null || users.findActiveByToken(token).isEmpty()) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
      }
      attributes.put("jobId", jobId(request));
      return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
        WebSocketHandler wsHandler, Exception exception) {
    }

    private String bearerToken(ServerHttpRequest request) {
      String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
      if (header != null && header.startsWith("Bearer ")) {
        return header.substring("Bearer ".length()).trim();
      }
      if (request instanceof ServletServerHttpRequest servletRequest) {
        return servletRequest.getServletRequest().getParameter("token");
      }
      return null;
    }

    private UUID jobId(ServerHttpRequest request) {
      String path = request.getURI().getPath();
      String raw = path.substring(path.lastIndexOf('/') + 1);
      return UUID.fromString(raw);
    }
  }
}
