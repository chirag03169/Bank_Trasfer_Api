package com.example.payment_service.messaging;

import com.example.payment_service.event.OutboxEvent;
import com.example.payment_service.event.OutboxStatus;
import com.example.payment_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxEventRepository outboxEventRepository;

    public void publishEvent(OutboxEvent event) {

        String payload = event.getPayload();

        if (payload == null || payload.isBlank()) {
            event.setStatus(OutboxStatus.FAILED);
            outboxEventRepository.save(event);

            System.err.println(
                    "Cannot publish empty payment event. Event ID: "
                            + event.getEventId()
            );
            return;
        }

        kafkaTemplate.send(
                "payment-events",
                String.valueOf(event.getEventId()),
                payload
        ).whenComplete((result, exception) -> {

            if (exception == null) {
                event.setStatus(OutboxStatus.PUBLISHED);
            } else {
                event.setStatus(OutboxStatus.FAILED);
                exception.printStackTrace();
            }

            outboxEventRepository.save(event);
        });
    }
}
