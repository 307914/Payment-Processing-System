package com.payments.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReversalRequest {

    @NotBlank(message = "Reference number is required")
    private String referenceNumber;

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;

    private String reason;
}
