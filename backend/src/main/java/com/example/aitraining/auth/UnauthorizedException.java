package com.example.aitraining.auth;

/**
 * Thrown when a request arrives without a valid bearer token or with a token that resolves to
 * an inactive or unknown user.
 *
 * <p>Mapped to HTTP 401 (Unauthorized) by
 * {@link com.example.aitraining.config.ApiExceptionHandler}.
 */
public class UnauthorizedException extends RuntimeException {

  /**
   * @param message human-readable explanation included in the API error response
   */
  public UnauthorizedException(String message) {
    super(message);
  }
}
