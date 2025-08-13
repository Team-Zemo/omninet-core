package org.zemo.omninet.notes.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class ValidationException extends RuntimeException {

    private final Map<String, Object> error;

    public ValidationException(Map<String, Object> error) {
        super("Validation failed");
        this.error = error;
    }


}
