package com.example.demo.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends AppException {
    public ResourceNotFoundException(String resource, Long id) {
        super(
            String.format("%s não encontrado (id=%d)", resource, id),
            HttpStatus.NOT_FOUND,
            "RESOURCE_NOT_FOUND"
        );
    }

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }
}

