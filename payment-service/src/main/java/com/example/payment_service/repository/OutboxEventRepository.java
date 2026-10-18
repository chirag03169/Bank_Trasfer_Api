package com.example.payment_service.repository;
import com.example.payment_service.event.OutboxEvent;
import com.example.payment_service.event.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(
            OutboxStatus status
    );
}

