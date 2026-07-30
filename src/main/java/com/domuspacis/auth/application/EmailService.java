package com.domuspacis.auth.application;

import com.domuspacis.aop.annotation.SensitiveParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.MimeMessagePreparator;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from-email:noreply@domuspacis.rw}")
    private String fromEmail;

    @Value("${app.mail.from-name:Domus Pacis Platform}")
    private String fromName;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public void sendPasswordResetEmail(String toEmail, String firstName, @SensitiveParam String resetToken) {
        try {
            String resetUrl = frontendUrl + "/reset-password?token=" + resetToken;
            String displayName = firstName != null ? firstName : "User";

            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #2c5f2d; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                        .content { background-color: #f9f9f9; padding: 30px; border: 1px solid #ddd; }
                        .button { display: inline-block; background-color: #2c5f2d; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                        .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #666; }
                        .warning { background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }
                    </style>
                </head>
                <body>
                    <div class="header">
                        <h1>Domus Pacis Platform</h1>
                    </div>
                    <div class="content">
                        <p>Dear %s,</p>
                        <p>We received a request to reset your password for your Domus Pacis Platform account.</p>
                        <p>Click the button below to reset your password:</p>
                        <a href="%s" class="button">Reset Password</a>
                        <p>Or copy and paste this link into your browser:</p>
                        <p style="word-break: break-all; color: #2c5f2d;">%s</p>
                        <div class="warning">
                            <strong>Important:</strong> This link will expire in %d hours for security reasons.
                        </div>
                        <p>If you did not request a password reset, please ignore this email or contact support if you have concerns.</p>
                        <p>Best regards,<br>Domus Pacis Platform Team</p>
                    </div>
                    <div class="footer">
                        <p>© 2024 Domus Pacis Platform - Catholic Archdiocese of Kigali</p>
                        <p>This is an automated message, please do not reply to this email.</p>
                    </div>
                </body>
                </html>
                """,
                displayName, resetUrl, resetUrl, 24
            );

            MimeMessagePreparator messagePreparator = message -> {
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(fromEmail, fromName);
                helper.setTo(toEmail);
                helper.setSubject("Password Reset Request - Domus Pacis Platform");
                helper.setText(htmlContent, true);
            };

            mailSender.send(messagePreparator);
            log.info("Password reset email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
            throw new EmailSendException("Failed to send password reset email", e);
        }
    }

    public static class EmailSendException extends RuntimeException {
        public EmailSendException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}