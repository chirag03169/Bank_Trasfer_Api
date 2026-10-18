
package com.example.payment_service.serviceImpl;

import com.example.payment_service.client.AccountServiceClient;
import com.example.payment_service.dto.AccountTransferRequest;
import com.example.payment_service.dto.AccountTransferResponse;
import com.example.payment_service.dto.PaymentRequest;
import com.example.payment_service.dto.PaymentResponse;
import com.example.payment_service.event.*;
import com.example.payment_service.messaging.PaymentEventPublisher;
import com.example.payment_service.repository.IdempotencyRecordRepository;
import com.example.payment_service.repository.OutboxEventRepository;
import com.example.payment_service.repository.PaymentRepository;
import com.example.payment_service.service.PaymentService;
import com.example.payment_service.service.RedisService;
import com.fasterxml.jackson.databind.ObjectMapper;


import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final PaymentRepository paymentRepository;
    private final AccountServiceClient accountServiceClient;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final PaymentEventPublisher paymentEventPublisher;
    private final RedisService redisService;

    @Override
    @Transactional
    public PaymentResponse createPayment(
            Long userId,
            PaymentRequest request,
            String idempotencyKey) {

        // 1. Validate the request.
        if (userId == null) {
            throw new IllegalArgumentException(
                    "Authenticated user is required");
        }

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Idempotency-Key header is required");
        }

        if (request == null
                || request.getFromAccountId() == null
                || request.getFromAccountId().isBlank()
                || request.getToAccountId() == null
                || request.getToAccountId().isBlank()) {
            throw new IllegalArgumentException(
                    "Source and destination account IDs are required");
        }

        if (request.getFromAccountId().trim()
                .equals(request.getToAccountId().trim())) {
            throw new IllegalArgumentException(
                    "Source and destination accounts must be different");
        }

        if (request.getAmount() == null
                || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be greater than zero");
        }

        if (request.getCurrency() == null
                || request.getCurrency().isBlank()) {
            throw new IllegalArgumentException(
                    "Currency is required");
        }

        String currency =
                request.getCurrency().trim().toUpperCase(Locale.ROOT);

        if (!"INR".equals(currency)) {
            throw new IllegalArgumentException(
                    "Only INR transfers are currently supported");
        }

        // 2. Calculate request hash.
        String requestHash = calculateRequestHash(request);
        // 3. Check idempotency.
        var existingRecord =
                idempotencyRecordRepository
                        .findByUserIdAndIdempotencyKey(
                                userId, idempotencyKey);

        if (existingRecord.isPresent()) {
            IdempotencyRecord existing = existingRecord.get();

            if (!requestHash.equals(existing.getRequestHash())) {
                throw new IllegalArgumentException(
                        "Idempotency key has already been used "
                                + "with different payment details");
            }

            return getPayment(existing.getPaymentId(), userId);
        }

        // 4. Create payment.
        Payment payment = new Payment();

        payment.setUserId(userId);
        payment.setFromAccountId(
                Long.valueOf(request.getFromAccountId().trim()));
        payment.setToAccountId(
                Long.valueOf(request.getToAccountId().trim()));
        payment.setAmount(request.getAmount());
        payment.setCurrency(currency);
        payment.setStatus(PaymentStatus.PENDING);

        payment = paymentRepository.saveAndFlush(payment);

        // 5. Save idempotency record.
        IdempotencyRecord idempotencyRecord =
                new IdempotencyRecord();

        idempotencyRecord.setUserId(userId);
        idempotencyRecord.setIdempotencyKey(idempotencyKey);
        idempotencyRecord.setPaymentId(payment.getPaymentId());
        idempotencyRecord.setRequestHash(requestHash);

        idempotencyRecordRepository.saveAndFlush(idempotencyRecord);

        // 6. Prepare Account Service request.
        AccountTransferRequest transferRequest =
                new AccountTransferRequest();

        transferRequest.setTransferId(
                payment.getPaymentId().toString());
        transferRequest.setSenderAccountNumber(
                request.getFromAccountId().trim());
        transferRequest.setReceiverAccountNumber(
                request.getToAccountId().trim());
        transferRequest.setAmount(request.getAmount());

        // 7. Call Account Service.
        AccountTransferResponse transferResponse =
                accountServiceClient.processTransfer(transferRequest);

        if (transferResponse == null
                || transferResponse.getStatus() == null) {
            throw new IllegalStateException(
                    "Account Service returned an invalid transfer response");
        }

        // 8. Update status only from a recognized response.
        if ("SUCCESS".equalsIgnoreCase(transferResponse.getStatus())) {
            payment.setStatus(PaymentStatus.SUCCESS);
            redisService.savePayment(
                    payment.getPaymentId(),
                    payment.getStatus().name()
            );
        } else if ("FAILED".equalsIgnoreCase(transferResponse.getStatus())) {
            payment.setStatus(PaymentStatus.FAILED);
        } else {
            throw new IllegalStateException(
                    "Account transfer is not confirmed. Status: "
                            + transferResponse.getStatus());
        }

        payment = paymentRepository.saveAndFlush(payment);

        // 9. Create an outbox event for the confirmed result.
        try {
            PaymentEvent paymentEvent = new PaymentEvent();

            paymentEvent.setEventId(UUID.randomUUID().toString());

            paymentEvent.setPaymentId(payment.getPaymentId());
            paymentEvent.setFromAccountId(
                    payment.getFromAccountId().toString());
            paymentEvent.setToAccountId(
                    payment.getToAccountId().toString());
            paymentEvent.setAmount(payment.getAmount());
            paymentEvent.setCurrency(payment.getCurrency());
            paymentEvent.setStatus(payment.getStatus().name());

            OutboxEvent outboxEvent = new OutboxEvent();

            outboxEvent.setPaymentId(payment.getPaymentId());
            outboxEvent.setEventType("PAYMENT_" + payment.getStatus().name());
            outboxEvent.setPayload(objectMapper.writeValueAsString(paymentEvent));
            outboxEvent.setStatus(OutboxStatus.PENDING);
            outboxEventRepository.save(outboxEvent);
            paymentEventPublisher.publishEvent(outboxEvent);

        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new IllegalStateException(
            );
        }



        // 10. Return payment response.
        return toResponse(payment);


    }

    private String calculateRequestHash(PaymentRequest request) {

        try {
            String canonicalRequest =
                    request.getFromAccountId().trim() + "|" +
                            request.getToAccountId().trim() + "|" +
                            request.getAmount()
                                    .stripTrailingZeros()
                                    .toPlainString() + "|" +
                            request.getCurrency()
                                    .trim()
                                    .toUpperCase(Locale.ROOT);

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = digest.digest(
                    canonicalRequest.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hashBytes);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "Unable to calculate payment request hash",
                    exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long paymentId, Long userId) {

        Payment payment = paymentRepository.findById(paymentId)
                .filter(p -> p.getUserId().equals(userId))
                .orElseThrow(() ->
                        new IllegalArgumentException("Payment not found"));

        return toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getMyPayments(Long userId) {

        return paymentRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private PaymentResponse toResponse(Payment payment) {

        return new PaymentResponse(
                payment.getPaymentId(),
                String.valueOf(payment.getFromAccountId()),
                String.valueOf(payment.getToAccountId()),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus().name(),
                payment.getCreatedAt()
        );
    }
}
