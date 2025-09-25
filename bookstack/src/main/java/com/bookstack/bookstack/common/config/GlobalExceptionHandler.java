package com.bookstack.bookstack.common.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.bookstack.bookstack.common.exception.BaseException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handle REST validation errors (Case 3)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();
        
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        
        response.put("message", "Validation failed");
        response.put("errors", errors);
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.badRequest().body(response);
    }

    // Handle REST business logic errors (Case 4)
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<Map<String, Object>> handleBaseException(BaseException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(Map.of(
                        "status", ex.getStatus().value(),
                        "error", ex.getStatus().getReasonPhrase(),
                        "message", ex.getMessage(),
                        "timestamp", System.currentTimeMillis()
                ));
    }

    // Handle HTTP method not supported
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Map.of(
                        "status", HttpStatus.METHOD_NOT_ALLOWED.value(),
                        "error", HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase(),
                        "message", ex.getMessage(),
                        "supportedMethods", ex.getSupportedHttpMethods(),
                        "timestamp", System.currentTimeMillis()
                ));
    }

    // Handle resource not found (like favicon.ico)
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "status", HttpStatus.NOT_FOUND.value(),
                        "error", HttpStatus.NOT_FOUND.getReasonPhrase(),
                        "message", "Resource not found: " + ex.getResourcePath(),
                        "path", ex.getResourcePath(),
                        "timestamp", System.currentTimeMillis()
                ));
    }

    // Fallback for unhandled errors
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        System.err.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
        System.err.println("Unhandled exception: " + ex.getClass().getName() + " - " + ex.getMessage());
        ex.printStackTrace(); // Add stack trace for better debugging
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "status", 500,
                        "error", "Internal Server Error",
                        "message", "Unexpected error",
                        "timestamp", System.currentTimeMillis()
                ));
    }
}