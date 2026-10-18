package com.example.account_service.controller;


import com.example.account_service.dto.AccountBalanceResponse;
import com.example.account_service.dto.TransferRequest;
import com.example.account_service.dto.TransferResponse;
import com.example.account_service.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    // Transfer money between two accounts
    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transferMoney(
            @Valid @RequestBody TransferRequest request) {

        TransferResponse response =
                accountService.processTransfer(request);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // Get the current balance of an account
    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<AccountBalanceResponse> getAccountBalance(
            @PathVariable String accountNumber) {

        AccountBalanceResponse response =
                accountService.getAccountBalance(accountNumber);

        return ResponseEntity.ok(response);
    }
}

