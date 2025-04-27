package com.chillflix.indexer.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.UUID;

public record MusicTrackDTO(
    UUID id,

    @NotNull(message = "Album ID is required")
    UUID albumId,

    @NotNull(message = "Track number is required")
    @PositiveOrZero(message = "Track number must be a positive number or zero")
    Integer trackNumber,

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    String title,

    @Size(max = 255, message = "Artist must be 255 characters or less")
    String artist,

    @PositiveOrZero(message = "Duration must be a positive number or zero")
    Integer duration,

    String filePath,

    @Size(max = 20, message = "File type must be 20 characters or less")
    String fileType,

    @Pattern(regexp = "^[a-fA-F0-9]{64}$", message = "Invalid SHA256 hash")
    String sha256Hash,

    LocalDateTime createdAt,

    LocalDateTime updatedAt
) {}