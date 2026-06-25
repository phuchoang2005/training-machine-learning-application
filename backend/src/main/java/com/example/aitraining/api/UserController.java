package com.example.aitraining.api;

import com.example.aitraining.auth.CurrentUserContext;
import com.example.aitraining.dto.CommonDtos.*;
import com.example.aitraining.dto.UserDtos.*;
import com.example.aitraining.repo.UserRepository;
import com.example.aitraining.service.AuthorizationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * Admin-only user management endpoints.
 *
 * <p>All routes require the caller to hold the {@code ADMIN} role; a non-admin receives 403.
 *
 * <p>Routes (all under {@code /api/v1}):
 * <ul>
 *   <li>{@code GET   /admin/users} — list all users, sorted by creation date.</li>
 *   <li>{@code PATCH /admin/users/{userId}/status} — activate or deactivate a user account.
 *       A {@code DISABLED} user can no longer authenticate.</li>
 * </ul>
 */
@RestController
public class UserController {
  private final UserRepository users;
  private final AuthorizationService authorization;

  public UserController(UserRepository users, AuthorizationService authorization) {
    this.users = users;
    this.authorization = authorization;
  }

  @GetMapping("/admin/users")
  UserPage list(@RequestParam(defaultValue = "50") int limit) {
    var user = CurrentUserContext.require();
    authorization.requireAdmin(user);
    return new UserPage(users.list(limit).stream()
        .map(u -> new CurrentUser(u.userId(), u.email(), u.fullName(), u.role(), u.status(), u.lastLoginAt()))
        .toList(), new Page(limit, null, false));
  }

  @PatchMapping("/admin/users/{userId}/status")
  UserStatusResponse updateStatus(@PathVariable java.util.UUID userId,
      @Valid @RequestBody UpdateUserStatusRequest request) {
    authorization.requireAdmin(CurrentUserContext.require());
    var updated = users.updateStatus(userId, request.status());
    return new UserStatusResponse(updated.userId(), updated.status());
  }
}
