package com.example.aitraining.config;

import com.example.aitraining.auth.CurrentUserContext;
import com.example.aitraining.auth.UnauthorizedException;
import com.example.aitraining.repo.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * <b>Chain of Responsibility Pattern</b> — bearer token authentication filter that intercepts
 * every HTTP request and populates {@link CurrentUserContext} for the request thread.
 *
 * <p>This is a <em>development-grade</em> implementation: the bearer token is the user's
 * email address (case-insensitive) or UUID.  There is no cryptographic signature or
 * expiry check.
 *
 * <h2>Filter behaviour</h2>
 * <ul>
 *   <li>{@code GET /api/v1/health} is the only unauthenticated endpoint; all others require
 *       {@code Authorization: Bearer <token>}.</li>
 *   <li>A missing or invalid token writes an inline 401 JSON response (bypassing
 *       {@link ApiExceptionHandler}) because the exception handler runs after filters.</li>
 *   <li>{@link CurrentUserContext#clear()} is called in a {@code finally} block so the
 *       {@link ThreadLocal} is never left set after the request completes.</li>
 * </ul>
 *
 * <p>Also enables {@link AppProperties} via {@code @EnableConfigurationProperties}.
 */
@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class WebConfig extends OncePerRequestFilter {
  private final UserRepository users;

  public WebConfig(UserRepository users) {
    this.users = users;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    try {
      if (isPublicHealthCheck(request)) {
        chain.doFilter(request, response);
        return;
      }
      String header = request.getHeader(HttpHeaders.AUTHORIZATION);
      if (header == null || !header.startsWith("Bearer ")) {
        throw new UnauthorizedException("Bearer token is required");
      }
      String token = header.substring("Bearer ".length()).trim();
      CurrentUserContext.set(users.findActiveByToken(token)
          .orElseThrow(() -> new UnauthorizedException("Unknown or inactive bearer identity")));
      chain.doFilter(request, response);
    } catch (UnauthorizedException ex) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType("application/json");
      response.getWriter().write("""
          {"error":{"code":"UNAUTHENTICATED","message":"%s","correlationId":null,"details":[]}}
          """.formatted(ex.getMessage()));
    } finally {
      CurrentUserContext.clear();
    }
  }

  private boolean isPublicHealthCheck(HttpServletRequest request) {
    return "GET".equals(request.getMethod()) && "/api/v1/health".equals(request.getRequestURI());
  }
}
