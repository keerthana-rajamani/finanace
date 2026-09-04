package com.examly.springapp.exception;

public class AccountSyncException extends RuntimeException {
    public AccountSyncException(String message) {
        super(message);
    }
}
