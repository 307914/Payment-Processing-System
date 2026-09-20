package com.payments.dto.response;

import com.payments.entity.Transaction;
import com.payments.entity.enums.TransactionStatus;
import com.payments.entity.enums.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class TransactionResponse {

    private Long id;
    private TransactionType type;
    private BigDecimal amount;
    private TransactionStatus status;
    private String referenceNumber;
    private String sourceAccountNumber;
    private String destinationAccountNumber;
    private String description;
    private String idempotencyKey;
    private Instant createdAt;

    public static TransactionResponse from(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .status(transaction.getStatus())
                .referenceNumber(transaction.getReferenceNumber())
                .sourceAccountNumber(transaction.getSourceAccount() != null
                        ? transaction.getSourceAccount().getAccountNumber() : null)
                .destinationAccountNumber(transaction.getDestinationAccount() != null
                        ? transaction.getDestinationAccount().getAccountNumber() : null)
                .description(transaction.getDescription())
                .idempotencyKey(transaction.getIdempotencyKey())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
