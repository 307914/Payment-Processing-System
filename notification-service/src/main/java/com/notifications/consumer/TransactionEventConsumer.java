package com.notifications.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifications.dto.TransactionEvent;
import com.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @KafkaListener(
            topics = "transaction-events",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleTransactionEvent(String message, Acknowledgment acknowledgment) {
        try {
            TransactionEvent event = objectMapper.readValue(message, TransactionEvent.class);

            log.info("Received transaction event: type={}, reference={}, email={}",
                    event.getType(), event.getReferenceNumber(), event.getUserEmail());

            if (event.getUserEmail() == null) {
                log.warn("No email in event, skipping notification: ref={}", event.getReferenceNumber());
                acknowledgment.acknowledge();
                return;
            }

            switch (event.getType()) {
                case "DEPOSIT" -> notificationService.sendDepositNotification(
                        event.getUserEmail(), event.getAmount(),
                        event.getDestinationAccountNumber(), event.getReferenceNumber());

                case "WITHDRAWAL" -> notificationService.sendWithdrawalNotification(
                        event.getUserEmail(), event.getAmount(),
                        event.getSourceAccountNumber(), event.getReferenceNumber());

                case "TRANSFER" -> handleTransfer(event);

                case "REVERSAL" -> handleReversal(event);

                default -> log.warn("Unknown transaction type: {}", event.getType());
            }

            acknowledgment.acknowledge();

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize transaction event: {}", e.getMessage());
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing transaction event: {}", e.getMessage());
        }
    }

    private void handleTransfer(TransactionEvent event) {
        // Notify sender
        notificationService.sendTransferSenderNotification(
                event.getUserEmail(), event.getAmount(),
                event.getSourceAccountNumber(), event.getDestinationAccountNumber(),
                event.getReferenceNumber());

        // Notify receiver
        if (event.getReceiverEmail() != null) {
            notificationService.sendTransferReceiverNotification(
                    event.getReceiverEmail(), event.getAmount(),
                    event.getSourceAccountNumber(), event.getDestinationAccountNumber(),
                    event.getReferenceNumber());
        }
    }

    private void handleReversal(TransactionEvent event) {
        // Notify the user who initiated the reversal
        notificationService.sendReversalNotification(
                event.getUserEmail(), event.getAmount(), event.getReferenceNumber());

        // Notify the other party affected by the reversal
        if (event.getReceiverEmail() != null) {
            notificationService.sendReversalNotification(
                    event.getReceiverEmail(), event.getAmount(), event.getReferenceNumber());
        }
    }
}
