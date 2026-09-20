package com.payments.dto.request;

import com.payments.entity.enums.Currency;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateAccountRequest {

    @NotNull(message = "Currency is required")
    private Currency currency;
}
