package com.example.aitraining.api;

import com.example.aitraining.auth.CurrentUserContext;
import com.example.aitraining.domain.Models.User;
import com.example.aitraining.dto.UserDtos.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints.
 *
 * <p>Routes (all under the {@code /api/v1} context path):
 * <ul>
 *   <li>{@code GET /auth/me} — returns the identity of the current bearer token.</li>
 *   <li>{@code POST /auth/logout} — no-op; returns 204.  Sessions are stateless so there is
 *       nothing to invalidate server-side.  The client discards its token.</li>
 * </ul>
 */
@RestController
public class AuthController {

  /**
   * Returns the currently authenticated user's profile.
   *
   * @return 200 with the caller's identity
   */
  @GetMapping("/auth/me")
  CurrentUser me() {
    User user = CurrentUserContext.require();
    return new CurrentUser(user.userId(), user.email(), user.fullName(), user.role(), user.status(),
        user.lastLoginAt());
  }

  @PostMapping("/auth/logout")
  ResponseEntity<Void> logout() {
    return ResponseEntity.noContent().build();
  }
}
