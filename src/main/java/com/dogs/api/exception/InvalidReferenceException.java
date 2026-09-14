package com.dogs.api.exception;

public class InvalidReferenceException extends RuntimeException {

    public InvalidReferenceException(String field, Object value) {
        super("%s %s does not exist or has been deleted".formatted(field, value));
    }
}
