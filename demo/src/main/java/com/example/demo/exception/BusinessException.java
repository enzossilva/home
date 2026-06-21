package com.example.demo.exception;

import org.springframework.http.HttpStatus;

/**
 * Exceção para violações de regras de negócio.
 */
public class BusinessException extends AppException {
    public BusinessException(String message) {
        super(message, HttpStatus.CONFLICT, "BUSINESS_RULE_VIOLATION");
    }

    public BusinessException(String message, Throwable cause) {
        super(message, HttpStatus.CONFLICT, "BUSINESS_RULE_VIOLATION", cause);
    }
}

