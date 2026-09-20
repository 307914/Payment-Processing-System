package com.notifications.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;

    public void sendDepositNotification(String email, BigDecimal amount, String accountNumber,
                                         String referenceNumber) {
        String subject = "Deposit Received - " + referenceNumber;
        String body = String.format(
                "Dear Customer,\n\n"
                + "A deposit of %s has been credited to your account %s.\n"
                + "Reference: %s\n\n"
                + "If you did not initiate this transaction, please contact support immediately.\n\n"
                + "Regards,\nPayments Team",
                amount, accountNumber, referenceNumber
        );

        sendEmail(email, subject, body);
    }

    public void sendWithdrawalNotification(String email, BigDecimal amount, String accountNumber,
                                            String referenceNumber) {
        String subject = "Withdrawal Processed - " + referenceNumber;
        String body = String.format(
                "Dear Customer,\n\n"
                + "A withdrawal of %s has been debited from your account %s.\n"
                + "Reference: %s\n\n"
                + "If you did not initiate this transaction, please contact support immediately.\n\n"
                + "Regards,\nPayments Team",
                amount, accountNumber, referenceNumber
        );

        sendEmail(email, subject, body);
    }

    public void sendTransferSenderNotification(String email, BigDecimal amount, String sourceAccount,
                                                String destinationAccount, String referenceNumber) {
        String subject = "Transfer Sent - " + referenceNumber;
        String body = String.format(
                "Dear Customer,\n\n"
                + "A transfer of %s from your account %s to account %s has been completed.\n"
                + "Reference: %s\n\n"
                + "If you did not initiate this transaction, please contact support immediately.\n\n"
                + "Regards,\nPayments Team",
                amount, sourceAccount, destinationAccount, referenceNumber
        );

        sendEmail(email, subject, body);
    }

    public void sendTransferReceiverNotification(String email, BigDecimal amount, String sourceAccount,
                                                  String destinationAccount, String referenceNumber) {
        String subject = "Transfer Received - " + referenceNumber;
        String body = String.format(
                "Dear Customer,\n\n"
                + "A transfer of %s has been received into your account %s from account %s.\n"
                + "Reference: %s\n\n"
                + "Regards,\nPayments Team",
                amount, destinationAccount, sourceAccount, referenceNumber
        );

        sendEmail(email, subject, body);
    }

    public void sendReversalNotification(String email, BigDecimal amount, String referenceNumber) {
        String subject = "Transaction Reversed - " + referenceNumber;
        String body = String.format(
                "Dear Customer,\n\n"
                + "A transaction of %s has been reversed.\n"
                + "Reference: %s\n\n"
                + "If you did not initiate this reversal, please contact support immediately.\n\n"
                + "Regards,\nPayments Team",
                amount, referenceNumber
        );

        sendEmail(email, subject, body);
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            message.setFrom("noreply@payments.com");

            mailSender.send(message);
            log.info("Email sent to {} with subject: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
