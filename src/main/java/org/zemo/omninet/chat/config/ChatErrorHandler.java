package org.zemo.omninet.chat.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.ConstraintViolationException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice(annotations = RestController.class)
@Slf4j
public class ChatErrorHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Invalid argument: {}", e.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("error", "Invalid input");
        error.put("message", e.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException e) {
        log.warn("Invalid state: {}", e.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("error", "Operation not allowed");
        error.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        log.warn("Validation error: {}", e.getMessage());
        Map<String, Object> error = new HashMap<>();
        error.put("error", "Validation failed");

        Map<String, String> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                    fieldError -> fieldError.getField(),
                    fieldError -> fieldError.getDefaultMessage(),
                    (existing, replacement) -> existing
                ));
        error.put("fieldErrors", fieldErrors);

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolation(ConstraintViolationException e) {
        log.warn("Constraint violation: {}", e.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("error", "Validation failed");
        error.put("message", e.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException e) {
        log.error("Runtime error: {}", e.getMessage(), e);
        Map<String, String> error = new HashMap<>();
        error.put("error", "Internal error");
        error.put("message", "An error occurred while processing your request");
        return ResponseEntity.internalServerError().body(error);
    }

    // WebSocket error handlers
    @MessageExceptionHandler(IllegalArgumentException.class)
    @SendToUser("/queue/errors")
    public Map<String, String> handleWebSocketIllegalArgument(IllegalArgumentException e) {
        log.warn("WebSocket invalid argument: {}", e.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("error", "Invalid input");
        error.put("message", e.getMessage());
        return error;
    }

    @MessageExceptionHandler(IllegalStateException.class)
    @SendToUser("/queue/errors")
    public Map<String, String> handleWebSocketIllegalState(IllegalStateException e) {
        log.warn("WebSocket invalid state: {}", e.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("error", "Operation not allowed");
        error.put("message", e.getMessage());
        return error;
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public Map<String, String> handleWebSocketGeneric(Exception e) {
        log.error("WebSocket error: {}", e.getMessage(), e);
        Map<String, String> error = new HashMap<>();
        error.put("error", "Internal error");
        error.put("message", "An error occurred while processing your request");
        return error;
    }
}
