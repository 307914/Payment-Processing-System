package com.payments.entity;

import com.payments.entity.enums.TransactionStatus;
import com.payments.entity.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_txn_idempotency_key", columnList = "idempotency_key", unique = true),
        @Index(name = "idx_txn_source_account_id", columnList = "source_account_id"),
        @Index(name = "idx_txn_destination_account_id", columnList = "destination_account_id"),
        @Index(name = "idx_txn_status", columnList = "status"),
        @Index(name = "idx_txn_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id")
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_account_id")
    private Account destinationAccount;

    @Column(length = 500)
    private String description;

    @Column(name = "reference_number", unique = true, length = 36)
    private String referenceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversed_by_id")
    private Transaction reversedBy;

    @Version
    private Long version;
}
