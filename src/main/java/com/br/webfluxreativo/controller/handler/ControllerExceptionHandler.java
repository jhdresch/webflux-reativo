package com.br.webfluxreativo.controller.handler;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;

import com.br.webfluxreativo.model.dtos.ValidationError;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler para controllers (reactive).
 */
@ControllerAdvice
public class ControllerExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleIllegalArgument(IllegalArgumentException ex, ServerHttpRequest request) {
        ErrorResponse body = new ErrorResponse("about:blank#invalid-argument", "Invalid argument", HttpStatus.BAD_REQUEST.value(), ex.getMessage(), request.getURI().toString());
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body(body));
    }

    @ExceptionHandler({ ServerWebInputException.class })
    public Mono<ResponseEntity<ErrorResponse>> handleServerWebInput(ServerWebInputException ex, ServerHttpRequest request) {
        String detail = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        ErrorResponse body = new ErrorResponse("about:blank#bad-request", "Bad request", HttpStatus.BAD_REQUEST.value(), detail, request.getURI().toString());
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body(body));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleDuplicateKey(DuplicateKeyException ex, ServerHttpRequest request) {
            ErrorResponse body = new ErrorResponse("about:blank#duplicate-key", "Duplicate key", HttpStatus.CONFLICT.value(), ex.getMessage(), request.getURI().toString());
        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body(body));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidation(WebExchangeBindException ex, ServerHttpRequest request) {
        List<ValidationError> errors = ex.getFieldErrors().stream()
                .map(fe -> new ValidationError(fe.getField(), fe.getDefaultMessage()))
                .collect(Collectors.toList());

        ErrorResponse body = new ErrorResponse("about:blank#validation-error", "Validation failed", HttpStatus.BAD_REQUEST.value(), "One or more fields are invalid", request.getURI().toString(), errors);
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body(body));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleConstraintViolation(ConstraintViolationException ex, ServerHttpRequest request) {
        List<ValidationError> errors = ex.getConstraintViolations().stream()
                .map(cv -> {
                    ConstraintViolation<?> v = (ConstraintViolation<?>) cv;
                    String path = v.getPropertyPath() != null ? v.getPropertyPath().toString() : null;
                    // se o path vier com notação de método (ex: create.arg0.email), pegar apenas o último segmento
                    if (path != null && path.contains(".")) {
                        String[] parts = path.split("\\.");
                        path = parts[parts.length - 1];
                    }
                    return new ValidationError(path, v.getMessage());
                })
                .collect(Collectors.toList());

        ErrorResponse body = new ErrorResponse("about:blank#validation-error", "Validation failed", HttpStatus.BAD_REQUEST.value(), "One or more constraints were violated", request.getURI().toString(), errors);
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body(body));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGeneral(Exception ex, ServerHttpRequest request) {
        // Log the exception server-side (omitted here) and do not expose stack traces to clients
        ErrorResponse body = new ErrorResponse("about:blank#internal-server-error", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR.value(), "An unexpected error occurred", request.getURI().toString());
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body(body));
    }

}

