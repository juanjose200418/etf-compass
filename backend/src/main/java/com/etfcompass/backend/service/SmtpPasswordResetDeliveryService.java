package com.etfcompass.backend.service;

import com.etfcompass.backend.config.MailProperties;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

@Slf4j
public class SmtpPasswordResetDeliveryService implements PasswordResetDeliveryService {

  private final JavaMailSender mailSender;
  private final MailProperties mailProperties;
  private final String frontendUrl;

  public SmtpPasswordResetDeliveryService(JavaMailSender mailSender, MailProperties mailProperties, String frontendUrl) {
    this.mailSender = mailSender;
    this.mailProperties = mailProperties;
    this.frontendUrl = frontendUrl;
  }

  @Override
  public void sendPasswordResetCode(String email, String displayName, String code, long expirationMinutes) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setFrom(mailProperties.fromAddress());
      helper.setTo(email);
      helper.setSubject("ETF Compass - codigo de recuperacion");
      helper.setText(buildHtmlBody(displayName, code, expirationMinutes), true);
      mailSender.send(message);
      log.info("Password reset email sent to {}", email);
    } catch (MailSendException e) {
      String rootCause = e.getRootCause() != null ? e.getRootCause().getClass().getSimpleName() : "unknown";
      log.error("SMTP {} to {}: {}. Check SMTP_HOST={}, SMTP_PORT={}, SMTP_SMTP_SSL={}, SMTP_SMTP_STARTTLS={}",
          rootCause, email, e.getRootCause() != null ? e.getRootCause().getMessage() : e.getMessage(),
          System.getenv("SMTP_HOST"), System.getenv("SMTP_PORT"),
          System.getenv("SMTP_SMTP_SSL"), System.getenv("SMTP_SMTP_STARTTLS"));
      throw new RuntimeException("Mail server connection failed", e);
    } catch (Exception e) {
      log.error("SMTP error for {}: {} ({}). Check SMTP_HOST={}, SMTP_PORT={}.",
          email, e.getMessage(), e.getClass().getSimpleName(),
          System.getenv("SMTP_HOST"), System.getenv("SMTP_PORT"));
      throw new RuntimeException("Failed to send password reset email", e);
    }
  }

  private String buildHtmlBody(String displayName, String code, long expirationMinutes) {
    String safeName = displayName == null || displayName.isBlank() ? "inversor" : escapeHtml(displayName.trim());
    String escapedCode = escapeHtml(code);

    return """
        <!DOCTYPE html>
        <html>
        <head><meta charset="UTF-8"></head>
        <body style="font-family: Arial, Helvetica, sans-serif; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
          <div style="text-align: center; margin-bottom: 24px;">
            <h1 style="color: #1a73e8; margin: 0;">ETF Compass</h1>
            <p style="color: #666; font-size: 14px;">Recuperacion de contrasena</p>
          </div>
          <div style="background: #f5f7fa; border-radius: 12px; padding: 24px;">
            <p style="font-size: 16px; margin-top: 0;">Hola <strong>%s</strong>,</p>
            <p>Hemos recibido una solicitud para restablecer la contrasena de tu cuenta.</p>
            <div style="text-align: center; margin: 24px 0;">
              <div style="display: inline-block; background: #1a73e8; color: #fff; font-size: 28px; font-weight: bold; letter-spacing: 8px; padding: 16px 32px; border-radius: 8px;">
                %s
              </div>
            </div>
            <p style="color: #666; font-size: 14px;">Este codigo caduca en <strong>%d minutos</strong>.</p>
            <p style="color: #666; font-size: 14px;">Si no has solicitado este cambio, puedes ignorar este correo.</p>
            <hr style="border: none; border-top: 1px solid #ddd; margin: 20px 0;">
            <p style="font-size: 12px; color: #999; text-align: center;">
              ETF Compass &mdash; <a href="%s" style="color: #1a73e8;">%s</a>
            </p>
          </div>
        </body>
        </html>
        """.formatted(safeName, escapedCode, expirationMinutes, frontendUrl, frontendUrl);
  }

  private static String escapeHtml(String input) {
    return input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }
}
