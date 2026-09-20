package com.payments.repository;

import com.payments.entity.Account;
import com.payments.entity.Transaction;
import com.payments.entity.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<Transaction> findByReferenceNumber(String referenceNumber);

    Page<Transaction> findBySourceAccountOrDestinationAccount(
            Account sourceAccount, Account destinationAccount, Pageable pageable);

    Page<Transaction> findBySourceAccount(Account sourceAccount, Pageable pageable);

    Page<Transaction> findByDestinationAccount(Account destinationAccount, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE (t.sourceAccount = :account OR t.destinationAccount = :account) AND t.status = :status")
    Page<Transaction> findByAccountAndStatus(
            @Param("account") Account account,
            @Param("status") TransactionStatus status,
            Pageable pageable);
}
