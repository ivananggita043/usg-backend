
package com.garmentqc.system.service;

import java.util.List;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import com.garmentqc.system.dto.LineResponse;
import com.garmentqc.system.dto.LoginRequest;
import com.garmentqc.system.dto.LoginResponse;
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

                try {
                        authenticationManager.authenticate(
                                        new UsernamePasswordAuthenticationToken(
                                                        request.getUsername(),
                                                        request.getPassword()));

                } catch (BadCredentialsException ex) {
                        throw new BadCredentialsException(
                                        "Username atau password salah");
                } catch (DisabledException ex) {
                        throw new DisabledException(
                                        "Akun tidak aktif. Hubungi administrator.");
                } catch (LockedException ex) {
                        throw new LockedException(
                                        "Akun terkunci. Hubungi administrator.");
                } catch (AuthenticationException ex) {
                        throw new BadCredentialsException(
                                        "Login gagal. Periksa username dan password.");
                }

                User user = userRepository
                                .findByUsername(request.getUsername())
                                .orElseThrow(() -> new BadCredentialsException(
                                                "Username atau password salah"));

                if (!user.isActive()) {
                        throw new DisabledException(
                                        "Akun tidak aktif. Hubungi administrator.");
                }

                String token = jwtService.generateToken(user);

                List<LineResponse> lines = userLineRepository
                                .findByUser(user)
                                .stream()
                                .map(UserLine::getLine)
                                .map(line -> LineResponse.builder()
                                                .id(line.getId())
                                                .name(line.getName())
                                                .build())
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
                                .lines(lines)
                                .build();
        }
}
