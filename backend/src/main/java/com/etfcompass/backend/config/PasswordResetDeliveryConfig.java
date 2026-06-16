package com.etfcompass.backend.config;

import com.etfcompass.backend.service.DisabledPasswordResetDeliveryService;
import com.etfcompass.backend.service.LoggingPasswordResetDeliveryService;
import com.etfcompass.backend.service.PasswordResetDeliveryService;
import com.etfcompass.backend.service.SmtpPasswordResetDeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
@RequiredArgsConstructor
public class PasswordResetDeliveryConfig {

  private final Environment environment;
  private final MailProperties mailProperties;

  @Bean
  PasswordResetDeliveryService passwordResetDeliveryService(JavaMailSender mailSender) {
    String mailHost = environment.getProperty("spring.mail.host");
    if (mailHost != null && !mailHost.isBlank()) {
      return new SmtpPasswordResetDeliveryService(mailSender, mailProperties);
    }

    if (environment.acceptsProfiles(Profiles.of("prod"))) {
      return new DisabledPasswordResetDeliveryService();
    }

    return new LoggingPasswordResetDeliveryService();
  }
}
