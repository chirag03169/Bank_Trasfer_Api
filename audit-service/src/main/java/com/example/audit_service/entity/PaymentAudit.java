package com.example.audit_service.entity;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(
        name = "payment_audit",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_audit_event_id",
                        columnNames = "event_id"
                )
        }
)
public class PaymentAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 100)
    private String eventId;

    @Column(name = "payment_id", length = 100)
    private String paymentId;

    @Column(name = "sender_account_number", length = 20)
    private String senderAccountNumber;

    @Column(name = "receiver_account_number", length = 20)
    private String receiverAccountNumber;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 3)
    private String currency;

    @Column(name = "payment_status", length = 30)
    private String paymentStatus;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "event_created_at")
    private LocalDateTime eventCreatedAt;

    @Column(name = "audited_at", nullable = false)
    private LocalDateTime auditedAt;

    @PrePersist
    protected void onCreate() {
        if (auditedAt == null) {
            auditedAt = LocalDateTime.now();
        }
    }
}
