package com.payments.dto.response;

import com.payments.entity.Account;
import com.payments.entity.enums.AccountStatus;
import com.payments.entity.enums.Currency;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class AccountResponse {

    private Long id;
    private String accountNumber;
    private Currency currency;
    private BigDecimal balance;
    private AccountStatus status;
    private Long ownerId;
    private Instant createdAt;

    public static AccountResponse from(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .currency(account.getCurrency())
                .balance(account.getBalance())
                .status(account.getStatus())
                .ownerId(account.getOwner().getId())
                .createdAt(account.getCreatedAt())
                .build();
    }
}
