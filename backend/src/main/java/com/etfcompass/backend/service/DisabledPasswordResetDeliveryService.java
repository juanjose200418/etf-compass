package com.etfcompass.backend.service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DisabledPasswordResetDeliveryService implements PasswordResetDeliveryService {

  @Override
  public void sendPasswordResetCode(String email, String displayName, String code, long expirationMinutes) {
    log.error("Password reset email is disabled because SMTP is not configured for the active production profile.");
    throw new IllegalStateException("Password reset email is not configured");
  }
}
