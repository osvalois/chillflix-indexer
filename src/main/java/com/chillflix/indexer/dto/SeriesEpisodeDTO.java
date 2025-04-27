package com.chillflix.indexer.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.UUID;

public record SeriesEpisodeDTO(
    UUID id,

    @NotNull(message = "Series ID is required")
    UUID seriesId,

    @NotNull(message = "Season number is required")
    @PositiveOrZero(message = "Season number must be a positive number or zero")
    Integer seasonNumber,

    @NotNull(message = "Episode number is required")
    @PositiveOrZero(message = "Episode number must be a positive number or zero")
    Integer episodeNumber,

    @Size(max = 255, message = "Title must be 255 characters or less")
    String title,

    String overview,

    LocalDateTime airDate,

    @PositiveOrZero(message = "Runtime must be a positive number or zero")
    Integer runtime,

    @Pattern(regexp = "^magnet:\\?xt=urn:[a-z0-9]+:[a-z0-9]{32,40}&dn=.+&tr=.+$", 
             message = "Invalid magnet link format")
    String magnet,

    @Size(max = 20, message = "Quality must be 20 characters or less")
    String quality,

    @PositiveOrZero(message = "Size must be a positive number or zero")
    Long size,

    @Size(max = 20, message = "File type must be 20 characters or less")
    String fileType,

    @Pattern(regexp = "^[a-fA-F0-9]{64}$", message = "Invalid SHA256 hash")
    String sha256Hash,

    LocalDateTime createdAt,

    LocalDateTime updatedAt
) {}