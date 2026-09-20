package com.payments.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public BusinessException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    // --- Auth ---
    public static BusinessException invalidCredentials() {
        return new BusinessException("AUTH_001", "Invalid email or password", HttpStatus.UNAUTHORIZED);
    }

    public static BusinessException tokenExpired() {
        return new BusinessException("AUTH_002", "Token has expired", HttpStatus.UNAUTHORIZED);
    }

    public static BusinessException tokenBlacklisted() {
        return new BusinessException("AUTH_003", "Token has been revoked", HttpStatus.UNAUTHORIZED);
    }

    public static BusinessException emailTaken(String email) {
        return new BusinessException("AUTH_004", "Email already registered: " + email, HttpStatus.CONFLICT);
    }

    // --- Account ---
    public static BusinessException accountNotFound(Long id) {
        return new BusinessException("ACC_001", "Account not found: " + id, HttpStatus.NOT_FOUND);
    }

    public static BusinessException accountFrozen(Long id) {
        return new BusinessException("ACC_002", "Account is frozen: " + id, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public static BusinessException accountClosed(Long id) {
        return new BusinessException("ACC_003", "Account is closed: " + id, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public static BusinessException currencyMismatch() {
        return new BusinessException("ACC_004", "Sender and receiver currency must match", HttpStatus.BAD_REQUEST);
    }

    // --- Transaction ---
    public static BusinessException insufficientBalance(String accountNumber) {
        return new BusinessException("TXN_001", "Insufficient balance in account: " + accountNumber, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public static BusinessException duplicateTransaction(String idempotencyKey) {
        return new BusinessException("TXN_002", "Duplicate transaction: " + idempotencyKey, HttpStatus.CONFLICT);
    }

    public static BusinessException sameAccountTransfer() {
        return new BusinessException("TXN_003", "Cannot transfer to the same account", HttpStatus.BAD_REQUEST);
    }

    public static BusinessException transactionNotFound(Long id) {
        return new BusinessException("TXN_004", "Transaction not found: " + id, HttpStatus.NOT_FOUND);
    }

    public static BusinessException transactionNotFound() {
        return new BusinessException("TXN_004", "Transaction not found", HttpStatus.NOT_FOUND);
    }

    public static BusinessException transactionNotReversible() {
        return new BusinessException("TXN_005", "Transaction cannot be reversed, only completed transactions can be reversed", HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public static BusinessException transactionAlreadyReversed() {
        return new BusinessException("TXN_006", "Transaction has already been reversed", HttpStatus.CONFLICT);
    }

    public static BusinessException alreadyReversed(Long id) {
        return new BusinessException("TXN_006", "Transaction already reversed: " + id, HttpStatus.CONFLICT);
    }

    public static BusinessException conflictRetryExhausted() {
        return new BusinessException("TXN_006", "Concurrent update conflict, please retry", HttpStatus.CONFLICT);
    }

    // --- Rate limit ---
    public static BusinessException rateLimitExceeded() {
        return new BusinessException("RATE_001", "Rate limit exceeded, please try again later", HttpStatus.TOO_MANY_REQUESTS);
    }
}
