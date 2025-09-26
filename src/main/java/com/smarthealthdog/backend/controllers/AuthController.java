package com.smarthealthdog.backend.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smarthealthdog.backend.domain.User;
import com.smarthealthdog.backend.dto.UserCreateRequest;
import com.smarthealthdog.backend.dto.UserEmailVerifyRequest;
import com.smarthealthdog.backend.services.AuthService;
import com.smarthealthdog.backend.services.EmailService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final AuthService authService;
    private final EmailService emailService;

    @Autowired
    public AuthController(
        AuthenticationManager authenticationManager,
        AuthService authService,
        EmailService emailService
    ) {
        this.authenticationManager = authenticationManager;
        this.authService = authService;
        this.emailService = emailService;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> createUser(@Valid @RequestBody UserCreateRequest request) {
        User newUser = authService.registerUser(request);
        emailService.sendEmailVerification(newUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(null);
    }

    @PostMapping("/register/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody UserEmailVerifyRequest request) {
        User user = authService.verifyEmailToken(request.email(), request.token());
        authService.activateUser(user);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }
}