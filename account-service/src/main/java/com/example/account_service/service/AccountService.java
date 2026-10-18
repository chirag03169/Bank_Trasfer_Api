package com.example.account_service.service;
import com.example.account_service.dto.AccountBalanceResponse;
import com.example.account_service.dto.TransferRequest;
import com.example.account_service.dto.TransferResponse;

public interface AccountService {

    TransferResponse processTransfer(TransferRequest request);

    AccountBalanceResponse getAccountBalance(String accountNumber);
}

