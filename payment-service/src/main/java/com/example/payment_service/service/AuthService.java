package com.example.payment_service.service;

import com.example.payment_service.dto.LoginRequest;
import com.example.payment_service.dto.LoginResponse;
import com.example.payment_service.dto.RegisterRequest;

public interface AuthService {

    void register(RegisterRequest request);

    LoginResponse login(LoginRequest request);
}

