package com.example.account_service.repository;
import com.example.account_service.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberForUpdate(
            @Param("accountNumber") String accountNumber
    );

}
/*
 * CONCURRENCY CONTROL IN BANKING FUND TRANSFERS
 *
 * 1. PESSIMISTIC LOCKING
 *    - Locks the account row before updating its balance.
 *    - Another transaction trying to lock the same row must wait.
 *    - Helps prevent concurrent transfers from using a stale balance.
 *    - Implemented in AccountRepository using:
 *      @Lock(LockModeType.PESSIMISTIC_WRITE)
 *
 * 2. OPTIMISTIC LOCKING
 *    - Does not hold a database row lock for the whole operation.
 *    - Uses @Version to detect concurrent updates.
 *    - If another transaction updates the row first,
 *      the conflicting update fails with an optimistic-locking exception.
 *    - The application must handle the conflict and retry safely if needed.
 *
 * 3. OUR PROJECT
 *    - We currently use PESSIMISTIC LOCKING.
 *    - Both sender and receiver accounts are locked during the transfer.
 *    - Accounts are locked in a consistent order to reduce deadlock risk.
 *    - @Transactional ensures balance updates and ledger entries
 *      are committed or rolled back together.
 *
 * 4. IDEMPOTENCY
 *    - Locking alone does not prevent duplicate transfers.
 *    - We use transferId and a unique database constraint to help
 *      prevent the same transfer from being processed more than once.
 */
