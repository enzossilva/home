package com.example.demo.exception;

import org.springframework.http.HttpStatus;

/**
 * Exceção base para a aplicação.
 * Todas as exceções customizadas devem herdar desta classe.
 */
public class AppException extends RuntimeException {
    private final HttpStatus httpStatus;
    private final String errorCode;

    public AppException(String message, HttpStatus httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public AppException(String message, HttpStatus httpStatus, String errorCode, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

