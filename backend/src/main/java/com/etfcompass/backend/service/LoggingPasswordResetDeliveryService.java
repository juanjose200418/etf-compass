package com.etfcompass.backend.service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggingPasswordResetDeliveryService implements PasswordResetDeliveryService {

  @Override
  public void sendPasswordResetCode(String email, String displayName, String code, long expirationMinutes) {
    String maskedEmail = maskEmail(email);
    log.info("Password reset code generated for {}: {}. Expires in {} minutes. Use this code in the reset form.",
        maskedEmail, code, expirationMinutes);
  }

  private static String maskEmail(String email) {
    if (email == null || !email.contains("@")) return email;
    int atIndex = email.indexOf('@');
    String localPart = email.substring(0, atIndex);
    String domain = email.substring(atIndex);
    if (localPart.length() <= 2) {
      return localPart.charAt(0) + "***" + domain;
    }
    return localPart.substring(0, 2) + "***" + domain;
  }
}
