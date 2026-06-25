package com.example.aitraining.dto;

import com.example.aitraining.domain.Enums.UserRole;
import com.example.aitraining.domain.Enums.UserStatus;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * <b>DTO Pattern</b> — request/response transfer objects for user management endpoints.
 */
public final class UserDtos {
  private UserDtos() {
  }

  /**
   * Identity response for {@code GET /auth/me} and user list entries.
   */
  public record CurrentUser(UUID userId, String email, String fullName, UserRole role, UserStatus status,
      Instant lastLoginAt) {
  }

  /** Paginated user list (admin only). */
  public record UserPage(List<CurrentUser> data, CommonDtos.Page page) {
  }

  /**
   * Request body for {@code PATCH /admin/users/{userId}/status}.
   *
   * @param status the new status to set; must be {@code ACTIVE} or {@code DISABLED}
   */
  public record UpdateUserStatusRequest(@NotNull UserStatus status) {
  }

  /** Minimal response confirming the updated status. */
  public record UserStatusResponse(UUID userId, UserStatus status) {
  }
}
