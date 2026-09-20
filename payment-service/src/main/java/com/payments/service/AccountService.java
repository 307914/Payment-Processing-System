package com.payments.service;

import com.payments.dto.request.CreateAccountRequest;
import com.payments.dto.response.AccountResponse;
import com.payments.entity.Account;
import com.payments.entity.User;
import com.payments.exception.BusinessException;
import com.payments.repository.AccountRepository;
import com.payments.repository.UserRepository;
import com.payments.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        Long userId = securityUtils.getCurrentUserId();

        User owner = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.accountNotFound(userId));

        String accountNumber = generateAccountNumber();

        Account account = Account.builder()
                .accountNumber(accountNumber)
                .currency(request.getCurrency())
                .owner(owner)
                .build();

        Account savedAccount = accountRepository.save(account);

        return AccountResponse.from(savedAccount);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getMyAccounts() {
        Long userId = securityUtils.getCurrentUserId();

        User owner = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.accountNotFound(userId));

        return accountRepository.findByOwner(owner).stream()
                .map(AccountResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(Long accountId) {
        Long userId = securityUtils.getCurrentUserId();

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> BusinessException.accountNotFound(accountId));

        if (!account.getOwner().getId().equals(userId)) {
            throw BusinessException.accountNotFound(accountId);
        }

        return AccountResponse.from(account);
    }

    private String generateAccountNumber() {
        return "ACC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
