package com.example.account_service.dto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {

    @NotBlank(message = "Transfer ID is required")
    @Size(max = 100, message = "Transfer ID cannot exceed 100 characters")
    private String transferId;

    @NotBlank(message = "Sender account number is required")
    @Pattern(regexp = "\\d{6,20}",
            message = "Sender account number must contain 6 to 20 digits")
    private String senderAccountNumber;

    @NotBlank(message = "Receiver account number is required")
    @Pattern(regexp = "\\d{6,20}",
            message = "Receiver account number must contain 6 to 20 digits")
    private String receiverAccountNumber;

    @NotNull(message = "Transfer amount is required")
    @DecimalMin(value = "0.01", message = "Transfer amount must be greater than zero")
    private BigDecimal amount;
}

