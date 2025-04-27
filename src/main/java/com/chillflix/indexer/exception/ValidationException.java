package com.chillflix.indexer.exception;

import org.springframework.validation.ObjectError;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ValidationException extends RuntimeException {

    private final List<ObjectError> errors;

    public ValidationException(String message) {
        super(message);
        this.errors = new ArrayList<>();
    }

    public ValidationException(List<ObjectError> errors) {
        super("Validation failed. " + formatErrors(errors));
        this.errors = errors;
    }

    public ValidationException(String message, List<ObjectError> errors) {
        super(message + " " + formatErrors(errors));
        this.errors = errors;
    }

    public List<ObjectError> getErrors() {
        return errors;
    }

    public List<String> getErrorMessages() {
        return errors.stream()
                .map(ObjectError::getDefaultMessage)
                .collect(Collectors.toList());
    }

    private static String formatErrors(List<ObjectError> errors) {
        return errors.stream()
                .map(error -> error.getObjectName() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
    }

    @Override
    public String toString() {
        return "ValidationException: " + getMessage();
    }
}