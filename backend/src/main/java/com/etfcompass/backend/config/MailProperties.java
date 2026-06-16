package com.etfcompass.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(
    String fromAddress
) {

  public MailProperties {
    if (fromAddress == null || fromAddress.isBlank()) {
      throw new IllegalStateException("MAIL_FROM must be configured before the backend starts");
    }
  }
}
