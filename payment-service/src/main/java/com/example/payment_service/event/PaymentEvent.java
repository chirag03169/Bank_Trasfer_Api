package com.example.payment_service.event;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentEvent {

    private String eventId;

    private Long paymentId;

    private String fromAccountId;

    private String toAccountId;

    private BigDecimal amount;

    private String currency;

    private String status;

    private LocalDateTime createdAt;
}
