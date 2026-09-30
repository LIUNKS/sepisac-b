package com.sepisac.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String username;
    private final String fromEmail;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String username,
            @Value("${mail.from.email:}") String fromEmail) {
        this.mailSender = mailSender;
        this.username = username != null ? username.trim() : "";
        this.fromEmail = (fromEmail != null && !fromEmail.isBlank()) ? fromEmail.trim() : this.username;
    }

    public void send2FaCode(String toEmail, String code) {
        if (username.isEmpty() || username.startsWith("tu_correo")) {
            log.warn("SMTP no configurado en MAIL_USERNAME. Código 2FA simulado para [{}]: {}", toEmail, code);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            helper.setFrom(fromEmail.isEmpty() ? username : fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Código de verificación 2FA - SEPI S.A.C.");

            String html = String.format(
                    "<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;border:1px solid #e0e0e0;border-radius:8px;'>"
                    + "<h2 style='color:#1e3a8a;'>SEPI S.A.C. - Verificación de Seguridad</h2>"
                    + "<p>Has solicitado iniciar sesión. Tu código de verificación de 2 factores es:</p>"
                    + "<p style='font-size:26px;font-weight:bold;letter-spacing:4px;color:#2563eb;background:#f3f4f6;padding:12px;text-align:center;border-radius:6px;'>%s</p>"
                    + "<p style='color:#6b7280;font-size:13px;'>Este código es de un solo uso y expirará en 5 minutos.</p>"
                    + "<p style='color:#6b7280;font-size:13px;'>Si no solicitaste este código, ignora este correo o cambia tu contraseña inmediatamente.</p>"
                    + "</div>",
                    code
            );

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Correo 2FA enviado vía SMTP exitosamente a {}", toEmail);
        } catch (MessagingException e) {
            log.error("Error al componer correo 2FA para {}", toEmail, e);
            throw new IllegalStateException("Error al componer correo 2FA", e);
        } catch (Exception e) {
            log.error("Excepción al enviar correo 2FA a {}", toEmail, e);
            throw new IllegalStateException("Fallo al enviar correo con código 2FA: " + e.getMessage(), e);
        }
    }
}
