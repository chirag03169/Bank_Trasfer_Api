package com.example.account_service.dto;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccountBalanceResponse {

    private String accountNumber;

    private BigDecimal balance;

    private String currency;

    private String status;
}

