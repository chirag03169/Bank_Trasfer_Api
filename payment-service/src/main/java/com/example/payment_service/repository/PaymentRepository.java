package com.example.payment_service.repository;

import com.example.payment_service.event.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Payment> findByFromAccountIdOrderByCreatedAtDesc(
            String fromAccountId
    );
}
