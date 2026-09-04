package com.examly.springapp.exception;

public class BudgetValidationException extends RuntimeException {
    public BudgetValidationException(String message) {
        super(message);
    }
}
