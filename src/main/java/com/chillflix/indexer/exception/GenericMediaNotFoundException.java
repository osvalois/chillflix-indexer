package com.chillflix.indexer.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * General exception for all media types not found cases
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class GenericMediaNotFoundException extends RuntimeException {
    public GenericMediaNotFoundException(String message) {
        super(message);
    }
}