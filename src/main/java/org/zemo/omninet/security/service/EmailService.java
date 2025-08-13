package org.zemo.omninet.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendOtpEmail(String to, String otp) {
        try {
            String subject = "OmniNet - Email Verification Code";
            String content = buildOtpEmailContent(otp);

            sendHtmlEmail(to, subject, content);

            log.info("OTP email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", to, e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    public void sendWelcomeEmail(String to, String name) {
        try {
            String subject = "Welcome to OmniNet";
            String content = buildWelcomeEmailContent(name);

            sendHtmlEmail(to, subject, content);

            log.info("Welcome email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", to, e);
        }
    }

    public void sendAccountMergeNotification(String to, String name, String newProvider) {
        try {
            String subject = "OmniNet - Account Merged";
            String content = buildAccountMergeEmailContent(name, newProvider);

            sendHtmlEmail(to, subject, content);

            log.info("Account merge notification sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send account merge notification to: {}", to, e);
        }
    }

    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = isHtml
            helper.setFrom("noreply@omninet.com");

            mailSender.send(mimeMessage);
        } catch (Exception e) {
            log.error("Failed to send HTML email to: {}", to, e);
            throw new RuntimeException("Failed to send HTML email", e);
        }
    }

    private String buildOtpEmailContent(String otp) {
        return String.format("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>OmniNet Verification Code</title>
                <style>
                    body {
                        font-family: 'Segoe UI', Arial, sans-serif;
                        background: linear-gradient(120deg, #4CAF50 0%%, #2E7D32 100%%);
                        margin: 0;
                        padding: 0;
                        min-height: 100vh;
                    }
                    .container {
                        max-width: 480px;
                        margin: 40px auto;
                        background: #fff;
                        border-radius: 16px;
                        box-shadow: 0 8px 32px rgba(76,175,80,0.15);
                        padding: 32px 24px;
                        overflow: hidden;
                    }
                    .header {
                        text-align: center;
                        margin-bottom: 20px;
                    }
                    /* .header img {
                        width: 60px;
                        margin-bottom: 8px;
                    } */
                    .header h1 {
                        margin: 0;
                        font-size: 2rem;
                        color: #2E7D32;
                        font-weight: bold;
                        letter-spacing: 1px;
                    }
                    .otp-box {
                        background: #e8f5e9;
                        border: 2px solid #4CAF50;
                        color: #222;
                        font-size: 2rem;
                        font-weight: bold;
                        letter-spacing: 8px;
                        text-align: center;
                        padding: 18px;
                        margin: 22px 0;
                        border-radius: 10px;
                    }
                    .message {
                        font-size: 1.1rem;
                        color: #333;
                        line-height: 1.7;
                        margin-bottom: 22px;
                    }
                    .cta {
                        text-align: center;
                    }
                    .cta-button {
                        background: linear-gradient(90deg, #4CAF50, #43A047);
                        color: #fff !important;
                        padding: 12px 28px;
                        border-radius: 6px;
                        text-decoration: none;
                        font-weight: bold;
                        font-size: 1rem;
                        box-shadow: 0 2px 8px rgba(76,175,80,0.13);
                        transition: background 0.2s;
                        margin-top: 14px;
                        display: inline-block;
                    }
                    .cta-button:hover {
                        background: linear-gradient(90deg, #43A047, #4CAF50);
                    }
                    .footer {
                        font-size: 0.95rem;
                        color: #aaa;
                        text-align: center;
                        margin-top: 28px;
                        border-top: 1px solid #e0e0e0;
                        padding-top: 12px;
                    }
                    @media (max-width: 600px) {
                        .container { padding: 18px 8px; }
                        .header h1 { font-size: 1.4rem; }
                        .otp-box { font-size: 1.3rem; }
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <img src="https://i.ibb.co/KcLkmC1T/IMG-20250812-WA0000.jpg" alt="OmniNet Logo"> 
                        <h1>Team Zemo | OmniNet</h1>
                    </div>
                    <div class="message">
                        <p>Hello,</p>
                        <p>Your <strong>OmniNet</strong> verification code is:</p>
                    </div>
                    <div class="otp-box">%s</div>
                    <div class="message">
                        <p>This code will expire in <strong>15 minutes</strong>. Please do not share this code with anyone.</p>
                        <p>If you didn't request this verification code, you can safely ignore this email.</p>
                    </div>
                    <div class="cta">
                        <a href="https://github.com/Team-Zemo" class="cta-button">Contact Support</a>
                    </div>
                    <div class="footer">
                        <p>Best regards,<br>Team Zemo - OmniNet</p>
                        <p style="margin-top:8px;">© 2025 Team Zemo. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """, otp);
    }

    private String buildWelcomeEmailContent(String name) {
        return String.format("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Welcome to OmniNet</title>
                <style>
                    body {
                        font-family: 'Segoe UI', Arial, sans-serif;
                        background: linear-gradient(120deg, #43A047 0%%, #388E3C 100%%);
                        margin: 0;
                        padding: 0;
                        min-height: 100vh;
                    }
                    .container {
                        max-width: 520px;
                        margin: 40px auto;
                        background: #fff;
                        border-radius: 16px;
                        box-shadow: 0 8px 32px rgba(67,160,71,0.17);
                        padding: 36px 28px;
                        overflow: hidden;
                    }
                    .header {
                        text-align: center;
                        margin-bottom: 18px;
                    }
                    /* .header img {
                        width: 64px;
                        margin-bottom: 8px;
                    } */
                    .header h1 {
                        margin: 0;
                        font-size: 2.2rem;
                        color: #388E3C;
                        font-weight: bold;
                        letter-spacing: 1px;
                    }
                    .welcome-banner {
                        text-align: center;
                        background: linear-gradient(135deg, #4CAF50, #2E7D32);
                        color: white;
                        padding: 18px;
                        border-radius: 10px;
                        margin: 24px 0;
                        font-size: 1.25rem;
                        font-weight: bold;
                    }
                    .message {
                        font-size: 1.13rem;
                        color: #333;
                        line-height: 1.7;
                        margin-bottom: 22px;
                    }
                    .cta {
                        text-align: center;
                    }
                    .cta-button {
                        background: linear-gradient(90deg, #43A047, #4CAF50);
                        color: #fff !important;
                        padding: 14px 30px;
                        border-radius: 7px;
                        text-decoration: none;
                        font-weight: bold;
                        font-size: 1.07rem;
                        box-shadow: 0 2px 8px rgba(67,160,71,0.15);
                        transition: background 0.2s;
                        margin-top: 18px;
                        display: inline-block;
                    }
                    .cta-button:hover {
                        background: linear-gradient(90deg, #4CAF50, #43A047);
                    }
                    .footer {
                        font-size: 1rem;
                        color: #aaa;
                        text-align: center;
                        margin-top: 30px;
                        border-top: 1px solid #e0e0e0;
                        padding-top: 14px;
                    }
                    @media (max-width: 600px) {
                        .container { padding: 18px 8px; }
                        .header h1 { font-size: 1.3rem; }
                        .welcome-banner { font-size: 1rem; }
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                                <img src="https://i.ibb.co/KcLkmC1T/IMG-20250812-WA0000.jpg" alt="OmniNet Logo"> 
                        <h1>Team Zemo | OmniNet</h1>
                    </div>
                    <div class="welcome-banner">
                        🎉 Welcome to OmniNet, %s! 🎉
                    </div>
                    <div class="message">
                        <p>We’re excited to have you onboard! Your account has been successfully created.</p>
                        <p>You can now access your dashboard and manage your security settings with ease.</p>
                        <p>If you have any questions, our support team is here to help you anytime.</p>
                    </div>
                    <div class="cta">
                        <a href="https://github.com/Team-Zemo" class="cta-button">Go to Dashboard</a>
                    </div>
                    <div class="footer">
                        <p>Best regards,<br>Team Zemo - OmniNet</p>
                        <p style="margin-top:8px;">© 2025 Team Zemo. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """, name);
    }

    private String buildAccountMergeEmailContent(String name, String newProvider) {
        return String.format("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Account Merge Notification</title>
                <style>
                    body {
                        font-family: 'Segoe UI', Arial, sans-serif;
                        background: linear-gradient(120deg, #1DE9B6 0%%, #00BFAE 100%%);
                        margin: 0;
                        padding: 0;
                        min-height: 100vh;
                    }
                    .container {
                        max-width: 500px;
                        margin: 44px auto;
                        background: #fff;
                        border-radius: 16px;
                        box-shadow: 0 8px 32px rgba(30,233,182,0.14);
                        padding: 34px 26px;
                        overflow: hidden;
                    }
                    .header {
                        text-align: center;
                        margin-bottom: 16px;
                    }
                    /* .header img {
                        width: 62px;
                        margin-bottom: 8px;
                    } */
                    .header h1 {
                        margin: 0;
                        font-size: 2rem;
                        color: #00897B;
                        font-weight: bold;
                        letter-spacing: 1px;
                    }
                    .merge-banner {
                        text-align: center;
                        background: linear-gradient(135deg, #1DE9B6, #00BFAE);
                        color: white;
                        padding: 16px;
                        border-radius: 10px;
                        margin: 22px 0;
                        font-size: 1.18rem;
                        font-weight: bold;
                    }
                    .message {
                        font-size: 1.13rem;
                        color: #333;
                        line-height: 1.65;
                        margin-bottom: 18px;
                    }
                    .highlight {
                        color: #00BFAE;
                        font-weight: bold;
                    }
                    .cta {
                        text-align: center;
                    }
                    .cta-button {
                        background: linear-gradient(90deg, #00BFAE, #1DE9B6);
                        color: #fff !important;
                        padding: 11px 26px;
                        border-radius: 6px;
                        text-decoration: none;
                        font-weight: bold;
                        font-size: 1.05rem;
                        box-shadow: 0 2px 8px rgba(30,233,182,0.12);
                        transition: background 0.2s;
                        margin-top: 14px;
                        display: inline-block;
                    }
                    .cta-button:hover {
                        background: linear-gradient(90deg, #1DE9B6, #00BFAE);
                    }
                    .footer {
                        font-size: 0.98rem;
                        color: #aaa;
                        text-align: center;
                        margin-top: 26px;
                        border-top: 1px solid #e0e0e0;
                        padding-top: 12px;
                    }
                    @media (max-width: 600px) {
                        .container { padding: 16px 6px; }
                        .header h1 { font-size: 1.2rem; }
                        .merge-banner { font-size: 1rem; }
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                               <img src="https://i.ibb.co/KcLkmC1T/IMG-20250812-WA0000.jpg" alt="OmniNet Logo"> 
                        <h1>Team Zemo | OmniNet</h1>
                    </div>
                    <div class="merge-banner">
                        🔗 Accounts Successfully Merged!
                    </div>
                    <div class="message">
                        <p>Hello %s,</p>
                        <p>Your <strong>OmniNet</strong> account has been successfully merged with your <span class="highlight">%s</span> account.</p>
                        <p>You can now use either authentication method to access your account securely.</p>
                        <p>If you didn’t initiate this merge, please <a href="https://github.com/Team-Zemo" style="color:#00BFAE; font-weight:bold;">contact our support team</a> immediately.</p>
                    </div>
                    <div class="cta">
                        <a href="https://github.com/Team-Zemo" class="cta-button">Contact Support</a>
                    </div>
                    <div class="footer">
                        <p>Best regards,<br>Team Zemo - OmniNet</p>
                        <p style="margin-top:8px;">© 2025 Team Zemo. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """, name, newProvider);
    }
}