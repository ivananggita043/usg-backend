package com.garmentqc.system.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

        private ResponseEntity<BaseErrorDetails> buildError(
                        HttpStatus status,
                        String message,
                        HttpServletRequest request) {

                BaseErrorDetails error = new BaseErrorDetails(
                                LocalDateTime.now(),
                                status.value(),
                                status.getReasonPhrase(),
                                message,
                                request.getRequestURI());

                return ResponseEntity.status(status).body(error);
        }

        @ExceptionHandler(BadRequestException.class)
        public ResponseEntity<BaseErrorDetails> handleBadRequest(
                        BadRequestException ex,
                        HttpServletRequest request) {

                return buildError(
                                HttpStatus.BAD_REQUEST,
                                ex.getMessage(),
                                request);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<BaseErrorDetails> handleValidation(
                        MethodArgumentNotValidException ex,
                        HttpServletRequest request) {

                Map<String, String> errors = new LinkedHashMap<>();

                ex.getBindingResult().getFieldErrors().forEach(error -> errors.putIfAbsent(
                                error.getField(),
                                error.getDefaultMessage() != null
                                                ? error.getDefaultMessage()
                                                : "Nilai field tidak valid"));

                String message = errors.entrySet().stream()
                                .map(entry -> entry.getKey() + ": " + entry.getValue())
                                .collect(java.util.stream.Collectors.joining("; "));

                return buildError(
                                HttpStatus.BAD_REQUEST,
                                message.isBlank()
                                                ? "Validasi request gagal"
                                                : message,
                                request);
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<BaseErrorDetails> handleInvalidJson(
                        HttpMessageNotReadableException ex,
                        HttpServletRequest request) {

                Throwable cause = ex.getMostSpecificCause();
                String message = "Format JSON tidak valid";

                if (cause instanceof InvalidFormatException invalidFormat) {
                        String field = invalidFormat.getPath().isEmpty()
                                        ? "field"
                                        : invalidFormat.getPath()
                                                        .get(invalidFormat.getPath().size() - 1)
                                                        .getFieldName();

                        Class<?> targetType = invalidFormat.getTargetType();

                        if (targetType != null && targetType.isEnum()) {
                                message = "Nilai field '" + field
                                                + "' tidak valid. Pilihan yang diperbolehkan: "
                                                + java.util.Arrays.toString(targetType.getEnumConstants());
                        } else {
                                message = "Nilai field '" + field
                                                + "' tidak sesuai tipe data yang diharapkan";
                        }
                } else if (cause instanceof MismatchedInputException mismatch) {
                        String field = mismatch.getPath().isEmpty()
                                        ? null
                                        : mismatch.getPath()
                                                        .get(mismatch.getPath().size() - 1)
                                                        .getFieldName();

                        if (field != null) {
                                message = "Field '" + field
                                                + "' tidak ada, kosong, atau tidak sesuai struktur JSON";
                        } else {
                                message = "Struktur JSON tidak valid atau tipe data tidak sesuai";
                        }
                }

                return buildError(
                                HttpStatus.BAD_REQUEST,
                                message,
                                request);
        }

        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<BaseErrorDetails> handleTypeMismatch(
                        MethodArgumentTypeMismatchException ex,
                        HttpServletRequest request) {

                String message = "Parameter '" + ex.getName()
                                + "' memiliki nilai atau tipe data yang tidak valid";

                if (ex.getRequiredType() != null
                                && ex.getRequiredType().isEnum()) {
                        message += ". Pilihan yang diperbolehkan: "
                                        + java.util.Arrays.toString(
                                                        ex.getRequiredType().getEnumConstants());
                }

                return buildError(
                                HttpStatus.BAD_REQUEST,
                                message,
                                request);
        }

        @ExceptionHandler(MissingServletRequestParameterException.class)
        public ResponseEntity<BaseErrorDetails> handleMissingParameter(
                        MissingServletRequestParameterException ex,
                        HttpServletRequest request) {

                return buildError(
                                HttpStatus.BAD_REQUEST,
                                "Parameter '" + ex.getParameterName() + "' wajib diisi",
                                request);
        }

        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<BaseErrorDetails> handleConstraintViolation(
                        ConstraintViolationException ex,
                        HttpServletRequest request) {

                String message = ex.getConstraintViolations().stream()
                                .map(v -> v.getPropertyPath() + ": "
                                                + v.getMessage())
                                .collect(java.util.stream.Collectors.joining("; "));

                return buildError(
                                HttpStatus.BAD_REQUEST,
                                message.isBlank() ? "Validasi input gagal" : message,
                                request);
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<BaseErrorDetails> handleNotFound(
                        ResourceNotFoundException ex,
                        HttpServletRequest request) {

                return buildError(
                                HttpStatus.NOT_FOUND,
                                ex.getMessage(),
                                request);
        }

        @ExceptionHandler(UnauthorizedException.class)
        public ResponseEntity<BaseErrorDetails> handleUnauthorized(
                        UnauthorizedException ex,
                        HttpServletRequest request) {

                return buildError(
                                HttpStatus.UNAUTHORIZED,
                                ex.getMessage(),
                                request);
        }

        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<BaseErrorDetails> handleDataIntegrity(
                        DataIntegrityViolationException ex,
                        HttpServletRequest request) {

                return buildError(
                                HttpStatus.BAD_REQUEST,
                                "Data tidak dapat disimpan karena melanggar aturan database",
                                request);
        }

        @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
        public ResponseEntity<BaseErrorDetails> handleUnsupportedMediaType(
                        HttpMediaTypeNotSupportedException ex,
                        HttpServletRequest request) {

                return buildError(
                                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                                "Content-Type request tidak didukung",
                                request);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<BaseErrorDetails> handleGeneral(
                        Exception ex,
                        HttpServletRequest request) {

                org.slf4j.LoggerFactory.getLogger(getClass())
                                .error("Unexpected error pada {}",
                                                request.getRequestURI(), ex);

                return buildError(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "Terjadi kesalahan pada server",
                                request);
        }

}
