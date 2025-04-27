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
import java.util.List;

@Table("series")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Series {

    @Id
    @Column("id")
    private UUID id;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must be 255 characters or less")
    private String title;

    @NotNull(message = "Year is required")
    private Integer year;

    @NotBlank(message = "Magnet link is required")
    @Column("magnet")
    private String magnet;

    @Column("tmdb_id")
    private Integer tmdbId;

    @Column("imdb_id")
    @Size(max = 20, message = "IMDB ID must be 20 characters or less")
    private String imdbId;

    @Size(max = 50, message = "Language must be 50 characters or less")
    private String language;

    @Column("original_language")
    @Size(max = 50, message = "Original language must be 50 characters or less")
    private String originalLanguage;

    @Size(max = 20, message = "Quality must be 20 characters or less")
    private String quality;

    @Column("file_type")
    @Size(max = 20, message = "File type must be 20 characters or less")
    private String fileType;

    @Column("sha256_hash")
    @Size(min = 64, max = 64, message = "SHA256 hash must be exactly 64 characters")
    private String sha256Hash;

    @Column("is_deleted")
    private Boolean isDeleted;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Column("search_vector")
    private Object searchVector;

    @Column("size")
    private Long size;

    @Column("seeds")
    private Integer seeds;

    @Column("peers")
    private Integer peers;

    @Column("overview")
    private String overview;

    @Column("poster_path")
    private String posterPath;

    @Column("genres")
    private List<String> genres;

    @Column("torrent_url")
    private String torrentUrl;

    @Column("trailer_url")
    private String trailerUrl;

    // Series specific fields
    @Column("seasons")
    private Integer seasons;

    @Column("episodes")
    private Integer episodes;

    @Column("network")
    private String network;

    @Column("status")
    private String status;

    @Column("episode_runtime")
    private Integer episodeRuntime;
}