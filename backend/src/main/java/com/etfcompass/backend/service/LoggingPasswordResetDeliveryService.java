package com.etfcompass.backend.service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggingPasswordResetDeliveryService implements PasswordResetDeliveryService {

  @Override
  public void sendPasswordResetCode(String email, String displayName, String code, long expirationMinutes) {
    log.warn(
        "SMTP is not configured. Password reset code for {} ({}): {}. Expires in {} minutes.",
        displayName,
        email,
        code,
        expirationMinutes);
  }
}
