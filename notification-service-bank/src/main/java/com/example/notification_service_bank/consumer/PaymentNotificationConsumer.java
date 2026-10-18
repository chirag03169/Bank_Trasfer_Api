package com.example.notification_service_bank.consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PaymentNotificationConsumer {

    @KafkaListener(
            topics = "payment-events",
            groupId = "notification-service-group"
    )
    public void consumePaymentEvent(PaymentEvent event) {

        log.info("Received payment event: {}", event);

        if ("SUCCESS".equalsIgnoreCase(event.getStatus())) {

            log.info(
                    "PAYMENT SUCCESSFUL | Payment ID: {} | Amount: {} {} | Receiver Account: {}",
                    event.getPaymentId(),
                    event.getAmount(),
                    event.getCurrency(),
                    event.getToAccountId()
            );

        } else {

            log.info(
                    "PAYMENT UPDATE | Payment ID: {} | Status: {}",
                    event.getPaymentId(),
                    event.getStatus()
            );
        }
    }
}

