package com.example.payment_service.service;

import com.example.payment_service.dto.PaymentRequest;
import com.example.payment_service.dto.PaymentResponse;

import java.util.List;

public interface PaymentService {

    PaymentResponse createPayment(
            Long userId,
            PaymentRequest request,
            String idempotencyKey
    );

    PaymentResponse getPayment(Long paymentId, Long userId);

    List<PaymentResponse> getMyPayments(Long userId);
}
