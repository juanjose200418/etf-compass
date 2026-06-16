package com.etfcompass.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
    String jwtSecret,
    long accessTokenExpirationMinutes,
    long passwordResetCodeExpirationMinutes
) {

  private static final String INSECURE_DEFAULT_SECRET = "replace-this-development-secret-with-a-strong-256-bit-production-secret";
  private static final int MIN_SECRET_LENGTH = 32;

  public SecurityProperties {
    if (jwtSecret == null || jwtSecret.isBlank()) {
      throw new IllegalStateException("JWT_SECRET must be configured before the backend starts");
    }
    if (INSECURE_DEFAULT_SECRET.equals(jwtSecret)) {
      throw new IllegalStateException("JWT_SECRET must not use the documented placeholder value");
    }
    if (jwtSecret.length() < MIN_SECRET_LENGTH) {
      throw new IllegalStateException("JWT_SECRET must be at least 32 characters long");
    }
    if (accessTokenExpirationMinutes <= 0) {
      throw new IllegalStateException("JWT access token expiration must be greater than zero");
    }
    if (passwordResetCodeExpirationMinutes <= 0) {
      throw new IllegalStateException("Password reset code expiration must be greater than zero");
    }
  }
}
