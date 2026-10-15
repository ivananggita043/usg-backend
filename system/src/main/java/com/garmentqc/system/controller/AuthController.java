package com.garmentqc.system.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.garmentqc.system.dto.CreateUserResponse;
import com.garmentqc.system.dto.LoginRequest;
import com.garmentqc.system.dto.LoginResponse;
import com.garmentqc.system.service.AuthService;
import com.garmentqc.system.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(
                authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<CreateUserResponse> getCurrentUser() {
        return ResponseEntity.ok(
                userService.getCurrentUserProfile());
    }
}
