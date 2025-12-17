package com.novaswap.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
        MethodArgumentNotValidException ex
    ) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Validation failed");
        response.put("details", errors);
        
        return ResponseEntity.badRequest().body(response);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(
        IllegalArgumentException ex
    ) {
        log.error("Invalid argument: {}", ex.getMessage());
        Map<String, String> response = new HashMap<>();
        response.put("error", ex.getMessage());
        response.put("type", "INVALID_ARGUMENT");
        return ResponseEntity.badRequest().body(response);
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime error: {}", ex.getMessage(), ex);
        
        Map<String, String> response = new HashMap<>();
        String message = ex.getMessage();
        
        // 根据错误信息分类
        if (message.contains("Insufficient liquidity")) {
            response.put("error", "Insufficient liquidity in the pool");
            response.put("type", "INSUFFICIENT_LIQUIDITY");
        } else if (message.contains("No available route")) {
            response.put("error", "No available route found for this swap");
            response.put("type", "NO_ROUTE_FOUND");
        } else if (message.contains("balance")) {
            response.put("error", "Insufficient balance");
            response.put("type", "INSUFFICIENT_BALANCE");
        } else if (message.contains("allowance")) {
            response.put("error", "Insufficient allowance, please approve first");
            response.put("type", "INSUFFICIENT_ALLOWANCE");
        } else if (message.contains("expired") || message.contains("deadline")) {
            response.put("error", "Transaction deadline expired");
            response.put("type", "TRANSACTION_EXPIRED");
        } else {
            response.put("error", message != null ? message : "An error occurred");
            response.put("type", "RUNTIME_ERROR");
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        Map<String, String> response = new HashMap<>();
        response.put("error", "An unexpected error occurred");
        response.put("type", "INTERNAL_ERROR");
        response.put("details", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
