package com.example.audit_service.respository;

import com.example.audit_service.entity.PaymentAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentAuditRepository
        extends JpaRepository<PaymentAudit, Long> {

    Optional<PaymentAudit> findByEventId(String eventId);
}
