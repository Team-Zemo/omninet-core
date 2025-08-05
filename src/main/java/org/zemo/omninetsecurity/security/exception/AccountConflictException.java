package org.zemo.omninetsecurity.security.exception;

import lombok.Getter;

@Getter
public class AccountConflictException extends RuntimeException {
    private final String email;
    private final String existingProvider;
    private final String newProvider;

    public AccountConflictException(String email, String existingProvider, String newProvider) {
        super(String.format("Account conflict detected for email %s between providers %s and %s",
                email, existingProvider, newProvider));
        this.email = email;
        this.existingProvider = existingProvider;
        this.newProvider = newProvider;
    }
}
