package com.example.account_service.serviceImpl;
import com.example.account_service.dto.AccountBalanceResponse;
import com.example.account_service.dto.TransferRequest;
import com.example.account_service.dto.TransferResponse;
import com.example.account_service.entity.Account;
import com.example.account_service.entity.AccountStatus;
import com.example.account_service.entity.LedgerEntry;
import com.example.account_service.entity.LedgerEntryType;
import com.example.account_service.entity.TransferOperation;
import com.example.account_service.entity.TransferStatus;
import com.example.account_service.repository.AccountRepository;
import com.example.account_service.repository.LedgerEntryRepository;
import com.example.account_service.repository.TransferOperationRepository;
import com.example.account_service.service.AccountService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final TransferOperationRepository transferOperationRepository;

    @Override
    @Transactional
    public TransferResponse processTransfer(TransferRequest request) {

        // 1. Validate the request.
        if (request.getTransferId() == null
                || request.getTransferId().isBlank()) {
            throw new IllegalArgumentException("Transfer ID is required");
        }

        if (request.getSenderAccountNumber() == null
                || request.getReceiverAccountNumber() == null
                || request.getSenderAccountNumber().isBlank()
                || request.getReceiverAccountNumber().isBlank()) {
            throw new IllegalArgumentException(
                    "Sender and receiver account numbers are required"
            );
        }

        if (request.getAmount() == null
                || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Transfer amount must be greater than zero"
            );
        }

        if (request.getSenderAccountNumber()
                .equals(request.getReceiverAccountNumber())) {
            throw new IllegalArgumentException(
                    "Sender and receiver accounts must be different"
            );
        }

        // 2. Return the saved result if this transfer already succeeded.
        var existingOperation =
                transferOperationRepository.findByTransferId(
                        request.getTransferId()
                );

        if (existingOperation.isPresent()) {
            TransferOperation operation = existingOperation.get();

            validateSameTransfer(operation, request);

            if (operation.getStatus() == TransferStatus.SUCCESS) {
                return buildResponse(
                        operation,
                        "Transfer was already processed successfully"
                );
            }

            if (operation.getStatus() == TransferStatus.PENDING) {
                throw new IllegalStateException(
                        "Transfer is pending; check its status before retrying"
                );
            }

            throw new IllegalArgumentException(
                    "This transfer ID belongs to a failed transfer"
            );
        }

        // 3. Lock accounts in a consistent order to reduce deadlocks.
        String firstAccountNumber =
                request.getSenderAccountNumber()
                        .compareTo(request.getReceiverAccountNumber()) < 0
                        ? request.getSenderAccountNumber()
                        : request.getReceiverAccountNumber();

        String secondAccountNumber =
                firstAccountNumber.equals(request.getSenderAccountNumber())
                        ? request.getReceiverAccountNumber()
                        : request.getSenderAccountNumber();

        Account firstAccount = accountRepository
                .findByAccountNumberForUpdate(firstAccountNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Account not found: " + firstAccountNumber
                ));

        Account secondAccount = accountRepository
                .findByAccountNumberForUpdate(secondAccountNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Account not found: " + secondAccountNumber
                ));

        Account sender = request.getSenderAccountNumber()
                .equals(firstAccount.getAccountNumber())
                ? firstAccount : secondAccount;

        Account receiver = request.getReceiverAccountNumber()
                .equals(firstAccount.getAccountNumber())
                ? firstAccount : secondAccount;

        // 4. Recheck idempotency after acquiring the account locks.
        existingOperation = transferOperationRepository
                .findByTransferId(request.getTransferId());

        if (existingOperation.isPresent()) {
            TransferOperation operation = existingOperation.get();

            validateSameTransfer(operation, request);

            if (operation.getStatus() == TransferStatus.SUCCESS) {
                return buildResponse(
                        operation,
                        "Transfer was already processed successfully"
                );
            }

            throw new IllegalStateException(
                    "This transfer ID has already been used; "
                            + "check its status before retrying"
            );
        }

        // 5. Validate account status.
        if (sender.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Sender account is not active"
            );
        }

        if (receiver.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Receiver account is not active"
            );
        }

        // 6. Check sufficient balance.
        BigDecimal amount = request.getAmount();

        if (sender.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException(
                    "Insufficient account balance"
            );
        }

        // 7. Create the transfer operation.
        TransferOperation operation = new TransferOperation();
        operation.setTransferId(request.getTransferId());
        operation.setSenderAccountNumber(sender.getAccountNumber());
        operation.setReceiverAccountNumber(receiver.getAccountNumber());
        operation.setAmount(amount);
        operation.setStatus(TransferStatus.PENDING);

        transferOperationRepository.save(operation);

        // 8. Debit the sender and credit the receiver.
        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));

        accountRepository.save(sender);
        accountRepository.save(receiver);

        // 9. Record both ledger entries.
        LedgerEntry debitEntry = new LedgerEntry();
        debitEntry.setTransferId(request.getTransferId());
        debitEntry.setAccountId(sender.getAccountId());
        debitEntry.setEntryType(LedgerEntryType.DEBIT);
        debitEntry.setAmount(amount);
        debitEntry.setBalanceAfter(sender.getBalance());
        debitEntry.setDescription("Fund transfer debit");

        LedgerEntry creditEntry = new LedgerEntry();
        creditEntry.setTransferId(request.getTransferId());
        creditEntry.setAccountId(receiver.getAccountId());
        creditEntry.setEntryType(LedgerEntryType.CREDIT);
        creditEntry.setAmount(amount);
        creditEntry.setBalanceAfter(receiver.getBalance());
        creditEntry.setDescription("Fund transfer credit");

        ledgerEntryRepository.save(debitEntry);
        ledgerEntryRepository.save(creditEntry);

        // 10. Mark the operation successful.
        operation.setStatus(TransferStatus.SUCCESS);
        transferOperationRepository.save(operation);

        return buildResponse(operation, "Transfer completed successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public AccountBalanceResponse getAccountBalance(String accountNumber) {

        Account account = accountRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Account not found: " + accountNumber
                ));

        AccountBalanceResponse response = new AccountBalanceResponse();
        response.setAccountNumber(account.getAccountNumber());
        response.setBalance(account.getBalance());
        response.setCurrency(account.getCurrency());
        response.setStatus(account.getStatus().name());

        return response;
    }

    private void validateSameTransfer(
            TransferOperation operation,
            TransferRequest request
    ) {
        boolean sameTransfer =
                operation.getSenderAccountNumber()
                        .equals(request.getSenderAccountNumber())
                        && operation.getReceiverAccountNumber()
                        .equals(request.getReceiverAccountNumber())
                        && operation.getAmount().compareTo(request.getAmount()) == 0;

        if (!sameTransfer) {
            throw new IllegalArgumentException(
                    "Transfer ID was already used for different transfer details"
            );
        }
    }

    private TransferResponse buildResponse(
            TransferOperation operation,
            String message
    ) {
        TransferResponse response = new TransferResponse();
        response.setTransferId(operation.getTransferId());
        response.setStatus(operation.getStatus().name());
        response.setMessage(message);
        response.setSenderAccountNumber(
                operation.getSenderAccountNumber()
        );
        response.setReceiverAccountNumber(
                operation.getReceiverAccountNumber()
        );
        response.setAmount(operation.getAmount());
        response.setProcessedAt(
                operation.getUpdatedAt() != null
                        ? operation.getUpdatedAt()
                        : LocalDateTime.now()
        );

        return response;
    }
}

