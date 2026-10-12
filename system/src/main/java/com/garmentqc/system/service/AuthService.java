package com.garmentqc.system.service;

import java.util.List;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import com.garmentqc.system.dto.LoginRequest;
import com.garmentqc.system.dto.LoginResponse;
import com.garmentqc.system.exception.ResourceNotFoundException;
import com.garmentqc.system.model.User;
import com.garmentqc.system.model.UserLine;
import com.garmentqc.system.repository.UserLineRepository;
import com.garmentqc.system.repository.UserRepository;
import com.garmentqc.system.security.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

        private final AuthenticationManager authenticationManager;
        private final UserRepository userRepository;
        private final UserLineRepository userLineRepository;
        private final JwtService jwtService;

        public LoginResponse login(LoginRequest request) {

                authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(
                                                request.getUsername(),
                                                request.getPassword()));

                User user = userRepository
                                .findByUsername(request.getUsername())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "User tidak ditemukan"));

                String token = jwtService.generateToken(user);
                List<String> lineIds = userLineRepository
                                .findByUser(user)
                                .stream()
                                .map(UserLine::getLine)
                                .map(line -> line.getId())
                                .toList();

                return LoginResponse.builder()
                                .token(token)
                                .id(user.getId())
                                .nama(user.getNama())
                                .username(user.getUsername())
                                .role(user.getRole())
                                .section(user.getSection())
                                .active(user.isActive())
                                .createdAt(user.getCreatedAt())
                                .updatedAt(user.getUpdatedAt())
                                .lineIds(lineIds)
                                .build();
        }
}