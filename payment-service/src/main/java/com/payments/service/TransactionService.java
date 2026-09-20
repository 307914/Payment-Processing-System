package com.payments.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.dto.request.DepositRequest;
import com.payments.dto.request.ReversalRequest;
import com.payments.dto.request.TransferRequest;
import com.payments.dto.request.WithdrawRequest;
import com.payments.dto.event.TransactionEventPayload;
import com.payments.dto.response.TransactionResponse;
import com.payments.entity.Account;
import com.payments.entity.OutboxEvent;
import com.payments.entity.Transaction;
import com.payments.entity.enums.AccountStatus;
import com.payments.entity.enums.TransactionStatus;
import com.payments.entity.enums.TransactionType;
import com.payments.exception.BusinessException;
import com.payments.repository.AccountRepository;
import com.payments.repository.OutboxEventRepository;
import com.payments.repository.TransactionRepository;
import com.payments.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final String IDEMPOTENCY_PREFIX = "idempotency:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public TransactionResponse deposit(DepositRequest request) {
        // 1. Idempotency check — Redis first
        String redisKey = IDEMPOTENCY_PREFIX + request.getIdempotencyKey();
        String cached = redisTemplate.opsForValue().get(redisKey);

        if (cached != null) {
            return deserialize(cached);
        }

        // 2. Idempotency check — DB fallback
        var existing = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            TransactionResponse response = TransactionResponse.from(existing.get());
            cacheIdempotencyResponse(redisKey, response);
            return response;
        }

        // 3. Find destination account
        Account destination = accountRepository.findByAccountNumber(request.getDestinationAccountNumber())
                .orElseThrow(() -> BusinessException.accountNotFound(0L));

        // 4. Ownership check
        Long userId = securityUtils.getCurrentUserId();
        if (!destination.getOwner().getId().equals(userId)) {
            throw BusinessException.accountNotFound(0L);
        }

        // 5. Account status check
        validateAccountStatus(destination);

        // 6. Credit destination
        destination.setBalance(destination.getBalance().add(request.getAmount()));
        accountRepository.save(destination);

        // 7. Create transaction record
        Transaction transaction = Transaction.builder()
                .type(TransactionType.DEPOSIT)
                .amount(request.getAmount())
                .status(TransactionStatus.COMPLETED)
                .idempotencyKey(request.getIdempotencyKey())
                .destinationAccount(destination)
                .description(request.getDescription())
                .referenceNumber(UUID.randomUUID().toString())
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        // 8. Save outbox event for Kafka
        saveOutboxEvent(savedTransaction, null);

        // 9. Cache idempotency response in Redis
        TransactionResponse response = TransactionResponse.from(savedTransaction);
        cacheIdempotencyResponse(redisKey, response);

        return response;
    }

    @Transactional
    public TransactionResponse withdraw(WithdrawRequest request) {
        // 1. Idempotency check — Redis first
        String redisKey = IDEMPOTENCY_PREFIX + request.getIdempotencyKey();
        String cached = redisTemplate.opsForValue().get(redisKey);

        if (cached != null) {
            return deserialize(cached);
        }

        // 2. Idempotency check — DB fallback
        var existing = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            TransactionResponse response = TransactionResponse.from(existing.get());
            cacheIdempotencyResponse(redisKey, response);
            return response;
        }

        // 3. Find source account
        Account source = accountRepository.findByAccountNumber(request.getSourceAccountNumber())
                .orElseThrow(() -> BusinessException.accountNotFound(0L));

        // 4. Ownership check
        Long userId = securityUtils.getCurrentUserId();
        if (!source.getOwner().getId().equals(userId)) {
            throw BusinessException.accountNotFound(0L);
        }

        // 5. Account status check
        validateAccountStatus(source);

        // 6. Sufficient balance check
        if (source.getBalance().compareTo(request.getAmount()) < 0) {
            throw BusinessException.insufficientBalance(source.getAccountNumber());
        }

        // 7. Debit source
        source.setBalance(source.getBalance().subtract(request.getAmount()));
        accountRepository.save(source);

        // 8. Create transaction record
        Transaction transaction = Transaction.builder()
                .type(TransactionType.WITHDRAWAL)
                .amount(request.getAmount())
                .status(TransactionStatus.COMPLETED)
                .idempotencyKey(request.getIdempotencyKey())
                .sourceAccount(source)
                .description(request.getDescription())
                .referenceNumber(UUID.randomUUID().toString())
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        // 9. Save outbox event for Kafka
        saveOutboxEvent(savedTransaction, null);

        // 10. Cache idempotency response in Redis
        TransactionResponse response = TransactionResponse.from(savedTransaction);
        cacheIdempotencyResponse(redisKey, response);

        return response;
    }

    @Transactional
    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100)
    )
    public TransactionResponse transfer(TransferRequest request) {
        // 1. Idempotency check — Redis first (fast path)
        String redisKey = IDEMPOTENCY_PREFIX + request.getIdempotencyKey();
        String cached = redisTemplate.opsForValue().get(redisKey);

        if (cached != null) {
            return deserialize(cached);
        }

        // 2. Idempotency check — DB fallback (source of truth)
        var existing = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            TransactionResponse response = TransactionResponse.from(existing.get());
            cacheIdempotencyResponse(redisKey, response);
            return response;
        }

        // 3. Same account check
        if (request.getSourceAccountNumber().equals(request.getDestinationAccountNumber())) {
            throw BusinessException.sameAccountTransfer();
        }

        // 4. Find accounts
        Account source = accountRepository.findByAccountNumber(request.getSourceAccountNumber())
                .orElseThrow(() -> BusinessException.accountNotFound(0L));

        Account destination = accountRepository.findByAccountNumber(request.getDestinationAccountNumber())
                .orElseThrow(() -> BusinessException.accountNotFound(0L));

        // 5. Ownership check
        Long userId = securityUtils.getCurrentUserId();
        if (!source.getOwner().getId().equals(userId)) {
            throw BusinessException.accountNotFound(0L);
        }

        // 6. Account status checks
        validateAccountStatus(source);
        validateAccountStatus(destination);

        // 7. Currency mismatch check
        if (source.getCurrency() != destination.getCurrency()) {
            throw BusinessException.currencyMismatch();
        }

        // 8. Sufficient balance check
        if (source.getBalance().compareTo(request.getAmount()) < 0) {
            throw BusinessException.insufficientBalance(source.getAccountNumber());
        }

        // 9. Debit source, credit destination
        source.setBalance(source.getBalance().subtract(request.getAmount()));
        destination.setBalance(destination.getBalance().add(request.getAmount()));

        accountRepository.save(source);
        accountRepository.save(destination);

        // 10. Create transaction record
        Transaction transaction = Transaction.builder()
                .type(TransactionType.TRANSFER)
                .amount(request.getAmount())
                .status(TransactionStatus.COMPLETED)
                .idempotencyKey(request.getIdempotencyKey())
                .sourceAccount(source)
                .destinationAccount(destination)
                .description(request.getDescription())
                .referenceNumber(UUID.randomUUID().toString())
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        // 11. Save outbox event for Kafka
        saveOutboxEvent(savedTransaction, destination.getOwner().getEmail());

        // 12. Cache idempotency response in Redis
        TransactionResponse response = TransactionResponse.from(savedTransaction);
        cacheIdempotencyResponse(redisKey, response);

        return response;
    }

    @Recover
    public TransactionResponse recoverTransfer(ObjectOptimisticLockingFailureException e,
                                                TransferRequest request) {
        throw BusinessException.conflictRetryExhausted();
    }

    @Transactional
    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100)
    )
    public TransactionResponse reverse(ReversalRequest request) {
        // 1. Idempotency check — Redis first
        String redisKey = IDEMPOTENCY_PREFIX + request.getIdempotencyKey();
        String cached = redisTemplate.opsForValue().get(redisKey);

        if (cached != null) {
            return deserialize(cached);
        }

        // 2. Idempotency check — DB fallback
        var existing = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            TransactionResponse response = TransactionResponse.from(existing.get());
            cacheIdempotencyResponse(redisKey, response);
            return response;
        }

        // 3. Find original transaction
        Transaction original = transactionRepository.findByReferenceNumber(request.getReferenceNumber())
                .orElseThrow(BusinessException::transactionNotFound);

        // 4. Validate original transaction can be reversed
        if (original.getStatus() != TransactionStatus.COMPLETED) {
            throw BusinessException.transactionNotReversible();
        }

        if (original.getReversedBy() != null) {
            throw BusinessException.transactionAlreadyReversed();
        }

        // 5. Ownership check
        Long userId = securityUtils.getCurrentUserId();
        boolean isOwner = false;

        if (original.getSourceAccount() != null
                && original.getSourceAccount().getOwner().getId().equals(userId)) {
            isOwner = true;
        }
        if (original.getDestinationAccount() != null
                && original.getDestinationAccount().getOwner().getId().equals(userId)) {
            isOwner = true;
        }

        if (!isOwner) {
            throw BusinessException.transactionNotFound();
        }

        // 6. Reverse the money movement
        if (original.getSourceAccount() != null) {
            Account source = original.getSourceAccount();
            validateAccountStatus(source);
            source.setBalance(source.getBalance().add(original.getAmount()));
            accountRepository.save(source);
        }

        if (original.getDestinationAccount() != null) {
            Account destination = original.getDestinationAccount();
            validateAccountStatus(destination);

            if (destination.getBalance().compareTo(original.getAmount()) < 0) {
                throw BusinessException.insufficientBalance(destination.getAccountNumber());
            }

            destination.setBalance(destination.getBalance().subtract(original.getAmount()));
            accountRepository.save(destination);
        }

        // 7. Create reversal transaction record
        Transaction reversal = Transaction.builder()
                .type(TransactionType.REVERSAL)
                .amount(original.getAmount())
                .status(TransactionStatus.COMPLETED)
                .idempotencyKey(request.getIdempotencyKey())
                .sourceAccount(original.getDestinationAccount())
                .destinationAccount(original.getSourceAccount())
                .description(request.getReason() != null ? request.getReason() : "Reversal of " + original.getReferenceNumber())
                .referenceNumber(UUID.randomUUID().toString())
                .build();

        Transaction savedReversal = transactionRepository.save(reversal);

        // 8. Mark original as reversed
        original.setReversedBy(savedReversal);
        original.setStatus(TransactionStatus.REVERSED);
        transactionRepository.save(original);

        // 9. Resolve the other party's email for notification
        String otherPartyEmail = null;
        if (original.getSourceAccount() != null && original.getDestinationAccount() != null) {
            // Original was a transfer — notify the other party
            if (original.getSourceAccount().getOwner().getId().equals(userId)) {
                otherPartyEmail = original.getDestinationAccount().getOwner().getEmail();
            } else {
                otherPartyEmail = original.getSourceAccount().getOwner().getEmail();
            }
        }

        // 10. Save outbox event for Kafka
        saveOutboxEvent(savedReversal, otherPartyEmail);

        // 10. Cache idempotency response in Redis
        TransactionResponse response = TransactionResponse.from(savedReversal);
        cacheIdempotencyResponse(redisKey, response);

        return response;
    }

    @Recover
    public TransactionResponse recoverReverse(ObjectOptimisticLockingFailureException e,
                                               ReversalRequest request) {
        throw BusinessException.conflictRetryExhausted();
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionHistory(String accountNumber, String status,
                                                            Pageable pageable) {
        // 1. Find account
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> BusinessException.accountNotFound(0L));

        // 2. Ownership check
        Long userId = securityUtils.getCurrentUserId();
        if (!account.getOwner().getId().equals(userId)) {
            throw BusinessException.accountNotFound(0L);
        }

        // 3. Query transactions
        Page<Transaction> transactions;

        if (status != null) {
            TransactionStatus txStatus = TransactionStatus.valueOf(status.toUpperCase());
            transactions = transactionRepository.findByAccountAndStatus(account, txStatus, pageable);
        } else {
            transactions = transactionRepository.findBySourceAccountOrDestinationAccount(
                    account, account, pageable);
        }

        return transactions.map(TransactionResponse::from);
    }

    private void validateAccountStatus(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            if (account.getStatus() == AccountStatus.FROZEN) {
                throw BusinessException.accountFrozen(account.getId());
            }
            throw BusinessException.accountClosed(account.getId());
        }
    }

    private void saveOutboxEvent(Transaction transaction, String receiverEmail) {
        try {
            String userEmail = securityUtils.getCurrentUserEmail();
            TransactionEventPayload eventPayload = TransactionEventPayload.from(transaction, userEmail, receiverEmail);
            String payload = objectMapper.writeValueAsString(eventPayload);

            String eventType = transaction.getType() == TransactionType.REVERSAL
                    ? "TRANSACTION_REVERSED" : "TRANSACTION_COMPLETED";

            outboxEventRepository.save(OutboxEvent.builder()
                    .aggregateType("Transaction")
                    .aggregateId(transaction.getId().toString())
                    .eventType(eventType)
                    .payload(payload)
                    .build());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize transaction event", e);
        }
    }

    private void cacheIdempotencyResponse(String redisKey, TransactionResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(redisKey, json, IDEMPOTENCY_TTL);
        } catch (JsonProcessingException e) {
            // Redis cache failure is non-fatal — DB unique constraint is the source of truth
        }
    }

    private TransactionResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, TransactionResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize cached idempotency response", e);
        }
    }
}
