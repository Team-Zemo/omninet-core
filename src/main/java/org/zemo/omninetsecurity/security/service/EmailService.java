package org.zemo.omninetsecurity.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendOtpEmail(String to, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("OmniNet Security - Email Verification Code");
            message.setText(buildOtpEmailContent(otp));
            message.setFrom("noreply@omninet.com");

            mailSender.send(message);
            log.info("OTP email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", to, e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    public void sendWelcomeEmail(String to, String name) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Welcome to OmniNet Security");
            message.setText(buildWelcomeEmailContent(name));
            message.setFrom("noreply@omninet.com");

            mailSender.send(message);
            log.info("Welcome email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", to, e);
            // Don't throw exception for welcome email failure
        }
    }

    public void sendAccountMergeNotification(String to, String name, String newProvider) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("OmniNet Security - Account Merged");
            message.setText(buildAccountMergeEmailContent(name, newProvider));
            message.setFrom("noreply@omninet.com");

            mailSender.send(message);
            log.info("Account merge notification sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send account merge notification to: {}", to, e);
            // Don't throw exception for notification email failure
        }
    }

    private String buildOtpEmailContent(String otp) {
        return String.format("""
            Hello,
            
            Your OmniNet Security verification code is: %s
            
            This code will expire in 15 minutes. Please do not share this code with anyone.
            
            If you didn't request this verification code, please ignore this email.
            
            Best regards,
            OmniNet Security Team
            """, otp);
    }

    private String buildWelcomeEmailContent(String name) {
        return String.format("""
            Hello %s,
            
            Welcome to OmniNet Security! Your account has been successfully created.
            
            You can now access your dashboard and manage your security settings.
            
            If you have any questions, please don't hesitate to contact our support team.
            
            Best regards,
            OmniNet Security Team
            """, name);
    }

    private String buildAccountMergeEmailContent(String name, String newProvider) {
        return String.format("""
            Hello %s,
            
            Your OmniNet Security account has been successfully merged with your %s account.
            
            You can now use either authentication method to access your account.
            
            If you didn't initiate this merge, please contact our support team immediately.
            
            Best regards,
            OmniNet Security Team
            """, name, newProvider);
    }
}
