package com.example.payment_service.client;

import com.example.payment_service.dto.AccountTransferRequest;
import com.example.payment_service.dto.AccountTransferResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@Component
public class AccountServiceClient {

    private static final Logger log =
            LoggerFactory.getLogger(AccountServiceClient.class);

    private final RestClient restClient;

    public AccountServiceClient(RestClient.Builder builder) {

        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        this.restClient = builder
                .baseUrl("http://localhost:8081")
                .requestFactory(requestFactory)
                .build();
    }

    @CircuitBreaker(
            name = "accountService",
            fallbackMethod = "accountServiceFallback"
    )
    public AccountTransferResponse processTransfer(
            AccountTransferRequest request) {

        log.info(
                "Calling Account Service for transferId={}",
                request.getTransferId()
        );

        AccountTransferResponse response = restClient.post()
                .uri("/api/accounts/transfer")
                .body(request)
                .retrieve()
                .body(AccountTransferResponse.class);

        if (response == null) {
            throw new IllegalStateException(
                    "Account Service returned an empty response"
            );
        }

        log.info(
                "Account Service response received: transferId={}, status={}",
                response.getTransferId(),
                response.getStatus()
        );

        return response;
    }

    private AccountTransferResponse accountServiceFallback(
            AccountTransferRequest request,
            Throwable exception) {

        log.error(
                "Account Service call failed for transferId={}. Type={}, message={}",
                request.getTransferId(),
                exception.getClass().getName(),
                exception.getMessage(),
                exception
        );

        // Preserve HTTP 4xx responses from Account Service.
        if (exception instanceof HttpClientErrorException clientError) {
            throw clientError;
        }

        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Account Service call failed. Check transfer status before retrying.",
                exception
        );
    }
}