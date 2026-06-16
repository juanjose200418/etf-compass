package com.etfcompass.backend.service;

import com.etfcompass.backend.config.MailProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@RequiredArgsConstructor
public class SmtpPasswordResetDeliveryService implements PasswordResetDeliveryService {

  private final JavaMailSender mailSender;
  private final MailProperties mailProperties;

  @Override
  public void sendPasswordResetCode(String email, String displayName, String code, long expirationMinutes) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(mailProperties.fromAddress());
    message.setTo(email);
    message.setSubject("ETF Compass - codigo de recuperacion");
    message.setText(buildBody(displayName, code, expirationMinutes));
    mailSender.send(message);
  }

  private String buildBody(String displayName, String code, long expirationMinutes) {
    String safeName = displayName == null || displayName.isBlank() ? "inversor" : displayName.trim();
    return String.join("\n\n",
        "Hola " + safeName + ",",
        "Hemos recibido una solicitud para restablecer la contrasena de tu cuenta en ETF Compass.",
        "Tu codigo de verificacion es: " + code,
        "Este codigo caduca en " + expirationMinutes + " minutos.",
        "Si no has solicitado este cambio, puedes ignorar este correo.",
        "ETF Compass");
  }
}
