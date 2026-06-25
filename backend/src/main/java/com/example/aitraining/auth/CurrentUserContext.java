package com.example.aitraining.auth;

import com.example.aitraining.domain.Models.User;

/**
 * Per-request user context backed by a {@link ThreadLocal}.
 *
 * <p>{@link com.example.aitraining.config.WebConfig} sets the resolved {@link User} at the
 * start of each HTTP request and clears it in a {@code finally} block so the value never
 * leaks across request boundaries.  WebSocket handshakes go through a separate interceptor
 * ({@link com.example.aitraining.config.WebSocketConfig.DevelopmentBearerHandshakeInterceptor})
 * which stores the identity in the session attributes instead.
 *
 * <p>Controllers and services obtain the caller via {@link #require()}.  They never call
 * {@link #set} or {@link #clear} directly.
 */
public final class CurrentUserContext {

  private static final ThreadLocal<User> CURRENT = new ThreadLocal<>();

  private CurrentUserContext() {}

  /**
   * Stores the authenticated user for the current thread (i.e., the current request).
   * Called exclusively by the authentication filter.
   *
   * @param user the resolved, non-null authenticated user
   */
  public static void set(User user) {
    CURRENT.set(user);
  }

  /**
   * Returns the authenticated user for the current request.
   *
   * @return the current user; never {@code null}
   * @throws UnauthorizedException if no user has been set (i.e., the request is unauthenticated)
   */
  public static User require() {
    User user = CURRENT.get();
    if (user == null) {
      throw new UnauthorizedException("Authentication is required");
    }
    return user;
  }

  /**
   * Removes the user from the current thread's storage.
   * Must be called in a {@code finally} block to prevent cross-request contamination.
   */
  public static void clear() {
    CURRENT.remove();
  }
}
