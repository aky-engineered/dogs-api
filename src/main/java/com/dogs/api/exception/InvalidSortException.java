package com.dogs.api.exception;

import java.util.Collection;

public class InvalidSortException extends RuntimeException {

    public InvalidSortException(String property, Collection<String> allowed) {
        super("Cannot sort by '%s'. Allowed fields: %s".formatted(property, String.join(", ", allowed)));
    }
}
