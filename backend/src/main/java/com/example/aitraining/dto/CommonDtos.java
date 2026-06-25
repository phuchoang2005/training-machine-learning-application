package com.example.aitraining.dto;

import java.util.List;
import java.util.UUID;

/**
 * <b>DTO Pattern</b> — shared transfer objects used across multiple API areas.
 *
 * <p>All records here are response-only (read by the client) except where noted.
 */
public final class CommonDtos {
  private CommonDtos() {
  }

  /**
   * Pagination envelope used in all list responses.
   *
   * @param limit      the page size that was requested
   * @param nextCursor opaque cursor for the next page; {@code null} when there is no next page
   * @param hasMore    {@code true} when additional pages exist
   */
  public record Page(int limit, String nextCursor, boolean hasMore) {
  }

  /**
   * Structured error details returned inside {@link ErrorResponse}.
   *
   * @param code          machine-readable error code (e.g. {@code "NOT_FOUND"})
   * @param message       human-readable description
   * @param correlationId unique ID per error instance for log correlation
   * @param details       field-level validation errors; empty for non-validation errors
   */
  public record ApiError(String code, String message, String correlationId, List<ValidationDetail> details) {
  }

  /**
   * A single field-level validation failure within an {@link ApiError}.
   *
   * @param field  the request field that failed validation
   * @param reason human-readable explanation of the failure
   */
  public record ValidationDetail(String field, String reason) {
  }

  /**
   * Standard error envelope returned on all non-2xx responses.
   */
  public record ErrorResponse(ApiError error) {
  }

  /**
   * Lightweight user reference embedded in job and project responses.
   */
  public record UserSummary(UUID userId, String email, String fullName) {
  }
}
