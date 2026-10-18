package com.example.audit_service.service;
import com.example.audit_service.entity.PaymentAudit;
import com.example.audit_service.respository.PaymentAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentAuditService {

    private final PaymentAuditRepository paymentAuditRepository;

    @Transactional
    public void saveAudit(PaymentAudit audit) {

        if (paymentAuditRepository.findByEventId(
                audit.getEventId()).isPresent()) {

            return;
        }

        paymentAuditRepository.save(audit);
    }
}