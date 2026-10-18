package com.example.payment_service.dto;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class AccountTransferRequest {

    private String transferId;
    private String senderAccountNumber;
    private String receiverAccountNumber;
    private BigDecimal amount;
}

