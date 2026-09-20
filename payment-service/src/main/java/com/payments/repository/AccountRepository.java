package com.payments.repository;

import com.payments.entity.Account;
import com.payments.entity.User;
import com.payments.entity.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByOwner(User owner);

    List<Account> findByOwnerAndStatus(User owner, AccountStatus status);

    boolean existsByAccountNumber(String accountNumber);
}
