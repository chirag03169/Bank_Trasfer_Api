
package com.example.notification_service_bank.consumer;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentEvent {

    private Long paymentId;

    private String fromAccountId;

    private String toAccountId;

    private BigDecimal amount;

    private String currency;

    private String status;

    private LocalDateTime createdAt;
}
