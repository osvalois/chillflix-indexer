package com.chillflix.indexer.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record MovieDTO(
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
    String sha256Hash
) {}