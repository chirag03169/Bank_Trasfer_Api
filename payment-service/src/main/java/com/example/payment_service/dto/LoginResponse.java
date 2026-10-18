package com.example.payment_service.dto;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;
    private String tokenType;
    private Long userId;
    private String username;
}

