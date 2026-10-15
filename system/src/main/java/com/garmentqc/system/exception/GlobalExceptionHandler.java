package com.garmentqc.system.exception;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(BadRequestException.class)
        public ResponseEntity<BaseErrorDetails> handleBadRequest(
                        BadRequestException ex,
                        HttpServletRequest request) {

                BaseErrorDetails error = new BaseErrorDetails(
                                LocalDateTime.now(),
                                HttpStatus.BAD_REQUEST.value(),
                                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                                ex.getMessage(),
                                request.getRequestURI());

                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<BaseErrorDetails> handleNotFound(
                        ResourceNotFoundException ex,
                        HttpServletRequest request) {

                BaseErrorDetails error = new BaseErrorDetails(
                                LocalDateTime.now(),
                                HttpStatus.NOT_FOUND.value(),
                                HttpStatus.NOT_FOUND.getReasonPhrase(),
                                ex.getMessage(),
                                request.getRequestURI());

                return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(UnauthorizedException.class)
        public ResponseEntity<BaseErrorDetails> handleUnauthorized(
                        UnauthorizedException ex,
                        HttpServletRequest request) {

                BaseErrorDetails error = new BaseErrorDetails(
                                LocalDateTime.now(),
                                HttpStatus.UNAUTHORIZED.value(),
                                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                                ex.getMessage(),
                                request.getRequestURI());

                return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
        }

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<Map<String, Object>> handleBadCredentials(
                        BadCredentialsException ex) {

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(Map.of(
                                                "status", 401,
                                                "error", "Unauthorized",
                                                "message", "Username atau password salah"));
        }

        @ExceptionHandler(DisabledException.class)
        public ResponseEntity<Map<String, Object>> handleDisabled(
                        DisabledException ex) {

                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of(
                                                "status", 403,
                                                "error", "Forbidden",
                                                "message", "Akun tidak aktif. Hubungi administrator."));
        }

        @ExceptionHandler(LockedException.class)
        public ResponseEntity<Map<String, Object>> handleLocked(
                        LockedException ex) {

                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of(
                                                "status", 403,
                                                "error", "Forbidden",
                                                "message", "Akun terkunci. Hubungi administrator."));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<BaseErrorDetails> handleGlobalException(
                        Exception ex,
                        HttpServletRequest request) {

                BaseErrorDetails error = new BaseErrorDetails(
                                LocalDateTime.now(),
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                                ex.getMessage(),
                                request.getRequestURI());

                return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }

}
