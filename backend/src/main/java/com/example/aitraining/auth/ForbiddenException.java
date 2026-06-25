package com.example.aitraining.auth;

/**
 * Thrown when the authenticated user does not have sufficient permission to access a resource.
 *
 * <p>Mapped to HTTP 403 (Forbidden) by
 * {@link com.example.aitraining.config.ApiExceptionHandler}.
 */
public class ForbiddenException extends RuntimeException {

  /**
   * @param message human-readable explanation included in the API error response
   */
  public ForbiddenException(String message) {
    super(message);
  }
}
