package com.chillflix.indexer.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SeriesDTO(
    UUID id,

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    String title,

    @NotNull(message = "Year is required")
    @Min(value = 1888, message = "Year must be 1888 or later")
    @Max(value = 2100, message = "Year must be 2100 or earlier")
    Integer year,

    @NotBlank(message = "Magnet link is required")
    @Pattern(regexp = "^magnet:\\?xt=urn:[a-z0-9]+:[a-z0-9]{32,40}&dn=.+&tr=.+$", 
             message = "Invalid magnet link format")
    String magnet,

    Integer tmdbId,

    @Pattern(regexp = "^tt\\d{7,8}$", message = "Invalid IMDB ID format")
    String imdbId,

    @Size(max = 50, message = "Language must be 50 characters or less")
    String language,

    @Size(max = 50, message = "Original language must be 50 characters or less")
    String originalLanguage,

    @Size(max = 20, message = "Quality must be 20 characters or less")
    String quality,

    @Size(max = 20, message = "File type must be 20 characters or less")
    String fileType,

    @Pattern(regexp = "^[a-fA-F0-9]{64}$", message = "Invalid SHA256 hash")
    String sha256Hash,

    Boolean isDeleted,

    LocalDateTime createdAt,

    LocalDateTime updatedAt,

    @Size(max = 1000, message = "Overview must be 1000 characters or less")
    String overview,

    @Size(max = 255, message = "Poster path must be 255 characters or less")
    String posterPath,

    List<@Size(max = 50, message = "Each genre must be 50 characters or less") String> genres,

    @Size(max = 255, message = "Torrent URL must be 255 characters or less")
    String torrentUrl,

    @Size(max = 255, message = "Trailer URL must be 255 characters or less")
    String trailerUrl,

    @PositiveOrZero(message = "Size must be a positive number or zero")
    Long size,

    @PositiveOrZero(message = "Seeds must be a positive number or zero")
    Integer seeds,

    @PositiveOrZero(message = "Peers must be a positive number or zero")
    Integer peers,

    // Series specific fields
    @PositiveOrZero(message = "Seasons must be a positive number or zero")
    Integer seasons,

    @PositiveOrZero(message = "Episodes must be a positive number or zero")
    Integer episodes,

    @Size(max = 100, message = "Network must be 100 characters or less")
    String network,

    @Size(max = 50, message = "Status must be 50 characters or less")
    String status,

    @PositiveOrZero(message = "Episode runtime must be a positive number or zero")
    Integer episodeRuntime
) {
    // Constructor temporal para mensajes de error
    public SeriesDTO(UUID id, String errorMessage, Integer ignored) {
        this(id, errorMessage, ignored, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
}