package com.example.payment_service.dto;
import lombok.Data;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Data
public class PaymentRequest {

    @NotBlank(message = "Source account ID is required")
    private String fromAccountId;

    @NotBlank(message = "Destination account ID is required")
    private String toAccountId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency = "INR";
}

