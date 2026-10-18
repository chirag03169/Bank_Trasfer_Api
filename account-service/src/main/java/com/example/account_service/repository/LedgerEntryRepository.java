package com.example.account_service.repository;
import com.example.account_service.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LedgerEntryRepository
        extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByTransferId(String transferId);

    List<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(Long accountId);
}

