package com.sepisac.backend.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Unit Tests")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Test
    @DisplayName("Should silently simulate sending email when username is empty")
    void shouldSimulateWhenUsernameIsEmpty() {
        EmailService emailService = new EmailService(mailSender, "", "");

        assertThatCode(() -> emailService.send2FaCode("test@sepisac.com", "123456"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should silently simulate sending email when username is placeholder")
    void shouldSimulateWhenUsernameIsPlaceholder() {
        EmailService emailService = new EmailService(mailSender, "tu_correo@gmail.com", "tu_correo@gmail.com");

        assertThatCode(() -> emailService.send2FaCode("test@sepisac.com", "123456"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should send email via JavaMailSender when credentials configured")
    void shouldSendEmailWhenConfigured() {
        EmailService emailService = new EmailService(mailSender, "real@gmail.com", "real@gmail.com");
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertThatCode(() -> emailService.send2FaCode("dest@gmail.com", "123456"))
                .doesNotThrowAnyException();

        verify(mailSender).send(mimeMessage);
    }
}
