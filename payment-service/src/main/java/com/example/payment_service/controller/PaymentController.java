package com.example.payment_service.controller;

import com.example.payment_service.dto.PaymentRequest;
import com.example.payment_service.dto.PaymentResponse;
import com.example.payment_service.repository.UserRepository;
import com.example.payment_service.service.PaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            Authentication authentication) {

        Long userId = getAuthenticatedUserId(authentication);

        PaymentResponse response = paymentService.createPayment(
                userId,
                request,
                idempotencyKey
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(
            @PathVariable Long paymentId,
            Authentication authentication) {

        Long userId = getAuthenticatedUserId(authentication);

        return ResponseEntity.ok(
                paymentService.getPayment(paymentId, userId)
        );
    }

    @GetMapping("/my")
    public ResponseEntity<List<PaymentResponse>> getMyPayments(
            Authentication authentication) {

        Long userId = getAuthenticatedUserId(authentication);

        return ResponseEntity.ok(
                paymentService.getMyPayments(userId)
        );
    }

    private Long getAuthenticatedUserId(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated");
        }

        String username = authentication.getName();

        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Authenticated user not found: " + username
                        ))
                .getUserId();
    }
}
