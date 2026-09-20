package com.payments.dto.event;

import com.payments.entity.Transaction;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class TransactionEventPayload {

    private Long id;
    private String type;
    private BigDecimal amount;
    private String status;
    private String referenceNumber;
    private String sourceAccountNumber;
    private String destinationAccountNumber;
    private String description;
    private String userEmail;
    private String receiverEmail;
    private Instant createdAt;

    public static TransactionEventPayload from(Transaction transaction, String userEmail,
                                                String receiverEmail) {
        return TransactionEventPayload.builder()
                .id(transaction.getId())
                .type(transaction.getType().name())
                .amount(transaction.getAmount())
                .status(transaction.getStatus().name())
                .referenceNumber(transaction.getReferenceNumber())
                .sourceAccountNumber(transaction.getSourceAccount() != null
                        ? transaction.getSourceAccount().getAccountNumber() : null)
                .destinationAccountNumber(transaction.getDestinationAccount() != null
                        ? transaction.getDestinationAccount().getAccountNumber() : null)
                .description(transaction.getDescription())
                .userEmail(userEmail)
                .receiverEmail(receiverEmail)
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
