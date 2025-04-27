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

@Table("music_tracks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MusicTrack {

    @Id
    @Column("id")
    private UUID id;

    @Column("album_id")
    @NotNull(message = "Album ID is required")
    private UUID albumId;

    @Column("track_number")
    @NotNull(message = "Track number is required")
    private Integer trackNumber;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must be 255 characters or less")
    private String title;

    @Size(max = 255, message = "Artist must be 255 characters or less")
    private String artist;

    private Integer duration;

    @Column("file_path")
    private String filePath;

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