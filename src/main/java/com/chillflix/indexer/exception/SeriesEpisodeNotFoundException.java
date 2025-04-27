package com.chillflix.indexer.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class SeriesEpisodeNotFoundException extends RuntimeException {
    public SeriesEpisodeNotFoundException(String message) {
        super(message);
    }
}