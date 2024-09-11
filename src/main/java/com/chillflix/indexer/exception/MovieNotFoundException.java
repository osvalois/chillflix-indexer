package com.chillflix.indexer.exception;

import java.util.UUID;

public class MovieNotFoundException extends RuntimeException {

    private final UUID movieId;

    public MovieNotFoundException(String message) {
        super(message);
        this.movieId = null;
    }

    public MovieNotFoundException(String message, UUID movieId) {
        super(message);
        this.movieId = movieId;
    }

    public MovieNotFoundException(String message, UUID movieId, Throwable cause) {
        super(message, cause);
        this.movieId = movieId;
    }

    public UUID getMovieId() {
        return movieId;
    }
}