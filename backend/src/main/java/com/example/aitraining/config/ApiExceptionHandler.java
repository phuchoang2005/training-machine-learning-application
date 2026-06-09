package com.example.aitraining.config;

import com.example.aitraining.auth.ForbiddenException;
import com.example.aitraining.auth.UnauthorizedException;
import com.example.aitraining.dto.ApiDtos.ApiError;
import com.example.aitraining.dto.ApiDtos.ErrorResponse;
import com.example.aitraining.dto.ApiDtos.ValidationDetail;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(UnauthorizedException.class)
    ResponseEntity<ErrorResponse> unauthorized(RuntimeException ex, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    ResponseEntity<ErrorResponse> forbidden(RuntimeException ex, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage());
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    ResponseEntity<ErrorResponse> notFound(RuntimeException ex) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource was not found");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorResponse> badRequest(RuntimeException ex) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ErrorResponse> conflict(RuntimeException ex) {
        return error(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> invalid(MethodArgumentNotValidException ex) {
        List<ValidationDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ValidationDetail(e.getField(), e.getDefaultMessage()))
                .toList();
        return new ResponseEntity<>(
                new ErrorResponse(new ApiError("VALIDATION_ERROR", "Request validation failed",
                        UUID.randomUUID().toString(), details)),
                HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message) {
        return new ResponseEntity<>(
                new ErrorResponse(new ApiError(code, message, UUID.randomUUID().toString(), List.of())),
                status);
    }
}
