package com.etfcompass.backend.config;

import com.etfcompass.backend.service.LoggingPasswordResetDeliveryService;
import com.etfcompass.backend.service.PasswordResetDeliveryService;
import com.etfcompass.backend.service.SmtpPasswordResetDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class PasswordResetDeliveryConfig {

  private final Environment environment;
  private final MailProperties mailProperties;

  @Bean
  PasswordResetDeliveryService passwordResetDeliveryService(JavaMailSender mailSender) {
    String deliveryMode = environment.getProperty("app.password-reset-delivery", "logging");
    String frontendUrl = environment.getProperty("app.frontend-url", "http://localhost:4200");

    if ("smtp".equalsIgnoreCase(deliveryMode.trim())) {
      log.info("Password reset delivery: SMTP");
      return new SmtpPasswordResetDeliveryService(mailSender, mailProperties, frontendUrl);
    }

    log.info("Password reset delivery: logging (codes visible in server logs)");
    return new LoggingPasswordResetDeliveryService();
  }
}
