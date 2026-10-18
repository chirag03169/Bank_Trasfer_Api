package com.example.payment_service.serviceImpl;


import com.example.payment_service.dto.LoginRequest;
import com.example.payment_service.dto.LoginResponse;
import com.example.payment_service.dto.RegisterRequest;
import com.example.payment_service.event.User;
import com.example.payment_service.repository.UserRepository;
import com.example.payment_service.security.JwtService;
import com.example.payment_service.service.AuthService;
import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional
    public void register(RegisterRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
        );
        user.setRole("CUSTOMER");
        user.setEnabled(true);

        userRepository.save(user);
    }



    @Override
    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByUsername(request.getUsername())
                .or(() -> userRepository.findByEmail(request.getUsername()))
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid username or password"));

        if (!user.isEnabled()
                || !passwordEncoder.matches(
                request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        String token = jwtService.generateToken(
                user.getUserId(),
                user.getUsername(),
                user.getRole()
        );

        return new LoginResponse(
                token,
                "Bearer",
                user.getUserId(),
                user.getUsername()
        );
    }
}

