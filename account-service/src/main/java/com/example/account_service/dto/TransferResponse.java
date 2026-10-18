package com.example.account_service.dto;


import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransferResponse {

    private String transferId;

    private String status;

    private String message;

    private String senderAccountNumber;

    private String receiverAccountNumber;

    private BigDecimal amount;

    private LocalDateTime processedAt;
}

