package com.chillflix.indexer.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Table("series_episodes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeriesEpisode {

    @Id
    @Column("id")
    private UUID id;

    @Column("series_id")
    @NotNull(message = "Series ID is required")
    private UUID seriesId;

    @Column("season_number")
    @NotNull(message = "Season number is required")
    private Integer seasonNumber;

    @Column("episode_number")
    @NotNull(message = "Episode number is required")
    private Integer episodeNumber;

    @Size(max = 255, message = "Title must be 255 characters or less")
    private String title;

    private String overview;

    @Column("air_date")
    private LocalDateTime airDate;

    private Integer runtime;

    private String magnet;

    @Size(max = 20, message = "Quality must be 20 characters or less")
    private String quality;

    private Long size;

    @Column("file_type")
    @Size(max = 20, message = "File type must be 20 characters or less")
    private String fileType;

    @Column("sha256_hash")
    @Size(min = 64, max = 64, message = "SHA256 hash must be exactly 64 characters")
    private String sha256Hash;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;
}