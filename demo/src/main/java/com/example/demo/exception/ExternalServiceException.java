package com.example.demo.exception;

import org.springframework.http.HttpStatus;

public class ExternalServiceException extends AppException {
    public ExternalServiceException(String message) {
        super(message, HttpStatus.BAD_GATEWAY, "EXTERNAL_SERVICE_ERROR");
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, HttpStatus.BAD_GATEWAY, "EXTERNAL_SERVICE_ERROR", cause);
    }
}