package com.chillflix.indexer.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record VideoGameDTO(
    UUID id,

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    String title,

    @Min(value = 1950, message = "Year must be 1950 or later")
    @Max(value = 2100, message = "Year must be 2100 or earlier")
    Integer year,

    @Size(max = 255, message = "Developer must be 255 characters or less")
    String developer,

    @Size(max = 255, message = "Publisher must be 255 characters or less")
    String publisher,

    @NotNull(message = "Platform is required")
    List<@Size(max = 50, message = "Each platform must be 50 characters or less") String> platform,

    @NotBlank(message = "Magnet link is required")
    @Pattern(regexp = "^magnet:\\?xt=urn:[a-z0-9]+:[a-z0-9]{32,40}&dn=.+&tr=.+$", 
             message = "Invalid magnet link format")
    String magnet,

    @Size(max = 50, message = "Quality must be 50 characters or less")
    String quality,

    @Size(max = 20, message = "File type must be 20 characters or less")
    String fileType,

    @PositiveOrZero(message = "Size must be a positive number or zero")
    Long size,

    @Pattern(regexp = "^[a-fA-F0-9]{64}$", message = "Invalid SHA256 hash")
    String sha256Hash,

    @PositiveOrZero(message = "Seeds must be a positive number or zero")
    Integer seeds,

    @PositiveOrZero(message = "Peers must be a positive number or zero")
    Integer peers,

    @Size(max = 255, message = "Cover path must be 255 characters or less")
    String coverPath,

    String description,

    Map<String, Object> systemRequirements,

    List<@Size(max = 50, message = "Each genre must be 50 characters or less") String> genre,

    List<String> screenshotPaths,

    @Size(max = 10, message = "Rating must be 10 characters or less")
    String rating,

    LocalDateTime releaseDate,

    @Size(max = 255, message = "Torrent URL must be 255 characters or less")
    String torrentUrl,

    @Size(max = 10, message = "ESRB rating must be 10 characters or less")
    String esrbRating,

    Boolean multiplayer,

    Boolean isDeleted,

    LocalDateTime createdAt,

    LocalDateTime updatedAt
) {}