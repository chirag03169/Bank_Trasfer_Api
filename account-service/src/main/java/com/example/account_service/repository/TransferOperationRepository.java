package com.example.account_service.repository;


import com.example.account_service.entity.TransferOperation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransferOperationRepository
        extends JpaRepository<TransferOperation, Long> {

    Optional<TransferOperation> findByTransferId(String transferId);
}

