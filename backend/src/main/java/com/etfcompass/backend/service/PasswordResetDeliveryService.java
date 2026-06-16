package com.etfcompass.backend.service;

public interface PasswordResetDeliveryService {

  void sendPasswordResetCode(String email, String displayName, String code, long expirationMinutes);
}
