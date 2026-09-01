package com.rentify.email;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@rentify.campus}")
    private String fromEmail;

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    @Async
    public void sendRentalRequestEmail(String toEmail, String ownerName, String renterName, String itemTitle, Long rentalId) {
        String subject = "New Rental Request for " + itemTitle;
        String content = """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;">
                <h2 style="color: #4F46E5;">Rentify Rental Notification</h2>
                <p>Hello %s,</p>
                <p><strong>%s</strong> has requested to rent your item: <strong>%s</strong>.</p>
                <p>Please log in to your Rentify dashboard to approve or decline this request.</p>
                <p style="margin-top: 30px; font-size: 12px; color: #6B7280;">Rentify - Campus Equipment Sharing Platform</p>
            </div>
            """.formatted(ownerName, renterName, itemTitle);

        sendHtmlEmail(toEmail, subject, content);
    }

    @Async
    public void sendRentalStatusEmail(String toEmail, String recipientName, String status, String itemTitle, Long rentalId) {
        String subject = "Rental Update: " + itemTitle + " is now " + status;
        String content = """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;">
                <h2 style="color: #4F46E5;">Rental Status Update</h2>
                <p>Hello %s,</p>
                <p>Your rental for <strong>%s</strong> has been updated to: <span style="font-weight: bold; color: #059669;">%s</span>.</p>
                <p style="margin-top: 30px; font-size: 12px; color: #6B7280;">Rentify - Campus Equipment Sharing Platform</p>
            </div>
            """.formatted(recipientName, itemTitle, status);

        sendHtmlEmail(toEmail, subject, content);
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        if (!emailEnabled || mailSender == null) {
            log.info("[SIMULATED EMAIL] To: {} | Subject: {}", to, subject);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent successfully to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
