package com.notifications.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionEvent {

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
    private LocalDateTime createdAt;
}
