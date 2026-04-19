package com.collaboration.service;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final Environment env;

    public EmailService(JavaMailSender mailSender, Environment env) {
        this.mailSender = mailSender;
        this.env = env;
    }

    public void sendEmail(String to, String subject, String htmlBody) {
        if (to == null || to.isBlank()) {
            log.warn("[EmailService] Cannot send email without recipient address");
            return;
        }

        String from = env.getProperty("spring.mail.username", "no-reply@codecollab.local");

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("[EmailService] Email sent to {} with subject={}", to, subject);
        } catch (MessagingException e) {
            log.warn("[EmailService] Email build failed for {}: {}", to, e.getMessage(), e);
        } catch (Exception e) {
            log.warn("[EmailService] Email send failed for {}: {}", to, e.getMessage(), e);
            log.info("[EmailService] Email body:\n{}", htmlBody);
        }
    }

    public void sendVerificationToken(String email, String verificationToken) {
        String verificationLink = String.format("%s/api/verify-email?token=%s", env.getProperty("app.base.url", "http://localhost:8080"), verificationToken);
        String subject = "Verify your CodeCollab account";
        String body = "<p>Welcome to CodeCollab!</p>"
                + "<p>Please verify your email by clicking the link below:</p>"
                + "<p><a href=\"" + verificationLink + "\">Verify email</a></p>";
        sendEmail(email, subject, body);
    }

    public void sendPasswordResetToken(String email, String resetToken) {
        String resetLink = String.format("%s/index.html?reset_token=%s", env.getProperty("frontend.base.url", "http://localhost:8000"), resetToken);
        String subject = "Reset your CodeCollab password";
        String body = "<p>We received a request to reset your password.</p>"
                + "<p>Use the token below or click the link to reset your password:</p>"
                + "<p><strong>" + resetToken + "</strong></p>"
                + "<p><a href=\"" + resetLink + "\">Reset password</a></p>";
        sendEmail(email, subject, body);
    }
}
