package com.example.audit_service.consumer;
import com.example.audit_service.entity.PaymentAudit;
import com.example.audit_service.service.PaymentAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final PaymentAuditService paymentAuditService;

    @KafkaListener(
            topics = "payment-events",
            groupId = "audit-service-group"
    )
    public void consumePaymentEvent(PaymentEvent event) {

        log.info("Received payment event: {}", event);


        PaymentAudit audit = new PaymentAudit();

        audit.setEventId(event.getEventId());

        audit.setPaymentId(
                String.valueOf(event.getPaymentId())
        );
        audit.setEventType("PAYMENT_" + event.getStatus());
        audit.setSenderAccountNumber(event.getFromAccountId());
        audit.setReceiverAccountNumber(event.getToAccountId());
        audit.setAmount(event.getAmount());
        audit.setCurrency(event.getCurrency());
        audit.setPaymentStatus(event.getStatus());
        audit.setEventCreatedAt(event.getCreatedAt());

        paymentAuditService.saveAudit(audit);


        paymentAuditService.saveAudit(audit);

        log.info("Payment audit processed for eventId: {}",
                event.getPaymentId());
    }
}
